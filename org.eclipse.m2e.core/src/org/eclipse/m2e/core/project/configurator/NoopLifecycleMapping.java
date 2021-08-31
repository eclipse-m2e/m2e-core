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

  /**
   * @since 1.3
   */
  public static final String LIFECYCLE_MAPPING_ID = "NULL";

  @Override
  public String getId() {
    return LIFECYCLE_MAPPING_ID;
  }

  @Override
  public String getName() {
    return "noop";
  }

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
  }

  @Override
  public void unconfigure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
  }

  @Override
  public List<AbstractProjectConfigurator> getProjectConfigurators(IMavenProjectFacade project, IProgressMonitor monitor) {
    return Collections.emptyList();
  }

  @Override
  public Map<MojoExecutionKey, List<AbstractBuildParticipant>> getBuildParticipants(IMavenProjectFacade project,
      IProgressMonitor monitor) {
    return Collections.emptyMap();
  }

  @Override
  public boolean hasLifecycleMappingChanged(IMavenProjectFacade newFacade,
      ILifecycleMappingConfiguration oldConfiguration, IProgressMonitor monitor) {
    return false;
  }
}
