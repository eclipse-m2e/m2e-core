/*******************************************************************************
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Andrew Eisenberg - Work on Bug 350414
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.editing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.undo.IStructuredTextUndoManager;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.format.FormatProcessorXML;


/**
 * this class contains tools for editing the pom files using dom tree operations.
 *
 * @author mkleint
 */
@SuppressWarnings("restriction")
public class PomEdits {

  public static final String NAMESPACE = "http://maven.apache.org/POM/4.0.0"; //$NON-NLS-1$

  public static final String NAMESPACE_LOCATION = "http://maven.apache.org/xsd/maven-4.0.0.xsd"; //$NON-NLS-1$

  public static final String PROJECT = "project"; //$NON-NLS-1$

  public static final String MODEL_VERSION = "modelVersion"; //$NON-NLS-1$

  public static final String MODEL_VERSION_VALUE = "4.0.0"; //$NON-NLS-1$

  public static final String DEPENDENCIES = "dependencies"; //$NON-NLS-1$

  public static final String GROUP_ID = "groupId";//$NON-NLS-1$

  public static final String ARTIFACT_ID = "artifactId"; //$NON-NLS-1$

  public static final String DEPENDENCY = "dependency"; //$NON-NLS-1$

  public static final String DEPENDENCY_MANAGEMENT = "dependencyManagement"; //$NON-NLS-1$

  public static final String EXCLUSIONS = "exclusions"; //$NON-NLS-1$

  public static final String EXCLUSION = "exclusion"; //$NON-NLS-1$

  public static final String VERSION = "version"; //$NON-NLS-1$

  public static final String PLUGIN = "plugin"; //$NON-NLS-1$

  public static final String CONFIGURATION = "configuration";//$NON-NLS-1$

  public static final String PLUGINS = "plugins";//$NON-NLS-1$

  public static final String PLUGIN_MANAGEMENT = "pluginManagement";//$NON-NLS-1$

  public static final String BUILD = "build";//$NON-NLS-1$

  public static final String PARENT = "parent";//$NON-NLS-1$

  public static final String RELATIVE_PATH = "relativePath";//$NON-NLS-1$

  public static final String TYPE = "type";//$NON-NLS-1$

  public static final String CLASSIFIER = "classifier";//$NON-NLS-1$

  public static final String OPTIONAL = "optional";//$NON-NLS-1$

  public static final String SCOPE = "scope";//$NON-NLS-1$

  public static final String MODULES = "modules";//$NON-NLS-1$

  public static final String MODULE = "module";//$NON-NLS-1$

  public static final String PROFILE = "profile";//$NON-NLS-1$

  public static final String ID = "id";//$NON-NLS-1$

  public static final String NAME = "name"; //$NON-NLS-1$

  public static final String URL = "url";//$NON-NLS-1$

  public static final String DESCRIPTION = "description";//$NON-NLS-1$

  public static final String INCEPTION_YEAR = "inceptionYear";//$NON-NLS-1$

  public static final String ORGANIZATION = "organization"; //$NON-NLS-1$

  public static final String SCM = "scm"; //$NON-NLS-1$

  public static final String CONNECTION = "connection";//$NON-NLS-1$

  public static final String DEV_CONNECTION = "developerConnection";//$NON-NLS-1$

  public static final String TAG = "tag";//$NON-NLS-1$

  public static final String ISSUE_MANAGEMENT = "issueManagement"; //$NON-NLS-1$

  public static final String SYSTEM = "system"; //$NON-NLS-1$

  public static final String SYSTEM_PATH = "systemPath"; //$NON-NLS-1$

  public static final String CI_MANAGEMENT = "ciManagement"; //$NON-NLS-1$

  public static final String PACKAGING = "packaging"; //$NON-NLS-1$

  public static final String PROPERTIES = "properties"; //$NON-NLS-1$

  public static final String EXTENSION = "extension"; //$NON-NLS-1$

  public static final String EXTENSIONS = "extensions"; //$NON-NLS-1$

