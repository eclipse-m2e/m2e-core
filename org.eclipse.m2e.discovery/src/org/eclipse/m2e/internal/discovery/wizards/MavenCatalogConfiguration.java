/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.discovery.wizards;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogConfiguration;


@SuppressWarnings("restriction")
public class MavenCatalogConfiguration extends CatalogConfiguration {

  private Set<String> selectedPackagingTypes;

  private Set<MojoExecution> selectedMojos;

  public Collection<MojoExecution> getSelectedMojos() {
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
  public void setSelectedMojos(Collection<MojoExecution> mojos) {
    if(selectedMojos == null) {
      selectedMojos = new HashSet<MojoExecution>(mojos);
    } else {
      selectedMojos.addAll(mojos);
    }
  }
}
