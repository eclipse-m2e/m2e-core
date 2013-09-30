/*******************************************************************************
 * Copyright (c) 2008-2013 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Red Hat, Inc. - discover proposals for ILifecycleMappingRequirements
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecyclemapping.discovery;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;


public interface IMavenDiscovery {

  /**
   * <p>
   * Calculates possibly empty list of discovery proposals. Multiple proposals per mapping configuration element
   * represent alternative possible changes.
   * </p>
   * <p>
   * To support incremental collection of user choices in the GUI, optional <code>preselected</code>
   * requirements/proposals map is used to eliminate new proposals that conflict with already selected choices. Result
   * is expected to include preselected proposals as-is. Implementation is expected to eliminate proposals that conflict
   * with already installed Eclipse bundles and preselected proposals.
   * </p>
   */
  public Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> discover(MavenProject mavenProject,
      List<MojoExecution> mojoExecutions, List<IMavenDiscoveryProposal> preselected, IProgressMonitor monitor)
      throws CoreException;

  /**
   * <p>
   * Calculates discovery proposals for a given collection of {@link ILifecycleMappingRequirement}s. Multiple proposals
   * per requirement element can be found.
   * </p>
   * <p>
   * To support incremental collection of user choices in the GUI, optional <code>preselected</code>
   * requirements/proposals map is used to eliminate new proposals that conflict with already selected choices. Result
   * is expected to include preselected proposals as-is. Implementation is expected to eliminate proposals that conflict
   * with already installed Eclipse bundles and preselected proposals.
   * </p>
   * 
   * @since 1.5.0
   */
  public Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> discover(
      Collection<ILifecycleMappingRequirement> requirements, List<IMavenDiscoveryProposal> preselected,
      IProgressMonitor monitor) throws CoreException;

}
