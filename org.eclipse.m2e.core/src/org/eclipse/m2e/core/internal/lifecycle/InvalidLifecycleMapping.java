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

package org.eclipse.m2e.core.internal.lifecycle;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMappingConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;


/**
 * Invlid lifecycle mapping provides additional information about lifecycle mapping problems.
 * 
 * @author igor
 */
public class InvalidLifecycleMapping extends AbstractLifecycleMapping {

  public String getId() {
    return "invalid";
  }

  public String getName() {
    return "invalid";
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
