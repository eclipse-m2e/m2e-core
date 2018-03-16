/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.jdt.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.jdt.MavenJdtPlugin;


/**
 * Adapter factory for Java elements
 * 
 * @author Igor Fedorenko
 * @author Eugene Kuleshov
 * @author Miles Parker (Split out into JavaUiElementsAdapterFactory)
 */
@SuppressWarnings({"rawtypes"})
public class JavaElementsAdapterFactory implements IAdapterFactory {
  private static final Logger log = LoggerFactory.getLogger(JavaElementsAdapterFactory.class);

  private static final Class[] ADAPTER_LIST = new Class[] {ArtifactKey.class, IPath.class, IMavenProjectFacade.class};

  public Class<?>[] getAdapterList() {
    return ADAPTER_LIST;
  }

  public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
    if(adapterType == ArtifactKey.class) {
      if(adaptableObject instanceof IPackageFragmentRoot) {
        IPackageFragmentRoot fragment = (IPackageFragmentRoot) adaptableObject;
        IProject project = fragment.getJavaProject().getProject();
        if(project.isAccessible() && fragment.isArchive()) {
          try {
            return adapterType.cast(getBuildPathManager().findArtifact(project, fragment.getPath()));
          } catch(CoreException ex) {
            log.error("Can't find artifact for " + fragment, ex);
            return null;
          }
        }

      } else if(adaptableObject instanceof IJavaProject) {
        return adapterType.cast(((IJavaProject) adaptableObject).getProject().getAdapter(ArtifactKey.class));

      }

    } else if(adapterType == IPath.class) {
      if(adaptableObject instanceof IJavaElement) {
        IResource resource = ((IJavaElement) adaptableObject).getResource();
        if(resource != null) {
          return adapterType.cast(resource.getLocation());
        }
      }

    } else if(adapterType == IMavenProjectFacade.class) {
      if(adaptableObject instanceof IJavaElement) {
        IProject project = ((IJavaElement) adaptableObject).getJavaProject().getProject();
        IMavenProjectFacade projectFacade = getProjectFacade(project);
        if(projectFacade != null) {
          return adapterType.cast(projectFacade);
        }
      }
    }
    return null;
  }

  private BuildPathManager getBuildPathManager() {
    return (BuildPathManager) MavenJdtPlugin.getDefault().getBuildpathManager();
  }

  private IMavenProjectFacade getProjectFacade(IProject project) {
    IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
    return projectManager.create(project, new NullProgressMonitor());
  }

}
