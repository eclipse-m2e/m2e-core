/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.jdt.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;


@Component(service = IAdapterFactory.class, property = {
    IAdapterFactory.SERVICE_PROPERTY_ADAPTABLE_CLASS + "=org.eclipse.jdt.core.IJavaProject",
    IAdapterFactory.SERVICE_PROPERTY_ADAPTER_NAMES + "=org.eclipse.m2e.core.project.IMavenProjectFacade"})
public class JavaProjectAdapterFactory implements IAdapterFactory {

  private static final Class<?>[] ADAPTER_LIST = {IMavenProjectFacade.class};

  @Reference
  private IMavenProjectRegistry mavenProjectRegistry;

  @Override
  public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
    if(adaptableObject instanceof IJavaProject javaProject) {
      if(adapterType == IMavenProjectFacade.class) {
        return adapterType.cast(mavenProjectRegistry.getProject(javaProject.getProject()));
      }
    }
    return null;
  }

  @Override
  public Class<?>[] getAdapterList() {
    return ADAPTER_LIST;
  }

}
