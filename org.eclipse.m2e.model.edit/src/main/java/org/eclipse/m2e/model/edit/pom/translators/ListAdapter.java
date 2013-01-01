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

package org.eclipse.m2e.model.edit.pom.translators;

import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;

import org.eclipse.m2e.model.edit.pom.PomFactory;


/**
 * Translates a multi valued feature into a <foos> <foo>bar</foo> </foos> structure.
 * 
 * @author Mike Poindexter
 */
@SuppressWarnings("restriction")
public class ListAdapter extends TranslatorAdapter {
  protected List list;

  private EClass elementType;

  public ListAdapter(SSESyncResource resc, Element containerNode, List<?> list, EClass elementType) {
    super(resc);
    this.node = containerNode;
    this.elementType = elementType;
    this.list = list;
    this.resource = resc;
  }

  public void notifyChanged(INodeNotifier notifier, int eventType, Object changedFeature, Object oldValue,
      Object newValue, int pos) {
    if(resource.isProcessEvents()) {
      try {
        resource.setProcessEvents(false);
        if(INodeNotifier.ADD == eventType && newValue instanceof Element) {
          if(notifier == node) {
            IDOMElement addedElement = (IDOMElement) newValue;
            int idx = absoluteIndexOf(node, addedElement);
            if(idx == -1)
              idx = 0;
            list.add(idx, getObject(addedElement, true));
          }
        } else if(INodeNotifier.REMOVE == eventType && oldValue instanceof Element) {
          if(notifier == node) {
            // Remove the corresponding object from the model.
            Object o = getObject((Element) oldValue, false);
            if(o instanceof String && o.toString().length() == 0) {
              // the removed oldValue has unfortunately no text value in it, so no way to identify the
              // remove entry.
              // -> just remove all and add the ones that stayed around.
              // only after checking that the object to be removed is a string, just to be sure..
              NodeList lst = node.getChildNodes();
              list.clear();
              for(int i = 0; i < lst.getLength(); i++ ) {
                Node nd = lst.item(i);
                if(nd instanceof Element) {
                  list.add(getElementText((Element) nd));
                }
              }
            } else if(o != null) {
              list.remove(o);
            }
            // TODO: What to do here? We don't know which model
            // child
            // to remove. I don't think this can happen. -MDP
          }
        } else if(changedFeature instanceof Text && elementType == null) {
          if(notifier != node && notifier instanceof Element) {
            Element e = (Element) notifier;
            int idx = absoluteIndexOf(node, e);
            if(idx < 0)
              idx = 0;
            list.remove(idx);
            list.add(idx, getObject(e, true));
          }
        }
      } finally {
        resource.setProcessEvents(true);
      }
    }
  }

  public void add(Object newValue, int position) {
    Object value = getElementValue(newValue);
    if(value instanceof EObject) {
      EObject eo = (EObject) value;
      if(EcoreUtil.getAdapter(eo.eAdapters(), ModelObjectAdapter.class) == null) {
        String tagName = getElementName(newValue);
        Element newElement = node.getOwnerDocument().createElement(tagName);
        Node before = getNthChildWithName(node, "*", position); //$NON-NLS-1$
        if(before != null) {
          node.insertBefore(newElement, before);
        } else {
          node.appendChild(newElement);
        }

        ModelObjectAdapter newAdapter = new ModelObjectAdapter(resource, eo, newElement);
        eo.eAdapters().add(newAdapter);
        formatNode(newElement);
        ((IDOMNode) newElement).addAdapter(newAdapter);
        newAdapter.save();

      }
    } else {
      String tagName = getElementName(newValue);
      Element newElement = node.getOwnerDocument().createElement(tagName);
      org.w3c.dom.Text text = node.getOwnerDocument().createTextNode(value.toString());
      newElement.appendChild(text);
      Node before = getNthChildWithName(node, "*", position); //$NON-NLS-1$
      if(before != null) {
        node.insertBefore(newElement, before);
      } else {
        node.appendChild(newElement);
      }
      formatNode(newElement);
      ((IDOMNode) newElement).addAdapter(this);
    }
  }

  public void remove(Object oldValue, int position) {
    if(position == -1) {
      position = 0;
    }
    Element n = getNthChildWithName(node, "*", position); //$NON-NLS-1$

    if(n != null)
      removeChildElement(n);
  }

  @Override
  public void update(Object oldValue, Object newValue, int index) {
    remove(oldValue, index);
    add(newValue, index);
  }

  protected String getElementName(Object o) {
    String name = node.getLocalName();
    if(name.endsWith("ies")) //$NON-NLS-1$
      name = name.replaceAll("ies$", "y"); //$NON-NLS-1$ //$NON-NLS-2$
    else
      name = name.replaceAll("s$", ""); //$NON-NLS-1$ //$NON-NLS-2$
    return name;
  }

  protected Object getElementValue(Object o) {
    return o;
  }

  protected Object getObject(Element childElement, boolean createIfNeeded) {
    if(elementType == null) {
      ListAdapter existing = (ListAdapter) ((IDOMNode) childElement).getExistingAdapter(ListAdapter.class);
      if(existing == null) {
        ((IDOMNode) childElement).addAdapter(this);
      }
      return getElementText(childElement);
    } else {
      ModelObjectAdapter existing = (ModelObjectAdapter) ((IDOMNode) childElement)
          .getExistingAdapter(ModelObjectAdapter.class);
      if(existing == null) {
        if(createIfNeeded) {
          EObject eo = PomFactory.eINSTANCE.create(elementType);
          existing = new ModelObjectAdapter(resource, eo, childElement);
          eo.eAdapters().add(existing);
          ((IDOMNode) childElement).addAdapter(existing);
          existing.load();
          return eo;
        } else {
          return null;
        }
      } else {
        return existing.getTarget();
      }
    }
  }

  public boolean isAdapterForType(Object type) {
    return ListAdapter.class.equals(type);
  }

  @Override
  public void load() {
    //MNGECLIPSE-2345, MNGECLIPSE-2694 when load is called on a list adapter already containing items, 
    // the old items shall be discarded to avoid duplicates.
    list.clear();
    NodeList children = node.getChildNodes();
    int nChildren = children.getLength();
    for(int i = 0; i < nChildren; i++ ) {
      Node child = children.item(i);
      if(child instanceof Element) {
        list.add(getObject((Element) child, true));
      }
    }
  }

  @Override
  public void save() {
    for(Object o : list) {
      add(o, -1);
    }
  }
}
