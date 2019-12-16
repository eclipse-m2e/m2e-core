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

package org.eclipse.m2e.model.edit.pom.translators;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.ibm.icu.lang.UCharacter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;


/**
 * A base class for all adapters that can translate a EMF to DOM and vice-versa. Each translator adaptor is expected to
 * have a single root element that controls its existence. It is responsible for the subtree of that node by updating it
 * directly or delegating to child adapters to do the work.
 * 
 * @author Mike Poindexter
 */
public abstract class TranslatorAdapter implements INodeAdapter {
  protected SSESyncResource resource;

  protected Element node;

  public TranslatorAdapter(SSESyncResource resource) {
    this.resource = resource;
  }

  /**
   * Returns the textual value of an element.
   * 
   * @param e
   * @return
   */
  protected static String getElementText(Element e) {
    StringBuilder ret = new StringBuilder();
    NodeList children = e.getChildNodes();
    int nChildren = children.getLength();
    for(int i = 0; i < nChildren; i++ ) {
      Node child = children.item(i);
      if(child instanceof Text) {
        ret.append(((Text) child).getData());
      }
    }
    return ret.toString().trim();
  }

  /**
   * Load the model value from this adapter's xml value
   */
  public abstract void load();

  /**
   * Save the xml value of this adapter from the model.
   */
  public abstract void save();

  /**
   * @param oldValue
   */
  public abstract void update(Object oldValue, Object newValue, int index);

  /**
   * Returns the index of the given element in the list of elements of the same name.
   * 
   * @param e
   * @return
   */
  protected int namedIndexOf(Element parentNode, Element element) {
    int ret = 0;
    NodeList children = parentNode.getChildNodes();
    int nChildren = children.getLength();
    for(int i = 0; i < nChildren; i++ ) {
      Node child = children.item(i);
      if(child instanceof Element) {
        Element e = (Element) child;
        if(e.getLocalName().equals(element.getLocalName())) {
          if(e == element) {
            return ret;
          }
          ret++ ;
        }
      }
    }
    return -1;
  }

  /**
   * Returns the index of the given element in the list of child elements.
   * 
   * @param e
   * @return
   */
  protected int absoluteIndexOf(Element parentNode, Element element) {
    int ret = 0;
    NodeList children = parentNode.getChildNodes();
    int nChildren = children.getLength();
    for(int i = 0; i < nChildren; i++ ) {
      Node child = children.item(i);
      if(child instanceof Element) {
        Element e = (Element) child;
        if(e.getLocalName().equals(element.getLocalName())) {
          if(e == element) {
            return ret;
          }
          ret++ ;
        }
      }
    }
    return -1;
  }

  /**
   * Returns the first child with the given name, or null if none exists.
   * 
   * @param name
   * @return
   */
  protected Element getFirstChildWithName(Element parent, String name) {
    return getNthChildWithName(parent, name, 0);
  }

  /**
   * Returns the nth child element with a given name, or null if no such element exists.
   * 
   * @param name
   * @param n
   * @return
   */
  protected Element getNthChildWithName(Element parent, String name, int n) {
    int matchCount = 0;
    NodeList children = parent.getChildNodes();
    int nChildren = children.getLength();
    for(int i = 0; i < nChildren; i++ ) {
      Node child = children.item(i);
      if(child instanceof Element) {
        Element e = (Element) child;
        if(e.getTagName().equals(name) || "*".equals(name)) { //$NON-NLS-1$
          if(matchCount == n) {
            return e;
          }
          matchCount++ ;
        }
      }
    }
    return null;
  }

  public Element getNode() {
    return node;
  }

  public void setNode(Element node) {
    this.node = node;
  }

  protected void formatNode(Element element) {
    createWSBefore(element);
    createWSAfter(element);
  }

