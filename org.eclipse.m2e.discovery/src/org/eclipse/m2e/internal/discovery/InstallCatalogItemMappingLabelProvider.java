/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.internal.discovery;

import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingConfiguration;
import org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider;


//TODO : can probably be removed..
@SuppressWarnings({"restriction", "unchecked", "rawtypes"})
public class InstallCatalogItemMappingLabelProvider implements IAdapterFactory {

  private final InstallCatalogItemMavenDiscoveryProposal proposal;

  /**
   * Factory instance constructor
   */
  public InstallCatalogItemMappingLabelProvider() {
    this(null);
  }

  public InstallCatalogItemMappingLabelProvider(InstallCatalogItemMavenDiscoveryProposal proposal) {
    this.proposal = proposal;
  }

  public String getMavenText() {
    return ""; //$NON-NLS-1$
  }

  public String getEclipseMappingText(LifecycleMappingConfiguration configuration) {
    return "INSTALL " + proposal.getCatalogItem().getName();
  }

  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if(adapterType.isAssignableFrom(ILifecycleMappingLabelProvider.class)
        && adaptableObject instanceof InstallCatalogItemMavenDiscoveryProposal) {
      return new InstallCatalogItemMappingLabelProvider((InstallCatalogItemMavenDiscoveryProposal) adaptableObject);
    }
    return null;
  }

  public Class[] getAdapterList() {
    return new Class[] {ILifecycleMappingLabelProvider.class};
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider#isError()
   */
  public boolean isError() {
    // TODO Auto-generated method isError
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider#getKey()
   */
  public ILifecycleMappingRequirement getKey() {
    // TODO Auto-generated method getKey
    return null;
  }

}
