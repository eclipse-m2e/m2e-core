/*******************************************************************************
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Takari, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.components;

import java.beans.Beans;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import org.eclipse.m2e.core.ui.internal.MavenImages;


/**
 * @since 1.5
 */
public class MavenProjectLabelProvider extends LabelProvider {
    @Override
    public Image getImage(Object element) {
    if(Beans.isDesignTime()) {
      // windowbuilder compat
      return null;
    }
    ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
    if(element instanceof IProject && !((IProject) element).isAccessible()) {
      return sharedImages.getImage(IDE.SharedImages.IMG_OBJ_PROJECT_CLOSED);
    }

    Image img = MavenImages.createOverlayImage(MavenImages.MVN_PROJECT,
        sharedImages.getImage(IDE.SharedImages.IMG_OBJ_PROJECT), MavenImages.MAVEN_OVERLAY, IDecoration.TOP_LEFT);

    return img;
  }

  @Override
  public String getText(Object element) {
    return element instanceof IProject ? ((IProject) element).getName() : ""; //$NON-NLS-1$
  }

}
