/*******************************************************************************
 * Copyright (c) 2012-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.jboss.tools.maven.apt.ui.preferences;

import java.util.ArrayList;
import java.util.List;

import org.jboss.tools.maven.apt.MavenJdtAptPlugin;
import org.jboss.tools.maven.apt.preferences.AnnotationProcessingMode;
import org.jboss.tools.maven.apt.preferences.IPreferencesManager;
import org.jboss.tools.maven.apt.ui.MavenJdtAptUIPlugin;
import org.jboss.tools.maven.apt.ui.preferences.xpl.PropertyAndPreferencePage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;


public class AnnotationProcessingSettingsPage extends PropertyAndPreferencePage {

  public static final String PREF_ID = "org.jboss.tools.maven.apt.ui.preferences.AnnotationProcessingPreferencePage"; //$NON-NLS-1$

  public static final String PROP_ID = "org.jboss.tools.maven.apt.ui.propertyPages.AnnotationProcessingPropertyPage"; //$NON-NLS-1$

  private Button disableAptButton;

  private Button useJdtAptButton;

  private Button mavenExecutionButton;

  private Button disableAptReconcileButton;
  
  private IPreferencesManager preferencesManager;

  private AnnotationProcessingMode annotationProcessingMode;

  private AnnotationProcessingMode initialAnnotationProcessingMode;
  
  private boolean shouldEnableAptDuringReconcile;

  private boolean hasConfigChanged = false;

  private Group modeGroup;

  
  public AnnotationProcessingSettingsPage() {
    setPreferenceStore(MavenJdtAptUIPlugin.getDefault().getPreferenceStore());
    setTitle(PreferenceMessages.AnnotationProcessingSettingsPage_Title);
    preferencesManager = MavenJdtAptPlugin.getDefault().getPreferencesManager();
  }

  @Override
  protected Control createPreferenceContent(Composite parent) {
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    layout.marginHeight = 0;
    layout.marginWidth = 0;

    Composite composite = new Composite(parent, SWT.NONE);
    composite.setFont(parent.getFont());
    composite.setLayout(layout);

    initialAnnotationProcessingMode = preferencesManager.getAnnotationProcessorMode(getProject());
    annotationProcessingMode = initialAnnotationProcessingMode;
    shouldEnableAptDuringReconcile = preferencesManager.shouldEnableAnnotationProcessDuringReconcile(getProject());
   
    createModeGroup(composite);
    createOptionsGroup(composite);

    resetButtons();
    return composite;
  }

  private void createModeGroup(Composite composite) {

    modeGroup = new Group(composite, SWT.LEFT);
    GridLayout layout = new GridLayout();
    modeGroup.setLayout(layout);
    GridData data = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
    modeGroup.setLayoutData(data);
    
    useJdtAptButton = createRadioButton(modeGroup,
        PreferenceMessages.AnnotationProcessingSettingsPage_Jdt_Apt_Mode_Label, 
        AnnotationProcessingMode.jdt_apt);

    mavenExecutionButton = createRadioButton(modeGroup,
        PreferenceMessages.AnnotationProcessingSettingsPage_Maven_Execution_Mode,
        AnnotationProcessingMode.maven_execution);

    disableAptButton = createRadioButton(modeGroup,
        PreferenceMessages.AnnotationProcessingSettingsPage_Disabled_Mode_Label, 
        AnnotationProcessingMode.disabled);
    
  }

  
  private void createOptionsGroup(Composite composite) {

    Group optionsGrp = new Group(composite, SWT.LEFT);
    GridLayout layout = new GridLayout();
    optionsGrp.setLayout(layout);
    GridData data = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
    optionsGrp.setLayoutData(data);
    optionsGrp.setText(PreferenceMessages.AnnotationProcessingSettingsPage_Other_Options);


    disableAptReconcileButton = new Button(optionsGrp, SWT.CHECK | SWT.LEFT);
    disableAptReconcileButton.setText(PreferenceMessages.AnnotationProcessingSettingsPage_Disable_APT_Processing);
    disableAptReconcileButton
        .setToolTipText(PreferenceMessages.AnnotationProcessingSettingsPage_Disable_APT_Processing_Tooltip);
    disableAptReconcileButton.addSelectionListener(new SelectionAdapter() {

      public void widgetSelected(SelectionEvent e) {
        hasConfigChanged = true;
      }

    });
  }

  
  /**
   * @return
   */
  private String getModeGroupTitle() {
    StringBuilder title = new StringBuilder(PreferenceMessages.AnnotationProcessingSettingsPage_Select_Annotation_Processing_Mode);
    IProject p = getProject();
    AnnotationProcessingMode pomMode = preferencesManager.getPomAnnotationProcessorMode(p);
    if (p!=null && !useProjectSettings() && pomMode != null) {
      title.append(" (<m2e.apt.activation> currently set in pom.xml)");
    }
    return title.toString();
  }

  @Override
  protected boolean hasProjectSpecificOptions(IProject project) {
    return preferencesManager.hasSpecificProjectSettings(project);
  }

  @Override
  protected String getPreferencePageID() {
    return PREF_ID;
  }

  @Override
  protected String getPropertyPageID() {
    return PROP_ID;
  }

  protected Button createRadioButton(Composite parent, String label, final AnnotationProcessingMode newMode) {
    Button button = new Button(parent, SWT.RADIO | SWT.LEFT);
    button.setText(label);
    button.addSelectionListener(new SelectionAdapter() {
      @SuppressWarnings("synthetic-access")
      public void widgetSelected(SelectionEvent e) {
        if (!newMode.equals(annotationProcessingMode)) {
          annotationProcessingMode = newMode;
          resetButtons();
          hasConfigChanged = true;
        }
      }
    });
    return button;
  }

  @Override
  public boolean performOk() {
    IProject project = getProject();
    boolean useProjectSettings = useProjectSettings();
    if(!useProjectSettings) {
      preferencesManager.clearSpecificSettings(project);
      project = null;
    }
    

    if (hasConfigChanged) {
      preferencesManager.setAnnotationProcessDuringReconcile(project, !disableAptReconcileButton.getSelection());
      preferencesManager.setAnnotationProcessorMode(project, annotationProcessingMode);
      
      boolean res = MessageDialog.openQuestion(getShell(), "Maven Annotation Processing Settings", //
          "m2e-apt settings have changed. Do you want to update project configuration?");
      
      if(res) {
        updateImpactedProjects();
        hasConfigChanged = false;
      }
    }
    
    return super.performOk();
  }

  @Override
  protected void enableProjectSpecificSettings(boolean useProjectSpecificSettings) {
    super.enableProjectSpecificSettings(useProjectSpecificSettings);
    annotationProcessingMode = null;
    //reload
    if (!useProjectSpecificSettings && getProject() != null) {
      annotationProcessingMode  = preferencesManager.getPomAnnotationProcessorMode(getProject()); 
      shouldEnableAptDuringReconcile = preferencesManager.shouldEnableAnnotationProcessDuringReconcile(getProject());
    }
    if (annotationProcessingMode == null) {
      annotationProcessingMode = preferencesManager.getAnnotationProcessorMode(getProject());
      shouldEnableAptDuringReconcile = preferencesManager.shouldEnableAnnotationProcessDuringReconcile(getProject());
    }
    resetButtons();
    hasConfigChanged = true;
  }

  private void resetButtons() {
    useJdtAptButton.setSelection(annotationProcessingMode == AnnotationProcessingMode.jdt_apt);
    disableAptButton.setSelection(annotationProcessingMode == AnnotationProcessingMode.disabled);
    mavenExecutionButton.setSelection(annotationProcessingMode == AnnotationProcessingMode.maven_execution);
    disableAptReconcileButton.setSelection(!shouldEnableAptDuringReconcile);
    modeGroup.setText(getModeGroupTitle());
  }
  
  /**
   * Update the configuration of maven projects impacted by the configuration change.
   */
  private void updateImpactedProjects() {

    final IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();

    final List<IMavenProjectFacade> facades = getImpactedProjects(projectManager);

    if(facades.isEmpty())
      return;
    
    final IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();

    WorkspaceJob job = new WorkspaceJob("Updating maven projects") {
  
      public IStatus runInWorkspace(IProgressMonitor monitor) {
        try {
          SubMonitor progress = SubMonitor.convert(monitor, "Updating Maven projects", 100);
          SubMonitor subProgress = SubMonitor.convert(progress.newChild(5), facades.size() * 100);
          //projectManager.sortProjects(facades, progress.newChild(5));
          for(IMavenProjectFacade facade : facades) {
            if(progress.isCanceled()) {
              throw new OperationCanceledException();
            }
            IProject project = facade.getProject();
            subProgress.subTask("Updating configuration for " + project.getName());

            configurationManager.updateProjectConfiguration(project, subProgress);
          }

        } catch(CoreException ex) {
          return ex.getStatus();
        }
        return Status.OK_STATUS;
      }
    };
    job.setRule(configurationManager.getRule());
    job.schedule();
  }  
  

  /**
   * Returns the list of Maven projects impacted by the configuration change.
   * 
   * @param projectManager
   * @return
   */
  private List<IMavenProjectFacade> getImpactedProjects(final IMavenProjectRegistry projectManager) {
    final List<IMavenProjectFacade> facades = new ArrayList<IMavenProjectFacade>();
    IProject project = getProject();
    if(project == null) {
      //Get all workspace projects that might be impacted by the configuration change 
      for(IMavenProjectFacade facade : projectManager.getProjects()) {
        if(isImpacted(facade.getProject())) {
          facades.add(facade);
        }
      }
    } else {
      facades.add(projectManager.getProject(project));
    }
    return facades;
  }

  /**
   * Checks if the project is impacted by the configuration change.
   * 
   * @param facade
   * @return
   */
  private boolean isImpacted(IProject project) {
    //TODO find more fine grained criteria to identify apt-enabled/eligible projects
    return !preferencesManager.hasSpecificProjectSettings(project);
  }  
  
}
