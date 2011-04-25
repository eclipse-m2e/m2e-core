/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.sonatype.aether.graph.DependencyNode;

/**
 * Helper IPropertyTester implementation to check if receiver can be launched with Maven.
 * E.g. it is pom.xml file of folder or project that has pom.xml. 
 *
 * @author Eugene Kuleshov
 */
public class MavenPropertyTester extends PropertyTester {
  

  private static final String IS_DIRECT_DEPENDENCY_TREE_NODE = "isDirectDependencyTreeNode"; //$NON-NLS-1$
  private static final String IS_TRANSITIVE_DEPENDENCY_TREE_NODE = "isTransitiveDependencyTreeNode"; //$NON-NLS-1$
  private static final String HAS_PROJECT_ARTIFACT_KEY = "hasProjectArtifactKey"; //$NON-NLS-1$
  private static final String HAS_ARTIFACT_KEY = "hasArtifactKey"; //$NON-NLS-1$
  private static final String WORKSPACE_RESULUTION_ENABLE = "workspaceResulutionEnable"; //$NON-NLS-1$
  private static final String LAUNCHABLE = "launchable"; //$NON-NLS-1$

  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    if (LAUNCHABLE.equals(property)) {
      IAdaptable adaptable = (IAdaptable) receiver;
      
      IProject projectAdapter = (IProject) adaptable.getAdapter(IProject.class);
      if(projectAdapter!=null) {
        return projectAdapter.getFile(IMavenConstants.POM_FILE_NAME).exists();
      }
      
      IFolder folderAdapter = (IFolder) adaptable.getAdapter(IFolder.class);
      if(folderAdapter!=null) {
        return folderAdapter.getFile(IMavenConstants.POM_FILE_NAME).exists();
      }
  
      IFile fileAdapter = (IFile) adaptable.getAdapter(IFile.class);
      if(fileAdapter!=null) {
        return fileAdapter.exists() && IMavenConstants.POM_FILE_NAME.equals(fileAdapter.getName());
      }
      return false;
    }
    if (WORKSPACE_RESULUTION_ENABLE.equals(property)) {
      boolean enableWorkspaceResolution = true;
      IAdaptable adaptable = (IAdaptable) receiver;
      
      IProject projectAdapter = (IProject) adaptable.getAdapter(IProject.class);
      if(projectAdapter!=null) {
          IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
          IMavenProjectFacade projectFacade = projectManager.create(projectAdapter, new NullProgressMonitor());
          if(projectFacade != null) {
            ResolverConfiguration configuration = projectFacade.getResolverConfiguration();
            return !configuration.shouldResolveWorkspaceProjects();
          }
      }
      return enableWorkspaceResolution;
    }
    
    if (HAS_ARTIFACT_KEY.equals(property)) {
      ArtifactKey ak = SelectionUtil.getType(receiver, ArtifactKey.class);
      return ak != null;
    }
    if (HAS_PROJECT_ARTIFACT_KEY.equals(property)) {
      ArtifactKey key = SelectionUtil.getType(receiver, ArtifactKey.class);
      if(key != null) {
        IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
        IMavenProjectFacade mavenProject = null;
        mavenProject = projectManager.getMavenProject( //
            key.getGroupId(), key.getArtifactId(), key.getVersion());
         return mavenProject != null;
      }
    }
    
    if (IS_TRANSITIVE_DEPENDENCY_TREE_NODE.equals(property)) {
      if (receiver instanceof DependencyNode) {
        DependencyNode nd = (DependencyNode) receiver;
        return nd.getData().get("LEVEL") == null;
      }
    }
    if (IS_DIRECT_DEPENDENCY_TREE_NODE.equals(property)) {
      if (receiver instanceof DependencyNode) {
        DependencyNode nd = (DependencyNode) receiver;
        return "DIRECT".equals(nd.getData().get("LEVEL"));
      }
    }
    
    return false;
    
  }

}

