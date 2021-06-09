/*******************************************************************************
 * Copyright (c) 2016 Anton Tanasenko
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

package org.eclipse.m2e.core.ui.internal.wizards;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IWorkingSet;

import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.IProjectCreationListener;
import org.eclipse.m2e.core.ui.internal.WorkingSets;


/**
 * {@linkplain IProjectCreationListener} which adds new projects to specified working sets. Should be used in
 * {@link IProjectConfigurationManager} methods instead of {@code IWorkingSet[]} argument to
 * {@link AbstractCreateMavenProjectJob} or {@link AbstractCreateMavenProjectsOperation}
 *
 * @since 1.8
 */
public class MavenProjectWorkspaceAssigner implements IProjectCreationListener {

  private final List<IWorkingSet> workingSets;

  public MavenProjectWorkspaceAssigner(List<IWorkingSet> workingSets) {
    this.workingSets = workingSets;
  }

  @Override
  public void projectCreated(IProject project) {
    WorkingSets.addToWorkingSets(new IProject[] {project}, workingSets);
  }

}
