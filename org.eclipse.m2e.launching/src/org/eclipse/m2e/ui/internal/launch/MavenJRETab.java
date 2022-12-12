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

import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;
import org.eclipse.jdt.internal.debug.ui.jres.JREDescriptor;
import org.eclipse.jdt.internal.debug.ui.launcher.VMArgumentsBlock;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.m2e.core.internal.project.ResolverConfigurationIO;
import org.eclipse.m2e.core.project.IProjectConfiguration;
import org.eclipse.m2e.internal.launch.MavenLaunchDelegate;
import org.eclipse.m2e.internal.launch.Messages;


@SuppressWarnings("restriction")
public class MavenJRETab extends JavaJRETab {

  private static final Logger log = LoggerFactory.getLogger(MavenJRETab.class);

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

  private IProjectConfiguration getResolverConfiguration() {
    IJavaProject javaProject = getJavaProject();
    if(javaProject == null) {
      return null;
    }
    return ResolverConfigurationIO.readResolverConfiguration(javaProject.getProject());
  }

  @Override
  protected IJavaProject getJavaProject() {
    IProject project = MavenLaunchDelegate.getProjectFromConfiguration(getLaunchConfiguration());
    if(project != null && project.exists()) {
      return JavaCore.create(project);
    }
    return null;
  }

  /**
   * Retrieves information about Maven JRE set in Maven project properties.
   * 
   * @return the descriptor for the default Maven JRE
   */
  protected JREDescriptor getDefaultJREDescriptor() {
    return new MavenJREDescriptor(() -> Optional.ofNullable(getResolverConfiguration())
        .map(IProjectConfiguration::getRequiredJavaVersion).orElse(null),
        () -> super.getDefaultJREDescriptor().getDescription());
  }

  private static final class MavenJREDescriptor extends JREDescriptor {

    private final Supplier<String> defaultDescription;

    private final Supplier<String> requiredJavaVersion;

    MavenJREDescriptor(Supplier<String> requiredJavaVersion, Supplier<String> defaultDescription) {
      this.requiredJavaVersion = requiredJavaVersion;
      this.defaultDescription = defaultDescription;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.debug.ui.jres.DefaultJREDescriptor#getDescription()
     */
    @Override
    public String getDescription() {
      // first check Maven JRE configuration
      String version = requiredJavaVersion.get();
      IVMInstall mavenJre = version != null ? MavenLaunchDelegate.getBestMatchingVM(version) : null;
      final String details;
      if(mavenJre != null) {
        // add link
        details = NLS.bind(Messages.MavenJRETab_lblDefaultDetailsRequiredJavaVersion, mavenJre.getName(),
            requiredJavaVersion.get());
      } else {
        // TODO: add logic for getting the underlying project
        // then fall back to default
        details = defaultDescription.get();
      }
      return NLS.bind(Messages.MavenJRETab_lblDefault, details);
    }
  };

  /**
   * Need to overwrite from parent, to prevent calling JavaJRETab.checkCompliance().
   */
  public boolean isValid(ILaunchConfiguration config) {
    setErrorMessage(null);
    setMessage(null);

    IStatus status = fJREBlock.getStatus();
    if(!status.isOK()) {
      setErrorMessage(status.getMessage());
      return false;
    }
    // prevent calling JavaJRETab.checkCompliance(), as that uses the wrong JDK for the checks when the default is checked
    // also Maven launch type is not detected as external program
    /**
     * if(!isExternalToolConfiguration(fLaunchConfiguration)) { status = checkCompliance(); if (!status.isOK()) {
     * setErrorMessage(status.getMessage()); return false; } }
     */

    ILaunchConfigurationTab dynamicTab = getDynamicTab();
    if(dynamicTab != null) {
      return dynamicTab.isValid(config);
    }
    return true;
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
