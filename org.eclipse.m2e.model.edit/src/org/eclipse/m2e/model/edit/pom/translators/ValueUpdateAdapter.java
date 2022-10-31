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

import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.wst.common.internal.emf.utilities.ExtendedEcoreUtil;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;


/**
 * Handles notifications from the DOM that a simple text value has changed.
 *
 * @author Mike Poindexter
 */
@SuppressWarnings("restriction")
class ValueUpdateAdapter extends TranslatorAdapter implements INodeAdapter {
  private final EObject modelObject;

  private final EStructuralFeature feature;

  private List<Node> linkedWhitespaceNodes = Collections.emptyList();

  public ValueUpdateAdapter(SSESyncResource resource, Element node, EObject object, EStructuralFeature feature) {
    super(resource);
    this.node = node;
    this.modelObject = object;
    this.feature = feature;
  }

  public boolean isAdapterForType(Object type) {
    return ValueUpdateAdapter.class.equals(type);
  }

  public void notifyChanged(INodeNotifier notifier, int eventType, Object changedFeature, Object oldValue,
      Object newValue, int pos) {
    if(resource.isProcessEvents()) {
      try {
        resource.setProcessEvents(false);
        if(newValue instanceof Text text) {
          changedFeature = newValue;
          newValue = text.getData();
        }
        if(changedFeature instanceof Text) {
          if(null == newValue) {
            ExtendedEcoreUtil.eUnsetOrRemove(modelObject, feature, oldValue);
          } else {
            ExtendedEcoreUtil.eSetOrAdd(modelObject, feature, newValue.toString().trim());
          }
        }
      } finally {
        resource.setProcessEvents(true);
      }

    }

  }

  @Override
  public void load() {
    Object value = getElementText(node);
    if(feature instanceof EAttribute ea) {
      value = EcoreUtil.createFromString(ea.getEAttributeType(), value.toString());
    }

    modelObject.eSet(feature, value);
  }

  @Override
  public void save() {
    setElementTextValue(node, null, modelObject.eGet(feature));
  }

  @Override
  public void update(Object oldValue, Object newValue, int index) {
    setElementTextValue(node, oldValue, modelObject.eGet(feature));
  }

  /**
   * Sets the text value of an existing node, attempting to preserve whitespace
   *
   * @param element
   * @param oldValue
   * @param newValue
   */
  private void setElementTextValue(Element element, Object oldValue, Object newValue) {
    newValue = newValue == null ? "" : newValue.toString(); //$NON-NLS-1$
    boolean replacedChild = false;

    if(oldValue != null) {
      // First try to find a text node with the old value and set it (to
      // preserve whitespace)
      NodeList children = element.getChildNodes();
      int nChildren = children.getLength();
      for(int i = 0; i < nChildren; i++ ) {
        Node child = children.item(i);
        if(child instanceof Text text) {
          String value = text.getData();
          int oldIdx = value.indexOf(oldValue.toString());
          if(oldIdx > -1) {
            String replacement = value.substring(0, oldIdx) + newValue.toString()
                + value.substring(oldIdx + oldValue.toString().length());
            ((Text) child).setData(replacement);
            replacedChild = true;
          }
        }
      }
    }

    // If for some reason we couldn't find a text to update, just clear the
    // element contents and put in our text.
    if(!replacedChild) {
      while(element.getFirstChild() != null) {
        element.removeChild(element.getFirstChild());
      }
      Text text = node.getOwnerDocument().createTextNode(newValue.toString());
      element.appendChild(text);
    }
  }

  public List<Node> getLinkedWhitespaceNodes() {
    return linkedWhitespaceNodes;
  }

  public void setLinkedWhitespaceNodes(List<Node> linkedWhitespaceNodes) {
    this.linkedWhitespaceNodes = linkedWhitespaceNodes;
  }
}
