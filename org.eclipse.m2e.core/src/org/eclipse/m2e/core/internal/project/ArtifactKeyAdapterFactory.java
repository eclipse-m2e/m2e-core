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

package org.eclipse.m2e.core.internal.project;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;


/**
 * Adapter factory for ArtifactKey
 * 
 * @author Igor Fedorenko
 */
@SuppressWarnings("unchecked")
public class ArtifactKeyAdapterFactory implements IAdapterFactory {

  private static final Class[] ADAPTER_LIST = new Class[] {ArtifactKey.class,};

  public Object getAdapter(Object adaptable, Class adapterType) {
    if(!ArtifactKey.class.equals(adapterType)) {
      return null;
    }

    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    if(adaptable instanceof IProject) {
      IProject project = (IProject) adaptable;
      IMavenProjectFacade facade = projectManager.create(project, new NullProgressMonitor());
      if(facade != null) {
        return facade.getArtifactKey();
      }
    } else if(adaptable instanceof IFile) {
      IFile file = (IFile) adaptable;
      if(IMavenConstants.POM_FILE_NAME.equals(file.getName())) {
        IMavenProjectFacade facade = projectManager.create(file, true, new NullProgressMonitor());
        if(facade != null) {
          return facade.getArtifactKey();
        }
      }
    }

    return null;
  }

  public Class[] getAdapterList() {
    // target type
    return ADAPTER_LIST;
  }

}
