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

  public abstract void resolveProjectDependencies(IMavenProjectFacade facade, MavenExecutionRequest mavenRequest,
      Set<Capability> capabilities, Set<RequiredCapability> requirements, IProgressMonitor monitor)
      throws CoreException;

  void setContextProjectRegistry(MutableProjectRegistry contextRegistry) {
    this.contextRegistry = contextRegistry;
  }

  protected List<MavenProjectFacade> getProjects() {
    return Arrays.asList(contextRegistry.getProjects());
  }
}
