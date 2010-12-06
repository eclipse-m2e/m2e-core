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

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc --> A representation of the model object '
 * <em><b>Activation</b></em>'. <!-- end-user-doc -->
 * 
 * <!-- begin-model-doc --> 4.0.0
 * 
 * The conditions within the build runtime environment which will trigger the
 * automatic inclusion of the build profile.
 * 
 * <!-- end-model-doc -->
 * 
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Activation#getActiveByDefault <em>
 * Active By Default</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Activation#getJdk <em>Jdk</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Activation#getOs <em>Os</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Activation#getProperty <em>Property
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Activation#getFile <em>File</em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getActivation()
 * @model extendedMetaData="name='Activation' kind='elementOnly'"
 * @generated
 */
public interface Activation extends EObject {
	/**
	 * Returns the value of the '<em><b>Active By Default</b></em>' attribute.
	 * The default value is <code>"false"</code>. <!-- begin-user-doc --> <!--
	 * end-user-doc --> <!-- begin-model-doc --> 4.0.0 Flag specifying whether
	 * this profile is active by default. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Active By Default</em>' attribute.
	 * @see #isSetActiveByDefault()
	 * @see #unsetActiveByDefault()
	 * @see #setActiveByDefault(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getActivation_ActiveByDefault()
	 * @model default="false" unsettable="true"
	 *        dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='activeByDefault' namespace='##targetNamespace'"
	 * @generated
	 */
	String getActiveByDefault();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Activation#getActiveByDefault
	 * <em>Active By Default</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Active By Default</em>' attribute.
	 * @see #isSetActiveByDefault()
	 * @see #unsetActiveByDefault()
	 * @see #getActiveByDefault()
	 * @generated
	 */
	void setActiveByDefault(String value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Activation#getActiveByDefault
	 * <em>Active By Default</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @see #isSetActiveByDefault()
	 * @see #getActiveByDefault()
	 * @see #setActiveByDefault(String)
	 * @generated
	 */
	void unsetActiveByDefault();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Activation#getActiveByDefault
	 * <em>Active By Default</em>}' attribute is set. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Active By Default</em>' attribute
	 *         is set.
	 * @see #unsetActiveByDefault()
	 * @see #getActiveByDefault()
	 * @see #setActiveByDefault(String)
	 * @generated
	 */
	boolean isSetActiveByDefault();

	/**
	 * Returns the value of the '<em><b>Jdk</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * Specifies that this profile will be activated when a matching JDK is
	 * detected. For example, &lt;code&gt;1.4&lt;/code&gt; only activates on
	 * JDKs versioned 1.4, while &lt;code&gt;!1.4&lt;/code&gt; matches any JDK
	 * that is not version 1.4.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Jdk</em>' attribute.
	 * @see #setJdk(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getActivation_Jdk()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='jdk' namespace='##targetNamespace'"
	 * @generated
	 */
	String getJdk();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Activation#getJdk <em>Jdk</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Jdk</em>' attribute.
	 * @see #getJdk()
	 * @generated
	 */
	void setJdk(String value);

	/**
	 * Returns the value of the '<em><b>Os</b></em>' containment reference. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * Specifies that this profile will be activated when matching operating
	 * system attributes are detected.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Os</em>' containment reference.
	 * @see #isSetOs()
	 * @see #unsetOs()
	 * @see #setOs(ActivationOS)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getActivation_Os()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='os' namespace='##targetNamespace'"
	 * @generated
	 */
	ActivationOS getOs();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Activation#getOs <em>Os</em>}'
	 * containment reference. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Os</em>' containment reference.
	 * @see #isSetOs()
	 * @see #unsetOs()
	 * @see #getOs()
	 * @generated
	 */
	void setOs(ActivationOS value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Activation#getOs <em>Os</em>}'
	 * containment reference. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #isSetOs()
	 * @see #getOs()
	 * @see #setOs(ActivationOS)
	 * @generated
	 */
	void unsetOs();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Activation#getOs <em>Os</em>}'
	 * containment reference is set. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @return whether the value of the '<em>Os</em>' containment reference is
	 *         set.
	 * @see #unsetOs()
	 * @see #getOs()
	 * @see #setOs(ActivationOS)
	 * @generated
	 */
	boolean isSetOs();

	/**
	 * Returns the value of the '<em><b>Property</b></em>' containment
	 * reference. <!-- begin-user-doc --> <!-- end-user-doc --> <!--
	 * begin-model-doc --> 4.0.0
	 * 
	 * Specifies that this profile will be activated when this system property
	 * is specified.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Property</em>' containment reference.
	 * @see #isSetProperty()
	 * @see #unsetProperty()
	 * @see #setProperty(ActivationProperty)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getActivation_Property()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='property' namespace='##targetNamespace'"
	 * @generated
	 */
	ActivationProperty getProperty();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Activation#getProperty
	 * <em>Property</em>}' containment reference. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Property</em>' containment
	 *            reference.
	 * @see #isSetProperty()
	 * @see #unsetProperty()
	 * @see #getProperty()
	 * @generated
	 */
	void setProperty(ActivationProperty value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Activation#getProperty
	 * <em>Property</em>}' containment reference. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @see #isSetProperty()
	 * @see #getProperty()
	 * @see #setProperty(ActivationProperty)
	 * @generated
	 */
	void unsetProperty();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Activation#getProperty
	 * <em>Property</em>}' containment reference is set. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Property</em>' containment
	 *         reference is set.
	 * @see #unsetProperty()
	 * @see #getProperty()
	 * @see #setProperty(ActivationProperty)
	 * @generated
	 */
	boolean isSetProperty();

	/**
	 * Returns the value of the '<em><b>File</b></em>' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc -->
	 * 4.0.0
	 * 
	 * Specifies that this profile will be activated based on existence of a
	 * file.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>File</em>' containment reference.
	 * @see #isSetFile()
	 * @see #unsetFile()
	 * @see #setFile(ActivationFile)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getActivation_File()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='file' namespace='##targetNamespace'"
	 * @generated
	 */
	ActivationFile getFile();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Activation#getFile <em>File</em>}'
	 * containment reference. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>File</em>' containment reference.
	 * @see #isSetFile()
	 * @see #unsetFile()
	 * @see #getFile()
	 * @generated
	 */
	void setFile(ActivationFile value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Activation#getFile <em>File</em>}'
	 * containment reference. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #isSetFile()
	 * @see #getFile()
	 * @see #setFile(ActivationFile)
	 * @generated
	 */
	void unsetFile();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Activation#getFile <em>File</em>}'
	 * containment reference is set. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @return whether the value of the '<em>File</em>' containment reference is
	 *         set.
	 * @see #unsetFile()
	 * @see #getFile()
	 * @see #setFile(ActivationFile)
	 * @generated
	 */
	boolean isSetFile();

} // Activation
