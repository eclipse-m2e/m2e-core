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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * ILifecyclePropertyPage
 *
 * @author dyocum
 */
public interface ILifecyclePropertyPage extends ILifecyclePropertyPageExtensionPoint{
  /**
   * Create and return the composite which will be shown in the parent properties page.
   * @param parent
   * @return
   */
  public Control createContents(Composite parent);
  
  /**
   * Called when the 'Restore Defaults' button is pressed in the properties page.
   */
  public void performDefaults();
  
  /**
   * Called when the 'OK' or 'Apply' buttons are pressed in the properties dialog.
   * @return
   */
  public boolean performOk();
  
  /**
   * The project that these lifecycle mapping properties apply to
   * @param project
   */
  public void setProject(IProject project);
  
  public IProject getProject();
  
  /**
   * The parent shell used for showing error messages.
   * @param shell
   */
  public void setShell(Shell shell);
  
  public Shell getShell();

  
}