  public static final String PROFILES = "profiles";//$NON-NLS-1$

  public static final String EXECUTIONS = "executions"; //$NON-NLS-1$

  public static final String EXECUTION = "execution";//$NON-NLS-1$

  public static final String GOAL = "goal";//$NON-NLS-1$

  public static final String GOALS = "goals";//$NON-NLS-1$

  public static Element findChild(Element parent, String name) {
    if(parent == null) {
      return null;
    }
    NodeList rootList = parent.getChildNodes();
    for(int i = 0; i < rootList.getLength(); i++ ) {
      Node nd = rootList.item(i);
      if(nd instanceof Element el) {
        if(name.equals(el.getNodeName())) {
          return el;
        }
      }
    }
    return null;
  }

  public static List<Element> findChilds(Element parent, String name) {
    List<Element> toRet = new ArrayList<>();
    if(parent != null) {
      NodeList rootList = parent.getChildNodes();
      for(int i = 0; i < rootList.getLength(); i++ ) {
        Node nd = rootList.item(i);
        if(nd instanceof Element el) {
          if(name.equals(el.getNodeName())) {
            toRet.add(el);
          }
        }
      }
    }
    return toRet;
  }

  public static String getTextValue(Node element) {
    if(element == null)
      return null;
    StringBuilder buff = new StringBuilder();
    NodeList list = element.getChildNodes();
    for(int i = 0; i < list.getLength(); i++ ) {
      Node child = list.item(i);
      if(child instanceof Text text) {
        buff.append(text.getData().trim()); //352416 the value is trimmed because of the multiline values
        //that get trimmed by maven itself as well, any comparison to resolved model needs to do the trimming
        // or risks false negative results.
      }
    }
    return buff.toString();
  }

  /**
   * finds exactly one (first) occurence of child element with the given name (eg. dependency) that fulfills conditions
   * expressed by the Matchers (eg. groupId/artifactId match)
   *
   * @param parent
   * @param name
   * @param matchers
   * @return
   */
  public static Element findChild(Element parent, String name, Matcher... matchers) {
    OUTTER: for(Element el : findChilds(parent, name)) {
      for(Matcher match : matchers) {
        if(!match.matches(el)) {
          continue OUTTER;
        }
      }
      return el;
    }
    return null;
  }

  /**
   * helper method, creates a subelement with text embedded. does not format the result. primarily to be used in cases
   * like <code>&lt;goals&gt;&lt;goal&gt;xxx&lt;/goal&gt;&lt;/goals&gt;</code>
   *
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
   * helper method, creates a subelement, does not format result.
   *
   * @param parent the parent element
   * @param name the name of the new element
   * @return the created element
   */
  public static Element createElement(Element parent, String name) {
    Document doc = parent.getOwnerDocument();
    Element newElement = doc.createElement(name);
    parent.appendChild(newElement);
    return newElement;
  }

  /**
   * sets text value to the given element. any existing text children are removed and replaced by this new one.
   *
   * @param element
   * @param value
   */
  public static void setText(Element element, String value) {
    NodeList list = element.getChildNodes();
    List<Node> toRemove = new ArrayList<>();
    for(int i = 0; i < list.getLength(); i++ ) {
      Node child = list.item(i);
      if(child instanceof Text) {
        toRemove.add(child);
      }
    }
    for(Node rm : toRemove) {
      element.removeChild(rm);
    }
    Document doc = element.getOwnerDocument();
    element.appendChild(doc.createTextNode(value));
  }

  /**
   * unlike the findChild() equivalent, this one creates the element if not present and returns it. Therefore it shall
   * only be invoked within the PomEdits.Operation
   *
   * @param parent
   * @param names chain of element names to find/create
   * @return
   */
  public static Element getChild(Element parent, String... names) {
    Element toFormat = null;
    Element toRet = null;
    if(names.length == 0) {
      throw new IllegalArgumentException("At least one child name has to be specified");
    }
    for(String name : names) {
      toRet = findChild(parent, name);
      if(toRet == null) {
        toRet = parent.getOwnerDocument().createElement(name);
        parent.appendChild(toRet);
        if(toFormat == null) {
          toFormat = toRet;
        }
      }
      parent = toRet;
    }
    if(toFormat != null) {
      format(toFormat);
    }
    return toRet;
  }

