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
 * <em><b>Notifier</b></em>'. <!-- end-user-doc -->
 * 
 * <!-- begin-model-doc --> 4.0.0
 * 
 * Configures one method for notifying users/developers when a build breaks.
 * 
 * <!-- end-model-doc -->
 * 
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Notifier#getType <em>Type</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Notifier#getSendOnError <em>Send On
 * Error</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Notifier#getSendOnFailure <em>Send On
 * Failure</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Notifier#getSendOnSuccess <em>Send On
 * Success</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Notifier#getSendOnWarning <em>Send On
 * Warning</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Notifier#getAddress <em>Address</em>}
 * </li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Notifier#getConfiguration <em>
 * Configuration</em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getNotifier()
 * @model extendedMetaData="name='Notifier' kind='elementOnly'"
 * @generated
 */
public interface Notifier extends EObject {
	/**
	 * Returns the value of the '<em><b>Type</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * The mechanism used to deliver notifications. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Type</em>' attribute.
	 * @see #isSetType()
	 * @see #unsetType()
	 * @see #setType(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getNotifier_Type()
	 * @model unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData
	 *        ="kind='element' name='type' namespace='##targetNamespace'"
	 * @generated
	 */
	String getType();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Notifier#getType <em>Type</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Type</em>' attribute.
	 * @see #isSetType()
	 * @see #unsetType()
	 * @see #getType()
	 * @generated
	 */
	void setType(String value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Notifier#getType <em>Type</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #isSetType()
	 * @see #getType()
	 * @see #setType(String)
	 * @generated
	 */
	void unsetType();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Notifier#getType <em>Type</em>}'
	 * attribute is set. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Type</em>' attribute is set.
	 * @see #unsetType()
	 * @see #getType()
	 * @see #setType(String)
	 * @generated
	 */
	boolean isSetType();

	/**
	 * Returns the value of the '<em><b>Send On Error</b></em>' attribute. The
	 * default value is <code>"true"</code>. <!-- begin-user-doc --> <!--
	 * end-user-doc --> <!-- begin-model-doc --> 4.0.0 Whether to send
	 * notifications on error. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Send On Error</em>' attribute.
	 * @see #isSetSendOnError()
	 * @see #unsetSendOnError()
	 * @see #setSendOnError(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getNotifier_SendOnError()
	 * @model default="true" unsettable="true"
	 *        dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='sendOnError' namespace='##targetNamespace'"
	 * @generated
	 */
	String getSendOnError();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Notifier#getSendOnError
	 * <em>Send On Error</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Send On Error</em>' attribute.
	 * @see #isSetSendOnError()
	 * @see #unsetSendOnError()
	 * @see #getSendOnError()
	 * @generated
	 */
	void setSendOnError(String value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Notifier#getSendOnError
	 * <em>Send On Error</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @see #isSetSendOnError()
	 * @see #getSendOnError()
	 * @see #setSendOnError(String)
	 * @generated
	 */
	void unsetSendOnError();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Notifier#getSendOnError
	 * <em>Send On Error</em>}' attribute is set. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Send On Error</em>' attribute is
	 *         set.
	 * @see #unsetSendOnError()
	 * @see #getSendOnError()
	 * @see #setSendOnError(String)
	 * @generated
	 */
	boolean isSetSendOnError();

	/**
	 * Returns the value of the '<em><b>Send On Failure</b></em>' attribute. The
	 * default value is <code>"true"</code>. <!-- begin-user-doc --> <!--
	 * end-user-doc --> <!-- begin-model-doc --> 4.0.0 Whether to send
	 * notifications on failure. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Send On Failure</em>' attribute.
	 * @see #isSetSendOnFailure()
	 * @see #unsetSendOnFailure()
	 * @see #setSendOnFailure(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getNotifier_SendOnFailure()
	 * @model default="true" unsettable="true"
	 *        dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='sendOnFailure' namespace='##targetNamespace'"
	 * @generated
	 */
	String getSendOnFailure();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Notifier#getSendOnFailure
	 * <em>Send On Failure</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Send On Failure</em>' attribute.
	 * @see #isSetSendOnFailure()
	 * @see #unsetSendOnFailure()
	 * @see #getSendOnFailure()
	 * @generated
	 */
	void setSendOnFailure(String value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Notifier#getSendOnFailure
	 * <em>Send On Failure</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @see #isSetSendOnFailure()
	 * @see #getSendOnFailure()
	 * @see #setSendOnFailure(String)
	 * @generated
	 */
	void unsetSendOnFailure();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Notifier#getSendOnFailure
	 * <em>Send On Failure</em>}' attribute is set. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Send On Failure</em>' attribute is
	 *         set.
	 * @see #unsetSendOnFailure()
	 * @see #getSendOnFailure()
	 * @see #setSendOnFailure(String)
	 * @generated
	 */
	boolean isSetSendOnFailure();

	/**
	 * Returns the value of the '<em><b>Send On Success</b></em>' attribute. The
	 * default value is <code>"true"</code>. <!-- begin-user-doc --> <!--
	 * end-user-doc --> <!-- begin-model-doc --> 4.0.0 Whether to send
	 * notifications on success. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Send On Success</em>' attribute.
	 * @see #isSetSendOnSuccess()
	 * @see #unsetSendOnSuccess()
	 * @see #setSendOnSuccess(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getNotifier_SendOnSuccess()
	 * @model default="true" unsettable="true"
	 *        dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='sendOnSuccess' namespace='##targetNamespace'"
	 * @generated
	 */
	String getSendOnSuccess();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Notifier#getSendOnSuccess
	 * <em>Send On Success</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Send On Success</em>' attribute.
	 * @see #isSetSendOnSuccess()
	 * @see #unsetSendOnSuccess()
	 * @see #getSendOnSuccess()
	 * @generated
	 */
	void setSendOnSuccess(String value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Notifier#getSendOnSuccess
	 * <em>Send On Success</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @see #isSetSendOnSuccess()
	 * @see #getSendOnSuccess()
	 * @see #setSendOnSuccess(String)
	 * @generated
	 */
	void unsetSendOnSuccess();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Notifier#getSendOnSuccess
	 * <em>Send On Success</em>}' attribute is set. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Send On Success</em>' attribute is
	 *         set.
	 * @see #unsetSendOnSuccess()
	 * @see #getSendOnSuccess()
	 * @see #setSendOnSuccess(String)
	 * @generated
	 */
	boolean isSetSendOnSuccess();

	/**
	 * Returns the value of the '<em><b>Send On Warning</b></em>' attribute. The
	 * default value is <code>"true"</code>. <!-- begin-user-doc --> <!--
	 * end-user-doc --> <!-- begin-model-doc --> 4.0.0 Whether to send
	 * notifications on warning. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Send On Warning</em>' attribute.
	 * @see #isSetSendOnWarning()
	 * @see #unsetSendOnWarning()
	 * @see #setSendOnWarning(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getNotifier_SendOnWarning()
	 * @model default="true" unsettable="true"
	 *        dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='sendOnWarning' namespace='##targetNamespace'"
	 * @generated
	 */
	String getSendOnWarning();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Notifier#getSendOnWarning
	 * <em>Send On Warning</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Send On Warning</em>' attribute.
	 * @see #isSetSendOnWarning()
	 * @see #unsetSendOnWarning()
	 * @see #getSendOnWarning()
	 * @generated
	 */
	void setSendOnWarning(String value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Notifier#getSendOnWarning
	 * <em>Send On Warning</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @see #isSetSendOnWarning()
	 * @see #getSendOnWarning()
	 * @see #setSendOnWarning(String)
	 * @generated
	 */
	void unsetSendOnWarning();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Notifier#getSendOnWarning
	 * <em>Send On Warning</em>}' attribute is set. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Send On Warning</em>' attribute is
	 *         set.
	 * @see #unsetSendOnWarning()
	 * @see #getSendOnWarning()
	 * @see #setSendOnWarning(String)
	 * @generated
	 */
	boolean isSetSendOnWarning();

	/**
	 * Returns the value of the '<em><b>Address</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * &lt;b&gt;Deprecated&lt;/b&gt;. Where to send the notification to - eg
	 * email address.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Address</em>' attribute.
	 * @see #setAddress(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getNotifier_Address()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='address' namespace='##targetNamespace'"
	 * @generated
	 */
	String getAddress();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Notifier#getAddress <em>Address</em>}
	 * ' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Address</em>' attribute.
	 * @see #getAddress()
	 * @generated
	 */
	void setAddress(String value);

	/**
	 * Returns the value of the '<em><b>Configuration</b></em>' containment
	 * reference list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.PropertyElement}. <!-- begin-user-doc
	 * -->
	 * <p>
	 * If the meaning of the '<em>Configuration</em>' containment reference list
	 * isn't clear, there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * 
	 * @return the value of the '<em>Configuration</em>' containment reference
	 *         list.
	 * @see #isSetConfiguration()
	 * @see #unsetConfiguration()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getNotifier_Configuration()
	 * @model containment="true" unsettable="true"
	 * @generated
	 */
	EList<PropertyElement> getConfiguration();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Notifier#getConfiguration
	 * <em>Configuration</em>}' containment reference list. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @see #isSetConfiguration()
	 * @see #getConfiguration()
	 * @generated
	 */
	void unsetConfiguration();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Notifier#getConfiguration
	 * <em>Configuration</em>}' containment reference list is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Configuration</em>' containment
	 *         reference list is set.
	 * @see #unsetConfiguration()
	 * @see #getConfiguration()
	 * @generated
	 */
	boolean isSetConfiguration();

} // Notifier
