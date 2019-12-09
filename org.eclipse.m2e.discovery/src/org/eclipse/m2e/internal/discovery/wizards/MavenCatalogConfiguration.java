/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
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

package org.eclipse.m2e.internal.discovery.wizards;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogConfiguration;

import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


@SuppressWarnings("restriction")
public class MavenCatalogConfiguration extends CatalogConfiguration {

  private Set<String> selectedPackagingTypes;

  private Set<MojoExecutionKey> selectedMojos;

  private Set<String> selectedLifecycleIds;

  private Set<String> selectedConfiguratorIds;

  public Collection<String> getSelectedLifecycleIds() {
    return selectedLifecycleIds;
  }

  public Collection<String> getSelectedConfiguratorIds() {
    return selectedConfiguratorIds;
  }

  public Collection<MojoExecutionKey> getSelectedMojos() {
    return selectedMojos;
  }

  public Collection<String> getSelectedPackagingTypes() {
    return selectedPackagingTypes;
  }

  /*
   * Set the packaging types that should be selected in the UI
   */
  public void setSelectedPackagingTypes(Collection<String> packagingTypes) {
    if(selectedPackagingTypes == null) {
      selectedPackagingTypes = new HashSet<String>(packagingTypes);
    } else {
      selectedPackagingTypes.addAll(packagingTypes);
    }
  }

  /*
   * Set the mojos that should be selected in the UI
   */
  public void setSelectedMojos(Collection<MojoExecutionKey> mojos) {
    if(selectedMojos == null) {
      selectedMojos = new HashSet<MojoExecutionKey>(mojos);
    } else {
      selectedMojos.addAll(mojos);
    }
  }

  public void setSelectedLifecycleIds(Collection<String> lifecycleIds) {
    if(selectedLifecycleIds == null) {
      selectedLifecycleIds = new HashSet<String>(lifecycleIds);
    } else {
      selectedLifecycleIds.addAll(lifecycleIds);
    }
  }

  public void setSelectedConfigurators(Collection<String> configuratorIds) {
    if(selectedConfiguratorIds == null) {
      selectedConfiguratorIds = new HashSet<String>(configuratorIds);
    } else {
      selectedConfiguratorIds.addAll(configuratorIds);
    }
  }
}
