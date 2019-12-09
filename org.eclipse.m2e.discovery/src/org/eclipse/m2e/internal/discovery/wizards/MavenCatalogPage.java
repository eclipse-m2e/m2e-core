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

import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogPage;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogViewer;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.m2e.internal.discovery.Messages;


@SuppressWarnings("restriction")
public class MavenCatalogPage extends CatalogPage {

  public MavenCatalogPage(Catalog catalog) {
    super(catalog);
    setTitle(Messages.MavenCatalogPage_Title);
    setDescription(Messages.MavenCatalogPage_Descripton);
  }

  protected CatalogViewer doCreateViewer(Composite parent) {
    MavenCatalogViewer viewer = new MavenCatalogViewer(getCatalog(), this, getContainer(), getWizard()
        .getConfiguration());
    viewer.setMinimumHeight(MINIMUM_HEIGHT);
    viewer.createControl(parent);
    return viewer;
  }
}
