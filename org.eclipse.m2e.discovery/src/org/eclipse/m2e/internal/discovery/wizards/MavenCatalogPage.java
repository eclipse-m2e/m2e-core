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

import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogPage;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogViewer;
import org.eclipse.m2e.internal.discovery.Messages;
import org.eclipse.swt.widgets.Composite;


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
