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

package org.eclipse.m2e.core.internal.project.registry;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.execution.MavenExecutionRequest;

import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * AbstractMavenDependencyResolver
 * 
 * @author igor
 */
public abstract class AbstractMavenDependencyResolver {

  private ProjectRegistryManager manager;

  private MutableProjectRegistry contextRegistry;

  protected IMaven getMaven() {
    return manager.getMaven();
  }

  final void setManager(ProjectRegistryManager manager) {
    this.manager = manager;
  }

  protected ProjectRegistryManager getManager() {
    return manager;
  }

  /**
   * @deprecated implement {@link #resolveProjectDependencies(IMavenProjectFacade, Set, Set, IProgressMonitor)} instead
   */
  @Deprecated
  public void resolveProjectDependencies(IMavenProjectFacade facade, MavenExecutionRequest mavenRequest,
      Set<Capability> capabilities, Set<RequiredCapability> requirements, IProgressMonitor monitor)
      throws CoreException {
    resolveProjectDependencies(facade, capabilities, requirements, monitor);
  }

  /**
   * Subclasses <strong>must</strong> implement this method. It is not abstract for backward compatibility reasons and
   * will be marked as <code>abstract</code> in m2e 2.0 (if we ever get there and if I don't forget).
   * 
   * @since 1.4
   */
  @SuppressWarnings("unused")
  public void resolveProjectDependencies(IMavenProjectFacade facade, Set<Capability> capabilities,
      Set<RequiredCapability> requirements, IProgressMonitor monitor) throws CoreException {
  }

  void setContextProjectRegistry(MutableProjectRegistry contextRegistry) {
    this.contextRegistry = contextRegistry;
  }

  protected List<MavenProjectFacade> getProjects() {
    return Arrays.asList(contextRegistry.getProjects());
  }
}
