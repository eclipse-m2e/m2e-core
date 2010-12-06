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
 * <em><b>Contributor</b></em>'. <!-- end-user-doc -->
 * 
 * <!-- begin-model-doc --> 3.0.0+
 * 
 * Description of a person who has contributed to the project, but who does not
 * have commit privileges. Usually, these contributions come in the form of
 * patches submitted.
 * 
 * <!-- end-model-doc -->
 * 
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Contributor#getName <em>Name</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Contributor#getEmail <em>Email</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Contributor#getUrl <em>Url</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Contributor#getOrganization <em>
 * Organization</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Contributor#getOrganizationUrl <em>
 * Organization Url</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Contributor#getTimezone <em>Timezone
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Contributor#getProperties <em>
 * Properties</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Contributor#getRoles <em>Roles</em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getContributor()
 * @model extendedMetaData="name='Contributor' kind='elementOnly'"
 * @generated
 */
public interface Contributor extends EObject {
	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * The full name of the contributor. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getContributor_Name()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='name' namespace='##targetNamespace'"
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Contributor#getName <em>Name</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Email</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * The email address of the contributor. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Email</em>' attribute.
	 * @see #setEmail(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getContributor_Email()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='email' namespace='##targetNamespace'"
	 * @generated
	 */
	String getEmail();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Contributor#getEmail <em>Email</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Email</em>' attribute.
	 * @see #getEmail()
	 * @generated
	 */
	void setEmail(String value);

	/**
	 * Returns the value of the '<em><b>Url</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * The URL for the homepage of the contributor. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Url</em>' attribute.
	 * @see #setUrl(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getContributor_Url()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='url' namespace='##targetNamespace'"
	 * @generated
	 */
	String getUrl();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Contributor#getUrl <em>Url</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Url</em>' attribute.
	 * @see #getUrl()
	 * @generated
	 */
	void setUrl(String value);

	/**
	 * Returns the value of the '<em><b>Organization</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * The organization to which the contributor belongs. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Organization</em>' attribute.
	 * @see #setOrganization(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getContributor_Organization()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='organization' namespace='##targetNamespace'"
	 * @generated
	 */
	String getOrganization();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Contributor#getOrganization
	 * <em>Organization</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Organization</em>' attribute.
	 * @see #getOrganization()
	 * @generated
	 */
	void setOrganization(String value);

	/**
	 * Returns the value of the '<em><b>Organization Url</b></em>' attribute.
	 * <!-- begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc -->
	 * 3.0.0+ The URL of the organization. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Organization Url</em>' attribute.
	 * @see #setOrganizationUrl(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getContributor_OrganizationUrl()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='organizationUrl' namespace='##targetNamespace'"
	 * @generated
	 */
	String getOrganizationUrl();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Contributor#getOrganizationUrl
	 * <em>Organization Url</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Organization Url</em>' attribute.
	 * @see #getOrganizationUrl()
	 * @generated
	 */
	void setOrganizationUrl(String value);

	/**
	 * Returns the value of the '<em><b>Timezone</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * 
	 * The timezone the contributor is in. This is a number in the range -11 to
	 * 12.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Timezone</em>' attribute.
	 * @see #setTimezone(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getContributor_Timezone()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='timezone' namespace='##targetNamespace'"
	 * @generated
	 */
	String getTimezone();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Contributor#getTimezone
	 * <em>Timezone</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @param value
	 *            the new value of the '<em>Timezone</em>' attribute.
	 * @see #getTimezone()
	 * @generated
	 */
	void setTimezone(String value);

	/**
	 * Returns the value of the '<em><b>Properties</b></em>' containment
	 * reference list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.PropertyElement}. <!-- begin-user-doc
	 * --> <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * 
	 * Properties about the contributor, such as an instant messenger handle.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Properties</em>' containment reference
	 *         list.
	 * @see #isSetProperties()
	 * @see #unsetProperties()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getContributor_Properties()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='properties' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<PropertyElement> getProperties();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Contributor#getProperties
	 * <em>Properties</em>}' containment reference list. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @see #isSetProperties()
	 * @see #getProperties()
	 * @generated
	 */
	void unsetProperties();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Contributor#getProperties
	 * <em>Properties</em>}' containment reference list is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Properties</em>' containment
	 *         reference list is set.
	 * @see #unsetProperties()
	 * @see #getProperties()
	 * @generated
	 */
	boolean isSetProperties();

	/**
	 * Returns the value of the '<em><b>Roles</b></em>' attribute list. The list
	 * contents are of type {@link java.lang.String}. <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Roles</em>' attribute list isn't clear, there
	 * really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * 
	 * @return the value of the '<em>Roles</em>' attribute list.
	 * @see #isSetRoles()
	 * @see #unsetRoles()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getContributor_Roles()
	 * @model unique="false" unsettable="true"
	 *        dataType="org.eclipse.emf.ecore.xml.type.String"
	 * @generated
	 */
	EList<String> getRoles();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Contributor#getRoles <em>Roles</em>}'
	 * attribute list. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #isSetRoles()
	 * @see #getRoles()
	 * @generated
	 */
	void unsetRoles();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Contributor#getRoles <em>Roles</em>}'
	 * attribute list is set. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Roles</em>' attribute list is set.
	 * @see #unsetRoles()
	 * @see #getRoles()
	 * @generated
	 */
	boolean isSetRoles();

} // Contributor
