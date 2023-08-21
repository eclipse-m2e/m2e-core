/*******************************************************************************
 * Copyright (c) 2008-2017 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.jdt.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.jdt.IClasspathManagerDelegate;
import org.eclipse.m2e.jdt.IJavaProjectConfigurator;


/**
 * DefaultClasspathManagerDelegate
 *
 * @author igor
 */
public class DefaultClasspathManagerDelegate implements IClasspathManagerDelegate {
  private final IProjectConfigurationManager configurationManager;

  private final IMavenProjectRegistry projectManager;

  public DefaultClasspathManagerDelegate() {
    this.configurationManager = MavenPlugin.getProjectConfigurationManager();
    this.projectManager = MavenPlugin.getMavenProjectRegistry();
  }

  @Override
  public void populateClasspath(final IClasspathDescriptor classpath, IMavenProjectFacade projectFacade, final int kind,
      final IProgressMonitor monitor) throws CoreException {

    addClasspathEntries(classpath, projectFacade, kind, monitor);

    for(IJavaProjectConfigurator configurator : getJavaProjectConfigurators(projectFacade, monitor)) {
      configurator.configureClasspath(projectFacade, classpath, monitor);
    }
  }

  private List<IJavaProjectConfigurator> getJavaProjectConfigurators(IMavenProjectFacade projectFacade,
      final IProgressMonitor monitor) throws CoreException {

    ArrayList<IJavaProjectConfigurator> configurators = new ArrayList<>();

    ILifecycleMapping lifecycleMapping = configurationManager.getLifecycleMapping(projectFacade);

    if(lifecycleMapping != null) {
      for(AbstractProjectConfigurator configurator : lifecycleMapping.getProjectConfigurators(projectFacade, monitor)) {
        if(configurator instanceof IJavaProjectConfigurator javaConfigurator) {
          configurators.add(javaConfigurator);
        }
      }
    }
    return configurators;
  }

  void addClasspathEntries(IClasspathDescriptor classpath, IMavenProjectFacade facade, int kind,
      IProgressMonitor monitor) throws CoreException {
    ArtifactFilter scopeFilter;

    if(IClasspathManager.CLASSPATH_RUNTIME == kind) {
      // ECLIPSE-33: runtime+provided scope
      // ECLIPSE-85: adding system scope
      scopeFilter = artifact -> BuildPathManager.SCOPE_FILTER_RUNTIME.include(artifact)
          || Artifact.SCOPE_PROVIDED.equals(artifact.getScope()) || Artifact.SCOPE_SYSTEM.equals(artifact.getScope());
    } else {
      // ECLIPSE-33: test scope (already includes provided)
      scopeFilter = BuildPathManager.SCOPE_FILTER_TEST;
    }

    MavenProject mavenProject = facade.getMavenProject(monitor);
    Set<Artifact> artifacts = mavenProject.getArtifacts();

    //if the project is used as a test-jar by another one, the test flag must be disabled on
    //its test sources to make them visible by other main code, AND test dependencies.
    // so that its test sources can compile
    boolean isTestFlagDisabled = MavenClasspathHelpers.hasTestFlagDisabled(mavenProject);
    Map<IPath, ProjectTestAttributes> projectTestAttributes = new HashMap<>(artifacts.size());

    for(Artifact a : artifacts) {
      if(!scopeFilter.include(a) || !a.getArtifactHandler().isAddedToClasspath()) {
        continue;
      }

      // project
      IMavenProjectFacade dependency = projectManager.getMavenProject(a.getGroupId(), a.getArtifactId(),
          a.getBaseVersion());
      if(dependency != null && dependency.getProject().equals(facade.getProject())) {
        continue;
      }

      IClasspathEntryDescriptor entry = null;
      boolean addTestFlag = !isTestFlagDisabled && isOnlyVisibleByTests(a);
      if(dependency != null && dependency.getFullPath(a.getFile()) != null) {
        IPath projectPath = dependency.getFullPath();
        entry = classpath.addProjectEntry(projectPath);
        ProjectTestAttributes testAttributes = projectTestAttributes.get(projectPath);
        boolean isTestArtifact = isTestArtifact(a);
        if(testAttributes == null) {
          testAttributes = new ProjectTestAttributes(addTestFlag, !isTestArtifact);
          projectTestAttributes.put(projectPath, testAttributes);
        } else {
          testAttributes.isTest &= addTestFlag;
          testAttributes.excludeTestSources &= !isTestArtifact;
        }

      } else {
        File artifactFile = a.getFile();
        if(artifactFile != null /*&& artifactFile.canRead()*/) {
          entry = classpath.addLibraryEntry(Path.fromOSString(artifactFile.getAbsolutePath()));
          entry.setClasspathAttribute(IClasspathManager.TEST_ATTRIBUTE, addTestFlag ? "true" : null);
        }
      }

      if(entry != null) {
        entry.setArtifactKey(new ArtifactKey(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getClassifier()));
        entry.setScope(a.getScope());
        entry.setOptionalDependency(a.isOptional());
        // Setting the entry's exported flag effectively changes the usual breath-first dependency resolution of Maven 
        // to a depth-first resolution for dependencies that are in the workspace.
        // See https://github.com/eclipse-m2e/m2e-core/issues/1501
        entry.setExported(false); // isExportedArtifact(a)
      }
    }

    //2nd pass to set project test related attributes
    projectTestAttributes.forEach((entryPath, testAttributes) -> {
      //the classpath definitely has an entry matching the path
      IClasspathEntryDescriptor descriptor = findClasspathDescriptor(classpath, entryPath);
      descriptor.setClasspathAttribute(IClasspathManager.TEST_ATTRIBUTE, (testAttributes.isTest) ? "true" : null);
      descriptor.setClasspathAttribute(IClasspathManager.WITHOUT_TEST_CODE,
          (testAttributes.excludeTestSources) ? "true" : null);
    });
  }

  @SuppressWarnings("unused")
  private boolean isExportedArtifact(Artifact a) {
    if(Artifact.SCOPE_PROVIDED.equals(a.getScope())) {
      //provided items are not transitive
      return false;
    }
    if(a.isOptional()) {
      //optional artifacts are also not transitive
      return false;
    }
    //everything else is considered transitive
    return true;
  }

  private boolean isOnlyVisibleByTests(Artifact a) {
    return Artifact.SCOPE_TEST.equals(a.getScope()) || Artifact.SCOPE_RUNTIME.equals(a.getScope());
  }

  private boolean isTestArtifact(Artifact a) {
    return BuildPathManager.CLASSIFIER_TESTS.equals(a.getClassifier()) || "test-jar".equals(a.getType());
  }

  private IClasspathEntryDescriptor findClasspathDescriptor(IClasspathDescriptor classpath, IPath p) {
    return classpath.getEntryDescriptors().stream().filter(e -> p.equals(e.getPath())).findFirst().orElse(null);
  }

  static class ProjectTestAttributes {
    ProjectTestAttributes(boolean isTest, boolean excludeTestSources) {
      this.isTest = isTest;
      this.excludeTestSources = excludeTestSources;
    }

    boolean isTest;

    boolean excludeTestSources;
  }

}