  /**
   * Ensure at least one NL between this node and the previous, and proper start tag indentation.
   * 
   * @param element
   * @return
   */
  protected void createWSBefore(Element element) {
    try {
      IStructuredDocument doc = ((IDOMNode) element).getStructuredDocument();
      int nodeStartOff = ((IDOMNode) element).getStartOffset();
      StringBuilder betweenText = new StringBuilder();
      int i = nodeStartOff - 1;
      while(i > -1) {
        char next = doc.getChar(i);
        if(next == '>') {
          break;
        }
        betweenText.insert(0, next);
        i-- ;
      }
      int origLen = betweenText.length();
      int nlIndex = betweenText.lastIndexOf("\n"); //$NON-NLS-1$
      if(nlIndex == -1) {
        String nl = getNewlineString();
        betweenText.insert(0, nl);
        nlIndex = nl.length() - 1;
      }

      String indent = getIndentForNode(element);
      if(!indent.equals(betweenText.substring(nlIndex + 1))) {
        betweenText.replace(nlIndex + 1, betweenText.length(), indent);
      }
      if(origLen > 0) {
        doc.replaceText(this, i + 1, origLen, betweenText.toString());
      } else {
        Text t = element.getOwnerDocument().createTextNode(betweenText.toString());
        element.getParentNode().insertBefore(t, element);
      }
    } catch(BadLocationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  /**
   * Ensure at least one NL between this node and the next, and proper indent to the next tag tag indentation.
   * 
   * @param element
   * @return
   */
  protected void createWSAfter(Element element) {
    try {
      IStructuredDocument doc = ((IDOMNode) element).getStructuredDocument();
      int nodeEndOff = ((IDOMNode) element).getEndOffset();
      StringBuilder betweenText = new StringBuilder();
      int i = nodeEndOff;
      while(i < doc.getLength()) {
        char next = doc.getChar(i);
        if(next == '<') {
          break;
        }
        betweenText.append(next);
        i++ ;
      }
      int origLen = betweenText.length();
      int nlIndex = betweenText.lastIndexOf("\n"); //$NON-NLS-1$
      if(nlIndex == -1) {
        String nl = getNewlineString();
        betweenText.insert(0, nl);
        nlIndex = nl.length() - 1;
      }

      Node refNode = element.getNextSibling();
      while(refNode != null && !(refNode instanceof Element)) {
        refNode = refNode.getNextSibling();
      }
      String indent = ""; //$NON-NLS-1$
      if(refNode == null) {
        indent = getIndentBeforeStartTag(element.getParentNode());
      } else {
        indent = getIndentForNode((Element) refNode);
      }
      if(!indent.equals(betweenText.substring(nlIndex + 1))) {
        betweenText.replace(nlIndex + 1, betweenText.length(), indent);
      }
      if(origLen > 0) {
        doc.replaceText(this, nodeEndOff, origLen, betweenText.toString());
      } else {
        Text t = element.getOwnerDocument().createTextNode(betweenText.toString());

        if(null == refNode) {
          element.getParentNode().appendChild(t);
        } else {
          element.getParentNode().insertBefore(t, refNode);
        }
      }
    } catch(BadLocationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  protected void removeChildElement(Element e) {
    IStructuredDocument doc = ((IDOMNode) e).getStructuredDocument();
    int nodeStartOff = ((IDOMNode) e).getStartOffset();
    int nodeEndOff = ((IDOMNode) e).getEndOffset();
    int i = nodeStartOff - 1;

    while(i > 0) {
      char c = ' ';
      try {
        c = doc.getChar(i);
      } catch(BadLocationException ble) {
        // We check for bad locations so this should not happen
      }
      if(UCharacter.isWhitespace(c)) {
        i-- ;
      }

      if(c == '\n') {
        if(i > 0) {
          try {
            c = doc.getChar(i);
          } catch(BadLocationException ble) {
            // We check for bad locations so this should not happen
          }
          if(c == '\r')
            i-- ;
        }
        break;
      }

    }
    doc.replaceText(this, i + 1, nodeEndOff - i - 1, null);
  }

  private String getIndentForNode(Element node) {
    String ret = null;
    Node prev = node.getPreviousSibling();
    while(prev != null) {
      if(prev instanceof Element) {
        ret = getIndentBeforeStartTag(prev);
        break;
      }
      prev = prev.getPreviousSibling();
    }

    if(null == ret) {
      ret = getIndentBeforeStartTag(node.getParentNode()) + "\t"; //$NON-NLS-1$
    }
    return ret;
  }

  private String getIndentBeforeStartTag(Node node) {
    StringBuilder builder = new StringBuilder(100);
    IStructuredDocument doc = ((IDOMNode) node).getStructuredDocument();
    int nodeStartOff = ((IDOMNode) node).getStartOffset();
    int i = nodeStartOff - 1;
    while(i > 0) {
      char c = ' ';
      try {
        c = doc.getChar(i);
      } catch(BadLocationException e) {
        // We check for bad locations so this should not happen
      }
      if(UCharacter.isWhitespace(c) && !(c == '\r' || c == '\n')) {
        builder.insert(0, c);
        i-- ;
      } else {
        break;
      }
    }
    return builder.toString();
  }

  private String getNewlineString() {
    return ((IDOMNode) node).getStructuredDocument().getLineDelimiter();
  }

}
