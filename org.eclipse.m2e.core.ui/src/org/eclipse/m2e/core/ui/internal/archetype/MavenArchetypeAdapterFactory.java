/*******************************************************************************
 * Copyright (c) Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.core.ui.internal.archetype;

import org.osgi.service.component.annotations.Component;

import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.m2e.core.internal.project.ProjectConfigurationManager;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;


/**
 * MavenArchetypeAdapterFactory is used in {@link ProjectConfigurationManager} to obtain the (maven) ArchetypeManager in
 * a way that do not bound it to the UI plugin
 */
@Component(service = IAdapterFactory.class, property = {
    IAdapterFactory.SERVICE_PROPERTY_ADAPTABLE_CLASS + "=org.eclipse.m2e.core.ui.internal.archetype.MavenArchetype",
    IAdapterFactory.SERVICE_PROPERTY_ADAPTER_NAMES + "=org.apache.maven.archetype.ArchetypeManager"})
public class MavenArchetypeAdapterFactory implements IAdapterFactory {

  @SuppressWarnings("restriction")
  @Override
  public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
    if(adaptableObject instanceof MavenArchetype && adapterType == org.apache.maven.archetype.ArchetypeManager.class) {
      return adapterType.cast(M2EUIPluginActivator.getDefault().getArchetypeManager().getArchetyper());
    }
    return null;
  }

  @SuppressWarnings("restriction")
  @Override
  public Class<?>[] getAdapterList() {
    return new Class<?>[] {org.apache.maven.archetype.ArchetypeManager.class};
  }

}
