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

/**
 * <!-- begin-user-doc --> A representation of the model object '
 * <em><b>Build</b></em>'. <!-- end-user-doc -->
 * 
 * <!-- begin-model-doc --> 3.0.0+ <!-- end-model-doc -->
 * 
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Build#getSourceDirectory <em>Source
 * Directory</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Build#getScriptSourceDirectory <em>
 * Script Source Directory</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Build#getTestSourceDirectory <em>Test
 * Source Directory</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Build#getOutputDirectory <em>Output
 * Directory</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Build#getTestOutputDirectory <em>Test
 * Output Directory</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Build#getExtensions <em>Extensions
 * </em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getBuild()
 * @model extendedMetaData="name='Build' kind='elementOnly'"
 * @generated
 */
public interface Build extends BuildBase {
	/**
	 * Returns the value of the '<em><b>Source Directory</b></em>' attribute.
	 * <!-- begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc -->
	 * 3.0.0+
	 * 
	 * This element specifies a directory containing the source of the project.
	 * The generated build system will compile the source in this directory when
	 * the project is built. The path given is relative to the project
	 * descriptor.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Source Directory</em>' attribute.
	 * @see #setSourceDirectory(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getBuild_SourceDirectory()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='sourceDirectory' namespace='##targetNamespace'"
	 * @generated
	 */
	String getSourceDirectory();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Build#getSourceDirectory
	 * <em>Source Directory</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Source Directory</em>' attribute.
	 * @see #getSourceDirectory()
	 * @generated
	 */
	void setSourceDirectory(String value);

	/**
	 * Returns the value of the '<em><b>Script Source Directory</b></em>'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc --> <!--
	 * begin-model-doc --> 4.0.0
	 * 
	 * This element specifies a directory containing the script sources of the
	 * project. This directory is meant to be different from the
	 * sourceDirectory, in that its contents will be copied to the output
	 * directory in most cases (since scripts are interpreted rather than
	 * compiled).
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Script Source Directory</em>' attribute.
	 * @see #setScriptSourceDirectory(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getBuild_ScriptSourceDirectory()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='scriptSourceDirectory' namespace='##targetNamespace'"
	 * @generated
	 */
	String getScriptSourceDirectory();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Build#getScriptSourceDirectory
	 * <em>Script Source Directory</em>}' attribute. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Script Source Directory</em>'
	 *            attribute.
	 * @see #getScriptSourceDirectory()
	 * @generated
	 */
	void setScriptSourceDirectory(String value);

	/**
	 * Returns the value of the '<em><b>Test Source Directory</b></em>'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc --> <!--
	 * begin-model-doc --> 4.0.0
	 * 
	 * This element specifies a directory containing the unit test source of the
	 * project. The generated build system will compile these directories when
	 * the project is being tested. The path given is relative to the project
	 * descriptor.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Test Source Directory</em>' attribute.
	 * @see #setTestSourceDirectory(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getBuild_TestSourceDirectory()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='testSourceDirectory' namespace='##targetNamespace'"
	 * @generated
	 */
	String getTestSourceDirectory();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Build#getTestSourceDirectory
	 * <em>Test Source Directory</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Test Source Directory</em>'
	 *            attribute.
	 * @see #getTestSourceDirectory()
	 * @generated
	 */
	void setTestSourceDirectory(String value);

	/**
	 * Returns the value of the '<em><b>Output Directory</b></em>' attribute.
	 * <!-- begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc -->
	 * 4.0.0
	 * 
	 * The directory where compiled application classes are placed.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Output Directory</em>' attribute.
	 * @see #setOutputDirectory(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getBuild_OutputDirectory()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='outputDirectory' namespace='##targetNamespace'"
	 * @generated
	 */
	String getOutputDirectory();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Build#getOutputDirectory
	 * <em>Output Directory</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Output Directory</em>' attribute.
	 * @see #getOutputDirectory()
	 * @generated
	 */
	void setOutputDirectory(String value);

	/**
	 * Returns the value of the '<em><b>Test Output Directory</b></em>'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc --> <!--
	 * begin-model-doc --> 4.0.0
	 * 
	 * The directory where compiled test classes are placed.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Test Output Directory</em>' attribute.
	 * @see #setTestOutputDirectory(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getBuild_TestOutputDirectory()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='testOutputDirectory' namespace='##targetNamespace'"
	 * @generated
	 */
	String getTestOutputDirectory();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Build#getTestOutputDirectory
	 * <em>Test Output Directory</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Test Output Directory</em>'
	 *            attribute.
	 * @see #getTestOutputDirectory()
	 * @generated
	 */
	void setTestOutputDirectory(String value);

	/**
	 * Returns the value of the '<em><b>Extensions</b></em>' containment
	 * reference list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.Extension}. <!-- begin-user-doc -->
	 * <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0 A set of build
	 * extensions to use from this project. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Extensions</em>' containment reference
	 *         list.
	 * @see #isSetExtensions()
	 * @see #unsetExtensions()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getBuild_Extensions()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='extensions' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<Extension> getExtensions();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Build#getExtensions
	 * <em>Extensions</em>}' containment reference list. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @see #isSetExtensions()
	 * @see #getExtensions()
	 * @generated
	 */
	void unsetExtensions();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Build#getExtensions
	 * <em>Extensions</em>}' containment reference list is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Extensions</em>' containment
	 *         reference list is set.
	 * @see #unsetExtensions()
	 * @see #getExtensions()
	 * @generated
	 */
	boolean isSetExtensions();

} // Build