  /**
   * proper remove of a child element
   */
  public static void removeChild(Element parent, Element child) {
    if(child != null) {
      Node prev = child.getPreviousSibling();
      if(prev instanceof Text txt) {
        int lastnewline = getLastEolIndex(txt.getData());
        if(lastnewline >= 0) {
          txt.setData(txt.getData().substring(0, lastnewline));
        }
      }
      parent.removeChild(child);
    }
  }

  private static int getLastEolIndex(String s) {
    if(s == null || s.length() == 0) {
      return -1;
    }
    for(int i = s.length() - 1; i >= 0; i-- ) {
      char c = s.charAt(i);
      if(c == '\r') {
        return i;
      }
      if(c == '\n') {
        if(i > 0 && s.charAt(i - 1) == '\r') {
          return i - 1;
        }
        return i;
      }
    }
    return -1;
  }

  /**
   * remove the current element if it doesn't contain any sublements, useful for lists etc, works recursively removing
   * all parents up that don't have any children elements.
   *
   * @param el
   */
  public static void removeIfNoChildElement(Element el) {
    NodeList nl = el.getChildNodes();
    boolean hasChilds = false;
    for(int i = 0; i < nl.getLength(); i++ ) {
      Node child = nl.item(i);
      if(child instanceof Element) {
        hasChilds = true;
      }
    }
    if(!hasChilds) {
      Node parent = el.getParentNode();
      if(parent instanceof Element parentElement) {
        removeChild(parentElement, el);
        removeIfNoChildElement(parentElement);
      }
    }
  }

  public static Element insertAt(Element newElement, int offset) {
    Document doc = newElement.getOwnerDocument();
    if(doc instanceof IDOMDocument domDoc) {
      IndexedRegion ir = domDoc.getModel().getIndexedRegion(offset);
      Node parent = ((Node) ir).getParentNode();
      if(ir instanceof Text txt) {
        String data = txt.getData();
        int dataSplitIndex = offset - ir.getStartOffset();
        String beforeText = data.substring(0, dataSplitIndex);
        String afterText = data.substring(dataSplitIndex);
        Text after = doc.createTextNode(afterText);
        Text before = doc.createTextNode(beforeText);
        parent.replaceChild(after, txt);
        parent.insertBefore(newElement, after);
        parent.insertBefore(before, newElement);
      } else if(ir instanceof Element element) {
        if(ir.getStartOffset() == offset) {
          // caret is before the tag, not within its bounds
          parent.insertBefore(newElement, element);
        } else {
          element.appendChild(newElement);
        }
      } else {
        throw new IllegalArgumentException();
      }
    } else {
      throw new IllegalArgumentException();
    }
    return newElement;
  }

  /**
   * finds the element at offset, if other type of node at offset, will return it's parent element (if any)
   *
   * @param doc
   * @param offset
   * @return
   */
  public static Element elementAtOffset(Document doc, int offset) {
    if(doc instanceof IDOMDocument domDoc) {
      IndexedRegion ir = domDoc.getModel().getIndexedRegion(offset);
      if(ir instanceof Element elem) {
        if(ir.getStartOffset() == offset) {
          // caret is before the tag, not within its bounds
          elem = (Element) elem.getParentNode();
        }
        return elem;
      }
      Node parent = ((Node) ir).getParentNode();
      if(parent instanceof Element parentElement) {
        return parentElement;
      }
    }
    return null;
  }

