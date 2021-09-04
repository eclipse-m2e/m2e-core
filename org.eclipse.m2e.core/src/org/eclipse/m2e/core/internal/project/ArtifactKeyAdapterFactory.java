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

package org.eclipse.m2e.core.internal.project;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;


/**
 * Adapter factory for ArtifactKey
 *
 * @author Igor Fedorenko
 */
public class ArtifactKeyAdapterFactory implements IAdapterFactory {

  private static final Class<?>[] ADAPTER_LIST = new Class[] {ArtifactKey.class,};

  @Override
  public <T> T getAdapter(Object adaptable, Class<T> adapterType) {
    if(!ArtifactKey.class.equals(adapterType)) {
      return null;
    }

    IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
    if(adaptable instanceof IProject) {
      IProject project = (IProject) adaptable;
      IMavenProjectFacade facade = projectManager.create(project, new NullProgressMonitor());
      if(facade != null) {
        return adapterType.cast(facade.getArtifactKey());
      }
    } else if(adaptable instanceof IFile) {
      IFile file = (IFile) adaptable;
      if(IMavenConstants.POM_FILE_NAME.equals(file.getName())) {
        IMavenProjectFacade facade = projectManager.create(file, true, new NullProgressMonitor());
        if(facade != null) {
          return adapterType.cast(facade.getArtifactKey());
        }
      }
    }

    return null;
  }

  @Override
  public Class<?>[] getAdapterList() {
    // target type
    return ADAPTER_LIST;
  }

}
