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

package org.eclipse.m2e.core.ui.internal.preferences;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IMessageProvider;
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
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import org.apache.maven.cli.MavenCli;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsProblem;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.MavenRuntime;
import org.eclipse.m2e.core.embedder.MavenRuntimeManager;
import org.eclipse.m2e.core.index.IndexManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * Maven installations preference page
 * 
 * @author Eugene Kuleshov
 */
public class MavenSettingsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
  private static final Logger log = LoggerFactory.getLogger(MavenSettingsPreferencePage.class);

  final MavenRuntimeManager runtimeManager;
  
  final IMavenConfiguration mavenConfiguration;
  
  final IMaven maven;

  MavenRuntime defaultRuntime;

  Text userSettingsText;

  Text localRepositoryText;

  boolean dirty = false;

  private Link userSettingsLink;

  public MavenSettingsPreferencePage() {
    setTitle(org.eclipse.m2e.core.ui.internal.Messages.MavenSettingsPreferencePage_title);

    this.runtimeManager = MavenPlugin.getMavenRuntimeManager();
    this.mavenConfiguration = MavenPlugin.getMavenConfiguration();
    this.maven = MavenPlugin.getMaven();
  }

  public void init(IWorkbench workbench) {
  }

  
  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
   */
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if(visible){
      updateLocalRepository();
    }
  }

  protected void performDefaults() {
    userSettingsText.setText(MavenCli.DEFAULT_USER_SETTINGS_FILE.getAbsolutePath());
    setDirty(true);
    updateLocalRepository();
    super.performDefaults();
  }

  protected void updateSettings(final boolean updateMavenDependencies){
    final String userSettings = getUserSettings();
    
    new Job(org.eclipse.m2e.core.ui.internal.Messages.MavenSettingsPreferencePage_job_updating) {
      protected IStatus run(IProgressMonitor monitor) {
        try {
          final File localRepositoryDir = new File(maven.getLocalRepository().getBasedir());
          if(userSettings.length() > 0) {
            mavenConfiguration.setUserSettingsFile(userSettings);
          } else {
            mavenConfiguration.setUserSettingsFile(null);
          }

          File newRepositoryDir = new File(maven.getLocalRepository().getBasedir());
          if(!newRepositoryDir.equals(localRepositoryDir)) {
            IndexManager indexManager = MavenPlugin.getIndexManager();
            indexManager.getWorkspaceIndex().updateIndex(true, monitor);
          }
          if(updateMavenDependencies){
            IMavenProjectFacade[] projects = MavenPlugin.getMavenProjectRegistry().getProjects();
            ArrayList<IProject> allProjects = new ArrayList<IProject>();
            if(projects != null){
              MavenPlugin.getMaven().reloadSettings();
              SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, projects.length);
              for(int i=0;i<projects.length;i++){
                subMonitor.beginTask(NLS.bind(org.eclipse.m2e.core.ui.internal.Messages.MavenSettingsPreferencePage_task_updating, projects[i].getProject().getName()), 1);
                allProjects.add(projects[i].getProject());
              }
              MavenPlugin.getMavenProjectRegistry()
                  .refresh(
                      new MavenUpdateRequest(allProjects.toArray(new IProject[] {}), mavenConfiguration.isOffline(),
                          true));
              subMonitor.done();
            }
          }
          return Status.OK_STATUS;
        } catch (CoreException e) {
          log.error(e.getMessage(), e);
          return e.getStatus();
        }
      }
    }.schedule();
  }
  
  protected void performApply() {
    if(dirty){
      updateSettings(false);
    }
  }
  
  public boolean performOk() {
    if (dirty) {
      updateSettings(false);
    }
    return true;
  }
  
  public void setDirty(boolean dirty){
    this.dirty = dirty;
  }
  
  public boolean isDirty(){
    return this.dirty;
  }

  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout(4, false);
    gridLayout.marginBottom = 5;
    gridLayout.marginRight = 5;
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    composite.setLayout(gridLayout);

    createUserSettings(composite);
    Label localRepositoryLabel = new Label(composite, SWT.NONE);
    GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1);
    gd.verticalIndent=25;
    localRepositoryLabel.setLayoutData(gd);
    localRepositoryLabel.setText(org.eclipse.m2e.core.ui.internal.Messages.MavenSettingsPreferencePage_lblLocal);
    
    localRepositoryText = new Text(composite, SWT.READ_ONLY|SWT.BORDER);
    localRepositoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
    localRepositoryText.setData("name", "localRepositoryText"); //$NON-NLS-1$ //$NON-NLS-2$
    localRepositoryText.setEditable(false);
    Button reindexButton = new Button(composite, SWT.NONE);
    reindexButton.setLayoutData(new GridData(SWT.FILL, SWT.RIGHT, false, false, 1, 1));
    reindexButton.setText(Messages.preferencesReindexButton);
    reindexButton.addSelectionListener(new SelectionAdapter(){

      /* (non-Javadoc)
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      public void widgetSelected(SelectionEvent e) {
        new WorkspaceJob(org.eclipse.m2e.core.ui.internal.Messages.MavenSettingsPreferencePage_job_indexing) {
            public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
            IndexManager indexManager = MavenPlugin.getIndexManager();
              indexManager.getWorkspaceIndex().updateIndex(true, monitor);
              return Status.OK_STATUS;
            }
         }.schedule();
      }
    });
    defaultRuntime = runtimeManager.getDefaultRuntime();

    String userSettings = mavenConfiguration.getUserSettingsFile();
    if(userSettings == null || userSettings.length() == 0) {
      userSettingsText.setText(MavenCli.DEFAULT_USER_SETTINGS_FILE.getAbsolutePath());
    } else {
      userSettingsText.setText(userSettings);
    }

    checkSettings();
    updateLocalRepository();

    userSettingsText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent modifyevent) {
        updateLocalRepository();
        checkSettings();
        setDirty(true);
      }
    });

    return composite;
  }
  
  public void updateSettingsLink(boolean active){
    String text = org.eclipse.m2e.core.ui.internal.Messages.MavenSettingsPreferencePage_link1;
    if(active){
      text = org.eclipse.m2e.core.ui.internal.Messages.MavenSettingsPreferencePage_link2;
    }
    userSettingsLink.setText(text);
  }
  /**
   * @param composite
   */
  private void createUserSettings(Composite composite) {

    userSettingsLink = new Link(composite, SWT.NONE);
    userSettingsLink.setData("name", "userSettingsLink"); //$NON-NLS-1$ //$NON-NLS-2$
    userSettingsLink.setText(org.eclipse.m2e.core.ui.internal.Messages.MavenSettingsPreferencePage_link2);
    userSettingsLink.setToolTipText(org.eclipse.m2e.core.ui.internal.Messages.MavenSettingsPreferencePage_link_tooltip);
    GridData gd_userSettingsLabel = new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1);
    
    gd_userSettingsLabel.verticalIndent = 15;
    userSettingsLink.setLayoutData(gd_userSettingsLabel);
    userSettingsLink.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        String userSettings = getUserSettings();
        if(userSettings.length() == 0) {
          userSettings = MavenCli.DEFAULT_USER_SETTINGS_FILE.getAbsolutePath();
        }
        openEditor(userSettings);
      }
    });
    userSettingsText = new Text(composite, SWT.BORDER);
    userSettingsText.setData("name", "userSettingsText"); //$NON-NLS-1$ //$NON-NLS-2$
    GridData gd_userSettingsText = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
    gd_userSettingsText.verticalIndent = 5;
    gd_userSettingsText.widthHint = 100;
    userSettingsText.setLayoutData(gd_userSettingsText);

    Button userSettingsBrowseButton = new Button(composite, SWT.NONE);
    GridData gd_userSettingsBrowseButton = new GridData(SWT.FILL, SWT.RIGHT, false, false, 1, 1);
   
    userSettingsBrowseButton.setLayoutData(gd_userSettingsBrowseButton);
    userSettingsBrowseButton.setText(org.eclipse.m2e.core.ui.internal.Messages.MavenSettingsPreferencePage_btnBrowse);
    userSettingsBrowseButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
        if(getUserSettings().length() > 0) {
          dialog.setFileName(getUserSettings());
        }
        String file = dialog.open();
        if(file != null) {
          file = file.trim();
          if(file.length() > 0) {
            userSettingsText.setText(file);
            updateLocalRepository();
            checkSettings();
          }
        }
      }
    });
    
    Button updateSettings = new Button(composite, SWT.NONE);
    GridData gd = new GridData(SWT.FILL, SWT.LEFT, false, false, 1, 1);
    updateSettings.setText(org.eclipse.m2e.core.ui.internal.Messages.MavenSettingsPreferencePage_btnUpdate);
    updateSettings.addSelectionListener(new SelectionAdapter(){
      public void widgetSelected(SelectionEvent e){
        updateSettings(true);
      }
    });
  }

  protected void updateLocalRepository() {
    final String userSettings = getUserSettings();
    String globalSettings = runtimeManager.getGlobalSettingsFile();
    try {
      Settings settings = maven.buildSettings(globalSettings, userSettings);
      String localRepository = settings.getLocalRepository();
      if(localRepository == null){
        localRepository = RepositorySystem.defaultUserLocalRepository.getAbsolutePath();
      }
      if(!localRepositoryText.isDisposed()) {
        localRepositoryText.setText(localRepository == null ? "" : localRepository); //$NON-NLS-1$
      }
    } catch (CoreException e) {
      setMessage(e.getMessage(), IMessageProvider.ERROR);
    }
  }

  protected void checkSettings() {
    setErrorMessage(null);
    setMessage(null);
    boolean fileExists = false;
    String userSettings = getUserSettings();
    if(userSettings != null && userSettings.length() > 0) {
      File userSettingsFile = new File(userSettings);
      if(!userSettingsFile.exists()) {
        setMessage(org.eclipse.m2e.core.ui.internal.Messages.MavenSettingsPreferencePage_error_missing, IMessageProvider.WARNING);
        userSettings = null;
        
      } else {
        fileExists = true;
      }
      
    } else {
      userSettings = null;
    }
    updateSettingsLink(fileExists);
    List<SettingsProblem> result = maven.validateSettings(userSettings);
    if(result.size() > 0) {
      setMessage(NLS.bind(org.eclipse.m2e.core.ui.internal.Messages.MavenSettingsPreferencePage_error_parse, result.get(0).getMessage()), IMessageProvider.WARNING);
    }
  }
  
  void openEditor(final String fileName) {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    IWorkbenchPage page = window.getActivePage();

    IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor("settings.xml"); //$NON-NLS-1$

    File file = new File(fileName);
    IEditorInput input = null;
    try {
      //class implementing editor input for external file has been renamed in eclipse 3.3, hence reflection
      Class javaInput = null;
      try {
        javaInput = Class.forName("org.eclipse.ui.internal.editors.text.JavaFileEditorInput"); //$NON-NLS-1$
        Constructor cons = javaInput.getConstructor(new Class[] {File.class});
        input = (IEditorInput) cons.newInstance(new Object[] {file});
      } catch(Exception e) {
        try {
          IFileStore fileStore = EFS.getLocalFileSystem().fromLocalFile(file);
          Class storeInput = Class.forName("org.eclipse.ui.ide.FileStoreEditorInput"); //$NON-NLS-1$
          Constructor cons = storeInput.getConstructor(new Class[] {IFileStore.class});
          input = (IEditorInput) cons.newInstance(new Object[] {fileStore});
        } catch(Exception ex) {
          //ignore...
        }
      }
      final IEditorPart editor = IDE.openEditor(page, input, desc.getId());
      editor.addPropertyListener(new IPropertyListener() {
        public void propertyChanged(Object source, int propId) {
          if(!editor.isDirty()) {
            log.info("Refreshing settings " + fileName);
          }
        }
      });

    } catch(PartInitException ex) {
      log.error(ex.getMessage(), ex);
    }
  }

  String getUserSettings() {
    return userSettingsText.getText().trim();
  }
}
