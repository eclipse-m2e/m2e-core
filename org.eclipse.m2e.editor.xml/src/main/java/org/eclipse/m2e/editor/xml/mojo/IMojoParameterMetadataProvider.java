/*******************************************************************************
 * Copyright (c) 2015 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Anton Tanasenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml.mojo;

import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.m2e.core.embedder.ArtifactKey;


/**
 * Provides available configuration options for maven plugin executions
 *
 * @author atanasenko
 * @since 1.6
 */
public interface IMojoParameterMetadataProvider {

  /**
   * Calculates available configuration of one specific mojo.
   */
  public MojoParameter getMojoConfiguration(ArtifactKey pluginKey, String mojo) throws CoreException;

  /**
   * Calculates available configuration of a number of mojos.
   */
  public MojoParameter getMojoConfiguration(ArtifactKey pluginKey, Collection<String> mojos) throws CoreException;

  /**
   * Calculates available configuration of all mojos provided by specified plugin.
   */
  public MojoParameter getMojoConfiguration(ArtifactKey pluginKey) throws CoreException;

  /**
   * Calculates available configuration of a single plugin class.
   */
  public MojoParameter getClassConfiguration(ArtifactKey pluginKey, String className) throws CoreException;

  @Deprecated
  public MojoParameter getMojoConfiguration(ArtifactKey pluginKey, String mojo, IProgressMonitor monitor)
      throws CoreException;

  @Deprecated
  public MojoParameter getMojoConfiguration(ArtifactKey pluginKey, Collection<String> mojos, IProgressMonitor monitor)
      throws CoreException;

  @Deprecated
  public MojoParameter getMojoConfiguration(ArtifactKey pluginKey, IProgressMonitor monitor) throws CoreException;

  @Deprecated
  public MojoParameter getClassConfiguration(ArtifactKey pluginKey, String className, IProgressMonitor monitor)
      throws CoreException;

}
