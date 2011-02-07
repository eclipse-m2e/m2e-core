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

package org.eclipse.m2e.core.actions;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.ui.dialogs.AddDependencyDialog;
import org.eclipse.m2e.model.edit.pom.Dependency;


public class AddDependencyAction extends MavenActionSupport implements IWorkbenchWindowActionDelegate {

  public static final String ID = "org.eclipse.m2e.addDependencyAction"; //$NON-NLS-1$

  public void run(IAction action) {
    IFile file = getPomFileFromPomEditorOrViewSelection();

    if(file == null) {
      return;
    }

    MavenPlugin plugin = MavenPlugin.getDefault();

    AddDependencyDialog dialog = new AddDependencyDialog(getShell(), file);
    if(dialog.open() == Window.OK) {
      List<Dependency> dependencies = dialog.getDependencies();
      plugin.getMavenModelManager().updateProject(file, new MavenModelManager.DependencyAdder(dependencies)); 
    }
  }
  
  public void dispose() {
  }

  public void init(IWorkbenchWindow window) {
  }
}
