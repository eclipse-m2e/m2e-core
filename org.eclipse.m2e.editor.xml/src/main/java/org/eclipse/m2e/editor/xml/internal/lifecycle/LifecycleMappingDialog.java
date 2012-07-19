/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Andrew Eisenberg - Work on Bug 350414
 *******************************************************************************/

package org.eclipse.m2e.editor.xml.internal.lifecycle;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.M2EUtils;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.components.PomHierarchyComposite;
import org.eclipse.m2e.editor.xml.internal.Messages;


@SuppressWarnings("restriction")
public class LifecycleMappingDialog extends Dialog implements ISelectionChangedListener {

  private PomHierarchyComposite pomComposite;

  private CLabel status;

  private IFile pomFile;

  private IMavenProjectFacade facade;

  private String pluginGroupId;

  private String pluginArtifactId;

  // TODO Unused...consider deleting
//  private String pluginVersion;

  private String goal;

  private MavenProject pluginProject;

  private boolean workspaceSettings = false;

  public LifecycleMappingDialog(Shell parentShell, IFile pom, String pluginGroupId, String pluginArtifactId,
      String pluginVersion, String goal) {
    super(parentShell);
    facade = MavenPlugin.getMavenProjectRegistry().create(pom, true, new NullProgressMonitor());
    this.pluginGroupId = pluginGroupId;
    this.pluginArtifactId = pluginArtifactId;
//    this.pluginVersion = pluginVersion;
    this.goal = goal;
  }

  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(NLS.bind(Messages.LifecycleMappingDialog_Ignore, goal));
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = (Composite) super.createDialogArea(parent);
    Label label = new Label(container, SWT.NONE);
    label.setText(Messages.LifecycleMappingDialog_LocationToIgnore);
    pomComposite = new PomHierarchyComposite(container, SWT.BORDER);
    pomComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    pomComposite.addSelectionChangedListener(this);
    pomComposite.computeHeirarchy(facade, new IRunnableContext() {

      public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable)
          throws InvocationTargetException, InterruptedException {
        runnable.run(new NullProgressMonitor());
      }
    });
    status = new CLabel(container, SWT.WRAP);
    status.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));

    // separator
    new Label(container, SWT.NONE);
    
    final Button workspaceSettingsButton = new Button(container, SWT.CHECK);
    workspaceSettingsButton.setText(Messages.LifecycleMappingDialog_UseWorkspaceSettings);
    workspaceSettingsButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        pomComposite.setEnabled(!workspaceSettingsButton.getSelection());
        workspaceSettings = workspaceSettingsButton.getSelection();
      }
    });
    new Label(container, SWT.NONE).
    setText(Messages.LifecycleMappingDialog_UseWorkspaceSettingsDesc);
    
    pluginProject = locatePlugin();
    return container;
  }
  
  public boolean useWorkspaceSettings() {
    return workspaceSettings;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    super.createButtonsForButtonBar(parent);
    getButton(OK).setEnabled(false);
    //350439
    //set selection here, because we listen on changes and update the ok button.
    //but the button is not created until super is called here..
    if(pluginProject != null) {
      pomComposite.setSelection(new StructuredSelection(pluginProject));
    }
  }

  public void selectionChanged(SelectionChangedEvent event) {
    MavenProject project = pomComposite.fromSelection();
    if(getButton(OK) != null) {
      getButton(OK).setEnabled(project != null && project.getFile() != null);
    }
    updateStatus(project);
  }

  private void updateStatus(MavenProject project) {
    if(project.getFile() == null) {
      status.setText(Messages.LifecycleMappingDialog_NonWorkspacePom);
      status.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
    } else if(project.equals(pluginProject)) {
      status.setText(Messages.LifecycleMappingDialog_PluginDefinitionInSelectedPom);
      status.setImage(null);
    } else {
      status.setText(""); //$NON-NLS-1$
      status.setImage(null);
    }
  }

  @Override
  protected void okPressed() {
    pomFile = M2EUtils.getPomFile(pomComposite.fromSelection());
    super.okPressed();
  }

  /*
   * Should only be called after dialog has closed with OK return code 
   */
  public IFile getPomFile() {
    return pomFile;
  }

  private MavenProject locatePlugin() {
    MavenProject project = facade.getMavenProject(); // if we got here, facade.getMavenProject cannot be null

    Plugin plugin = project.getPlugin(pluginGroupId + ":" + pluginArtifactId); //$NON-NLS-1$

    if(plugin == null) {
      return null; // can't really happy
    }

    InputLocation location = plugin.getLocation(""); //$NON-NLS-1$

    if(location == null || location.getSource() == null || location.getSource().getLocation() == null) {
      // that's odd. where does this come from???
      return null;
    }

    File basedir = new File(location.getSource().getLocation()).getParentFile(); // should be canonical file already
    for(MavenProject other : pomComposite.getHierarchy()) {
      if(basedir.equals(other.getBasedir())) {
        return other;
      }
    }

    return null;
  }
}
