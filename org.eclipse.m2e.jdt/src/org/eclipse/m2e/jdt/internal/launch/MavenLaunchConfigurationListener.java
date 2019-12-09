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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;


public class MavenLaunchConfigurationListener implements ILaunchConfigurationListener, IMavenProjectChangedListener {
  private static final Logger log = LoggerFactory.getLogger(MavenLaunchConfigurationListener.class);

  public void launchConfigurationAdded(ILaunchConfiguration configuration) {
    updateLaunchConfiguration(configuration);
  }

  public void launchConfigurationChanged(ILaunchConfiguration configuration) {
    updateLaunchConfiguration(configuration);
  }

  public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
    // do nothing
  }

  private void updateLaunchConfiguration(ILaunchConfiguration configuration) {
    try {
      if(!MavenRuntimeClasspathProvider.isSupportedType(configuration.getType().getIdentifier())) {
        return;
      }
      if(configuration.getAttributes().containsKey(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER)) {
        return;
      }
      IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
      if(javaProject != null && javaProject.getProject().hasNature(IMavenConstants.NATURE_ID)) {
        MavenRuntimeClasspathProvider.enable(configuration);
      }
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    }
  }

  public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
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
