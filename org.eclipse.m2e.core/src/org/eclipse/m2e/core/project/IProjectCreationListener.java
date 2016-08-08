/*******************************************************************************
 * Copyright (c) 2016 Anton Tanasenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Anton Tanasenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.core.project;

import org.eclipse.core.resources.IProject;


/**
 * @since 1.8
 */
public interface IProjectCreationListener {

  /**
   * Called when a new maven project gets imported/created in the workspace but before it is actually configured.
   */
  void projectCreated(IProject project);

}
