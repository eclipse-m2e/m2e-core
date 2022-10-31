/*******************************************************************************
 * Copyright (c) 2008-2013 Sonatype, Inc. and others.
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

package org.eclipse.m2e.internal.discovery;

import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;

import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscoveryProposal;


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
    return obj instanceof InstallCatalogItemMavenDiscoveryProposal other
        && item.getSiteUrl().equals(other.item.getSiteUrl())
        && item.getInstallableUnits().equals(other.item.getInstallableUnits());
  }

  public String getDescription() {
    return item.getOverview() == null ? "" : item.getOverview().getSummary();
  }

  public String getLicense() {
    return item.getLicense();
  }
}
