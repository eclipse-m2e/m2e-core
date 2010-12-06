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
 * <em><b>Plugin</b></em>'. <!-- end-user-doc -->
 * 
 * <!-- begin-model-doc --> 4.0.0 <!-- end-model-doc -->
 * 
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Plugin#getGroupId <em>Group Id</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Plugin#getArtifactId <em>Artifact Id
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Plugin#getVersion <em>Version</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Plugin#getExtensions <em>Extensions
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Plugin#getExecutions <em>Executions
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Plugin#getDependencies <em>
 * Dependencies</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Plugin#getInherited <em>Inherited
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Plugin#getConfiguration <em>
 * Configuration</em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getPlugin()
 * @model extendedMetaData="name='Plugin' kind='elementOnly'"
 * @generated
 */
public interface Plugin extends EObject {
	/**
	 * Returns the value of the '<em><b>Group Id</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * The group ID of the plugin in the repository. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Group Id</em>' attribute.
	 * @see #isSetGroupId()
	 * @see #unsetGroupId()
	 * @see #setGroupId(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getPlugin_GroupId()
	 * @model unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData
	 *        ="kind='element' name='groupId' namespace='##targetNamespace'"
	 * @generated
	 */
	String getGroupId();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Plugin#getGroupId <em>Group Id</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Group Id</em>' attribute.
	 * @see #isSetGroupId()
	 * @see #unsetGroupId()
	 * @see #getGroupId()
	 * @generated
	 */
	void setGroupId(String value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Plugin#getGroupId <em>Group Id</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #isSetGroupId()
	 * @see #getGroupId()
	 * @see #setGroupId(String)
	 * @generated
	 */
	void unsetGroupId();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Plugin#getGroupId <em>Group Id</em>}'
	 * attribute is set. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Group Id</em>' attribute is set.
	 * @see #unsetGroupId()
	 * @see #getGroupId()
	 * @see #setGroupId(String)
	 * @generated
	 */
	boolean isSetGroupId();

	/**
	 * Returns the value of the '<em><b>Artifact Id</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * The artifact ID of the plugin in the repository. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Artifact Id</em>' attribute.
	 * @see #setArtifactId(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getPlugin_ArtifactId()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='artifactId' namespace='##targetNamespace'"
	 * @generated
	 */
	String getArtifactId();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Plugin#getArtifactId
	 * <em>Artifact Id</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Artifact Id</em>' attribute.
	 * @see #getArtifactId()
	 * @generated
	 */
	void setArtifactId(String value);

	/**
	 * Returns the value of the '<em><b>Version</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * The version (or valid range of verisons) of the plugin to be used. <!--
	 * end-model-doc -->
	 * 
	 * @return the value of the '<em>Version</em>' attribute.
	 * @see #setVersion(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getPlugin_Version()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='version' namespace='##targetNamespace'"
	 * @generated
	 */
	String getVersion();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Plugin#getVersion <em>Version</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Version</em>' attribute.
	 * @see #getVersion()
	 * @generated
	 */
	void setVersion(String value);

	/**
	 * Returns the value of the '<em><b>Extensions</b></em>' attribute. The
	 * default value is <code>"false"</code>. <!-- begin-user-doc --> <!--
	 * end-user-doc --> <!-- begin-model-doc --> 4.0.0 Whether to load Maven
	 * extensions (such as packaging and type handlers) from this plugin. For
	 * performance reasons, this should only be enabled when necessary. <!--
	 * end-model-doc -->
	 * 
	 * @return the value of the '<em>Extensions</em>' attribute.
	 * @see #isSetExtensions()
	 * @see #unsetExtensions()
	 * @see #setExtensions(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getPlugin_Extensions()
	 * @model default="false" unsettable="true"
	 *        dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='extensions' namespace='##targetNamespace'"
	 * @generated
	 */
	String getExtensions();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Plugin#getExtensions
	 * <em>Extensions</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Extensions</em>' attribute.
	 * @see #isSetExtensions()
	 * @see #unsetExtensions()
	 * @see #getExtensions()
	 * @generated
	 */
	void setExtensions(String value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Plugin#getExtensions
	 * <em>Extensions</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @see #isSetExtensions()
	 * @see #getExtensions()
	 * @see #setExtensions(String)
	 * @generated
	 */
	void unsetExtensions();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Plugin#getExtensions
	 * <em>Extensions</em>}' attribute is set. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Extensions</em>' attribute is set.
	 * @see #unsetExtensions()
	 * @see #getExtensions()
	 * @see #setExtensions(String)
	 * @generated
	 */
	boolean isSetExtensions();

	/**
	 * Returns the value of the '<em><b>Executions</b></em>' containment
	 * reference list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.PluginExecution}. <!-- begin-user-doc
	 * --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0 Multiple
	 * specifications of a set of goals to execute during the build lifecycle,
	 * each having (possibly) different configuration. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Executions</em>' containment reference
	 *         list.
	 * @see #isSetExecutions()
	 * @see #unsetExecutions()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getPlugin_Executions()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='executions' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<PluginExecution> getExecutions();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Plugin#getExecutions
	 * <em>Executions</em>}' containment reference list. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @see #isSetExecutions()
	 * @see #getExecutions()
	 * @generated
	 */
	void unsetExecutions();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Plugin#getExecutions
	 * <em>Executions</em>}' containment reference list is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Executions</em>' containment
	 *         reference list is set.
	 * @see #unsetExecutions()
	 * @see #getExecutions()
	 * @generated
	 */
	boolean isSetExecutions();

	/**
	 * Returns the value of the '<em><b>Dependencies</b></em>' containment
	 * reference list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.Dependency}. <!-- begin-user-doc -->
	 * <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0 Additional
	 * dependencies that this project needs to introduce to the plugin's
	 * classloader. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Dependencies</em>' containment reference
	 *         list.
	 * @see #isSetDependencies()
	 * @see #unsetDependencies()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getPlugin_Dependencies()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='dependencies' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<Dependency> getDependencies();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Plugin#getDependencies
	 * <em>Dependencies</em>}' containment reference list. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @see #isSetDependencies()
	 * @see #getDependencies()
	 * @generated
	 */
	void unsetDependencies();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Plugin#getDependencies
	 * <em>Dependencies</em>}' containment reference list is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Dependencies</em>' containment
	 *         reference list is set.
	 * @see #unsetDependencies()
	 * @see #getDependencies()
	 * @generated
	 */
	boolean isSetDependencies();

	/**
	 * Returns the value of the '<em><b>Inherited</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * Whether any configuration should be propagated to child POMs. <!--
	 * end-model-doc -->
	 * 
	 * @return the value of the '<em>Inherited</em>' attribute.
	 * @see #setInherited(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getPlugin_Inherited()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='inherited' namespace='##targetNamespace'"
	 * @generated
	 */
	String getInherited();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Plugin#getInherited
	 * <em>Inherited</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @param value
	 *            the new value of the '<em>Inherited</em>' attribute.
	 * @see #getInherited()
	 * @generated
	 */
	void setInherited(String value);

	/**
	 * Returns the value of the '<em><b>Configuration</b></em>' reference. <!--
	 * begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Configuration</em>' reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * 
	 * @return the value of the '<em>Configuration</em>' reference.
	 * @see #setConfiguration(Configuration)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getPlugin_Configuration()
	 * @model
	 * @generated
	 */
	Configuration getConfiguration();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Plugin#getConfiguration
	 * <em>Configuration</em>}' reference. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Configuration</em>' reference.
	 * @see #getConfiguration()
	 * @generated
	 */
	void setConfiguration(Configuration value);

} // Plugin
