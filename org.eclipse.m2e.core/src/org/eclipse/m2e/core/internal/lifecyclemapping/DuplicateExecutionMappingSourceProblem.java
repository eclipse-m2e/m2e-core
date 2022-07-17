/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecyclemapping;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


/**
 * DuplicateExecutionMappingSourceProblem
 */
public class DuplicateExecutionMappingSourceProblem extends DuplicateMappingSourceProblem {

  private final MojoExecutionKey executionKey;

  public DuplicateExecutionMappingSourceProblem(SourceLocation location, String message, MojoExecutionKey executionKey,
      DuplicatePluginExecutionMetadataException error) {
    super(location, message, IMavenConstants.MARKER_DUPLICATEMAPPING_TYPE_GOAL, executionKey.goal(), error);
    this.executionKey = executionKey;
  }

  @Override
  public void processMarker(IMarker marker) throws CoreException {
    super.processMarker(marker);
    MojoExecutionProblemInfo.setExecutionInfo(executionKey, marker);
  }

}
