/*******************************************************************************
 * Copyright (c) 2012-2022 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.apt.internal;

import static org.eclipse.m2e.apt.internal.utils.ProjectUtils.containsAptProcessors;
import static org.eclipse.m2e.apt.internal.utils.ProjectUtils.convertToProjectRelativePath;
import static org.eclipse.m2e.apt.internal.utils.ProjectUtils.filterToResolvedJars;
import static org.eclipse.m2e.apt.internal.utils.ProjectUtils.getProjectArtifacts;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.apt.core.internal.util.FactoryContainer;
import org.eclipse.jdt.apt.core.internal.util.FactoryContainer.FactoryType;
import org.eclipse.jdt.apt.core.internal.util.FactoryPath;
import org.eclipse.jdt.apt.core.util.AptConfig;
import org.eclipse.jdt.apt.core.util.IFactoryPath;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.apt.internal.utils.ProjectUtils;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;


/**
 * AbstractAptConfiguratorDelegate
 *
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
public abstract class AbstractAptConfiguratorDelegate implements AptConfiguratorDelegate {

  private static final String M2E_APT_KEY = "m2e-apt";

  private static final String M2_REPO = "M2_REPO";

  private static final Logger LOG = LoggerFactory.getLogger(AbstractAptConfiguratorDelegate.class);

  protected IMavenProjectFacade mavenFacade;

  protected MavenSession mavenSession;

  @Override
  public void setSession(MavenSession mavenSession) {
    this.mavenSession = mavenSession;
  }

  @Override
  public void setFacade(IMavenProjectFacade mavenProjectFacade) {
    this.mavenFacade = mavenProjectFacade;
  }

  @Override
  public boolean isIgnored(IProgressMonitor monitor) {
    return false;
  }

  @Override
  public AbstractBuildParticipant getMojoExecutionBuildParticipant(MojoExecution execution) {
    return null;
  }

  /**
   * Configures APT for the specified Maven project.
   */
  @Override
  public void configureProject(IProgressMonitor monitor) throws CoreException {

    IProject eclipseProject = mavenFacade.getProject();

    AnnotationProcessorConfiguration configuration = getAnnotationProcessorConfiguration(monitor);
    if(configuration == null) {
      return;
    }

    // In case the Javaconfigurator was not called yet (eg. maven-processor-plugin being bound to process-sources, 
    // that project configurator runs first) We need to add the Java Nature before setting the APT config.
    if(!eclipseProject.hasNature(JavaCore.NATURE_ID)) {
      AbstractProjectConfigurator.addNature(eclipseProject, JavaCore.NATURE_ID, monitor);
    }

    File generatedSourcesDirectory = configuration.getOutputDirectory();
    File generatedTestSourcesDirectory = configuration.getTestOutputDirectory();

    // If this project has no valid generatedSourcesDirectory, we have nothing to do
    if(generatedSourcesDirectory == null && generatedTestSourcesDirectory == null) {
      return;
    }

    IJavaProject javaProject = JavaCore.create(eclipseProject);

    //The plugin dependencies are added first to the classpath
    LinkedHashSet<File> resolvedJarArtifacts = new LinkedHashSet<>(getJars(configuration.getDependencies()));

    // Get the project's dependencies
    if(configuration.isAddProjectDependencies()) {
      List<Artifact> artifacts = getProjectArtifacts(mavenFacade);
      resolvedJarArtifacts.addAll(filterToResolvedJars(artifacts));
    }

    // Inspect the dependencies to see if any contain APT processors
    boolean isAnnotationProcessingEnabled = configuration.isAnnotationProcessingEnabled()
        && containsAptProcessors(resolvedJarArtifacts);
    //first set the output path to not create default folders for no use... 
    if(isAnnotationProcessingEnabled) {
      //due to https://github.com/eclipse-jdt/eclipse.jdt.core/issues/689 setting the folder preferences can hang forever if APT is enabled, so disable it temporarily
      AptConfig.setEnabled(javaProject, false);
      if(generatedSourcesDirectory != null) {
        // Configure APT output path
        AptConfig.setGenSrcDir(javaProject, getOutputPath(eclipseProject, generatedSourcesDirectory));
      }
      if(generatedTestSourcesDirectory != null) {
        AptConfig.setGenTestSrcDir(javaProject, getOutputPath(eclipseProject, generatedTestSourcesDirectory));
      }
    }
    // Enable/Disable APT (depends on whether APT processors were found)
    AptConfig.setEnabled(javaProject, isAnnotationProcessingEnabled);

    //If no annotation processor is disabled, we can leave.
    if(!isAnnotationProcessingEnabled) {
      return;
    }
    LOG.debug("Enabling APT support on {}", eclipseProject.getName());

    /* 
     * Add all of the compile-scoped JAR artifacts to a new IFactoryPath (in 
     * addition to the workspace's default entries).
     * 
     * Please note that--until JDT-APT supports project factory path entries 
     * (as opposed to just JARs)--this will be a bit wonky. Specifically, any
     * project dependencies will be excluded, but their transitive JAR
     * dependencies will be included.
     * 
     * Also note: we add the artifacts in reverse order as 
     * IFactoryPath.addExternalJar(File) adds items to the top of the factory 
     * list.
     */
    List<File> resolvedJarArtifactsInReverseOrder = new ArrayList<>(resolvedJarArtifacts);
    Collections.reverse(resolvedJarArtifactsInReverseOrder);
    IFactoryPath factoryPath = AptConfig.getDefaultFactoryPath(javaProject);

    //Quick fix for https://github.com/jbosstools/m2e-apt/issues/65: Disable all plugins APs.
    //Since they're unknown to Maven, they represent a configuration mismatch between Maven and Eclipse
    for(FactoryContainer fc : ((FactoryPath) factoryPath).getEnabledContainers().keySet()) {
      if(FactoryType.PLUGIN.equals(fc.getType())) {
        factoryPath.disablePlugin(fc.getId());
      }
    }

    IPath m2RepoPath = JavaCore.getClasspathVariable(M2_REPO);

    for(File resolvedJarArtifact : resolvedJarArtifactsInReverseOrder) {
      IPath absolutePath = IPath.fromOSString(resolvedJarArtifact.getAbsolutePath());
      //reference jars in a portable way
      if((m2RepoPath != null) && m2RepoPath.isPrefixOf(absolutePath)) {
        IPath relativePath = absolutePath.removeFirstSegments(m2RepoPath.segmentCount()).makeRelative().setDevice(null);
        IPath variablePath = IPath.fromOSString(M2_REPO).append(relativePath);
        factoryPath.addVarJar(variablePath);
      } else {
        //fall back on using absolute references.
        factoryPath.addExternalJar(resolvedJarArtifact);
      }
    }

    Map<String, String> currentOptions = AptConfig.getRawProcessorOptions(javaProject);
    Map<String, String> newOptions = configuration.getAnnotationProcessorOptions();
    if(!currentOptions.equals(newOptions)) {
      AptConfig.setProcessorOptions(newOptions, javaProject);
    }

    // Apply that IFactoryPath to the project
    AptConfig.setFactoryPath(javaProject, factoryPath);
  }

  private String getOutputPath(IProject eclipseProject, File dir) {
    File generatedSourcesRelativeDirectory = convertToProjectRelativePath(eclipseProject, dir);
    String generatedSourcesRelativeDirectoryPath = generatedSourcesRelativeDirectory.getPath();

    if (File.separatorChar != '/') {
      generatedSourcesRelativeDirectoryPath = generatedSourcesRelativeDirectoryPath.replace(File.separatorChar, '/');
    }
    return generatedSourcesRelativeDirectoryPath;
  }

  private List<File> getJars(List<File> files) {
    if(files == null || files.isEmpty()) {
      return Collections.emptyList();
    }
    List<File> jars = new ArrayList<>(files);
    Iterator<File> ite = jars.iterator();
    while(ite.hasNext()) {
      File jar = ite.next();
      if(!ProjectUtils.isJar(jar)) {
        ite.remove();
      }
    }
    return jars;
  }

  @Override
  public void configureClasspath(IClasspathDescriptor classpath, IProgressMonitor monitor) throws CoreException {

    AnnotationProcessorConfiguration configuration = getAnnotationProcessorConfiguration(monitor);

    if((configuration == null) || !configuration.isAnnotationProcessingEnabled()) {
      return;
    }

    //Add generated source directory to classpath
    File generatedSourcesDirectory = configuration.getOutputDirectory();
    MavenProject mavenProject = mavenFacade.getMavenProject();
    IProject eclipseProject = mavenFacade.getProject();

    if(generatedSourcesDirectory != null) {
      addToClassPath(eclipseProject, generatedSourcesDirectory, null /* targetdirectory */, classpath, false);
    }

    //Add generated test source directory to classpath
    File generatedTestSourcesDirectory = configuration.getTestOutputDirectory();
    if(generatedTestSourcesDirectory != null) {
      File outputFolder = new File(mavenProject.getBuild().getTestOutputDirectory());
      addToClassPath(eclipseProject, generatedTestSourcesDirectory, outputFolder, classpath, true);
    }
  }

  protected abstract AnnotationProcessorConfiguration getAnnotationProcessorConfiguration(IProgressMonitor monitor)
      throws CoreException;

  private void addToClassPath(IProject project, File sourceDirectory, File targetDirectory,
      IClasspathDescriptor classpath, boolean isTest) {
    // Get the generated annotation sources directory as an IFolder
    File generatedSourcesRelativeDirectory = convertToProjectRelativePath(project, sourceDirectory);
    String generatedSourcesRelativeDirectoryPath = generatedSourcesRelativeDirectory.getPath();
    IFolder generatedSourcesFolder = project.getFolder(generatedSourcesRelativeDirectoryPath);

    // Get the output folder to use as an IPath
    IPath outputPath = null;
    if(targetDirectory != null) {
      File outputRelativeFile = convertToProjectRelativePath(project, targetDirectory);
      IFolder outputFolder = project.getFolder(outputRelativeFile.getPath());
      outputPath = outputFolder.getFullPath();
    }

    // Create the includes & excludes specifiers
    IPath[] includes = new IPath[] {};
    IPath[] excludes = new IPath[] {};

    // If the source folder is non-nested, add it
    if((generatedSourcesFolder != null) && generatedSourcesFolder.getProject().equals(project)) {
      IClasspathEntryDescriptor enclosing = getEnclosingEntryDescriptor(classpath,
          generatedSourcesFolder.getFullPath());
      if((enclosing == null) || (getEntryDescriptor(classpath, generatedSourcesFolder.getFullPath()) != null)) {
        IClasspathEntryDescriptor entry = classpath.addSourceEntry(generatedSourcesFolder.getFullPath(), outputPath,
            includes, excludes, true);
        entry.setClasspathAttribute(IClasspathAttribute.IGNORE_OPTIONAL_PROBLEMS, "true"); //$NON-NLS-1$
        entry.setClasspathAttribute(M2E_APT_KEY, "true"); //$NON-NLS-1$
        if(isTest) {
          for(IClasspathEntry classpathEntry : classpath.getEntries()) {
            // test source folder should be found with test attribute, unless the property m2e.disableTestClasspathFlag is in effect.
            // (don't directly check for the property because only newer m2e versions handle it) 
            for(IClasspathAttribute attribute : classpathEntry.getExtraAttributes()) {
              if("test".equals(attribute.getName()) && "true".equals(attribute.getValue())) { //$NON-NLS-1$ //$NON-NLS-2$
                entry.setClasspathAttribute("test", "true"); //$NON-NLS-1$ //$NON-NLS-2$
                break;
              }
            }
          }
        }
      }
    } else {
      if(generatedSourcesFolder != null) {
        classpath.removeEntry(generatedSourcesFolder.getFullPath());
      }
    }

  }

  /**
   * Returns the {@link IClasspathEntryDescriptor} in the specified {@link IClasspathDescriptor} that is a prefix of the
   * specified {@link IPath}.
   *
   * @param classpath the {@link IClasspathDescriptor} to be searched for a matching {@link IClasspathEntryDescriptor}
   * @param path the {@link IPath} to find a matching {@link IClasspathEntryDescriptor} for
   * @return the {@link IClasspathEntryDescriptor} in the specified {@link IClasspathDescriptor} that is a prefix of the
   *         specified {@link IPath}
   */
  private static IClasspathEntryDescriptor getEnclosingEntryDescriptor(IClasspathDescriptor classpath, IPath path) {
    for(IClasspathEntryDescriptor cped : classpath.getEntryDescriptors()) {
      if(cped.getPath().isPrefixOf(path)) {
        return cped;
      }
    }
    return null;
  }

  private IClasspathEntryDescriptor getEntryDescriptor(IClasspathDescriptor classpath, IPath fullPath) {
    List<IPath> stalePaths = new ArrayList<>();
    IClasspathEntryDescriptor matchingDescriptor = null;
    for(IClasspathEntryDescriptor cped : classpath.getEntryDescriptors()) {
      if(cped.getPath().equals(fullPath)) {
        matchingDescriptor = cped;
      } else if(Boolean.parseBoolean(cped.getClasspathAttributes().get(M2E_APT_KEY))) {
        stalePaths.add(cped.getPath());
      }
    }
    for(IPath stalePath : stalePaths) {
      classpath.removeEntry(stalePath);
    }
    return matchingDescriptor;
  }

  protected <T> T getParameterValue(String parameter, Class<T> asType, MojoExecution mojoExecution)
      throws CoreException {
    PluginExecution execution = new PluginExecution();
    execution.setConfiguration(mojoExecution.getConfiguration());
    MavenProject mavenProject = mavenFacade.getMavenProject();
    return mavenFacade.createExecutionContext().execute(mavenProject, (context, monitor) -> {
      //TODO provide as part of the execution context? We then probably won't need the project parameter at all?
      return MavenPlugin.getMaven().getMojoParameterValue(mavenProject, parameter, asType, mojoExecution.getPlugin(),
          execution, mojoExecution.getGoal(), null);
    }, null);
  }

}
