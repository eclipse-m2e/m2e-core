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

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * Simple GUI component which allows the user to choose between a workspace location and a user specified external
 * location. This component is mainly used for choosing the location at which to create a new project.
 */
public class MavenLocationComponent extends Composite {

  /** Radio button indicating whether the workspace location has been chosen. */
  protected Button inWorkspaceButton;

  /** Radio button indicating whether an external location has been chosen. */
  protected Button inExternalLocationButton;

  /** Text field for defining a user specified external location. */
  protected Combo locationCombo;

  /** Button allowing to choose a directory on the file system as the external location. */
  protected Button locationBrowseButton;

  protected ModifyListener modifyingListener;

  protected Label locationLabel;

  /**
   * Constructor. Constructs all the GUI components contained in this <code>Composite</code>. These components allow the
   * user to choose between a workspace location and a user specified external location.
   *
   * @param parent The widget which will be the parent of this component.
   * @param styles The widget style for this component.
   * @param modifyingListener Listener which is notified when the contents of this component change due to user input.
   */
  public MavenLocationComponent(final Composite parent, int styles) {
    super(parent, styles);

    GridLayout gridLayout = new GridLayout();
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    setLayout(gridLayout);

    Group locationGroup = new Group(this, SWT.NONE);
    locationGroup.setText(Messages.locationComponentLocation);
    locationGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 3, 1));
    GridLayout groupLayout = new GridLayout();
    groupLayout.numColumns = 3;
    groupLayout.marginLeft = 0;
    locationGroup.setLayout(groupLayout);

    GridData gridData = new GridData();
    gridData.horizontalSpan = 3;

    // first radio button
    inWorkspaceButton = new Button(locationGroup, SWT.RADIO);
    inWorkspaceButton.setText(Messages.locationComponentInWorkspace);
    inWorkspaceButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
    inWorkspaceButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
        boolean isEnabled = !inWorkspaceButton.getSelection();
        locationLabel.setEnabled(isEnabled);
        locationCombo.setEnabled(isEnabled);
        locationBrowseButton.setEnabled(isEnabled);
        if(modifyingListener != null) {
          modifyingListener.modifyText(null);
        }
      }
      ));

    // second radio button
    inExternalLocationButton = new Button(locationGroup, SWT.RADIO);
    inExternalLocationButton.setText(Messages.locationComponentAtExternal);
    inExternalLocationButton.setLayoutData(gridData);

    // choose directory
    locationLabel = new Label(locationGroup, SWT.NONE);
    GridData gd_locationLabel = new GridData();
    gd_locationLabel.horizontalIndent = 10;
    locationLabel.setLayoutData(gd_locationLabel);
    locationLabel.setText(Messages.locationComponentDirectory);

    locationCombo = new Combo(locationGroup, SWT.BORDER);
    locationCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    locationBrowseButton = new Button(locationGroup, SWT.PUSH);
    locationBrowseButton.setText(Messages.locationComponentBrowse);

    gridData = new GridData(SWT.FILL, SWT.DEFAULT, false, false);
    locationBrowseButton.setLayoutData(gridData);

    locationBrowseButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      DirectoryDialog dialog = new DirectoryDialog(getShell());
      dialog.setText(Messages.locationComponentSelectLocation);

      String path = locationCombo.getText();
      if(path.length() == 0) {
        path = ResourcesPlugin.getWorkspace().getRoot().getLocation().toPortableString();
      }
      dialog.setFilterPath(path);

      String selectedDir = dialog.open();
      if(selectedDir != null) {
        locationCombo.setText(selectedDir.trim());
      }
    }));

    inWorkspaceButton.setSelection(true);

    locationLabel.setEnabled(false);
    locationCombo.setEnabled(false);
    locationBrowseButton.setEnabled(false);
  }

  /**
   * Returns the path of the location chosen by the user. According to the user input, the path either points to the
   * workspace or to a valid user specified location on the filesystem.
   *
   * @return The path of the location chosen by the user. Is never <code>null</code>.
   */
  public IPath getLocationPath() {
    if(isInWorkspace()) {
      return Platform.getLocation();
    }
    return IPath.fromOSString(locationCombo.getText().trim());
  }

  /**
   * Returns whether the workspace has been chosen as the location to use.
   *
   * @return <code>true</code> if the workspace is chosen as the location to use, <code>false</code> if the specified
   *         external location is to be used.
   */
  public boolean isInWorkspace() {
    return inWorkspaceButton.getSelection();
  }

  public void setModifyingListener(ModifyListener modifyingListener) {
    this.modifyingListener = modifyingListener;
    locationCombo.addModifyListener(modifyingListener);
  }

  public Combo getLocationCombo() {
    return locationCombo;
  }

  @Override
  public void dispose() {
    super.dispose();
    if(modifyingListener != null) {
      locationCombo.removeModifyListener(modifyingListener);
    }
  }
}
