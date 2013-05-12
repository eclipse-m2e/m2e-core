/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

  public void dispose() {
  }

  public void init(IWorkbenchWindow window) {
  }

}
