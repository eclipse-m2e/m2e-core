/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IPath;

import org.eclipse.m2e.core.embedder.IComponentLookup;
import org.eclipse.m2e.core.embedder.IMavenExecutableLocation;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.IMavenToolbox;
import org.eclipse.m2e.core.internal.embedder.PlexusContainerManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;


@Component(service = IAdapterFactory.class, property = {
    IAdapterFactory.SERVICE_PROPERTY_ADAPTABLE_CLASS + "=org.eclipse.core.resources.IResource",
    IAdapterFactory.SERVICE_PROPERTY_ADAPTER_NAMES + "=org.eclipse.m2e.core.embedder.IPomFacade"})
public class PomFacadeAdapterFactory implements IAdapterFactory {

  @Reference
  private IMavenProjectRegistry mavenProjectRegistry;

  @Reference
  private PlexusContainerManager containerManager;

  @Override
  public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
    if(adapterType == IMavenExecutableLocation.class) {
      if(adaptableObject instanceof IFile file) {
        if(file.getName().equalsIgnoreCase(IMavenConstants.POM_FILE_NAME)) {
          return adapterType.cast(getFacadeForPom(file));
        }
        //TODO not a pom file, but actually pom files can have any name...
        return null;
      }
      if(adaptableObject instanceof IContainer container) {
        IMavenExecutableLocation facade = Optional.of(container.getLocation())//
            .map(IPath::toFile)//
            .flatMap(basedir -> {
              IComponentLookup lookup = containerManager.getComponentLookup(basedir);
              return IMavenToolbox.of(lookup).locatePom(basedir);
            })//
            .map(pomfile -> container.getFile(IPath.fromPortableString(pomfile.getName())))//
            .map(this::getFacadeForPom)//
            .orElse(null);
        return adapterType.cast(facade);
      }
      return null;
    }
    return null;
  }

  private IMavenExecutableLocation getFacadeForPom(IFile file) {
    if(file == null) {
      return null;
    }
    //we must refresh, as the file might be created right now by the lookup...
    try {
      file.refreshLocal(IResource.DEPTH_ZERO, null);
    } catch(CoreException ex) {
    }
    if(!file.exists()) {
      //still not there...
      return null;
    }

    IContainer parent = file.getParent();
    if(parent instanceof IProject project) {
      //a direct child of a project, lets check if the project itself could be used...
      IMavenProjectFacade mavenProjectFacade = mavenProjectRegistry.getProject(project);
      if(mavenProjectFacade != null) {
        return mavenProjectFacade;
      }
    }
    //a "detached" resource pom facade is needed
    return new ResourcePomFacade(file);
  }

  @Override
  public Class<?>[] getAdapterList() {
    return new Class[] {IMavenExecutableLocation.class};
  }

}
