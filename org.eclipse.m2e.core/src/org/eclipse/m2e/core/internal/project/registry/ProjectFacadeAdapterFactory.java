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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;

public class ProjectFacadeAdapterFactory implements IAdapterFactory {

  private static final Class<?>[] ADAPTER_LIST = {IMavenProjectFacade.class};

  @Override
  public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
    if(adaptableObject instanceof IResource && adapterType == IMavenProjectFacade.class) {
      return (T) MavenPlugin.getMavenProjectRegistry().getProject(((IResource) adaptableObject).getProject());
    }
    return null;
  }

  @Override
  public Class<?>[] getAdapterList() {
    return ADAPTER_LIST;
  }

}
