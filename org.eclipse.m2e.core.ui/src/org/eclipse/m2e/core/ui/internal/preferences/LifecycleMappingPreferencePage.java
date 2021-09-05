/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Andrew Eisenberg - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.preferences;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * This preferences page provides preferences for managing workspace-scoped lifecycle mappings
 */
@SuppressWarnings("restriction")
public class LifecycleMappingPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  private static final Logger log = LoggerFactory.getLogger(LifecycleMappingPreferencePage.class);

  private String mappingFilePath;

  private Text mappingFileTextBox;

  public LifecycleMappingPreferencePage() {
    setMessage(Messages.LifecycleMappingPreferencePage_this_message);
    setTitle(Messages.LifecycleMappingPreferencePage_LifecycleMapping);
  }

  // reset to default lifecycle mappings file
  @Override
  protected void performDefaults() {
    // set to default
    mappingFilePath = getDefaultLocation();
    mappingFileTextBox.setText(mappingFilePath);
    super.performDefaults();
  }

  @Override
  public boolean performOk() {
    try {
      MavenPlugin.getMavenConfiguration().setWorkspaceLifecycleMappingMetadataFile(mappingFilePath);
      LifecycleMappingFactory.getWorkspaceMetadata(true);
      return super.performOk();
    } catch(CoreException ex) {
      setErrorMessage(ex.getStatus().getMessage());
      return false;
    }
  }

  @Override
  public void init(IWorkbench workbench) {
    mappingFilePath = getCurrentLocation();
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout(1, false);
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    composite.setLayout(gridLayout);

    new Label(composite, SWT.WRAP).setText(Messages.LifecycleMappingPreferencePage_WorkspaceMappingsDescription);
    Button editLifecyclesButton = new Button(composite, SWT.PUSH);
    editLifecyclesButton.setText(Messages.LifecycleMappingPreferencePage_WorkspaceMappingsOpen);
    editLifecyclesButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      try {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchPage workbenchPage = workbench.getActiveWorkbenchWindow().getActivePage();
        IEditorDescriptor desc = workbench.getEditorRegistry()
            .getDefaultEditor(LifecycleMappingFactory.LIFECYCLE_MAPPING_METADATA_SOURCE_NAME);
        IEditorInput input = new FileStoreEditorInput(
            EFS.getLocalFileSystem().fromLocalFile(new File(mappingFilePath)));
        IDE.openEditor(workbenchPage, input, desc.getId());
      } catch(PartInitException ex) {
        log.error(ex.getMessage(), ex);
      }
    }));

    Button refreshLifecyclesButton = new Button(composite, SWT.NONE);
    refreshLifecyclesButton.addSelectionListener(
        SelectionListener.widgetSelectedAdapter(e -> LifecycleMappingFactory.getWorkspaceMetadata(true)));
    refreshLifecyclesButton.setText(Messages.LifecycleMappingPreferencePage_btnRefreshLifecycles_text);

    new Label(composite, SWT.NONE).setText(Messages.LifecycleMappingPreferencePage_ChangeLocation);

    mappingFileTextBox = new Text(composite, SWT.BORDER);
    mappingFileTextBox.setText(getCurrentLocation());
    mappingFileTextBox.addModifyListener(e -> mappingFilePath = mappingFileTextBox.getText());

    Button newFileButton = new Button(composite, SWT.PUSH);
    newFileButton.setText(Messages.LifecycleMappingPreferencePage_Browse);
    newFileButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      FileDialog dialog = new FileDialog(LifecycleMappingPreferencePage.this.getShell(), SWT.NONE);

      dialog.setText(Messages.LifecycleMappingPreferencePage_ChooseNewLocation);
      dialog.setFilterExtensions(new String[] {"*.xml", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
      String res = dialog.open();
      if(res == null) {
        return;
      }
      mappingFileTextBox.setText(dialog.getFilterPath() + "/" + dialog.getFileName()); //$NON-NLS-1$
    }));

    return composite;
  }

  private String getCurrentLocation() {
    return MavenPlugin.getMavenConfiguration().getWorkspaceLifecycleMappingMetadataFile();
  }

  private String getDefaultLocation() {
    IPath stateLocation = MavenPluginActivator.getDefault().getStateLocation();
    return stateLocation.append(LifecycleMappingFactory.LIFECYCLE_MAPPING_METADATA_SOURCE_NAME).toString();
  }
}
