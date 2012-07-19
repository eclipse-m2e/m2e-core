/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Andrew Eisenberg - adapted for workspace preferences
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;

import org.eclipse.m2e.core.ui.internal.Messages;


public class LifecycleMappingPropertyPage extends PropertyPage {

  private LifecycleMappingsViewer mappingsViewer;

  public LifecycleMappingPropertyPage() {
    setMessage(Messages.LifecycleMappingPropertyPage_pageMessage);
    noDefaultAndApplyButton();
    mappingsViewer = new LifecycleMappingsViewer();
  }

  @Override
  public Control createContents(Composite parent) {
    mappingsViewer.setShell(parent.getShell());
    return mappingsViewer.createContents(parent);
  }

  public void setElement(IAdaptable element) {
    super.setElement(element);
  
    IProject project = (IProject) getElement().getAdapter(IProject.class);
    if (project != null) {
      mappingsViewer.setTarget(project);
    }
  }
}
