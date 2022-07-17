/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.views;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import org.eclipse.m2e.core.ui.internal.preferences.LifecycleMappingsViewer;


/**
 * MavenLifecycleMappingsView
 */
public class MavenLifecycleMappingsView extends ViewPart {

  private LifecycleMappingsViewer mappingsViewer;

  private Composite composite;

  /* (non-Javadoc)
   * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
   */
  @Override
  public void createPartControl(Composite parent) {
    mappingsViewer = new LifecycleMappingsViewer();
    this.composite = mappingsViewer.createContents(parent);

  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.part.ViewPart#init(org.eclipse.ui.IViewSite)
   */
  @Override
  public void init(IViewSite site) throws PartInitException {
    super.init(site);
    site.getPage().addSelectionListener(new ISelectionListener() {

      @Override
      public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        Object element;
        if(selection instanceof IStructuredSelection structuredSelection) {
          element = structuredSelection.getFirstElement();
        } else {
          element = null;
        }
        IResource resource = Adapters.adapt(element, IResource.class);
        if(resource != null) {
          mappingsViewer.setTarget(resource.getProject());
        } else {
          mappingsViewer.setTarget(null);
        }
      }
    });
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
   */
  @Override
  public void setFocus() {
    composite.setFocus();
  }

}
