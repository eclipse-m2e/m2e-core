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

package org.eclipse.m2e.model.edit.pom;

import java.util.List;

import org.w3c.dom.Node;

import org.eclipse.emf.ecore.EObject;


/**
 * <!-- begin-user-doc --> A representation of the model object ' <em><b>Configuration</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Configuration#getNode <em>Node</em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getConfiguration()
 * @model
 * @generated NOT
 */
public interface Configuration extends EObject {
  public Node getConfigurationNode();

  public String getStringValue(String xpath);

  public void setStringValue(String xpath, String value);

  List<String> getListValue(String xpath);

  List<Node> getListNodes(String xpath);

  Node getNode(String xpath);

  void setNodeValues(String xpath, String name, String[] values);

  void setNodeValues(String xpath, String[] names, String[] values);

  void setNodeValues(Node node, String xpath, String[] names, String[] values);

  Node createNode(String xpath);

  void removeNode(String xpath);

} // Configuration
