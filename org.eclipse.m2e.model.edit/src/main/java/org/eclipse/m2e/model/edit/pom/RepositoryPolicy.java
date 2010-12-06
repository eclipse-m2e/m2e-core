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
 * <em><b>Repository Policy</b></em>'. <!-- end-user-doc -->
 * 
 * <!-- begin-model-doc --> 4.0.0 Download policy <!-- end-model-doc -->
 * 
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.RepositoryPolicy#getEnabled <em>
 * Enabled</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.RepositoryPolicy#getUpdatePolicy <em>
 * Update Policy</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.RepositoryPolicy#getChecksumPolicy
 * <em>Checksum Policy</em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getRepositoryPolicy()
 * @model extendedMetaData="name='RepositoryPolicy' kind='elementOnly'"
 * @generated
 */
public interface RepositoryPolicy extends EObject {
	/**
	 * Returns the value of the '<em><b>Enabled</b></em>' attribute. The default
	 * value is <code>"true"</code>. <!-- begin-user-doc --> <!-- end-user-doc
	 * --> <!-- begin-model-doc --> 4.0.0 Whether to use this repository for
	 * downloading this type of artifact. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Enabled</em>' attribute.
	 * @see #isSetEnabled()
	 * @see #unsetEnabled()
	 * @see #setEnabled(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getRepositoryPolicy_Enabled()
	 * @model default="true" unsettable="true"
	 *        dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='enabled' namespace='##targetNamespace'"
	 * @generated
	 */
	String getEnabled();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.RepositoryPolicy#getEnabled
	 * <em>Enabled</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @param value
	 *            the new value of the '<em>Enabled</em>' attribute.
	 * @see #isSetEnabled()
	 * @see #unsetEnabled()
	 * @see #getEnabled()
	 * @generated
	 */
	void setEnabled(String value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.RepositoryPolicy#getEnabled
	 * <em>Enabled</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @see #isSetEnabled()
	 * @see #getEnabled()
	 * @see #setEnabled(String)
	 * @generated
	 */
	void unsetEnabled();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.RepositoryPolicy#getEnabled
	 * <em>Enabled</em>}' attribute is set. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Enabled</em>' attribute is set.
	 * @see #unsetEnabled()
	 * @see #getEnabled()
	 * @see #setEnabled(String)
	 * @generated
	 */
	boolean isSetEnabled();

	/**
	 * Returns the value of the '<em><b>Update Policy</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * The frequency for downloading updates - can be
	 * &lt;code&gt;always,&lt;/code&gt; &lt;code&gt;daily&lt;/code&gt;
	 * (default), &lt;code&gt;interval:XXX&lt;/code&gt; (in minutes) or
	 * &lt;code&gt;never&lt;/code&gt; (only if it doesn't exist locally).
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Update Policy</em>' attribute.
	 * @see #setUpdatePolicy(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getRepositoryPolicy_UpdatePolicy()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='updatePolicy' namespace='##targetNamespace'"
	 * @generated
	 */
	String getUpdatePolicy();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.RepositoryPolicy#getUpdatePolicy
	 * <em>Update Policy</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Update Policy</em>' attribute.
	 * @see #getUpdatePolicy()
	 * @generated
	 */
	void setUpdatePolicy(String value);

	/**
	 * Returns the value of the '<em><b>Checksum Policy</b></em>' attribute.
	 * <!-- begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc -->
	 * 4.0.0
	 * 
	 * What to do when verification of an artifact checksum fails. Valid values
	 * are &lt;code&gt;ignore&lt;/code&gt; , &lt;code&gt;fail&lt;/code&gt; or
	 * &lt;code&gt;warn&lt;/code&gt; (the default).
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Checksum Policy</em>' attribute.
	 * @see #setChecksumPolicy(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getRepositoryPolicy_ChecksumPolicy()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='checksumPolicy' namespace='##targetNamespace'"
	 * @generated
	 */
	String getChecksumPolicy();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.RepositoryPolicy#getChecksumPolicy
	 * <em>Checksum Policy</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Checksum Policy</em>' attribute.
	 * @see #getChecksumPolicy()
	 * @generated
	 */
	void setChecksumPolicy(String value);

} // RepositoryPolicy
