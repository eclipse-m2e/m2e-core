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

package org.eclipse.m2e.jdt.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer.RequiredProjectWrapper;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.jdt.MavenJdtPlugin;


/**
 * Adapter factory for Java elements
 * 
 * @author Igor Fedorenko
 * @author Eugene Kuleshov
 */
@SuppressWarnings({"unchecked", "restriction"})
public class JavaElementsAdapterFactory implements IAdapterFactory {

  private static final Class[] ADAPTER_LIST = new Class[] {ArtifactKey.class, IPath.class, IMavenProjectFacade.class};

  public Class[] getAdapterList() {
    return ADAPTER_LIST;
  }

  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if(adapterType == ArtifactKey.class) {
      if(adaptableObject instanceof IPackageFragmentRoot) {
        IPackageFragmentRoot fragment = (IPackageFragmentRoot) adaptableObject;
        IProject project = fragment.getJavaProject().getProject();
        if(project.isAccessible() && fragment.isArchive()) {
          try {
            return MavenJdtPlugin.getDefault().getBuildpathManager().findArtifact(project, fragment.getPath());
          } catch(CoreException ex) {
            MavenLogger.log(ex);
            MavenPlugin.getDefault().getConsole().logError("Can't find artifact for " + fragment);
            return null;
          }
        }
        
      } else if(adaptableObject instanceof RequiredProjectWrapper) {
        IMavenProjectFacade projectFacade = getProjectFacade(adaptableObject);
        if(projectFacade!=null) {
          return projectFacade.getArtifactKey();
        }
        
      } else if(adaptableObject instanceof IJavaProject) {
        return ((IJavaProject) adaptableObject).getProject().getAdapter(ArtifactKey.class);
        
      }

    } else if(adapterType == IPath.class) {
      if(adaptableObject instanceof IJavaElement) {
        IResource resource = ((IJavaElement) adaptableObject).getResource();
        if(resource != null) {
          return resource.getLocation();
        }
      }
      
    } else if(adapterType == IMavenProjectFacade.class) {
      if(adaptableObject instanceof IJavaElement) {
        IProject project = ((IJavaElement) adaptableObject).getJavaProject().getProject();
        IMavenProjectFacade projectFacade = getProjectFacade(project);
        if(projectFacade != null) {
          return projectFacade;
        }

      } else if(adaptableObject instanceof RequiredProjectWrapper) {
        ClassPathContainer container = ((RequiredProjectWrapper) adaptableObject).getParentClassPathContainer();
        IProject project = container.getJavaProject().getProject();
        IMavenProjectFacade projectFacade = getProjectFacade(project);
        if(projectFacade != null) {
          return projectFacade;
        }
      }
    }

    return null;
  }

  private IMavenProjectFacade getProjectFacade(Object adaptableObject) {
    RequiredProjectWrapper wrapper = (RequiredProjectWrapper) adaptableObject;
    return getProjectFacade(wrapper.getProject().getProject());
  }

  private IMavenProjectFacade getProjectFacade(IProject project) {
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    return projectManager.create(project, new NullProgressMonitor());
  }
  
}
