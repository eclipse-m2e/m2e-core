/*******************************************************************************
 * Copyright (c) 2011 Knowledge Computing Corp. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Karl M. Davis (Knowledge Computing Corp.) - initial API and implementation
 *    Red Hat, Inc - refactoring and abstraction of the logic
 *******************************************************************************/

package org.jboss.tools.maven.apt.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.apt.core.util.AptConfig;
import org.eclipse.jdt.apt.core.util.IFactoryPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
import org.eclipse.m2e.jdt.IJavaProjectConfigurator;
import org.jboss.tools.maven.apt.internal.AnnotationServiceLocator.ServiceEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * This {@link AbstractProjectConfigurator} implementation will set the APT configuration for an Eclipse Java project.
 * </p>
 * <p>
 * Please note that the <code>maven-compiler-plugin</code> (at least as of version 2.3.2) will automatically perform
 * annotation processing and generate annotation sources. This processing will include all annotation processors in the
 * project's compilation classpath.
 * </p>
 * <p>
 * However, there are a couple of problems that prevent the <code>maven-compiler-plugin</code>'s annotation processing
 * from being sufficient when run within m2eclipse:
 * </p>
 * <ul>
 * <li>The generated annotation sources are not added to the Maven project's source folders (nor should they be) and are
 * thus not found by m2eclipse.</li>
 * <li>Due to contention between Eclipse's JDT compilation and <code>maven-compiler-plugin</code> compilation, the Java
 * compiler used by Eclipse may not recognize when the generated annotation sources/classes are out of date.</li>
 * </ul>
 * <p>
 * The {@link AbstractAptProjectConfigurator} works around those limitations by configuring Eclipse's built-in annotation
 * processing: APT. Unfortunately, the APT configuration will not allow for libraries, such as m2eclipse's
 * "Maven Dependencies" to be used in the search path for annotation processors. Instead, the
 * {@link AbstractAptProjectConfigurator} adds all of the project's <code>.jar</code> dependencies to the annotation processor
 * search path.
 * </p>
 */
abstract class AbstractAptProjectConfigurator extends AbstractProjectConfigurator implements IJavaProjectConfigurator {
  
  
  private static final String M2_REPO = "M2_REPO";

  private static final Logger LOG = LoggerFactory.getLogger(AbstractAptProjectConfigurator.class);

  protected abstract AnnotationProcessorConfiguration getAnnotationProcessorConfiguration(IMavenProjectFacade mavenProjectFacade, MavenSession mavenSession, IProgressMonitor monitor) throws CoreException;

  /**
   * {@inheritDoc}
   */
  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    // This method may be called with null parameters to ensure its API is correct. We
    // can ignore such calls.
    if(request == null || monitor == null)
      return;

    // Get the objects needed for APT configuration
    IMavenProjectFacade mavenProjectFacade = request.getMavenProjectFacade();
    IProject eclipseProject = mavenProjectFacade.getProject();
    MavenSession mavenSession = request.getMavenSession();
    
    if (ignoreConfigurator(mavenProjectFacade, monitor)) {
      return;
    }
    
