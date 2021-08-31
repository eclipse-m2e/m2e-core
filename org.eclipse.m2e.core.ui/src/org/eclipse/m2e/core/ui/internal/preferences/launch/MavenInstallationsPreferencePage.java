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

package org.eclipse.m2e.core.ui.internal.preferences.launch;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.launch.AbstractMavenRuntime;
import org.eclipse.m2e.core.internal.launch.MavenRuntimeManagerImpl;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * Maven installations preference page
 *
 * @author Eugene Kuleshov
 */
@SuppressWarnings("restriction")
public class MavenInstallationsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  final MavenRuntimeManagerImpl runtimeManager;

  final IMavenConfiguration mavenConfiguration;

  final IMaven maven;

  String defaultRuntime;

  List<AbstractMavenRuntime> runtimes;

  CheckboxTableViewer runtimesViewer;

  public MavenInstallationsPreferencePage() {
    setTitle(Messages.MavenInstallationsPreferencePage_title);

    this.runtimeManager = MavenPluginActivator.getDefault().getMavenRuntimeManager();
    this.mavenConfiguration = MavenPlugin.getMavenConfiguration();
    this.maven = MavenPlugin.getMaven();
  }

  @Override
  public void init(IWorkbench workbench) {
  }

  @Override
  protected void performDefaults() {
    runtimeManager.reset();
    defaultRuntime = runtimeManager.getRuntime(MavenRuntimeManagerImpl.DEFAULT).getName();
    runtimes = runtimeManager.getMavenRuntimes(false);

    runtimesViewer.setInput(runtimes);
    refreshRuntimesViewer();

    super.performDefaults();
  }

  @Override
  public boolean performOk() {
    runtimeManager.setRuntimes(runtimes);
    runtimeManager.setDefaultRuntime(getDefaultRuntime());
    return true;
  }

  @Override
  protected Control createContents(Composite parent) {

    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout(3, false);
    gridLayout.marginBottom = 5;
    gridLayout.marginRight = 5;
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    composite.setLayout(gridLayout);

    Label link = new Label(composite, SWT.NONE);
    link.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));
    link.setText(Messages.MavenInstallationsPreferencePage_link);

    createTable(composite);
    new Label(composite, SWT.NONE);

    defaultRuntime = runtimeManager.getRuntime(MavenRuntimeManagerImpl.DEFAULT).getName();
    runtimes = runtimeManager.getMavenRuntimes(false);

    runtimesViewer.setInput(runtimes);
    refreshRuntimesViewer();

    return composite;
  }

  private AbstractMavenRuntime getDefaultRuntime() {
    AbstractMavenRuntime embedded = null;
    for(AbstractMavenRuntime runtime : runtimes) {
      if(defaultRuntime.equals(runtime.getName())) {
        return runtime;
      } else if(MavenRuntimeManagerImpl.EMBEDDED.equals(runtime.getName())) {
        embedded = runtime;
      }
    }
    return embedded;
  }

  protected void refreshRuntimesViewer() {
    runtimesViewer.refresh(); // should listen on property changes instead?

    Object[] checkedElements = runtimesViewer.getCheckedElements();
    if(checkedElements == null || checkedElements.length == 0) {
      AbstractMavenRuntime runtime = getDefaultRuntime();
      runtimesViewer.setChecked(runtime, true);
      defaultRuntime = runtime.getName();
    }

    for(TableColumn column : runtimesViewer.getTable().getColumns()) {
      column.pack();
    }
  }

  protected AbstractMavenRuntime getSelectedMavenRuntime() {
    IStructuredSelection sel = (IStructuredSelection) runtimesViewer.getSelection();
    return (AbstractMavenRuntime) sel.getFirstElement();
  }

  private void createTable(Composite composite) {
    runtimesViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.FULL_SELECTION);

    runtimesViewer.setLabelProvider(new RuntimesLabelProvider());

    runtimesViewer.setContentProvider(new IStructuredContentProvider() {

      @Override
      public Object[] getElements(Object input) {
        if(input instanceof List<?>) {
          List<?> list = (List<?>) input;
          if(list.size() > 0) {
            return list.toArray(new AbstractMavenRuntime[list.size()]);
          }
        }
        return new Object[0];
      }

      @Override
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      @Override
      public void dispose() {
      }

    });

    Table table = runtimesViewer.getTable();
    table.setLinesVisible(true);
    table.setHeaderVisible(true);
    GridData gd_table = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 3);
    gd_table.heightHint = 151;
    gd_table.widthHint = 333;
    table.setLayoutData(gd_table);

    TableColumn tblclmnName = new TableColumn(table, SWT.NONE);
    tblclmnName.setWidth(100);
    tblclmnName.setText(Messages.MavenInstallationsPreferencePage_tblclmnName_text);

    TableColumn tblclmnDetails = new TableColumn(table, SWT.NONE);
    tblclmnDetails.setWidth(100);
    tblclmnDetails.setText(Messages.MavenInstallationsPreferencePage_tblclmnDetails_text);

    Button addButton = new Button(composite, SWT.NONE);
    addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    addButton.setText(Messages.MavenInstallationsPreferencePage_btnAdd);
    addButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      MavenInstallationWizard wizard = new MavenInstallationWizard(getForbiddenNames(null));
      WizardDialog dialog = new WizardDialog(getShell(), wizard);
      if(dialog.open() == Window.OK) {
        runtimes.add(wizard.getResult());
        refreshRuntimesViewer();
      }
    }));

    final Button editButton = new Button(composite, SWT.NONE);
    editButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    editButton.setEnabled(false);
    editButton.setText(Messages.MavenInstallationsPreferencePage_btnEdit);
    editButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      AbstractMavenRuntime runtime = getSelectedMavenRuntime();
      MavenInstallationWizard wizard = new MavenInstallationWizard(runtime, getForbiddenNames(runtime));
      WizardDialog dialog = new WizardDialog(getShell(), wizard);
      if(dialog.open() == Window.OK) {
        AbstractMavenRuntime updatedRuntime = wizard.getResult();
        for(int i = 0; i < runtimes.size(); i++ ) {
          if(runtime == runtimes.get(i)) {
            runtimes.set(i, updatedRuntime);
            break;
          }
        }
        refreshRuntimesViewer();
      }
    }));

    final Button removeButton = new Button(composite, SWT.NONE);
    removeButton.setEnabled(false);
    removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    removeButton.setText(Messages.MavenInstallationsPreferencePage_btnRemove);
    removeButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      AbstractMavenRuntime runtime = getSelectedMavenRuntime();
      runtimes.remove(runtime);
      refreshRuntimesViewer();
    }));

    runtimesViewer.addSelectionChangedListener(event -> {
      if(runtimesViewer.getSelection() instanceof IStructuredSelection) {
        AbstractMavenRuntime runtime = getSelectedMavenRuntime();
        boolean isEnabled = runtime != null && runtime.isEditable();
        removeButton.setEnabled(isEnabled);
        editButton.setEnabled(isEnabled);
      }
    });

    runtimesViewer.addCheckStateListener(event -> setCheckedRuntime((AbstractMavenRuntime) event.getElement()));
    Label noteLabel = new Label(composite, SWT.WRAP);
    GridData noteLabelData = new GridData(SWT.FILL, SWT.TOP, false, false, 2, 1);
    noteLabelData.widthHint = 100;

    noteLabel.setLayoutData(noteLabelData);
    noteLabel.setText(Messages.MavenInstallationsPreferencePage_lblNote);
  }

  protected Set<String> getForbiddenNames(AbstractMavenRuntime runtime) {
    Set<String> names = new HashSet<>();
    for(AbstractMavenRuntime other : runtimes) {
      if(other != runtime) {
        names.add(other.getName());
      }
    }
    return names;
  }

  protected void setCheckedRuntime(AbstractMavenRuntime runtime) {
    runtimesViewer.setAllChecked(false);
    if(runtime == null || !runtime.isAvailable()) {
      runtime = getDefaultRuntime();
    } else {
      defaultRuntime = runtime.getName();
    }
    runtimesViewer.setChecked(runtime, true);
  }

  static class RuntimesLabelProvider implements ITableLabelProvider, IColorProvider {

    @Override
    public String getColumnText(Object element, int columnIndex) {
      AbstractMavenRuntime runtime = (AbstractMavenRuntime) element;
      switch(columnIndex) {
        case 0:
          return !runtime.isLegacy() ? runtime.getName() : null;
        case 1:
          StringBuilder sb = new StringBuilder();
          if(!runtime.isAvailable()) {
            sb.append(Messages.MavenInstallationsPreferencePage_runtimeUnavailable);
          }
          sb.append(runtime.toString());
          return sb.toString();
      }
      return null;
    }

    @Override
    public Image getColumnImage(Object element, int columnIndex) {
      if(columnIndex == 1 && !((AbstractMavenRuntime) element).isAvailable()) {
        return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
      }
      return null;
    }

    @Override
    public Color getBackground(Object element) {
      return null;
    }

    @Override
    public Color getForeground(Object element) {
      AbstractMavenRuntime runtime = (AbstractMavenRuntime) element;
      if(!runtime.isEditable()) {
        return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
      }
      return null;
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    @Override
    public void addListener(ILabelProviderListener listener) {
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
    }
  }

}
