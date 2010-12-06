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

package org.eclipse.m2e.core.project.configurator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.m2e.core.project.IMavenProjectFacade;

/**
 * NoopLifecycleMapping
 *
 * @author igor
 */
public class NoopLifecycleMapping extends AbstractLifecycleMapping {

  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    // do nothing
  }

  public List<AbstractBuildParticipant> getBuildParticipants(IMavenProjectFacade facade, IProgressMonitor monitor)
      throws CoreException {
    return new ArrayList<AbstractBuildParticipant>();
  }

  public List<AbstractProjectConfigurator> getProjectConfigurators(IMavenProjectFacade facade, IProgressMonitor monitor)
      throws CoreException {
    return new ArrayList<AbstractProjectConfigurator>();
  }

  public void unconfigure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    // do nothing
  }
  
  public List<String> getPotentialMojoExecutionsForBuildKind(IMavenProjectFacade facade, int kind, IProgressMonitor progressMonitor) {
    return Collections.emptyList();
  }

  public boolean isInterestingPhase(String phase) {
    return false;
  }
}
