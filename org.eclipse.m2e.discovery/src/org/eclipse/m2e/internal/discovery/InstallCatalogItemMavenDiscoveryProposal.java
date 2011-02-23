/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.internal.discovery;

import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.m2e.core.internal.lifecycle.discovery.IMavenDiscoveryProposal;


/**
 * InstallIUMavenDiscoveryProposal
 * 
 * @author igor
 */
@SuppressWarnings("restriction")
public class InstallCatalogItemMavenDiscoveryProposal implements IMavenDiscoveryProposal {

  private final CatalogItem item;

  public InstallCatalogItemMavenDiscoveryProposal(CatalogItem item) {
    this.item = item;
  }

  public CatalogItem getCatalogItem() {
    return item;
  }

  @Override
  public String toString() {
    return item.getDescription();
  }

  @Override
  public int hashCode() {
    int hash = item.getSiteUrl().hashCode();
    hash = 17 * hash + item.getInstallableUnits().hashCode();
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == this) {
      return true;
    }

    if(!(obj instanceof InstallCatalogItemMavenDiscoveryProposal)) {
      return false;
    }

    InstallCatalogItemMavenDiscoveryProposal other = (InstallCatalogItemMavenDiscoveryProposal) obj;

    return item.getSiteUrl().equals(other.item.getSiteUrl())
        && item.getInstallableUnits().equals(other.item.getInstallableUnits());
  }
}
