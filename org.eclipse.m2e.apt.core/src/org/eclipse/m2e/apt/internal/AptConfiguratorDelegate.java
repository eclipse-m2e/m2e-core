/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.apt.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.jdt.IClasspathDescriptor;


/**
 * AptConfiguratorDelegate
 *
 * @author Fred Bricon
 */
public interface AptConfiguratorDelegate {

  void setSession(MavenSession mavenSession);

  void setFacade(IMavenProjectFacade mavenProjectFacade);

  void configureProject(IProgressMonitor monitor) throws CoreException;

  void configureClasspath(IClasspathDescriptor classpath, IProgressMonitor monitor) throws CoreException;

  boolean isIgnored(IProgressMonitor monitor);

  AbstractBuildParticipant getMojoExecutionBuildParticipant(MojoExecution execution);

}
