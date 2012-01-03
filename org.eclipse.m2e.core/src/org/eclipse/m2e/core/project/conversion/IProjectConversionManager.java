/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 */
public interface IProjectConversionManager {

  /**
   * Converts an existing Eclipse project configuration to its Maven Model counterpart
   */
  void convert(IProject project, Model model, IProgressMonitor monitor) throws CoreException;
  
  /**
   * Returns an unmodifiable list of all known {@link AbstractProjectConversionParticipant}s 
   */
  List<AbstractProjectConversionParticipant> getAllConversionParticipants();

  /**
   * Returns an unmodifiable list of all {@link AbstractProjectConversionParticipant}s applying to this project
   */
  List<AbstractProjectConversionParticipant> getConversionParticipants(IProject project) throws CoreException;

}
