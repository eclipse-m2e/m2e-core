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
 * <em><b>Ci Management</b></em>'. <!-- end-user-doc -->
 * 
 * <!-- begin-model-doc --> 4.0.0 <!-- end-model-doc -->
 * 
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.CiManagement#getSystem <em>System
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.CiManagement#getUrl <em>Url</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.CiManagement#getNotifiers <em>
 * Notifiers</em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getCiManagement()
 * @model extendedMetaData="name='CiManagement' kind='elementOnly'"
 * @generated
 */
public interface CiManagement extends EObject {
	/**
	 * Returns the value of the '<em><b>System</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * The name of the continuous integration system, e.g.
	 * &lt;code&gt;continuum&lt;/code&gt;. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>System</em>' attribute.
	 * @see #setSystem(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getCiManagement_System()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='system' namespace='##targetNamespace'"
	 * @generated
	 */
	String getSystem();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.CiManagement#getSystem
	 * <em>System</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @param value
	 *            the new value of the '<em>System</em>' attribute.
	 * @see #getSystem()
	 * @generated
	 */
	void setSystem(String value);

	/**
	 * Returns the value of the '<em><b>Url</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * URL for the continuous integration system used by the project if it has a
	 * web interface. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Url</em>' attribute.
	 * @see #setUrl(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getCiManagement_Url()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='url' namespace='##targetNamespace'"
	 * @generated
	 */
	String getUrl();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.CiManagement#getUrl <em>Url</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Url</em>' attribute.
	 * @see #getUrl()
	 * @generated
	 */
	void setUrl(String value);

	/**
	 * Returns the value of the '<em><b>Notifiers</b></em>' containment
	 * reference list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.Notifier}. <!-- begin-user-doc -->
	 * <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * Configuration for notifying developers/users when a build is
	 * unsuccessful, including user information and notification mode.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Notifiers</em>' containment reference list.
	 * @see #isSetNotifiers()
	 * @see #unsetNotifiers()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getCiManagement_Notifiers()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='notifiers' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<Notifier> getNotifiers();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.CiManagement#getNotifiers
	 * <em>Notifiers</em>}' containment reference list. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @see #isSetNotifiers()
	 * @see #getNotifiers()
	 * @generated
	 */
	void unsetNotifiers();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.CiManagement#getNotifiers
	 * <em>Notifiers</em>}' containment reference list is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Notifiers</em>' containment
	 *         reference list is set.
	 * @see #unsetNotifiers()
	 * @see #getNotifiers()
	 * @generated
	 */
	boolean isSetNotifiers();

} // CiManagement
