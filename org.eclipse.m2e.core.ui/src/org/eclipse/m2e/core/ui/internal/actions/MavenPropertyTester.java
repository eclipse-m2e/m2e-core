/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc. and others.
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

package org.eclipse.m2e.core.ui.internal.actions;

import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IFileEditorInput;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.project.ResolverConfigurationIO;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.MavenProjectUtils;


/**
 * Helper IPropertyTester implementation to check if receiver can be launched with Maven. E.g. it is pom.xml file of
 * folder or project that has pom.xml.
 *
 * @author Eugene Kuleshov
 */
public class MavenPropertyTester extends PropertyTester {

  private static final String IS_DIRECT_DEPENDENCY_TREE_NODE = "isDirectDependencyTreeNode"; //$NON-NLS-1$

  private static final String IS_TRANSITIVE_DEPENDENCY_TREE_NODE = "isTransitiveDependencyTreeNode"; //$NON-NLS-1$

  private static final String HAS_PROJECT_ARTIFACT_KEY = "hasProjectArtifactKey"; //$NON-NLS-1$

  private static final String HAS_ARTIFACT_KEY = "hasArtifactKey"; //$NON-NLS-1$

  private static final String WORKSPACE_RESULUTION_ENABLE = "workspaceResulutionEnable"; //$NON-NLS-1$

  private static final String IS_BUILD_DIRECTORY = "isBuildDirectory"; //$NON-NLS-1$

  private static final String DEFAULT_BUILD_DIR = "target"; //$NON-NLS-1$

  private static final String HAS_MAVEN_NATURE = "hasMavenNature"; //$NON-NLS-1$

  @Override
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    if(WORKSPACE_RESULUTION_ENABLE.equals(property)) {
      IProject projectAdapter = Adapters.adapt(receiver, IProject.class);
      if(projectAdapter != null) {
        return !ResolverConfigurationIO.isResolveWorkspaceProjects(projectAdapter);
      }
      return true;
    }

    if(HAS_ARTIFACT_KEY.equals(property)) {
      ArtifactKey ak = SelectionUtil.getType(receiver, ArtifactKey.class);
      return ak != null;
    }

    if(HAS_PROJECT_ARTIFACT_KEY.equals(property)) {
      ArtifactKey key = SelectionUtil.getType(receiver, ArtifactKey.class);
      if(key != null) {
        IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
        IMavenProjectFacade mavenProject = null;
        mavenProject = projectManager.getMavenProject( //
            key.groupId(), key.artifactId(), key.version());
        return mavenProject != null;
      }
    }

    if(IS_TRANSITIVE_DEPENDENCY_TREE_NODE.equals(property)) {
      if(receiver instanceof DependencyNode nd) {
        return nd.getData().get("LEVEL") == null;
      }
    }
    if(IS_DIRECT_DEPENDENCY_TREE_NODE.equals(property)) {
      if(receiver instanceof DependencyNode nd) {
        return "DIRECT".equals(nd.getData().get("LEVEL"));
      }
    }

    if(IS_BUILD_DIRECTORY.equals(property)) {
      if(receiver instanceof IFolder folder) {
        IProject project = folder.getProject();
        if(project != null) {
          IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
          //Lookup project facade in the cache registry to minimize UI locking
          IMavenProjectFacade projectFacade = projectManager.create(project, new NullProgressMonitor());
          IPath outputLocation;
          if(projectFacade == null || projectFacade.getMavenProject() == null) {
            //If the project facade has not been cached yet (ex. during workspace startup),
            //fall back on the default value
            outputLocation = IPath.fromOSString(DEFAULT_BUILD_DIR);
          } else {
            String buildDir = projectFacade.getMavenProject().getBuild().getDirectory();
            outputLocation = MavenProjectUtils.getProjectRelativePath(project, buildDir);
          }
          if(outputLocation != null) {
            return folder.equals(project.getFolder(outputLocation));
          }
        }
      }
    }

    if(HAS_MAVEN_NATURE.equals(property)) {
      if(receiver instanceof IFileEditorInput editor) {
        return checkProjectNature(editor.getFile().getProject(), expectedValue);
      }
      if(receiver instanceof IResource resource) {
        IProject project = resource.getProject();
        return checkProjectNature(project, expectedValue);
      }
    }

    return false;

  }

  private boolean checkProjectNature(IProject project, Object expectedValue) {
    boolean expectedBoolean = expectedValue instanceof Boolean b ? b : true;
    try {
      return project.hasNature(IMavenConstants.NATURE_ID) == expectedBoolean;
    } catch(CoreException ex) {
      throw new RuntimeException("Unable to get project natures for " + project + ", error= " + ex.getMessage(), ex);
    }
  }

}
