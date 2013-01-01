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

package org.eclipse.m2e.model.edit.pom.impl;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.m2e.model.edit.pom.Configuration;
import org.eclipse.m2e.model.edit.pom.PomPackage;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Configuration</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ConfigurationImpl#getConfigurationNode <em>Configuration Node</em>}</li>
 * </ul>
 * </p>
 * 
 * @generated NOT
 */
public class ConfigurationImpl extends EObjectImpl implements Configuration {
  private Node configurationNode;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected ConfigurationImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.CONFIGURATION;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public Node getConfigurationNode() {
    return configurationNode;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setConfigurationNode(Node newConfigurationNode) {
    this.configurationNode = newConfigurationNode;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public String toString() {
    if(eIsProxy())
      return super.toString();

    StringBuffer result = new StringBuffer(super.toString());
    result.append(" (configurationNode: "); //$NON-NLS-1$
    result.append(configurationNode);
    result.append(')');
    return result.toString();
  }

  public String getStringValue(String xpath) throws RuntimeException {

    NodeList children = configurationNode.getChildNodes();
    for(int i = 0; i < children.getLength(); i++ ) {
      Node current = children.item(i);
      if(current.getNodeName().equals(xpath)) {
        return children.item(i).getChildNodes().item(0).getNodeValue();
      }
    }
    return null;
  }

  public void setStringValue(String xpath, String value) throws RuntimeException {
    NodeList children = configurationNode.getChildNodes();
    boolean set = false;
    for(int i = 0; i < children.getLength(); i++ ) {
      Node current = children.item(i);
      if(current.getNodeName().equals(xpath)) {
        while(current.getFirstChild() != null) {
          current.removeChild(current.getFirstChild());
        }
        current.appendChild(current.getOwnerDocument().createTextNode(value));
        set = true;
        break;
      }
    }
    if(!set) {
      Element e = configurationNode.getOwnerDocument().createElement(xpath);
      e.appendChild(configurationNode.getOwnerDocument().createTextNode(value));
      configurationNode.appendChild(e);
    }
  }

  public List<String> getListValue(String xpath) throws RuntimeException {
    NodeList children = configurationNode.getChildNodes();
    for(int i = 0; i < children.getLength(); i++ ) {
      Node current = children.item(i);
      if(current.getNodeName().equals(xpath)) {
        NodeList items = current.getChildNodes();
        List<String> res = new ArrayList<String>();
        for(int j = 0; j < items.getLength(); j++ ) {
          if(items.item(j).getNodeType() == Node.ELEMENT_NODE) {
            res.add(items.item(j).getChildNodes().item(0).getNodeValue());
          }
        }
        return res;
      }
    }
    return null;
  }

  public List<Node> getListNodes(String xpath) {
    return getListNodes(configurationNode, xpath);
  }

  public List<Node> getListNodes(Node node, String xpath) {
    NodeList children = node.getChildNodes();
    for(int i = 0; i < children.getLength(); i++ ) {
      Node current = children.item(i);
      if(current.getNodeName().equals(xpath)) {
        List<Node> res = new ArrayList<Node>();
        NodeList items = current.getChildNodes();
        for(int j = 0; j < items.getLength(); j++ ) {
          if(items.item(j).getNodeType() == Node.ELEMENT_NODE) {
            res.add(items.item(j).getChildNodes().item(0));
          }
        }
        return res;
      }
    }
    return null;
  }

  public Node getNode(String xpath) {
    return getNode(configurationNode, xpath);
  }

  public Node getNode(Node node, String xpath) {
    NodeList children = node.getChildNodes();
    for(int i = 0; i < children.getLength(); i++ ) {
      Node current = children.item(i);
      if(current.getNodeName().equals(xpath)) {
        return current;
      }
    }
    return null;
  }

  public void setNodeValues(String xpath, String names[], String[] values) {
    setNodeValues(configurationNode, xpath, names, values);
  }

  public void setNodeValues(String xpath, String name, String[] values) {
    String[] names = new String[values.length];
    for(int i = 0; i < names.length; i++ ) {
      names[i] = name;
    }
    setNodeValues(xpath, names, values);
  }

  public Node createNode(String xpath) {
    Node element = configurationNode.getOwnerDocument().createElement(xpath);
    configurationNode.appendChild(element);
    return element;
  }

  public void removeNode(String xpath) {
    configurationNode.removeChild(getNode(xpath));
  }

  public void setNodeValues(Node node, String xpath, String[] names, String[] values) {
    Node parent = getNode(node, xpath);

    if(parent == null) {
      //create node
      parent = node.getOwnerDocument().createElement(xpath);
      node.appendChild(parent);
    }

    List<Node> nodes = getListNodes(node, xpath);

    //append missing nodes if required
    int diff = values.length - nodes.size();
    for(int i = 0; i < diff; i++ ) {
      Node element = parent.getOwnerDocument().createElement(names[i]);
      parent.appendChild(element);
      Text text = parent.getOwnerDocument().createTextNode(""); //$NON-NLS-1$
      element.appendChild(text);
      nodes.add(text);
    }

    //remove extra nodes if required
    for(int i = 0; i < -diff; i++ ) {
      Node element = nodes.remove(nodes.size() - 1 - i);
      parent.removeChild(element.getParentNode());
    }

    //set values
    for(int i = 0; i < nodes.size(); i++ ) {
      nodes.get(i).setNodeValue(values[i]);
    }
  }

  public void doNotify(int eventType, Object changedFeature, Object oldValue, Object newValue) {
    // A catch-all notificator. 
    // The configuration section can differ with every plugin, so we cannot really have a
    // static EMF model. So we'll just notify the subscribers and let them act accordingly.
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, eventType, PomPackage.CONFIGURATION, oldValue, newValue));
  }
} // ConfigurationImpl