  /**
   * formats the node (and content). please make sure to only format the node you have created..
   *
   * @param newNode
   */
  public static void format(Node newNode) {
    Node parentNode = newNode.getParentNode();
    if(parentNode != null && newNode.equals(parentNode.getLastChild())) {
      //add a new line to get the newly generated content correctly formatted.
      Document ownerDocument;
      if(parentNode instanceof Document doc) {
        ownerDocument = doc;
      } else {
        ownerDocument = parentNode.getOwnerDocument();
      }
      parentNode.appendChild(ownerDocument.createTextNode("\n")); //$NON-NLS-1$
    }
    FormatProcessorXML formatProcessor = new FormatProcessorXML();
    //ignore any line width settings, causes wrong formatting of <foo>bar</foo>
    formatProcessor.getFormatPreferences().setLineWidth(2000);
    formatProcessor.formatNode(newNode);
  }

  /**
   * performs an modifying operation on top the
   *
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
        DocumentRewriteSession session = null;
        IStructuredTextUndoManager undo = null;
        if(tuple.isReadOnly()) {
          domModel = (IDOMModel) StructuredModelManager.getModelManager().getExistingModelForRead(tuple.getDocument());
          if(domModel == null) {
            domModel = (IDOMModel) StructuredModelManager.getModelManager().getModelForRead(
                (IStructuredDocument) tuple.getDocument());
          }
        } else {
          domModel = tuple.getModel() != null ? tuple.getModel()
              : (tuple.getFile() != null ? (IDOMModel) StructuredModelManager.getModelManager().getModelForEdit(
                  tuple.getFile()) : (IDOMModel) StructuredModelManager.getModelManager().getExistingModelForEdit(
                  tuple.getDocument())); //existing shall be ok here..

          //let the model know we make changes
          domModel.aboutToChangeModel();
          undo = domModel.getStructuredDocument().getUndoManager();
          //let the document know we make changes
          if(domModel.getStructuredDocument() instanceof IDocumentExtension4 ext4) {
            session = ext4.startRewriteSession(DocumentRewriteSessionType.UNRESTRICTED_SMALL);
          }
          undo.beginRecording(domModel);

          // fill with minimal pom content
          Document doc = domModel.getDocument();
          if(doc.getDocumentElement() == null) {

            Node first = doc.getFirstChild();
            if(first == null || !(first instanceof ProcessingInstruction)) {
              doc.insertBefore(doc.createProcessingInstruction("xml", "version=\"1.0\" encoding=\"UTF-8\""), first); //$NON-NLS-1$ //$NON-NLS-2$
              doc.insertBefore(doc.createTextNode("\n"), first); //$NON-NLS-1$
            }

            Element project = doc.createElement(PROJECT);
            project.setAttribute("xmlns", NAMESPACE); //$NON-NLS-1$
            project.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance"); //$NON-NLS-1$ //$NON-NLS-2$
            project.setAttribute("xsi:schemaLocation", NAMESPACE + " " + NAMESPACE_LOCATION); //$NON-NLS-1$ //$NON-NLS-2$
            doc.appendChild(project);

            Element modelVersion = doc.createElement(MODEL_VERSION);
            modelVersion.appendChild(doc.createTextNode(MODEL_VERSION_VALUE));
            project.appendChild(modelVersion);
            format(project);
          }
        }

        if(domModel != null) {
          try {
            tuple.getOperation().process(domModel.getDocument());
          } finally {
            if(!tuple.isReadOnly()) {
              undo.endRecording(domModel);
              if(session != null && domModel.getStructuredDocument() instanceof IDocumentExtension4 ext4) {
                ext4.stopRewriteSession(session);
              }
              domModel.changedModel();
            }
          }
        }
      } finally {
        if(domModel != null) {
          if(tuple.isReadOnly()) {
            domModel.releaseFromRead();
          } else if(domModel.getId() != null) { // id will be null for files outside of workspace
            //for ducuments saving shall generally only happen when the model is not held elsewhere (eg. in opened view)
            //for files, save always
            if(tuple.isForceSave() || domModel.getReferenceCountForEdit() == 1) {
              domModel.save();
            }
            domModel.releaseFromEdit();
          }
        }
      }
    }
  }

  public static final class OperationTuple {
    private final PomEdits.Operation operation;

    private final IFile file;

    private final IDocument document;

    private final IDOMModel model;

    private boolean readOnly = false;

    private boolean forceSave = false;

    /**
     * operation on top of IFile is always saved
     *
     * @param file
     * @param operation
     */
    public OperationTuple(IFile file, PomEdits.Operation operation) {
      assert file != null;
      assert operation != null;
      this.file = file;
      this.operation = operation;
      document = null;
      model = null;
      forceSave = true;
    }

