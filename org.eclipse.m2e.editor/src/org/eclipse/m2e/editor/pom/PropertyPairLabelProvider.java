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

package org.eclipse.m2e.editor.pom;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.model.edit.pom.PropertyElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider for <code>PropertyPair</code>
 * 
 * @author Eugene Kuleshov
 */
public class PropertyPairLabelProvider extends LabelProvider {

  public String getText(Object element) {
    if(element instanceof PropertyElement) {
      PropertyElement pair = (PropertyElement) element;
      return NLS.bind(Messages.PropertyPairLabelProvider_0, pair.getName(), pair.getValue());
    }
    return super.getText(element);
  }
  
  public Image getImage(Object element) {
    return MavenEditorImages.IMG_PROPERTY;
  }
  
}
