/*******************************************************************************
 * Copyright (c) 2012-2013 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.project.conversion;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.model.Model;


/**
 * Manages conversion of existing Eclipse projects into Maven ones.
 *
 * @author Fred Bricon
 * @since 1.1
 */
public interface IProjectConversionManager {

  /**
   * Converts an existing Eclipse project configuration to its Maven Model counterpart
   */
  void convert(IProject project, Model model, IProgressMonitor monitor) throws CoreException;

  /**
   * Returns an unmodifiable list of all {@link AbstractProjectConversionParticipant}s applying to the given project and
   * packaging.
   *
   * @since 1.3
   */
  List<AbstractProjectConversionParticipant> getConversionParticipants(IProject project, String packaging)
      throws CoreException;

  /**
   * Returns an {@link IProjectConversionEnabler} for the project, if one exists
   *
   * @return an {@link IProjectConversionEnabler} for the project, or null if one cannot be found.
   */

  IProjectConversionEnabler getConversionEnablerForProject(IProject project);
}
