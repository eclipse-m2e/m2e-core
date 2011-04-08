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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogPage;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryWizard;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.m2e.internal.discovery.MavenDiscoveryIcons;
import org.eclipse.m2e.internal.discovery.Messages;


@SuppressWarnings("restriction")
public class MavenDiscoveryWizard extends DiscoveryWizard {

  public MavenDiscoveryWizard(Catalog catalog, MavenCatalogConfiguration configuration) {
    super(catalog, configuration);
    setWindowTitle(Messages.MavenDiscoveryWizard_Title);
    setDefaultPageImageDescriptor(MavenDiscoveryIcons.WIZARD_BANNER);
  }

  @Override
  protected CatalogPage doCreateCatalogPage() {
    return new MavenCatalogPage(getCatalog());
  }

  @Override
  public boolean performFinish() {
    try {
      return MavenDiscoveryUi.install(getCatalogPage().getInstallableConnectors(), null, getContainer());
    } catch(CoreException e) {
      IWizardPage page = getContainer().getCurrentPage();
      if (page instanceof WizardPage) {
        ((WizardPage) page).setMessage(e.getMessage(), IMessageProvider.ERROR);
      }
      return false;
    }
  }
}
