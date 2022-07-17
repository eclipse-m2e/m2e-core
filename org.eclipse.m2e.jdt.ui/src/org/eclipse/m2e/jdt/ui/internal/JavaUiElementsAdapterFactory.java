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

package org.eclipse.m2e.jdt.ui.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer.RequiredProjectWrapper;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;


/**
 * Adapter factory for Java elements
 *
 * @author Igor Fedorenko
 * @author Eugene Kuleshov
 * @author Miles Parker
 */
@SuppressWarnings({"restriction"})
public class JavaUiElementsAdapterFactory implements IAdapterFactory {

  private static final Class<?>[] ADAPTER_LIST = new Class[] {ArtifactKey.class, IMavenProjectFacade.class};

  public Class<?>[] getAdapterList() {
    return ADAPTER_LIST;
  }

  public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
    if(adapterType == ArtifactKey.class) {
      if(adaptableObject instanceof RequiredProjectWrapper) {
        IMavenProjectFacade projectFacade = getProjectFacade(adaptableObject);
        if(projectFacade != null) {
          return adapterType.cast(projectFacade.getArtifactKey());
        }

      }
    } else if(adapterType == IMavenProjectFacade.class) {
      if(adaptableObject instanceof RequiredProjectWrapper wrapper) {
        ClassPathContainer container = wrapper.getParentClassPathContainer();
        IProject project = container.getJavaProject().getProject();
        IMavenProjectFacade projectFacade = getProjectFacade(project);
        if(projectFacade != null) {
          return adapterType.cast(projectFacade);
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
    IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
    return projectManager.create(project, new NullProgressMonitor());
  }

}
