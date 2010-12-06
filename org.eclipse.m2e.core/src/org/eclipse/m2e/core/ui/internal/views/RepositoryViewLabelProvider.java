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

package org.eclipse.m2e.core.ui.internal.views;

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.eclipse.m2e.core.ui.internal.views.nodes.IMavenRepositoryNode;
import org.eclipse.m2e.core.ui.internal.views.nodes.RepositoryNode;
import org.eclipse.m2e.core.util.M2EUtils;

/**
 * RepositoryViewLabelProvider
 *
 * @author dyocum
 */
public class RepositoryViewLabelProvider extends LabelProvider implements IColorProvider, IFontProvider {

  private Font italicFont;
  public RepositoryViewLabelProvider(Font treeFont){
    int size = 0;
    FontData[] data = treeFont.getFontData();
    if(data == null){
      size = 12;
    } else {
      for(int i=0;i<data.length;i++){
        size = Math.max(size, data[i].getHeight());
      }
    }
    italicFont = M2EUtils.deriveFont(treeFont, SWT.ITALIC, size);
  }
  
  
  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
   */
  public void dispose() {
    italicFont.dispose();
    super.dispose();
  }


  public String getText(Object obj) {
    if(obj instanceof IMavenRepositoryNode){
      return ((IMavenRepositoryNode)obj).getName();
    }
    return obj.toString();
  }

  public Image getImage(Object obj) {
    if(obj instanceof IMavenRepositoryNode){
      return ((IMavenRepositoryNode)obj).getImage();
    }
    return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
  }

  public Color getBackground(Object element) {
    return null;
  }

  public Color getForeground(Object element) {
    if(element instanceof RepositoryNode){
      if(((RepositoryNode)element).isEnabledIndex()){
        return Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
      }
      return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
    } 
    return Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
  }

  public Font getFont(Object element) {
    if(element instanceof IMavenRepositoryNode){
      boolean updating = ((IMavenRepositoryNode)element).isUpdating();
      return updating ? italicFont : null;
    }
    return null;
  }

}
