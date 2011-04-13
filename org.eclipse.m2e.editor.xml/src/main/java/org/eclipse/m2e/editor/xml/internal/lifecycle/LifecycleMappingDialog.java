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
package org.eclipse.m2e.editor.xml.internal.lifecycle;

import java.lang.reflect.InvocationTargetException;

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
import org.eclipse.swt.layout.GridData;
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


public class LifecycleMappingDialog extends Dialog implements ISelectionChangedListener {

  private PomHierarchyComposite pomComposite;

  private CLabel status;

  private IFile pomFile;

  private IMavenProjectFacade facade;

  private String pluginGroupId;

  private String pluginArtifactId;

  private String pluginVersion;

  private String goal;

  private MavenProject pluginProject;

  public LifecycleMappingDialog(Shell parentShell, IFile pom, String pluginGroupId, String pluginArtifactId,
      String pluginVersion, String goal) {
    super(parentShell);
    facade = MavenPlugin.getMavenProjectRegistry().create(pom, true, new NullProgressMonitor());
    this.pluginGroupId = pluginGroupId;
    this.pluginArtifactId = pluginArtifactId;
    this.pluginVersion = pluginVersion;
    this.goal = goal;
  }

  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(NLS.bind("Ignore {0}", goal));
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = (Composite) super.createDialogArea(parent);
    Label label = new Label(container, SWT.NONE);
    label.setText("Select location to place ignore");
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

    pluginProject = locatePlugin();
    pomComposite.setSelection(new StructuredSelection(pluginProject));
    return container;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    super.createButtonsForButtonBar(parent);
    getButton(OK).setEnabled(false);
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
      status.setText("Non-workspace pom");
      status.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
    } else if(project.equals(pluginProject)) {
      status.setText("Plugin definition in selected pom.");
      status.setImage(null);
    } else {
      status.setText("");
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
    for (MavenProject project : pomComposite.getHierarchy()) {
      if (project.getOriginalModel().getBuild() != null) {
        for (Plugin plugin : project.getOriginalModel().getBuild().getPlugins()) {
          if(plugin.getGroupId().equals(pluginGroupId) && plugin.getArtifactId().equals(pluginArtifactId)
              && (plugin.getVersion() == null || pluginVersion.equals(plugin.getVersion()))) {
            return project;
          }
        }
      }
    }
    return null;
  }
}
