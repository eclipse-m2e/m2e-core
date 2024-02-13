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

package org.eclipse.m2e.jdt.internal.launch;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.jdt.internal.UnitTestSupport;


public class MavenLaunchConfigurationListener implements ILaunchConfigurationListener, IMavenProjectChangedListener {
  private static final Logger log = LoggerFactory.getLogger(MavenLaunchConfigurationListener.class);

  @Override
  public void launchConfigurationAdded(ILaunchConfiguration configuration) {
    updateLaunchConfiguration(configuration);
    UnitTestSupport.setupLaunchConfigurationFromMavenConfiguration(configuration);
  }

  @Override
  public void launchConfigurationChanged(ILaunchConfiguration configuration) {
    updateLaunchConfiguration(configuration);
  }

  @Override
  public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
    // do nothing
  }

  private void updateLaunchConfiguration(ILaunchConfiguration configuration) {
    try {
      if(!MavenRuntimeClasspathProvider.isSupportedType(configuration.getType().getIdentifier())) {
        return;
      }

      IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
      if(javaProject != null && javaProject.getProject().hasNature(IMavenConstants.NATURE_ID)) {
        if(!configuration.getAttributes().containsKey(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER)) {
          MavenRuntimeClasspathProvider.enable(configuration);
        }

        setModuleNameForLaunchersFromTestFolder(javaProject, configuration);
      }
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    }
  }

  /**
   * As the test source folder can't have a second module-info.java in its package fragment root all launch
   * configurations with a main class in the source folder won't find the module definition. Thus the module name
   * derived from the project is set here for the launch configuration.
   *
   * @param javaProject
   * @param configuration
   * @throws CoreException
   */
  private void setModuleNameForLaunchersFromTestFolder(IJavaProject javaProject, ILaunchConfiguration config)
      throws CoreException {
    if(isLaunchConfigWithMainFromTestFolder(javaProject, config)) {
      IModuleDescription module = javaProject.getModuleDescription();
      String modName = module == null ? null : module.getElementName();

      String currentModuleName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MODULE_NAME, (String) null);
      if(modName != null && modName.length() > 0 && !modName.equals(currentModuleName)) {
        if(config instanceof ILaunchConfigurationWorkingCopy wc) {
          wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MODULE_NAME, modName);
        } else {
          ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
          wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MODULE_NAME, modName);
          wc.doSave();
        }
      }
    }
  }

  private boolean isLaunchConfigWithMainFromTestFolder(IJavaProject javaProject, ILaunchConfiguration config)
      throws CoreException {
    String mainType = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String) null);
    if(mainType == null) {
      return false;
    }

    IType findType = javaProject.findType(mainType);
    if(findType == null) {
      return false;
    }

    IJavaElement javaElement = findType.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
    if(javaElement instanceof IPackageFragmentRoot) {
      IPath path = javaElement.getPath();
      return "test".equals(path.segment(path.segmentCount() - 2));
    }

    return true;
  }

  @Override
  public void mavenProjectChanged(List<MavenProjectChangedEvent> events, IProgressMonitor monitor) {
    for(MavenProjectChangedEvent event : events) {
      try {
        switch(event.getKind()) {
          case MavenProjectChangedEvent.KIND_ADDED:
            MavenRuntimeClasspathProvider.enable(event.getMavenProject().getProject());
            break;
          case MavenProjectChangedEvent.KIND_REMOVED:
            MavenRuntimeClasspathProvider.disable(event.getOldMavenProject().getProject());
            break;
          default:
            break;
        }
      } catch(Exception e) {
        log.error("Could not update launch configuration", e);
      }
    }
  }
}
