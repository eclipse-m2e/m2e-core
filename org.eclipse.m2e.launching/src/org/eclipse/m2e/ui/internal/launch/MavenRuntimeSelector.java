/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.ui.internal.launch;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PreferencesUtil;

import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.launch.AbstractMavenRuntime;
import org.eclipse.m2e.core.internal.launch.MavenEmbeddedRuntime;
import org.eclipse.m2e.core.internal.launch.MavenExternalRuntime;
import org.eclipse.m2e.core.internal.launch.MavenRuntimeManagerImpl;
import org.eclipse.m2e.core.internal.launch.MavenWorkspaceRuntime;


/**
 * @since 1.4
 */
@SuppressWarnings("restriction")
public class MavenRuntimeSelector extends Composite {

  ComboViewer runtimeComboViewer;

  private static MavenRuntimeManagerImpl getRuntimeManager() {
    return MavenPluginActivator.getDefault().getMavenRuntimeManager();
  }

  public MavenRuntimeSelector(final Composite mainComposite) {
    super(mainComposite, SWT.NONE);

    GridLayout gridLayout = new GridLayout(3, false);
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    setLayout(gridLayout);

    Label mavenRuntimeLabel = new Label(this, SWT.NONE);
    mavenRuntimeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    mavenRuntimeLabel.setText(org.eclipse.m2e.internal.launch.Messages.MavenLaunchMainTab_lblRuntime);

    runtimeComboViewer = new ComboViewer(this, SWT.BORDER | SWT.READ_ONLY);
    runtimeComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    runtimeComboViewer.setContentProvider(new IStructuredContentProvider() {

      public Object[] getElements(Object input) {
        return ((List<?>) input).toArray();
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      public void dispose() {
      }

    });
    runtimeComboViewer.setLabelProvider(new ILabelProvider() {

      public void removeListener(ILabelProviderListener listener) {
      }

      public boolean isLabelProperty(Object element, String property) {
        return false;
      }

      public void dispose() {
      }

      public void addListener(ILabelProviderListener listener) {
      }

      public String getText(Object element) {
        AbstractMavenRuntime runtime = (AbstractMavenRuntime) element;
        StringBuilder sb = new StringBuilder();

        if(runtime.isLegacy()) {
          sb.append(MavenRuntimeManagerImpl.EXTERNAL).append(" ").append(runtime.toString());
        } else if(!runtime.isEditable()) {
          sb.append(getType(runtime)).append(" (").append(runtime.toString()).append(')');
        } else {
          sb.append(runtime.getName()).append(" (");
          sb.append(getType(runtime)).append(' ').append(runtime.toString());
          sb.append(')');
        }

        return sb.toString();
      }

      public Image getImage(Object element) {
        return null;
      }
    });

    try {
      setInput();
    } catch(NullPointerException e) {
      // ignore, this only happens inside windowbuilder
    }

    Button configureRuntimesButton = new Button(this, SWT.NONE);
    configureRuntimesButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    configureRuntimesButton.setText(org.eclipse.m2e.internal.launch.Messages.MavenLaunchMainTab_btnConfigure);
    configureRuntimesButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(mainComposite.getShell(),
            "org.eclipse.m2e.core.preferences.MavenInstallationsPreferencePage", null, null); //$NON-NLS-1$
        if(dialog.open() == Window.OK) {
          setInput();
        }
      }
    });
  }

  protected String getType(AbstractMavenRuntime runtime) {
    if(runtime instanceof MavenEmbeddedRuntime) {
      return MavenRuntimeManagerImpl.EMBEDDED;
    } else if(runtime instanceof MavenWorkspaceRuntime) {
      return MavenRuntimeManagerImpl.WORKSPACE;
    } else if(runtime instanceof MavenExternalRuntime) {
      return MavenRuntimeManagerImpl.EXTERNAL;
    }
    throw new IllegalArgumentException();
  }

  protected void setInput() {
    MavenRuntimeManagerImpl runtimeManager = getRuntimeManager();
    runtimeComboViewer.setInput(runtimeManager.getMavenRuntimes());
    runtimeComboViewer
        .setSelection(new StructuredSelection(runtimeManager.getRuntime(MavenRuntimeManagerImpl.DEFAULT)));
  }

  public void setSelectRuntime(AbstractMavenRuntime runtime) {
    this.runtimeComboViewer.setSelection(new StructuredSelection(runtime));
  }

  public AbstractMavenRuntime getSelectedRuntime() {
    IStructuredSelection selection = (IStructuredSelection) runtimeComboViewer.getSelection();
    return (AbstractMavenRuntime) selection.getFirstElement();
  }

  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    runtimeComboViewer.addSelectionChangedListener(listener);
  }

  public void initializeFrom(ILaunchConfiguration configuration) {
    String name = "";
    try {
      name = configuration.getAttribute(MavenLaunchConstants.ATTR_RUNTIME, ""); //$NON-NLS-1$
    } catch(CoreException ex) {
      // TODO log
    }
    AbstractMavenRuntime runtime = getRuntimeManager().getRuntime(name);
    if(runtime != null) {
      setSelectRuntime(runtime);
    }
  }

  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    AbstractMavenRuntime runtime = getSelectedRuntime();
    configuration.setAttribute(MavenLaunchConstants.ATTR_RUNTIME, runtime.getName());
  }
}
