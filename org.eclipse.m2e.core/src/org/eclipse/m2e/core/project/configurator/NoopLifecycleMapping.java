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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * NoopLifecycleMapping
 * 
 * @author igor
 */
public class NoopLifecycleMapping extends AbstractLifecycleMapping {

  public String getId() {
    return "noop";
  }

  public String getName() {
    return "noop";
  }

  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
  }

  public void unconfigure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
  }

  public List<AbstractProjectConfigurator> getProjectConfigurators(IMavenProjectFacade project, IProgressMonitor monitor) {
    return Collections.emptyList();
  }

  public Map<MojoExecutionKey, List<AbstractBuildParticipant>> getBuildParticipants(IMavenProjectFacade project,
      IProgressMonitor monitor) {
    return Collections.emptyMap();
  }

  public boolean hasLifecycleMappingChanged(IMavenProjectFacade newFacade,
      ILifecycleMappingConfiguration oldConfiguration, IProgressMonitor monitor) {
    return false;
  }
}
