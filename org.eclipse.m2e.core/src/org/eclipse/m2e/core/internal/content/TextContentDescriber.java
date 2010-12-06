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

package org.eclipse.m2e.core.internal.content;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.ITextContentDescriber;

/**
 * A copy of org.eclipse.core.internal.content.TextContentDescriber to avoid internal API use.
 * 
 * This class provides internal basis for text-based content describers. 
 * 
 * <p>
 * Note: do not add protected/public members to this class if you don't intend to 
 * make them public API.
 * </p>  
 * 
 * @see org.eclipse.core.runtime.content.XMLRootElementContentDescriber2
 * @since 3.0
 */
class TextContentDescriber implements ITextContentDescriber {

  private final static QualifiedName[] SUPPORTED_OPTIONS = {IContentDescription.BYTE_ORDER_MARK};

  /*
   *  (non-Javadoc)
   * @see org.eclipse.core.runtime.content.ITextContentDescriber#describe(java.io.Reader, org.eclipse.core.runtime.content.IContentDescription)
   */
  @SuppressWarnings("unused")
  public int describe(Reader contents, IContentDescription description) throws IOException {
    // we want to be pretty loose on detecting the text content type  
    return INDETERMINATE;
  }

  /*
   *  (non-Javadoc)
   * @see org.eclipse.core.runtime.content.IContentDescriber#describe(java.io.InputStream, org.eclipse.core.runtime.content.IContentDescription)
   */
  public int describe(InputStream contents, IContentDescription description) throws IOException {
    if (description == null || !description.isRequested(IContentDescription.BYTE_ORDER_MARK))
      return INDETERMINATE;
    byte[] bom = getByteOrderMark(contents);
    if (bom != null)
      description.setProperty(IContentDescription.BYTE_ORDER_MARK, bom);
    // we want to be pretty loose on detecting the text content type      
    return INDETERMINATE;
  }

  /*
   *  (non-Javadoc)
   * @see org.eclipse.core.runtime.content.IContentDescriber#getSupportedOptions()
   */
  public QualifiedName[] getSupportedOptions() {
    return SUPPORTED_OPTIONS;
  }

  byte[] getByteOrderMark(InputStream input) throws IOException {
    int first = input.read();
    if (first == 0xEF) {
      //look for the UTF-8 Byte Order Mark (BOM)
      int second = input.read();
      int third = input.read();
      if (second == 0xBB && third == 0xBF)
        return IContentDescription.BOM_UTF_8;
    } else if (first == 0xFE) {
      //look for the UTF-16 BOM
      if (input.read() == 0xFF)
        return IContentDescription.BOM_UTF_16BE;
    } else if (first == 0xFF) {
      if (input.read() == 0xFE)
        return IContentDescription.BOM_UTF_16LE;
    }
    return null;
  }
}
