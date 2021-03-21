/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.ui.internal.views;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.eclipse.m2e.core.ui.internal.util.M2EUIUtils;
import org.eclipse.m2e.core.ui.internal.views.nodes.IMavenRepositoryNode;
import org.eclipse.m2e.core.ui.internal.views.nodes.RepositoryNode;


/**
 * RepositoryViewLabelProvider
 * 
 * @author dyocum
 */
public class RepositoryViewLabelProvider extends LabelProvider implements IStyledLabelProvider, IColorProvider,
    IFontProvider {

  private Font italicFont;

  public RepositoryViewLabelProvider(Font treeFont) {
    int size = 0;
    FontData[] data = treeFont.getFontData();
    if(data == null) {
      size = 12;
    } else {
      for(FontData element : data) {
        size = Math.max(size, element.getHeight());
      }
    }
    italicFont = M2EUIUtils.deriveFont(treeFont, SWT.ITALIC, size);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
   */
  public void dispose() {
    italicFont.dispose();
    super.dispose();
  }

  public String getText(Object obj) {
    if(obj instanceof IMavenRepositoryNode) {
      return ((IMavenRepositoryNode) obj).getName();
    }
    return obj.toString();
  }

  public Image getImage(Object obj) {
    if(obj instanceof IMavenRepositoryNode) {
      return ((IMavenRepositoryNode) obj).getImage();
    }
    return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
  }

  public Color getBackground(Object element) {
    return null;
  }

  public Color getForeground(Object element) {
    if(element instanceof RepositoryNode && !((RepositoryNode) element).isEnabledIndex()) {
        return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
    }
    return null;
  }

  public Font getFont(Object element) {
    if(element instanceof IMavenRepositoryNode) {
      boolean updating = ((IMavenRepositoryNode) element).isUpdating();
      return updating ? italicFont : null;
    }
    return null;
  }

  public StyledString getStyledText(Object element) {
    return new StyledString(getText(element));
  }

}
