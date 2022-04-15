/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.core.internal.project.registry;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;


@Component(service = IAdapterFactory.class, property = {
    IAdapterFactory.SERVICE_PROPERTY_ADAPTABLE_CLASS + "=org.eclipse.core.resources.IResource",
    IAdapterFactory.SERVICE_PROPERTY_ADAPTER_NAMES + "=org.eclipse.m2e.core.project.IMavenProjectFacade"})
public class ProjectFacadeAdapterFactory implements IAdapterFactory {

  private static final Class<?>[] ADAPTER_LIST = {IMavenProjectFacade.class};

  @Reference
  private IMavenProjectRegistry mavenProjectRegistry;

  @Override
  public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
    if(adaptableObject instanceof IResource) {
      IResource resource = (IResource) adaptableObject;
      if(adapterType == IMavenProjectFacade.class) {
        return adapterType.cast(mavenProjectRegistry.getProject(resource.getProject()));
      }
    }
    return null;
  }

  @Override
  public Class<?>[] getAdapterList() {
    return ADAPTER_LIST;
  }

}
