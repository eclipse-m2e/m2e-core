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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.ITextContentDescriber;

/**
 * A copy of org.eclipse.core.internal.content.XMLContentDescriber to avoid internal API use.
 * 
 * A content interpreter for XML files. 
 * This class provides internal basis for XML-based content describers.
 * <p>
 * Note: do not add protected/public members to this class if you don't intend to 
 * make them public API.
 * </p>
 *
 * @see org.eclipse.core.runtime.content.XMLRootElementContentDescriber2
 * @see "http://www.w3.org/TR/REC-xml *"
 */
class XMLContentDescriber extends TextContentDescriber implements ITextContentDescriber {
  private static final QualifiedName[] SUPPORTED_OPTIONS = new QualifiedName[] {IContentDescription.CHARSET, IContentDescription.BYTE_ORDER_MARK};
  private static final String ENCODING = "encoding="; //$NON-NLS-1$
  private static final String XML_PREFIX = "<?xml "; //$NON-NLS-1$

  public int describe(InputStream input, IContentDescription description) throws IOException {
    byte[] bom = getByteOrderMark(input);
    String xmlDeclEncoding = "UTF-8"; //$NON-NLS-1$
    input.reset();
    if (bom != null) {
      if (bom == IContentDescription.BOM_UTF_16BE)
        xmlDeclEncoding = "UTF-16BE"; //$NON-NLS-1$
      else if (bom == IContentDescription.BOM_UTF_16LE)
        xmlDeclEncoding = "UTF-16LE"; //$NON-NLS-1$
      // skip BOM to make comparison simpler
      input.skip(bom.length);
      // set the BOM in the description if requested
      if (description != null && description.isRequested(IContentDescription.BYTE_ORDER_MARK))
        description.setProperty(IContentDescription.BYTE_ORDER_MARK, bom);
    }
    byte[] xmlPrefixBytes = XML_PREFIX.getBytes(xmlDeclEncoding);
    byte[] prefix = new byte[xmlPrefixBytes.length];
    if (input.read(prefix) < prefix.length)
      // there is not enough info to say anything
      return INDETERMINATE;
    for (int i = 0; i < prefix.length; i++)
      if (prefix[i] != xmlPrefixBytes[i])
        // we don't have a XMLDecl... there is not enough info to say anything
        return INDETERMINATE;
    if (description == null)
      return VALID;
    // describe charset if requested
    if (description.isRequested(IContentDescription.CHARSET)) {
      String fullXMLDecl = readFullXMLDecl(input, xmlDeclEncoding);
      if (fullXMLDecl != null) {
        String charset = getCharset(fullXMLDecl);
        if (charset != null && !"UTF-8".equalsIgnoreCase(charset)) //$NON-NLS-1$
          // only set property if value is not default (avoid using a non-default content description)
          description.setProperty(IContentDescription.CHARSET, getCharset(fullXMLDecl));
      }
    }
    return VALID;
  }

  private String readFullXMLDecl(InputStream input, String unicodeEncoding) throws IOException {
    byte[] xmlDecl = new byte[100];
    int c = 0;
    // looks for XMLDecl ending char (?)
    int read = 0;
    while (read < xmlDecl.length && (c = input.read()) != -1 && c != '?')
      xmlDecl[read++] = (byte) c;
    return c == '?' ? new String(xmlDecl, 0, read, unicodeEncoding) : null;
  }

  public int describe(Reader input, IContentDescription description) throws IOException {
    BufferedReader reader = new BufferedReader(input);
    String line = reader.readLine();
    // end of stream
    if (line == null)
      return INDETERMINATE;
    // XMLDecl should be the first string (no blanks allowed)
    if (!line.startsWith(XML_PREFIX))
      return INDETERMINATE;
    if (description == null)
      return VALID;
    // describe charset if requested
    if ((description.isRequested(IContentDescription.CHARSET)))
      description.setProperty(IContentDescription.CHARSET, getCharset(line));
    return VALID;
  }

  private String getCharset(String firstLine) {
    int encodingPos = firstLine.indexOf(ENCODING);
    if (encodingPos == -1)
      return null;
    char quoteChar = '"';
    int firstQuote = firstLine.indexOf(quoteChar, encodingPos);
    if (firstQuote == -1) {
      quoteChar = '\'';
      firstQuote = firstLine.indexOf(quoteChar, encodingPos);
    }
    if (firstQuote == -1 || firstLine.length() == firstQuote - 1)
      return null;
    int secondQuote = firstLine.indexOf(quoteChar, firstQuote + 1);
    if (secondQuote == -1)
      return null;
    return firstLine.substring(firstQuote + 1, secondQuote);
  }

  public QualifiedName[] getSupportedOptions() {
    return SUPPORTED_OPTIONS;
  }
}
