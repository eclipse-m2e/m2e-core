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

package org.eclipse.m2e.ui.internal.launch;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.internal.launch.MavenLaunchParticipantInfo;
import org.eclipse.m2e.internal.launch.Messages;


public class MavenLaunchExtensionsTab extends AbstractLaunchConfigurationTab {

  private Set<String> disabledParticipants;

  private final List<MavenLaunchParticipantInfo> participants;

  private CheckboxTableViewer checkboxTableViewer;

  public MavenLaunchExtensionsTab(List<MavenLaunchParticipantInfo> participants) {
    this.participants = participants;
  }

  /**
   * @wbp.parser.entryPoint
   */
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    setControl(composite);
    composite.setLayout(new GridLayout(1, false));

    Label lblMavenLaunchExtensions = new Label(composite, SWT.NONE);
    lblMavenLaunchExtensions.setText(Messages.MavenLaunchExtensionsTab_lblExtensions);

    checkboxTableViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.FULL_SELECTION);
    checkboxTableViewer.addCheckStateListener(event -> {
      if(event.getElement() instanceof MavenLaunchParticipantInfo) {
        MavenLaunchParticipantInfo participant = (MavenLaunchParticipantInfo) event.getElement();
        if(event.getChecked()) {
          disabledParticipants.remove(participant.getId());
        } else {
          disabledParticipants.add(participant.getId());
        }
        setDirty(true);
        updateLaunchConfigurationDialog();
      }
    });
    Table table = checkboxTableViewer.getTable();
    table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

    checkboxTableViewer.setContentProvider(new IStructuredContentProvider() {
      public void dispose() {
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      public Object[] getElements(Object inputElement) {
        if(inputElement instanceof Collection<?>) {
          return ((Collection<?>) inputElement).toArray();
        }
        return null;
      }
    });

    checkboxTableViewer.setCheckStateProvider(new ICheckStateProvider() {
      public boolean isChecked(Object element) {
        if(element instanceof MavenLaunchParticipantInfo) {
          return !disabledParticipants.contains(((MavenLaunchParticipantInfo) element).getId());
        }
        return false;
      }

      public boolean isGrayed(Object element) {
        return false;
      }
    });

    checkboxTableViewer.setLabelProvider(new ILabelProvider() {

      public void addListener(ILabelProviderListener listener) {
      }

      public void dispose() {
      }

      public boolean isLabelProperty(Object element, String property) {
        return false;
      }

      public void removeListener(ILabelProviderListener listener) {
      }

      public Image getImage(Object element) {
        return null;
      }

      public String getText(Object element) {
        if(element instanceof MavenLaunchParticipantInfo) {
          return ((MavenLaunchParticipantInfo) element).getName();
        }
        return null;
      }
    });
  }

  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
  }

  public void initializeFrom(ILaunchConfiguration configuration) {
    try {
      disabledParticipants = new HashSet<>(
          configuration.getAttribute(MavenLaunchConstants.ATTR_DISABLED_EXTENSIONS, Collections.emptySet()));
    } catch(CoreException ex) {
      disabledParticipants = new HashSet<>();
    }

    checkboxTableViewer.setInput(participants);
  }

  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    Set<String> disabledParticipants = this.disabledParticipants.isEmpty() ? null : this.disabledParticipants;
    configuration.setAttribute(MavenLaunchConstants.ATTR_DISABLED_EXTENSIONS, disabledParticipants);
  }

  public String getName() {
    return Messages.MavenLaunchExtensionsTab_name;
  }

}
