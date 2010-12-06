/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.search;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

import org.eclipse.m2e.core.internal.Messages;

/**
 * Maven Search Page
 *
 * @author Eugene Kuleshov
 */
public class MavenSearchPage extends DialogPage implements ISearchPage {

  private Table table;
  private Combo classNameText;
  private Combo sha1Text;
  private Combo versionText;
  private Combo packagingIdText;
  private Combo artifactIdText;
  private Combo groupIdText;
  
  // private ISearchPageContainer container;

  public MavenSearchPage() {
  }

  public MavenSearchPage(String title) {
    super(title);
  }

  public MavenSearchPage(String title, ImageDescriptor image) {
    super(title, image);
  }

  public void setContainer(ISearchPageContainer container) {
    // this.container = container;
  }
  
  public boolean performAction() {
    // TODO Auto-generated method performAction
    return false;
  }

  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(3, false));
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    setControl(parent);

    Label groupIdLabel = new Label(composite, SWT.NONE);
    groupIdLabel.setText(Messages.MavenSearchPage_lblGroupid);

    groupIdText = new Combo(composite, SWT.NONE);
    groupIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

    Label artifactIdLabel = new Label(composite, SWT.NONE);
    artifactIdLabel.setText(Messages.MavenSearchPage_lblArtifactid);

    artifactIdText = new Combo(composite, SWT.NONE);
    artifactIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

    Label versionLabel = new Label(composite, SWT.NONE);
    versionLabel.setText(Messages.MavenSearchPage_lblVersion);

    versionText = new Combo(composite, SWT.NONE);
    versionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

    Label packagingIdLabel = new Label(composite, SWT.NONE);
    packagingIdLabel.setText(Messages.MavenSearchPage_lblPackaging);

    packagingIdText = new Combo(composite, SWT.NONE);
    GridData packagingIdTextData = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    packagingIdTextData.widthHint = 208;
    packagingIdText.setLayoutData(packagingIdTextData);

    Label sha1Label = new Label(composite, SWT.NONE);
    sha1Label.setText(Messages.MavenSearchPage_lblSha);

    sha1Text = new Combo(composite, SWT.NONE);
    sha1Text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Button browseButton = new Button(composite, SWT.NONE);
    browseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    browseButton.setText(Messages.MavenSearchPage_btnBrowse);

    Label classNameLabel = new Label(composite, SWT.NONE);
    classNameLabel.setText(Messages.MavenSearchPage_lblClass);

    classNameText = new Combo(composite, SWT.NONE);
    classNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

    Label separator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
    GridData separatorData = new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1);
    separatorData.heightHint = 15;
    separatorData.minimumHeight = 15;
    separator.setLayoutData(separatorData);
    separator.setText(Messages.MavenSearchPage_separator);

    Label repositoriesLabel = new Label(composite, SWT.NONE);
    repositoriesLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
    repositoriesLabel.setText(Messages.MavenSearchPage_lblRepos);

    CheckboxTableViewer tableViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER);
    table = tableViewer.getTable();
    table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));

    Button selectAllButton = new Button(composite, SWT.NONE);
    selectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    selectAllButton.setText(Messages.MavenSearchPage_btnSelect);
    new Label(composite, SWT.NONE);

    Button deselectAllButton = new Button(composite, SWT.NONE);
    deselectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    deselectAllButton.setText(Messages.MavenSearchPage_btnUnselect);
  }

}
