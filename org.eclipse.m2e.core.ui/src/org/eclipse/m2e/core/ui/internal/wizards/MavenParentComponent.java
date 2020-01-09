/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.ui.internal.wizards;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * Wizard page component showing panel project properties.
 */
public class MavenParentComponent extends Composite {

  /** parent artifact id input field */
  private Combo parentArtifactIdCombo;

  /** parent group id input field */
  private Combo parentGroupIdCombo;

  /** parent version input field */
  private Combo parentVersionCombo;

  /** the "clear parent section" button */
  private Button parentClearButton;

  /** the "browse..." button */
  private Button parentBrowseButton;

  private Label groupIdLabel;

  private Label artifactIdLabel;

  private Label versionLabel;

  /** Creates a new panel with parent controls. */
  public MavenParentComponent(Composite parent, int style) {
    super(parent, SWT.NONE);

    boolean readonly = (style & SWT.READ_ONLY) != 0;

    GridLayout topLayout = new GridLayout();
    topLayout.marginHeight = 0;
    topLayout.marginWidth = 0;
    setLayout(topLayout);

    Group group = new Group(this, SWT.NONE);
    group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    group.setText(Messages.wizardProjectPageArtifactParentTitle);

    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 3;
    group.setLayout(gridLayout);

    groupIdLabel = new Label(group, SWT.NONE);
    groupIdLabel.setText(Messages.wizardProjectPageArtifactParentGroupId);

    parentGroupIdCombo = new Combo(group, SWT.NONE);
    parentGroupIdCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    parentGroupIdCombo.setData("name", "parentGroupIdCombo"); //$NON-NLS-1$ //$NON-NLS-2$
    parentGroupIdCombo.setEnabled(!readonly);

    artifactIdLabel = new Label(group, SWT.NONE);
    artifactIdLabel.setText(Messages.wizardProjectPageArtifactParentArtifactId);

    parentArtifactIdCombo = new Combo(group, SWT.NONE);
    parentArtifactIdCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    parentArtifactIdCombo.setData("name", "parentArtifactIdCombo"); //$NON-NLS-1$ //$NON-NLS-2$
    parentArtifactIdCombo.setEnabled(!readonly);

    versionLabel = new Label(group, SWT.NONE);
    versionLabel.setText(Messages.wizardProjectPageArtifactParentVersion);

    parentVersionCombo = new Combo(group, SWT.NONE);
    GridData gd_versionCombo = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    gd_versionCombo.widthHint = 150;
    parentVersionCombo.setLayoutData(gd_versionCombo);
    parentVersionCombo.setEnabled(!readonly);
    parentVersionCombo.setData("name", "parentVersionCombo"); //$NON-NLS-1$ //$NON-NLS-2$

    if(!readonly) {
      Composite buttonPanel = new Composite(group, SWT.NONE);
      RowLayout rowLayout = new RowLayout();
      rowLayout.pack = false;
      rowLayout.marginTop = 0;
      rowLayout.marginRight = 0;
      rowLayout.marginLeft = 0;
      rowLayout.marginBottom = 0;
      buttonPanel.setLayout(rowLayout);
      buttonPanel.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));

      parentBrowseButton = new Button(buttonPanel, SWT.NONE);
      parentBrowseButton.setText(Messages.wizardProjectPageArtifactParentBrowse);
      parentBrowseButton.setData("name", "parentBrowseButton"); //$NON-NLS-1$ //$NON-NLS-2$

      parentClearButton = new Button(buttonPanel, SWT.NONE);
      parentClearButton.setText(Messages.wizardProjectPageArtifactParentClear);
      parentClearButton.setData("name", "parentClearButton"); //$NON-NLS-1$ //$NON-NLS-2$
      parentClearButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> setValues("", "", "")));
    }
  }

  public Combo getGroupIdCombo() {
    return parentGroupIdCombo;
  }

  public Combo getArtifactIdCombo() {
    return this.parentArtifactIdCombo;
  }

  public Combo getVersionCombo() {
    return this.parentVersionCombo;
  }

  public void setWidthGroup(WidthGroup widthGroup) {
    widthGroup.addControl(this.groupIdLabel);
    widthGroup.addControl(this.artifactIdLabel);
    widthGroup.addControl(this.versionLabel);
  }

  /** Adds modify listener to the input controls. */
  public void addModifyListener(ModifyListener listener) {
    parentArtifactIdCombo.addModifyListener(listener);
    parentGroupIdCombo.addModifyListener(listener);
    parentVersionCombo.addModifyListener(listener);
  }

  /** Removes the listener from the input controls. */
  public void removeModifyListener(ModifyListener listener) {
    parentArtifactIdCombo.removeModifyListener(listener);
    parentGroupIdCombo.removeModifyListener(listener);
    parentVersionCombo.removeModifyListener(listener);
  }

  /** Adds selection listener to the "browse" button. */
  public void addBrowseButtonListener(SelectionListener listener) {
    if(parentBrowseButton != null) {
      parentBrowseButton.addSelectionListener(listener);
    }
  }

  /** Removes the selection listener from the "browse" button. */
  public void removeBrowseButtonListener(SelectionListener listener) {
    if(parentBrowseButton != null) {
      parentBrowseButton.removeSelectionListener(listener);
    }
  }

  /** Enables the "clear" button. */
  public void setClearButtonEnabled(boolean enabled) {
    if(parentClearButton != null) {
      parentClearButton.setEnabled(enabled);
    }
  }

  /** Sets the parent group values. */
  public void setValues(String groupId, String artifactId, String version) {
    parentGroupIdCombo.setText(groupId == null ? "" : groupId); //$NON-NLS-1$
    parentArtifactIdCombo.setText(artifactId == null ? "" : artifactId); //$NON-NLS-1$
    parentVersionCombo.setText(version == null ? "" : version); //$NON-NLS-1$
  }

  /** Updates a Maven model. */
  public void updateModel(Model model) {
    String groupId = parentGroupIdCombo.getText().trim();
    if(groupId.length() > 0) {
      Parent parent = new Parent();
      parent.setGroupId(groupId);
      parent.setArtifactId(parentArtifactIdCombo.getText().trim());
      parent.setVersion(parentVersionCombo.getText().trim());
      model.setParent(parent);
    }
  }

  /**
   * Validates the inputs to make sure all three fields are present in the same time, or none at all.
   */
  public boolean validate() {
    int parentCheck = 0;
    if(parentGroupIdCombo.getText().trim().length() > 0) {
      parentCheck++ ;
    }
    if(parentArtifactIdCombo.getText().trim().length() > 0) {
      parentCheck++ ;
    }
    if(parentVersionCombo.getText().trim().length() > 0) {
      parentCheck++ ;
    }

    setClearButtonEnabled(parentCheck > 0);

    return parentCheck == 0 || parentCheck == 3;
  }

}
