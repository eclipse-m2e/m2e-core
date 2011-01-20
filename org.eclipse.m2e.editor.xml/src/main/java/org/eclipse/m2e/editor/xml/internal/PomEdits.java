/*******************************************************************************
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/


package org.eclipse.m2e.editor.xml.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.format.IStructuredFormatProcessor;
import org.eclipse.wst.sse.core.internal.undo.IStructuredTextUndoManager;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.format.FormatProcessorXML;

import org.eclipse.m2e.core.internal.project.MarkerUtils;

/**
 * this class contains tools for editing the pom files using dom tree operations.
 * @author mkleint
 *
 */
public class PomEdits {

  
  public static Element findChild(Element parent, String name) {
    NodeList rootList = parent.getChildNodes(); 
    for (int i = 0; i < rootList.getLength(); i++) {
        Node nd = rootList.item(i);
        if (nd instanceof Element) {
          Element el = (Element)nd;
          if (name.equals(el.getNodeName())) {
            return el;
          }
        }
    }
    return null;
  }

  public static List<Element> findChilds(Element parent, String name) {
    NodeList rootList = parent.getChildNodes();
    List<Element> toRet = new ArrayList<Element>();
    for (int i = 0; i < rootList.getLength(); i++) {
        Node nd = rootList.item(i);
        if (nd instanceof Element) {
          Element el = (Element)nd;
          if (name.equals(el.getNodeName())) {
            toRet.add(el);
          }
        }
    }
    return toRet;
  }

