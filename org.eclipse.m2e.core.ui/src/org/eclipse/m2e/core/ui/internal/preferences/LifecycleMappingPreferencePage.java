/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Andrew Eisenberg - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.preferences;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.internal.filesystem.local.LocalFile;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * This preferences page provides preferences for managing workspace-scoped lifecycle mappings
 */
@SuppressWarnings("restriction")
public class LifecycleMappingPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  private static final Logger log = LoggerFactory.getLogger(LifecycleMappingPreferencePage.class);
  
  private LifecycleMappingsViewer mappingsViewer;

  private String mappingFilePath;

  private Text mappingFileTextBox;
  
  public LifecycleMappingPreferencePage() {
    setTitle(Messages.LifecycleMappingPreferencePage_LifecycleMapping);
    mappingsViewer = new LifecycleMappingsViewer();
  }

  // reset to default lifecycle mappings file
  protected void performDefaults() {
    // set to default
    mappingFilePath = getDefaultLocation();
    mappingFileTextBox.setText(mappingFilePath);
    super.performDefaults();
  }

  public boolean performOk() {
    if (isValid()) {
      setCurrentLocation(mappingFilePath);
      return super.performOk();
    }
    return false;
  }
  
  public void init(IWorkbench workbench) {
    mappingFilePath = getCurrentLocation();
  }
  
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout(1, false);
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    composite.setLayout(gridLayout);

    new Label(composite, SWT.WRAP).setText(
        Messages.LifecycleMappingPreferencePage_WorkspaceMappingsDescription);
    Button editLifecyclesButton = new Button(composite, SWT.PUSH);
    editLifecyclesButton.setText(Messages.LifecycleMappingPreferencePage_WorkspaceMappingsOpen);
    editLifecyclesButton.addSelectionListener(new SelectionAdapter() {
     public void widgetSelected(SelectionEvent e) {
       try {
         IDE.openEditorOnFileStore(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), 
             new LocalFile(new File(mappingFilePath)));
       } catch(PartInitException ex) {
         log.error(ex.getMessage(), ex);
       }
     }
   });

    
    new Label(composite, SWT.NONE).setText(Messages.LifecycleMappingPreferencePage_ChangeLocation);

    mappingFileTextBox = new Text(composite, SWT.BORDER);
    mappingFileTextBox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
    mappingFileTextBox.setText(getCurrentLocation());
    mappingFileTextBox.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        // validate current file name
        mappingFilePath = mappingFileTextBox.getText();
        if (isValid()) {
          LifecycleMappingPreferencePage.this.setErrorMessage(null);
        } else {
          LifecycleMappingPreferencePage.this.setErrorMessage(NLS.bind(Messages.LifecycleMappingPreferencePage_FileDoesNotExist, mappingFilePath));
        }
      }
    });
    
    Button newFileButton = new Button(composite, SWT.PUSH);
    newFileButton.setText(Messages.LifecycleMappingPreferencePage_Browse);
    newFileButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        FileDialog dialog = new FileDialog(LifecycleMappingPreferencePage.this.getShell(), SWT.NONE);
        
        dialog.setText(Messages.LifecycleMappingPreferencePage_ChooseNewLocation); 
        dialog.setFilterExtensions(new String[] {"*.xml","*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
        String res = dialog.open();
        if (res == null) {
          return;
        }
        mappingFileTextBox.setText(dialog.getFilterPath() + "/" + dialog.getFileName()); //$NON-NLS-1$
      }
    });
    
    // TODO FIXADE Commwented out until we learn how to display workspace lifecylce mappings.
//    mappingsViewer.createContents(composite);
    
    
    return composite;
  }
  
  private String getCurrentLocation() {
    return MavenPluginActivator.getDefault().getMavenConfiguration().getWorkspaceMappingsFile();
  }
  
  private String getDefaultLocation() {
    return MavenPluginActivator.getDefault().getMavenConfiguration().getDefaultWorkspaceMappingsFile();
  }
  
  private void setCurrentLocation(String newLocation) {
    MavenPluginActivator.getDefault().getMavenConfiguration().setWorkspaceMappingsFile(newLocation);
  }

  /**
   * @param mappingFileTextBox
   */
  public boolean isValid() {
    File maybeFile = new File(mappingFilePath);
    return maybeFile.exists();
  }
  
}
