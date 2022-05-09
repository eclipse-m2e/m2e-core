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

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;
import org.eclipse.jdt.internal.debug.ui.launcher.VMArgumentsBlock;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;


@SuppressWarnings("restriction")
public class MavenJRETab extends JavaJRETab {

  private final VMArgumentsBlock vmArgumentsBlock = new VMArgumentsBlock();

  @Override
  public void createControl(Composite parent) {
    super.createControl(parent);

    Composite comp = (Composite) fJREBlock.getControl();
    ((GridData) comp.getLayoutData()).grabExcessVerticalSpace = true;
    ((GridData) comp.getLayoutData()).verticalAlignment = SWT.FILL;

    vmArgumentsBlock.createControl(comp);
    ((GridData) vmArgumentsBlock.getControl().getLayoutData()).horizontalSpan = 2;
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    super.performApply(configuration);
    vmArgumentsBlock.performApply(configuration);
    setLaunchConfigurationWorkingCopy(configuration);
  }

//  private boolean useDefaultSeparateJRE(ILaunchConfigurationWorkingCopy configuration) {
//    boolean deflt = false;
//    String vmInstallType = null;
//    String jreContainerPath = null;
//    try {
//      vmInstallType = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, (String) null);
//      jreContainerPath = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH,
//          (String) null);
//    } catch(CoreException e) {
//    }
//    if(vmInstallType != null) {
//      configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, (String) null);
//    }
//    if(jreContainerPath != null) {
//      configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, (String) null);
//    }
//    IVMInstall defaultVMInstall = getDefaultVMInstall(configuration);
//    if(defaultVMInstall != null) {
//      IVMInstall vm = fJREBlock.getJRE();
//      deflt = defaultVMInstall.equals(vm);
//    }
//
//    if(vmInstallType != null) {
//      configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, vmInstallType);
//    }
//    if(jreContainerPath != null) {
//      configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, jreContainerPath);
//    }
//    return deflt;
//  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    super.initializeFrom(configuration);
    vmArgumentsBlock.initializeFrom(configuration);
    // fVMArgumentsBlock.setEnabled(!fJREBlock.isDefaultJRE());
  }

  @Override
  public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
    super.setLaunchConfigurationDialog(dialog);
    vmArgumentsBlock.setLaunchConfigurationDialog(dialog);
  }

  @Override
  public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
    setLaunchConfigurationWorkingCopy(workingCopy);
  }

  @Override
  public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
  }
}
