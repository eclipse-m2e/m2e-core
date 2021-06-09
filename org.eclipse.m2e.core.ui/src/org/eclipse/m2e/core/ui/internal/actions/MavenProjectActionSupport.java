/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.ui.internal.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;


public abstract class MavenProjectActionSupport extends MavenActionSupport implements IWorkbenchWindowActionDelegate {

  protected IProject[] getProjects() {
    return SelectionUtil.getProjects(selection, true);
  }

  @Override
  public void dispose() {
  }

  @Override
  public void init(IWorkbenchWindow window) {
  }

}