    AnnotationProcessorConfiguration configuration = getAnnotationProcessorConfiguration(mavenProjectFacade, mavenSession, monitor);
    // Configure APT
    configureAptForProject(eclipseProject, mavenProjectFacade, configuration, monitor);
  }

  protected boolean ignoreConfigurator(IMavenProjectFacade mavenProjectFacade, IProgressMonitor monitor) throws CoreException {
    return false;
  }

  /**
   * Configures APT for the specified Maven project.
   * 
   * @param eclipseProject an {@link IProject} reference to the Eclipse project being configured
   * @param mavenProject {@link IMavenProjectFacade} reference to the Maven project being configured
   * @param monitor the {@link IProgressMonitor} to use
   * @param mavenProjectFacade 
   * @throws CoreException Any {@link CoreException}s thrown will be passed through.
   */
  private void configureAptForProject(IProject eclipseProject, IMavenProjectFacade mavenProjectFacade, AnnotationProcessorConfiguration configuration, IProgressMonitor monitor) throws CoreException {

    // In case the Javaconfigurator was not called yet (eg. maven-processor-plugin being bound to process-sources, 
    // that project configurator runs first) We need to add the Java Nature before setting the APT config.
    if(!eclipseProject.hasNature(JavaCore.NATURE_ID)) {
      addNature(eclipseProject, JavaCore.NATURE_ID, monitor);
    }
    
    File generatedSourcesDirectory = configuration.getOutputDirectory();

    // If this project has no valid generatedSourcesDirectory, we have nothing to do
    if(generatedSourcesDirectory == null)
      return;

    IJavaProject javaProject = JavaCore.create(eclipseProject);

    //The plugin dependencies are added first to the classpath
    LinkedHashSet<File> resolvedJarArtifacts = new LinkedHashSet<File>(configuration.getDependencies());
    // Get the project's dependencies
    List<Artifact> artifacts = getProjectArtifacts(mavenProjectFacade);
    resolvedJarArtifacts.addAll(filterToResolvedJars(artifacts));
    
    // Inspect the dependencies to see if any contain APT processors
    boolean isAnnotationProcessingEnabled = configuration.isAnnotationProcessingEnabled()
                                            && containsAptProcessors(resolvedJarArtifacts); 
    
    // Enable/Disable APT (depends on whether APT processors were found)
    AptConfig.setEnabled(javaProject, isAnnotationProcessingEnabled);
    
    //If no annotation processor is disabled, we can leave.
    if (!isAnnotationProcessingEnabled) {
      return;
    }

    // Configure APT output path
    File generatedSourcesRelativeDirectory = convertToProjectRelativePath(eclipseProject, generatedSourcesDirectory);
    String generatedSourcesRelativeDirectoryPath = generatedSourcesRelativeDirectory.getPath();
    
    //createFolder(eclipseProject.getFolder(generatedSourcesRelativeDirectoryPath), monitor);
    AptConfig.setGenSrcDir(javaProject, generatedSourcesRelativeDirectoryPath);

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
    List<File> resolvedJarArtifactsInReverseOrder = new ArrayList<File>(resolvedJarArtifacts);
    Collections.reverse(resolvedJarArtifactsInReverseOrder);
    IFactoryPath factoryPath = AptConfig.getDefaultFactoryPath(javaProject);
    
    IPath m2RepoPath = JavaCore.getClasspathVariable(M2_REPO);
    
    for(File resolvedJarArtifact : resolvedJarArtifactsInReverseOrder) {
      IPath absolutePath = new Path(resolvedJarArtifact.getAbsolutePath());
      //reference jars in a portable way
      if (m2RepoPath != null && m2RepoPath.isPrefixOf(absolutePath)) {
        IPath relativePath = absolutePath.removeFirstSegments(m2RepoPath.segmentCount()).makeRelative().setDevice(null);
        IPath variablePath = new Path(M2_REPO).append(relativePath);
        factoryPath.addVarJar(variablePath);
      } else {
        //fall back on using absolute references.
        factoryPath.addExternalJar(resolvedJarArtifact);
      }
    }

    Map<String, String> currentOptions = AptConfig.getProcessorOptions(javaProject);
    Map<String, String> newOptions = configuration.getAnnotationProcessorOptions();
    if (!currentOptions.equals(newOptions)) {
      AptConfig.setProcessorOptions(newOptions, javaProject);
    }
    
    // Apply that IFactoryPath to the project
    AptConfig.setFactoryPath(javaProject, factoryPath);
  }


  /**
   * @param mavenProjectFacade the {@link IMavenProjectFacade} to get the {@link Artifact}s for
   * @return an ordered {@link List} of the specified {@link IMavenProjectFacade}'s {@link Artifact}s
   */
  private static List<Artifact> getProjectArtifacts(IMavenProjectFacade mavenProjectFacade) {
    /*
     * This method essentially wraps org.apache.maven.project.MavenProject.getArtifacts(), 
     * returning a List instead of a Set to indicate that ordering is maintained and important.
     * The set being "wrapped" is actually a LinkedHashSet, which does guarantee a consistent 
     * insertion & iteration order.
     */

    Set<Artifact> unorderedArtifacts = mavenProjectFacade.getMavenProject().getArtifacts();
    List<Artifact> orderedArtifacts = new ArrayList<Artifact>(unorderedArtifacts.size());
    for(Artifact artifact : unorderedArtifacts)
      orderedArtifacts.add(artifact);
    return orderedArtifacts;
  }


  /**
   * <p>
   * Filters the specified {@link Artifact}s to those that match the following criteria:
   * </p>
   * <ul>
   * <li>{@link Artifact#isResolved()} is <code>true</code></li>
   * <li>{@link Artifact#getArtifactHandler().getExtension()} equals "jar"</li>
   * <li>{@link Artifact#getScope()} equals {@link Artifact#SCOPE_COMPILE}</li>
   * <li>{@link Artifact#getFile()} returns a {@link File} where {@link File#isFile()} is <code>true</code></li>
   * </ul>
   * 
   * @param artifacts the {@link Set} of {@link Artifact}s to filter
   * @return the actual JAR {@link File}s available from the specified {@link Artifact}s
   */
  private static List<File> filterToResolvedJars(List<Artifact> artifacts) {
    List<File> resolvedJarArtifacts = new ArrayList<File>();
    ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_COMPILE_PLUS_RUNTIME);

    for(Artifact artifact : artifacts) {
      // Ensure that this Artifact should be included
      if(!artifact.isResolved())
        continue;
      if(artifact.getArtifactHandler() == null 
          || !"jar".equalsIgnoreCase(artifact.getArtifactHandler().getExtension()))
        continue;
      if(!filter.include(artifact))
        continue;

      // Ensure that the Artifact resolves to a File that we can use
      File artifactJarFile = artifact.getFile();
      if(!artifactJarFile.isFile())
        continue;

      resolvedJarArtifacts.add(artifactJarFile);
    }

    return resolvedJarArtifacts;
  }

  /**
   * Returns <code>true</code> if any of the specified JARs contain a Java 5 or Java 6 annotation processor,
   * <code>false</code> if none of them do.
   * 
   * @param resolvedJarArtifacts the JAR {@link File}s to inspect for annotation processors
   * @return <code>true</code> if any of the specified JARs contain a Java 5 or Java 6 annotation processor,
   *         <code>false</code> if none of them do
   */
  private static boolean containsAptProcessors(Collection<File> resolvedJarArtifacts) {
    // Read through all JARs, checking for any APT service entries
    try {
      for(File resolvedJarArtifact : resolvedJarArtifacts) {
        Set<ServiceEntry> aptServiceEntries = AnnotationServiceLocator.getAptServiceEntries(resolvedJarArtifact);
        if(!aptServiceEntries.isEmpty())
          return true;
      }
    } catch(IOException e) {
      MavenJdtAptPlugin.createErrorStatus(e, "Error while reading artifact JARs.");
    }

    // No service entries were found
    return false;
  }

  /**
   * Converts the specified relative or absolute {@link File} to a {@link File} that is relative to the base directory
   * of the specified {@link IProject}.
   * 
   * @param project the {@link IProject} whose base directory the returned {@link File} should be relative to
   * @param fileToConvert the relative or absolute {@link File} to convert
   * @return a {@link File} that is relative to the base directory of the specified {@link IProject}
   */
  protected File convertToProjectRelativePath(IProject project, File fileToConvert) {
    // Get an absolute version of the specified file
    File absoluteFile = fileToConvert.getAbsoluteFile();
    String absoluteFilePath = absoluteFile.getAbsolutePath();

    // Get a File for the absolute path to the project's directory
    File projectBasedirFile = project.getLocation().toFile().getAbsoluteFile();
    String projectBasedirFilePath = projectBasedirFile.getAbsolutePath();

    // Compute the relative path
    if(absoluteFile.equals(projectBasedirFile)) {
      return new File(".");
    } else if(absoluteFilePath.startsWith(projectBasedirFilePath)) {
      String projectRelativePath = absoluteFilePath.substring(projectBasedirFilePath.length() + 1);
      return new File(projectRelativePath);
    } else {
      return absoluteFile;
    }
  }

  /**
   * {@inheritDoc}
   */
  public void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor) {
    /*
     * Implementations of this method are supposed to configure the Maven project
     * classpath: the "Maven Dependencies" container. We don't have any need to do
     * that here.
     */
  }

  /**
   * {@inheritDoc}
   */
  public void configureRawClasspath(ProjectConfigurationRequest request, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {
    /*
     * We need to prevent/recover from the JavaProjectConfigurator removing the
     * generated annotation sources directory from the classpath: it will be added
     * when we configure the Eclipse APT preferences and then removed when the
     * JavaProjectConfigurator runs.
     */

    // Get the various project references we'll need
    IProject eclipseProject = request.getProject();
    MavenProject mavenProject = request.getMavenProject();
    IMavenProjectFacade projectFacade = request.getMavenProjectFacade();
    MavenSession mavenSession = request.getMavenSession();

    // If this isn't a Java project, we have nothing to do
    if(!eclipseProject.hasNature(JavaCore.NATURE_ID))
      return;

    if (ignoreConfigurator(projectFacade, monitor)) {
      return;
    }
    
    //If APT is not enabled, nothing to do either
    IJavaProject javaProject = JavaCore.create(eclipseProject);
    if (!AptConfig.isEnabled(javaProject)) {
      return;
    }
    
    AnnotationProcessorConfiguration configuration = getAnnotationProcessorConfiguration(projectFacade, mavenSession, monitor);
    if (!configuration.isAnnotationProcessingEnabled()) {
      return;
    }
    
    // If this project has no valid compiler plugin config, we have nothing to do
    File generatedSourcesDirectory = configuration.getOutputDirectory();
    if(generatedSourcesDirectory == null)
      return;

    // Get the generated annotation sources directory as an IFolder
    File generatedSourcesRelativeDirectory = convertToProjectRelativePath(eclipseProject, generatedSourcesDirectory);
    String generatedSourcesRelativeDirectoryPath = generatedSourcesRelativeDirectory.getPath();
    IFolder generatedSourcesFolder = eclipseProject.getFolder(generatedSourcesRelativeDirectoryPath);

    // Get the output folder to use as an IPath
    File outputFile = new File(mavenProject.getBuild().getOutputDirectory());
    File outputRelativeFile = convertToProjectRelativePath(eclipseProject, outputFile);
    IFolder outputFolder = eclipseProject.getFolder(outputRelativeFile.getPath());
    IPath outputPath = outputFolder.getFullPath();

    // Create the includes & excludes specifiers
    IPath[] includes = new IPath[] {};
    IPath[] excludes = new IPath[] {};

    // If the source folder exists and is non-nested, add it
    if(generatedSourcesFolder != null && generatedSourcesFolder.exists()
        && generatedSourcesFolder.getProject().equals(eclipseProject)) {
      IClasspathEntryDescriptor cped = getEnclosingEntryDescriptor(classpath, generatedSourcesFolder.getFullPath());
      if(cped == null) {
        classpath.addSourceEntry(generatedSourcesFolder.getFullPath(), outputPath, includes, excludes, false);
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

  private void createFolder(final IFolder folder, IProgressMonitor monitor)
      throws CoreException {
    if (!folder.exists()) {
      if (folder.getParent() instanceof IFolder) {
        createFolder((IFolder) folder.getParent(), monitor);
      }
      folder.create(true /* force */, true /* local */, monitor);
    } else {
      IContainer x = folder;
      while (x instanceof IFolder && x.isDerived()) {
        x.setDerived(false, monitor);
        x = x.getParent();
      }
    }
  }
  
}
