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

package org.eclipse.m2e.core.ui.internal.lifecycle;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Shell;

/**
 * AbstractLifecyclePropertyPage
 * Holds the pieces used in the common lifecycle mapping properties pages.
 *
 * @author dyocum
 */
public abstract class AbstractLifecyclePropertyPage extends AbstractPropertyPageExtensionPoint implements ILifecyclePropertyPage{
  private IProject project;
  private Shell shell;
  
  public AbstractLifecyclePropertyPage(){
  }
  
  public void setupPage(IProject project, Shell shell){
    this.project = project;
    this.setShell(shell);
  }

  public void setProject(IProject project){
    this.project = project;
  }
  
  public IProject getProject(){
    return project;
  }

  /**
   * @param shell The shell to set.
   */
  public void setShell(Shell shell) {
    this.shell = shell;
  }

  /**
   * @return Returns the shell.
   */
  public Shell getShell() {
    return shell;
  }
  
}