    /**
     * operation on top of IDocument is only saved when noone else is editing the document.
     *
     * @param document
     * @param operation
     */
    public OperationTuple(IDocument document, PomEdits.Operation operation) {
      this(document, operation, false);
    }

    /**
     * operation on top of IDocument is only saved when noone else is editing the document.
     *
     * @param document
     * @param operation
     * @param readonly operation that doesn't modify the content. Will only get the read, not edit model, up to the user
     *          of the code to ensure no edits happen
     */
    public OperationTuple(IDocument document, PomEdits.Operation operation, boolean readOnly) {
      assert operation != null;
      this.document = document;
      this.operation = operation;
      file = null;
      model = null;
      this.readOnly = readOnly;
    }

    /**
     * only use for unmanaged models
     *
     * @param model
     * @param operation
     */
    public OperationTuple(IDOMModel model, PomEdits.Operation operation) {
      assert model != null;
      this.operation = operation;
      this.model = model;
      document = null;
      file = null;
    }

    /**
     * force saving the document after performing the operation
     */
    public void setForceSave() {
      forceSave = true;
    }

    public boolean isForceSave() {
      return forceSave;
    }

    /**
     * @return Returns the readOnly.
     */
    public boolean isReadOnly() {
      return readOnly;
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

    public IDOMModel getModel() {
      return model;
    }

  }

  /**
   * operation to perform on top of the DOM document. see performOnDOMDocument()
   *
   * @author mkleint
   */
  public interface Operation {
    void process(Document document);
  }

  /**
   * an Operation instance that aggregates multiple operations and performs then in given order.
   *
   * @author mkleint
   */
  public static final class CompoundOperation implements Operation {

    private final Operation[] operations;

    public CompoundOperation(Operation... operations) {
      this.operations = operations;
    }

    @Override
    public void process(Document document) {
      for(Operation oper : operations) {
        oper.process(document);
      }
    }
  }

  /**
   * an interface for identifying child elements that fulfill conditions expressed by the matcher.
   *
   * @author mkleint
   */
  public interface Matcher {
    /**
     * returns true if the given element matches the condition.
     *
     * @param child
     * @return
     */
    boolean matches(Element element);
  }

  public static Matcher childEquals(final String elementName, final String matchingValue) {
    return child -> {
      String toMatch = PomEdits.getTextValue(PomEdits.findChild(child, elementName));
      return toMatch != null && toMatch.trim().equals(matchingValue);
    };
  }

  public static Matcher textEquals(final String matchingValue) {
    return child -> {
      String toMatch = PomEdits.getTextValue(child);
      return toMatch != null && toMatch.trim().equals(matchingValue);
    };
  }

  public static Matcher childMissingOrEqual(final String elementName, final String matchingValue) {
    return child -> {
      Element match = PomEdits.findChild(child, elementName);
      if(match == null) {
        return true;
      }
      String toMatch = PomEdits.getTextValue(match);
      return toMatch != null && toMatch.trim().equals(matchingValue);
    };
  }

  /**
   * keeps internal state, needs to be recreated for each query, when used in conjunction with out matchers shall
   * probably be placed last.
   *
   * @param elementName
   * @param index
   * @return
   */
  public static Matcher childAt(final int index) {
    return new Matcher() {
      int count = 0;

        @Override
        public boolean matches(Element child) {
        if(count == index) {
          return true;
        }
        count++ ;
        return false;
      }
    };
  }

}
