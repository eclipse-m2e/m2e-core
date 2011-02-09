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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * Maven project preference page
 *
 * @author Eugene Kuleshov
 */
public class MavenProjectPreferencePage extends PropertyPage {

  private Button resolveWorspaceProjectsButton;
//  private Button includeModulesButton;
  
  private Text activeProfilesText;

  public MavenProjectPreferencePage() {
    setTitle(Messages.MavenProjectPreferencePage_title);
  }

  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));
    composite.setLayoutData(new GridData(GridData.FILL));

    Label profilesLabel = new Label(composite, SWT.NONE);
    profilesLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
    profilesLabel.setText(Messages.MavenProjectPreferencePage_lblProfiles);

    activeProfilesText = new Text(composite, SWT.BORDER);
    activeProfilesText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

    resolveWorspaceProjectsButton = new Button(composite, SWT.CHECK);
    GridData resolveWorspaceProjectsButtonData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    resolveWorspaceProjectsButton.setLayoutData(resolveWorspaceProjectsButtonData);
    resolveWorspaceProjectsButton.setText(Messages.MavenProjectPreferencePage_btnResolve);

//    includeModulesButton = new Button(composite, SWT.CHECK);
//    GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
//    gd.verticalIndent = 15;
//    includeModulesButton.setLayoutData(gd);
//    includeModulesButton.setText("Include &Modules");
//
//    Text includeModulesText = new Text(composite, SWT.WRAP | SWT.READ_ONLY | SWT.MULTI);
//    includeModulesText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
//    GridData gd_includeModulesText = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1);
//    gd_includeModulesText.horizontalIndent = 15;
//    gd_includeModulesText.verticalIndent = 0;
//    gd_includeModulesText.widthHint = 300;
//    gd_includeModulesText.heightHint = 120;
//    includeModulesText.setLayoutData(gd_includeModulesText);
//    includeModulesText.setBackground(composite.getBackground());
//    includeModulesText.setText("When enabled, dependencies from all nested modules "
//        + "are added to the \"Maven Dependencies\" container and "
//        + "source folders from nested modules are added to the current "
//        + "project build path (use \"Update Sources\" action)");

    init(getResolverConfiguration());
    
    return composite;
  }

  protected void performDefaults() {
    init(new ResolverConfiguration());
  }
  
  private void init(ResolverConfiguration configuration) {

    resolveWorspaceProjectsButton.setSelection(configuration.shouldResolveWorkspaceProjects());
//    includeModulesButton.setSelection(configuration.shouldIncludeModules());
    activeProfilesText.setText(configuration.getActiveProfiles());
  }

  public boolean performOk() {
    final IProject project = getProject();
    try {
      if(!project.isAccessible() || !project.hasNature(IMavenConstants.NATURE_ID)) {
        return true;
      }
    } catch(CoreException ex) {
      MavenLogger.log(ex);
      return false;
    }

    final ResolverConfiguration configuration = getResolverConfiguration();
    if(configuration.getActiveProfiles().equals(activeProfilesText.getText()) &&
//        configuration.shouldIncludeModules()==includeModulesButton.getSelection() &&
        configuration.shouldResolveWorkspaceProjects()==resolveWorspaceProjectsButton.getSelection()) {
      return true;
    }
    
    configuration.setResolveWorkspaceProjects(resolveWorspaceProjectsButton.getSelection());
//    configuration.setIncludeModules(includeModulesButton.getSelection());
    configuration.setActiveProfiles(activeProfilesText.getText());
    
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    boolean isSet = projectManager.setResolverConfiguration(getProject(), configuration);
    if(isSet) {

        boolean res = MessageDialog.openQuestion(getShell(), Messages.MavenProjectPreferencePage_dialog_title, //
            Messages.MavenProjectPreferencePage_dialog_message);
        if(res) {
          final MavenPlugin plugin = MavenPlugin.getDefault();
          WorkspaceJob job = new WorkspaceJob(NLS.bind(Messages.MavenProjectPreferencePage_job, project.getName() )) {
            public IStatus runInWorkspace(IProgressMonitor monitor) {
              try {
                plugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);
              } catch(CoreException ex) {
                return ex.getStatus();
              }
              return Status.OK_STATUS;
            }
          };
          job.setRule(plugin.getProjectConfigurationManager().getRule());
          job.schedule();
        }

    }
    
    return isSet;
  }

  private ResolverConfiguration getResolverConfiguration() {
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    return projectManager.getResolverConfiguration(getProject());
  }

  private IProject getProject() {
    return (IProject) getElement();
  }

}