  public static String getTextValue(Node element) {
    if (element == null) return null;
    StringBuffer buff = new StringBuffer();
    NodeList list = element.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      Node child = list.item(i);
      if (child instanceof Text) {
        Text text = (Text)child;
        buff.append(text.getData());
      }
    }
    return buff.toString();
  }
  
  /**
   * finds exactly one (first) occurence of child element with the given name (eg. dependency)
   * that fulfills conditions expressed by the Matchers (eg. groupId/artifactId match)
   * @param parent
   * @param name
   * @param matchers
   * @return
   */
  public static Element findChild(Element parent, String name, Matcher... matchers) {
    OUTTER: for (Element el : findChilds(parent, name)) {
      for (Matcher match : matchers) {
        if (!match.matches(el)) {
          continue OUTTER;
        }
      }
      return el;
    }
    return null;
  }
  
  /**
   * node is expected to be the node containing <dependencies> node, so <project>, <dependencyManagement> etc..
   * @param node
   * @return
   */
  public static List<Element> findDependencies(Element node) {
    return findChilds(findChild(node, "dependencies"), "dependency");
  }
  
  /** for the root <project> node (or equivalent) finds or creates the <dm> and <dependencies> sections.
   * returns the <dependencies> section element.
   *  
   * @param root
   * @return
   */
  public static Element getManagedDependencies(Element root) {
    Element toRet = getChild(root, "dependencyManagement");
    toRet = getChild(toRet, "dependencies");
    return toRet;
  }
  
  /**
   * creates and adds new dependency to the parent.
   * @param parentList
   * @param groupId null or value
   * @param artifactId never null
   * @param version null or value
   * @return
   */
  public static Element createDependency(Element parentList, String groupId, String artifactId, String version) {
    Document doc = parentList.getOwnerDocument();
    Element dep = doc.createElement("dependency");
    parentList.appendChild(dep);
    
    if (groupId != null) {
      createElementWithText(dep, "groupId", groupId);
    }
    createElementWithText(dep, "artifactId", artifactId);
    if (version != null) {
      createElementWithText(dep, "version", version);
    }
    format(dep);
    return dep;
  }
  
  /**
   * creates and adds new plugin to the parent. Formats the result.
   * @param parentList
   * @param groupId null or value
   * @param artifactId never null
   * @param version null or value
   * @return
   */
  public static Element createPlugin(Element parentList, String groupId, String artifactId, String version) {
    Document doc = parentList.getOwnerDocument();
    Element plug = doc.createElement("plugin");
    parentList.appendChild(plug);
    
    if (groupId != null) {
      createElementWithText(plug, "groupId", groupId);
    }
    createElementWithText(plug, "artifactId", artifactId);
    if (version != null) {
      createElementWithText(plug, "version", version);
    }
    format(plug);
    return plug;
  }
  
  /**
   * helper method, creates a subelement with text embedded. does not format the result.
   * @param parent
   * @param name
   * @param value
   * @return
   */
  public static Element createElementWithText(Element parent, String name, String value) {
    Document doc = parent.getOwnerDocument();
    Element newElement = doc.createElement(name);
    parent.appendChild(newElement);
    newElement.appendChild(doc.createTextNode(value));
    return newElement;
  }
  
  /**
   * sets text value to the given element. any existing text children are removed and replaced by this new one. 
   * @param element
   * @param value
   */
  public static void setText(Element element, String value) {
    NodeList list = element.getChildNodes();
    List<Node> toRemove = new ArrayList<Node>();
    for (int i = 0; i < list.getLength(); i++) {
      Node child = list.item(i);
      if (child instanceof Text) {
        toRemove.add(child);
      }
    }
    for (Node rm : toRemove) {
      element.removeChild(rm);
    }
    Document doc = element.getOwnerDocument();
    element.appendChild(doc.createTextNode(value));
  }
  
  
  /**
   * unlike the findChild() equivalent, this one creates the element if not present and returns it.
   * Therefore it shall only be invoked within the PomEdits.Operation
   * @param parent
   * @param names chain of element names to find/create
   * @return
   */
  public static Element getChild(Element parent, String... names) {
    Element toFormat = null;
    Element toRet = null;
    if (names.length == 0) {
      throw new IllegalArgumentException("At least one child name has to be specified");
    }
    for (String name : names) {
      toRet = findChild(parent, name);
      if (toRet == null) {
        toRet = parent.getOwnerDocument().createElement(name);
        parent.appendChild(toRet);
        if (toFormat == null) {
          toFormat = toRet;
        }
      }
      parent = toRet;
    }
    if (toFormat != null) {
      format(toFormat);
    }
    return toRet;
  }
  
  /**
   * proper remove of a child element
   * @param parent
   * @param name
   */
  public static void removeChild(Element parent, String name) {
    Element child = PomEdits.findChild(parent, name);
    if (child != null) {
      Node prev = child.getPreviousSibling();
      if (prev instanceof Text) {
        Text txt = (Text)prev;
        int lastnewline = txt.getData().lastIndexOf("\n");
        txt.setData(txt.getData().substring(0, lastnewline));
      }
      parent.removeChild(child);
    }
  }
  
  /**
   * formats the node (and content). please make sure to only format the node you have created..
   * @param newNode
   */
  public static void format(Node newNode) {
    if (newNode.getParentNode() != null && newNode.equals(newNode.getParentNode().getLastChild())) {
      //add a new line to get the newly generated content correctly formatted.
      newNode.getParentNode().appendChild(newNode.getParentNode().getOwnerDocument().createTextNode("\n"));
    }
    FormatProcessorXML formatProcessor = new FormatProcessorXML();
    //ignore any line width settings, causes wrong formatting of <foo>bar</foo>
    formatProcessor.getFormatPreferences().setLineWidth(2000);
    formatProcessor.formatNode(newNode);
  }

  /**
   * performs an modifying operation on top the  
   * @param file
   * @param operation
   * @throws IOException
   * @throws CoreException
   */
  public static void performOnDOMDocument(PomEdits.OperationTuple... fileOperations) throws IOException, CoreException {
    for(OperationTuple tuple : fileOperations) {
      IDOMModel domModel = null;
      //TODO we might want to attempt iterating opened editors and somehow initialize those
      // that were not yet initialized. Then we could avoid saving a file that is actually opened, but was never used so far (after restart)
      try {
        domModel = tuple.getFile() != null 
            ? (IDOMModel) StructuredModelManager.getModelManager().getModelForEdit(tuple.getFile()) 
            : (IDOMModel) StructuredModelManager.getModelManager().getExistingModelForEdit(tuple.getDocument()); //existing shall be ok here..
        domModel.aboutToChangeModel();
      IStructuredTextUndoManager undo = domModel.getStructuredDocument().getUndoManager();
      undo.beginRecording(domModel);
        try {
          tuple.getOperation().process(domModel.getDocument());
        } finally {
          undo.endRecording(domModel);
          domModel.changedModel();
        }
      } finally {
        if(domModel != null) {
          //for ducuments saving shall only happen when the model is not held elsewhere (eg. in opened view)
          //for files, save always
          if(tuple.getFile() != null || domModel.getReferenceCountForEdit() == 1) {
            domModel.save();
          }
          domModel.releaseFromEdit();
        }
      }
    }
  }
  
  public static final class OperationTuple {
    private final PomEdits.Operation operation;
    private final IFile file;
    private final IDocument document;

    /**
     * operation on top of IFile is always saved
     * @param file
     * @param operation
     */
    public OperationTuple(IFile file, PomEdits.Operation operation) {
      assert file != null;
      assert operation != null;
      this.file = file;
      this.operation = operation;
      document = null;
    }
    /**
     * operation on top of IDocument is only saved when noone else is editing the document. 
     * @param document
     * @param operation
     */
    public OperationTuple(IDocument document, PomEdits.Operation operation) {
      assert operation != null;
      this.document = document;
      this.operation = operation;
      file = null;
    }
    

    public IFile getFile() {
      return file;
    }

    public PomEdits.Operation getOperation() {
      return operation;
    }

    public IDocument getDocument() {
      return document;
    }
  
  }
  
  /**
   * operation to perform on top of the DOM document. see performOnDOMDocument()
   * @author mkleint
   *
   */
  public static interface Operation {
    void process(Document document);    
  }
  
  /**
   * an Operation instance that aggregates multiple operations and performs then in given order.
   * @author mkleint
   *
   */
  public static final class CompoundOperation implements Operation {
    
    private final Operation[] operations;

    public CompoundOperation(Operation... operations) {
      this.operations = operations;
    }
    
    public void process(Document document) {
      for (Operation oper : operations) {
        oper.process(document);
      }
    }
    
  }
  
  /**
   * an interface for identifying child elements that fulfill conditions expressed by the matcher. 
   * @author mkleint
   *
   */
  public static interface Matcher {
    /**
     * returns true if the given element matches the condition.
     * @param child
     * @return
     */
    boolean matches(Element element);
  }
  
  public static Matcher childEquals(final String elementName, final String matchingValue) {
    return new Matcher() {
      
      public boolean matches(Element child) {
        String toMatch = PomEdits.getTextValue(PomEdits.findChild(child, elementName));
        return toMatch != null && toMatch.trim().equals(matchingValue); 
      }
    };
  }
  
}