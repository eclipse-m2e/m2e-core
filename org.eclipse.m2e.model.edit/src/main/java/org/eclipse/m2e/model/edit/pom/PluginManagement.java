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

package org.eclipse.m2e.model.edit.pom;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc --> A representation of the model object '
 * <em><b>Plugin Management</b></em>'. <!-- end-user-doc -->
 * 
 * <!-- begin-model-doc --> 4.0.0
 * 
 * Section for management of default plugin information for use in a group of
 * POMs.
 * 
 * <!-- end-model-doc -->
 * 
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.PluginManagement#getPlugins <em>
 * Plugins</em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getPluginManagement()
 * @model extendedMetaData="name='PluginManagement' kind='elementOnly'"
 * @generated
 */
public interface PluginManagement extends EObject {
	/**
	 * Returns the value of the '<em><b>Plugins</b></em>' containment reference
	 * list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.Plugin}. <!-- begin-user-doc --> <!--
	 * end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * The list of plugins to use.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Plugins</em>' containment reference list.
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getPluginManagement_Plugins()
	 * @model containment="true" extendedMetaData=
	 *        "kind='element' name='plugins' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<Plugin> getPlugins();

} // PluginManagement
