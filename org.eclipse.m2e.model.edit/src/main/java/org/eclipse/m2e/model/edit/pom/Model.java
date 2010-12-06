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
 * <em><b>Model</b></em>'. <!-- end-user-doc -->
 * 
 * <!-- begin-model-doc --> 3.0.0+
 * 
 * The &lt;code&gt;&amp;lt;project&amp;gt;&lt;/code&gt; element is the root of
 * the descriptor. The following table lists all of the possible child elements.
 * 
 * <!-- end-model-doc -->
 * 
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getParent <em>Parent</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getModelVersion <em>Model
 * Version</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getGroupId <em>Group Id</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getArtifactId <em>Artifact Id
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getPackaging <em>Packaging
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getName <em>Name</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getVersion <em>Version</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getDescription <em>Description
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getUrl <em>Url</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getPrerequisites <em>
 * Prerequisites</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getIssueManagement <em>Issue
 * Management</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getCiManagement <em>Ci
 * Management</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getInceptionYear <em>Inception
 * Year</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getMailingLists <em>Mailing
 * Lists</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getDevelopers <em>Developers
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getContributors <em>
 * Contributors</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getLicenses <em>Licenses</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getScm <em>Scm</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getOrganization <em>
 * Organization</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getBuild <em>Build</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getProfiles <em>Profiles</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getRepositories <em>
 * Repositories</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getPluginRepositories <em>
 * Plugin Repositories</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getDependencies <em>
 * Dependencies</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getReporting <em>Reporting
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getDependencyManagement <em>
 * Dependency Management</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getDistributionManagement <em>
 * Distribution Management</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getProperties <em>Properties
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Model#getModules <em>Modules</em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel()
 * @model extendedMetaData="name='Model' kind='elementOnly'"
 * @generated
 */
public interface Model extends EObject {
	/**
	 * Returns the value of the '<em><b>Parent</b></em>' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc -->
	 * 4.0.0
	 * 
	 * The location of the parent project, if one exists. Values from the parent
	 * project will be the default for this project if they are left
	 * unspecified. The location is given as a group ID, artifact ID and
	 * version.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Parent</em>' containment reference.
	 * @see #isSetParent()
	 * @see #unsetParent()
	 * @see #setParent(Parent)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_Parent()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='parent' namespace='##targetNamespace'"
	 * @generated
	 */
	Parent getParent();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getParent <em>Parent</em>}'
	 * containment reference. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Parent</em>' containment reference.
	 * @see #isSetParent()
	 * @see #unsetParent()
	 * @see #getParent()
	 * @generated
	 */
	void setParent(Parent value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getParent <em>Parent</em>}'
	 * containment reference. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #isSetParent()
	 * @see #getParent()
	 * @see #setParent(Parent)
	 * @generated
	 */
	void unsetParent();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getParent <em>Parent</em>}'
	 * containment reference is set. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @return whether the value of the '<em>Parent</em>' containment reference
	 *         is set.
	 * @see #unsetParent()
	 * @see #getParent()
	 * @see #setParent(Parent)
	 * @generated
	 */
	boolean isSetParent();

	/**
	 * Returns the value of the '<em><b>Model Version</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * Declares to which version of project descriptor this POM conforms. <!--
	 * end-model-doc -->
	 * 
	 * @return the value of the '<em>Model Version</em>' attribute.
	 * @see #setModelVersion(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_ModelVersion()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='modelVersion' namespace='##targetNamespace'"
	 * @generated
	 */
	String getModelVersion();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getModelVersion
	 * <em>Model Version</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Model Version</em>' attribute.
	 * @see #getModelVersion()
	 * @generated
	 */
	void setModelVersion(String value);

	/**
	 * Returns the value of the '<em><b>Group Id</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * 
	 * A universally unique identifier for a project. It is normal to use a
	 * fully-qualified package name to distinguish it from other projects with a
	 * similar name (eg. &lt;code&gt;org.apache.maven&lt;/code&gt;).
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Group Id</em>' attribute.
	 * @see #setGroupId(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_GroupId()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='groupId' namespace='##targetNamespace'"
	 * @generated
	 */
	String getGroupId();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getGroupId <em>Group Id</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Group Id</em>' attribute.
	 * @see #getGroupId()
	 * @generated
	 */
	void setGroupId(String value);

	/**
	 * Returns the value of the '<em><b>Artifact Id</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * 
	 * The identifier for this artifact that is unique within the group given by
	 * the group ID. An artifact is something that is either produced or used by
	 * a project. Examples of artifacts produced by Maven for a project include:
	 * JARs, source and binary distributions, and WARs.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Artifact Id</em>' attribute.
	 * @see #setArtifactId(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_ArtifactId()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='artifactId' namespace='##targetNamespace'"
	 * @generated
	 */
	String getArtifactId();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getArtifactId
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
	 * Returns the value of the '<em><b>Packaging</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * The type of artifact this project produces, for example
	 * &lt;code&gt;jar&lt;/code&gt; &lt;code&gt;war&lt;/code&gt;
	 * &lt;code&gt;ear&lt;/code&gt; &lt;code&gt;pom&lt;/code&gt;. Plugins can
	 * create their own packaging, and therefore their own packaging types, so
	 * this list does not contain all possible types.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Packaging</em>' attribute.
	 * @see #isSetPackaging()
	 * @see #unsetPackaging()
	 * @see #setPackaging(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_Packaging()
	 * @model unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData
	 *        ="kind='element' name='packaging' namespace='##targetNamespace'"
	 * @generated
	 */
	String getPackaging();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getPackaging
	 * <em>Packaging</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @param value
	 *            the new value of the '<em>Packaging</em>' attribute.
	 * @see #isSetPackaging()
	 * @see #unsetPackaging()
	 * @see #getPackaging()
	 * @generated
	 */
	void setPackaging(String value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getPackaging
	 * <em>Packaging</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @see #isSetPackaging()
	 * @see #getPackaging()
	 * @see #setPackaging(String)
	 * @generated
	 */
	void unsetPackaging();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getPackaging
	 * <em>Packaging</em>}' attribute is set. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Packaging</em>' attribute is set.
	 * @see #unsetPackaging()
	 * @see #getPackaging()
	 * @see #setPackaging(String)
	 * @generated
	 */
	boolean isSetPackaging();

	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * 
	 * The full name of the project.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_Name()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='name' namespace='##targetNamespace'"
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link org.eclipse.m2e.model.edit.pom.Model#getName
	 * <em>Name</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Version</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * The current version of the artifact produced by this project.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Version</em>' attribute.
	 * @see #setVersion(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_Version()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='version' namespace='##targetNamespace'"
	 * @generated
	 */
	String getVersion();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getVersion <em>Version</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Version</em>' attribute.
	 * @see #getVersion()
	 * @generated
	 */
	void setVersion(String value);

	/**
	 * Returns the value of the '<em><b>Description</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * 
	 * A detailed description of the project, used by Maven whenever it needs to
	 * describe the project, such as on the web site. While this element can be
	 * specified as CDATA to enable the use of HTML tags within the description,
	 * it is discouraged to allow plain text representation. If you need to
	 * modify the index page of the generated web site, you are able to specify
	 * your own instead of adjusting this text.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Description</em>' attribute.
	 * @see #setDescription(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_Description()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='description' namespace='##targetNamespace'"
	 * @generated
	 */
	String getDescription();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getDescription
	 * <em>Description</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Description</em>' attribute.
	 * @see #getDescription()
	 * @generated
	 */
	void setDescription(String value);

	/**
	 * Returns the value of the '<em><b>Url</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * 
	 * The URL to the project's homepage.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Url</em>' attribute.
	 * @see #setUrl(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_Url()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='url' namespace='##targetNamespace'"
	 * @generated
	 */
	String getUrl();

	/**
	 * Sets the value of the '{@link org.eclipse.m2e.model.edit.pom.Model#getUrl
	 * <em>Url</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Url</em>' attribute.
	 * @see #getUrl()
	 * @generated
	 */
	void setUrl(String value);

	/**
	 * Returns the value of the '<em><b>Prerequisites</b></em>' containment
	 * reference. <!-- begin-user-doc --> <!-- end-user-doc --> <!--
	 * begin-model-doc --> 4.0.0
	 * 
	 * Describes the prerequisites in the build environment for this project.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Prerequisites</em>' containment reference.
	 * @see #isSetPrerequisites()
	 * @see #unsetPrerequisites()
	 * @see #setPrerequisites(Prerequisites)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_Prerequisites()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='prerequisites' namespace='##targetNamespace'"
	 * @generated
	 */
	Prerequisites getPrerequisites();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getPrerequisites
	 * <em>Prerequisites</em>}' containment reference. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Prerequisites</em>' containment
	 *            reference.
	 * @see #isSetPrerequisites()
	 * @see #unsetPrerequisites()
	 * @see #getPrerequisites()
	 * @generated
	 */
	void setPrerequisites(Prerequisites value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getPrerequisites
	 * <em>Prerequisites</em>}' containment reference. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @see #isSetPrerequisites()
	 * @see #getPrerequisites()
	 * @see #setPrerequisites(Prerequisites)
	 * @generated
	 */
	void unsetPrerequisites();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getPrerequisites
	 * <em>Prerequisites</em>}' containment reference is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Prerequisites</em>' containment
	 *         reference is set.
	 * @see #unsetPrerequisites()
	 * @see #getPrerequisites()
	 * @see #setPrerequisites(Prerequisites)
	 * @generated
	 */
	boolean isSetPrerequisites();

	/**
	 * Returns the value of the '<em><b>Issue Management</b></em>' containment
	 * reference. <!-- begin-user-doc --> <!-- end-user-doc --> <!--
	 * begin-model-doc --> 4.0.0 The project's issue management system
	 * information. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Issue Management</em>' containment
	 *         reference.
	 * @see #isSetIssueManagement()
	 * @see #unsetIssueManagement()
	 * @see #setIssueManagement(IssueManagement)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_IssueManagement()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='issueManagement' namespace='##targetNamespace'"
	 * @generated
	 */
	IssueManagement getIssueManagement();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getIssueManagement
	 * <em>Issue Management</em>}' containment reference. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Issue Management</em>' containment
	 *            reference.
	 * @see #isSetIssueManagement()
	 * @see #unsetIssueManagement()
	 * @see #getIssueManagement()
	 * @generated
	 */
	void setIssueManagement(IssueManagement value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getIssueManagement
	 * <em>Issue Management</em>}' containment reference. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @see #isSetIssueManagement()
	 * @see #getIssueManagement()
	 * @see #setIssueManagement(IssueManagement)
	 * @generated
	 */
	void unsetIssueManagement();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getIssueManagement
	 * <em>Issue Management</em>}' containment reference is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Issue Management</em>' containment
	 *         reference is set.
	 * @see #unsetIssueManagement()
	 * @see #getIssueManagement()
	 * @see #setIssueManagement(IssueManagement)
	 * @generated
	 */
	boolean isSetIssueManagement();

	/**
	 * Returns the value of the '<em><b>Ci Management</b></em>' containment
	 * reference. <!-- begin-user-doc --> <!-- end-user-doc --> <!--
	 * begin-model-doc --> 4.0.0 The project's continuous integration
	 * information. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Ci Management</em>' containment reference.
	 * @see #isSetCiManagement()
	 * @see #unsetCiManagement()
	 * @see #setCiManagement(CiManagement)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_CiManagement()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='ciManagement' namespace='##targetNamespace'"
	 * @generated
	 */
	CiManagement getCiManagement();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getCiManagement
	 * <em>Ci Management</em>}' containment reference. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Ci Management</em>' containment
	 *            reference.
	 * @see #isSetCiManagement()
	 * @see #unsetCiManagement()
	 * @see #getCiManagement()
	 * @generated
	 */
	void setCiManagement(CiManagement value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getCiManagement
	 * <em>Ci Management</em>}' containment reference. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @see #isSetCiManagement()
	 * @see #getCiManagement()
	 * @see #setCiManagement(CiManagement)
	 * @generated
	 */
	void unsetCiManagement();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getCiManagement
	 * <em>Ci Management</em>}' containment reference is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Ci Management</em>' containment
	 *         reference is set.
	 * @see #unsetCiManagement()
	 * @see #getCiManagement()
	 * @see #setCiManagement(CiManagement)
	 * @generated
	 */
	boolean isSetCiManagement();

	/**
	 * Returns the value of the '<em><b>Inception Year</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * 
	 * The year of the project's inception, specified with 4 digits. This value
	 * is used when generating copyright notices as well as being informational.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Inception Year</em>' attribute.
	 * @see #setInceptionYear(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_InceptionYear()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='inceptionYear' namespace='##targetNamespace'"
	 * @generated
	 */
	String getInceptionYear();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getInceptionYear
	 * <em>Inception Year</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Inception Year</em>' attribute.
	 * @see #getInceptionYear()
	 * @generated
	 */
	void setInceptionYear(String value);

	/**
	 * Returns the value of the '<em><b>Mailing Lists</b></em>' containment
	 * reference list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.MailingList}. <!-- begin-user-doc -->
	 * <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * 
	 * Contains information about a project's mailing lists.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Mailing Lists</em>' containment reference
	 *         list.
	 * @see #isSetMailingLists()
	 * @see #unsetMailingLists()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_MailingLists()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='mailingLists' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<MailingList> getMailingLists();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getMailingLists
	 * <em>Mailing Lists</em>}' containment reference list. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @see #isSetMailingLists()
	 * @see #getMailingLists()
	 * @generated
	 */
	void unsetMailingLists();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getMailingLists
	 * <em>Mailing Lists</em>}' containment reference list is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Mailing Lists</em>' containment
	 *         reference list is set.
	 * @see #unsetMailingLists()
	 * @see #getMailingLists()
	 * @generated
	 */
	boolean isSetMailingLists();

	/**
	 * Returns the value of the '<em><b>Developers</b></em>' containment
	 * reference list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.Developer}. <!-- begin-user-doc -->
	 * <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * 
	 * Describes the committers of a project.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Developers</em>' containment reference
	 *         list.
	 * @see #isSetDevelopers()
	 * @see #unsetDevelopers()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_Developers()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='developers' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<Developer> getDevelopers();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getDevelopers
	 * <em>Developers</em>}' containment reference list. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @see #isSetDevelopers()
	 * @see #getDevelopers()
	 * @generated
	 */
	void unsetDevelopers();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getDevelopers
	 * <em>Developers</em>}' containment reference list is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Developers</em>' containment
	 *         reference list is set.
	 * @see #unsetDevelopers()
	 * @see #getDevelopers()
	 * @generated
	 */
	boolean isSetDevelopers();

	/**
	 * Returns the value of the '<em><b>Contributors</b></em>' containment
	 * reference list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.Contributor}. <!-- begin-user-doc -->
	 * <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * 
	 * Describes the contributors to a project that are not yet committers.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Contributors</em>' containment reference
	 *         list.
	 * @see #isSetContributors()
	 * @see #unsetContributors()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_Contributors()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='contributors' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<Contributor> getContributors();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getContributors
	 * <em>Contributors</em>}' containment reference list. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @see #isSetContributors()
	 * @see #getContributors()
	 * @generated
	 */
	void unsetContributors();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getContributors
	 * <em>Contributors</em>}' containment reference list is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Contributors</em>' containment
	 *         reference list is set.
	 * @see #unsetContributors()
	 * @see #getContributors()
	 * @generated
	 */
	boolean isSetContributors();

	/**
	 * Returns the value of the '<em><b>Licenses</b></em>' containment reference
	 * list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.License}. <!-- begin-user-doc -->
	 * <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * 
	 * This element describes all of the licenses for this project. Each license
	 * is described by a &lt;code&gt;license&lt;/code&gt; element, which is then
	 * described by additional elements. Projects should only list the
	 * license(s) that applies to the project and not the licenses that apply to
	 * dependencies. If multiple licenses are listed, it is assumed that the
	 * user can select any of them, not that they must accept all.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Licenses</em>' containment reference list.
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_Licenses()
	 * @model containment="true" extendedMetaData=
	 *        "kind='element' name='licenses' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<License> getLicenses();

	/**
	 * Returns the value of the '<em><b>Scm</b></em>' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc -->
	 * 4.0.0
	 * 
	 * Specification for the SCM used by the project, such as CVS, Subversion,
	 * etc. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Scm</em>' containment reference.
	 * @see #isSetScm()
	 * @see #unsetScm()
	 * @see #setScm(Scm)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_Scm()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='scm' namespace='##targetNamespace'"
	 * @generated
	 */
	Scm getScm();

	/**
	 * Sets the value of the '{@link org.eclipse.m2e.model.edit.pom.Model#getScm
	 * <em>Scm</em>}' containment reference. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Scm</em>' containment reference.
	 * @see #isSetScm()
	 * @see #unsetScm()
	 * @see #getScm()
	 * @generated
	 */
	void setScm(Scm value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getScm <em>Scm</em>}'
	 * containment reference. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #isSetScm()
	 * @see #getScm()
	 * @see #setScm(Scm)
	 * @generated
	 */
	void unsetScm();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getScm <em>Scm</em>}'
	 * containment reference is set. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @return whether the value of the '<em>Scm</em>' containment reference is
	 *         set.
	 * @see #unsetScm()
	 * @see #getScm()
	 * @see #setScm(Scm)
	 * @generated
	 */
	boolean isSetScm();

	/**
	 * Returns the value of the '<em><b>Organization</b></em>' containment
	 * reference. <!-- begin-user-doc --> <!-- end-user-doc --> <!--
	 * begin-model-doc --> 3.0.0+
	 * 
	 * This element describes various attributes of the organization to which
	 * the project belongs. These attributes are utilized when documentation is
	 * created (for copyright notices and links).
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Organization</em>' containment reference.
	 * @see #isSetOrganization()
	 * @see #unsetOrganization()
	 * @see #setOrganization(Organization)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_Organization()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='organization' namespace='##targetNamespace'"
	 * @generated
	 */
	Organization getOrganization();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getOrganization
	 * <em>Organization</em>}' containment reference. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Organization</em>' containment
	 *            reference.
	 * @see #isSetOrganization()
	 * @see #unsetOrganization()
	 * @see #getOrganization()
	 * @generated
	 */
	void setOrganization(Organization value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getOrganization
	 * <em>Organization</em>}' containment reference. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @see #isSetOrganization()
	 * @see #getOrganization()
	 * @see #setOrganization(Organization)
	 * @generated
	 */
	void unsetOrganization();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getOrganization
	 * <em>Organization</em>}' containment reference is set. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Organization</em>' containment
	 *         reference is set.
	 * @see #unsetOrganization()
	 * @see #getOrganization()
	 * @see #setOrganization(Organization)
	 * @generated
	 */
	boolean isSetOrganization();

	/**
	 * Returns the value of the '<em><b>Build</b></em>' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc -->
	 * 3.0.0+ Information required to build the project. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Build</em>' containment reference.
	 * @see #isSetBuild()
	 * @see #unsetBuild()
	 * @see #setBuild(Build)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_Build()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='build' namespace='##targetNamespace'"
	 * @generated
	 */
	Build getBuild();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getBuild <em>Build</em>}'
	 * containment reference. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Build</em>' containment reference.
	 * @see #isSetBuild()
	 * @see #unsetBuild()
	 * @see #getBuild()
	 * @generated
	 */
	void setBuild(Build value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getBuild <em>Build</em>}'
	 * containment reference. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #isSetBuild()
	 * @see #getBuild()
	 * @see #setBuild(Build)
	 * @generated
	 */
	void unsetBuild();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getBuild <em>Build</em>}'
	 * containment reference is set. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @return whether the value of the '<em>Build</em>' containment reference
	 *         is set.
	 * @see #unsetBuild()
	 * @see #getBuild()
	 * @see #setBuild(Build)
	 * @generated
	 */
	boolean isSetBuild();

	/**
	 * Returns the value of the '<em><b>Profiles</b></em>' containment reference
	 * list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.Profile}. <!-- begin-user-doc -->
	 * <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * A listing of project-local build profiles which will modify the build
	 * process when activated.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Profiles</em>' containment reference list.
	 * @see #isSetProfiles()
	 * @see #unsetProfiles()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_Profiles()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='profiles' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<Profile> getProfiles();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getProfiles <em>Profiles</em>}'
	 * containment reference list. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #isSetProfiles()
	 * @see #getProfiles()
	 * @generated
	 */
	void unsetProfiles();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getProfiles <em>Profiles</em>}'
	 * containment reference list is set. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Profiles</em>' containment
	 *         reference list is set.
	 * @see #unsetProfiles()
	 * @see #getProfiles()
	 * @generated
	 */
	boolean isSetProfiles();

	/**
	 * Returns the value of the '<em><b>Repositories</b></em>' containment
	 * reference list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.Repository}. <!-- begin-user-doc -->
	 * <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0 The lists of the
	 * remote repositories for discovering dependencies and extensions. <!--
	 * end-model-doc -->
	 * 
	 * @return the value of the '<em>Repositories</em>' containment reference
	 *         list.
	 * @see #isSetRepositories()
	 * @see #unsetRepositories()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_Repositories()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='repositories' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<Repository> getRepositories();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getRepositories
	 * <em>Repositories</em>}' containment reference list. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @see #isSetRepositories()
	 * @see #getRepositories()
	 * @generated
	 */
	void unsetRepositories();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getRepositories
	 * <em>Repositories</em>}' containment reference list is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Repositories</em>' containment
	 *         reference list is set.
	 * @see #unsetRepositories()
	 * @see #getRepositories()
	 * @generated
	 */
	boolean isSetRepositories();

	/**
	 * Returns the value of the '<em><b>Plugin Repositories</b></em>'
	 * containment reference list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.Repository}. <!-- begin-user-doc -->
	 * <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * The lists of the remote repositories for discovering plugins for builds
	 * and reports. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Plugin Repositories</em>' containment
	 *         reference list.
	 * @see #isSetPluginRepositories()
	 * @see #unsetPluginRepositories()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_PluginRepositories()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='pluginRepositories' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<Repository> getPluginRepositories();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getPluginRepositories
	 * <em>Plugin Repositories</em>}' containment reference list. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #isSetPluginRepositories()
	 * @see #getPluginRepositories()
	 * @generated
	 */
	void unsetPluginRepositories();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getPluginRepositories
	 * <em>Plugin Repositories</em>}' containment reference list is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Plugin Repositories</em>'
	 *         containment reference list is set.
	 * @see #unsetPluginRepositories()
	 * @see #getPluginRepositories()
	 * @generated
	 */
	boolean isSetPluginRepositories();

	/**
	 * Returns the value of the '<em><b>Dependencies</b></em>' containment
	 * reference list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.Dependency}. <!-- begin-user-doc -->
	 * <!-- end-user-doc --> <!-- begin-model-doc --> 3.0.0+
	 * 
	 * This element describes all of the dependencies associated with a project.
	 * These dependencies are used to construct a classpath for your project
	 * during the build process. They are automatically downloaded from the
	 * repositories defined in this project. See &lt;a href="http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html"
	 * &gt;the dependency mechanism&lt;/a&gt; for more information.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Dependencies</em>' containment reference
	 *         list.
	 * @see #isSetDependencies()
	 * @see #unsetDependencies()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_Dependencies()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='dependencies' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<Dependency> getDependencies();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getDependencies
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
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getDependencies
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
	 * Returns the value of the '<em><b>Reporting</b></em>' containment
	 * reference. <!-- begin-user-doc --> <!-- end-user-doc --> <!--
	 * begin-model-doc --> 4.0.0
	 * 
	 * This element includes the specification of report plugins to use to
	 * generate the reports on the Maven-generated site. These reports will be
	 * run when a user executes &lt;code&gt;mvn site&lt;/code&gt;. All of the
	 * reports will be included in the navigation bar for browsing.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Reporting</em>' containment reference.
	 * @see #isSetReporting()
	 * @see #unsetReporting()
	 * @see #setReporting(Reporting)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_Reporting()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='reporting' namespace='##targetNamespace'"
	 * @generated
	 */
	Reporting getReporting();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getReporting
	 * <em>Reporting</em>}' containment reference. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Reporting</em>' containment
	 *            reference.
	 * @see #isSetReporting()
	 * @see #unsetReporting()
	 * @see #getReporting()
	 * @generated
	 */
	void setReporting(Reporting value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getReporting
	 * <em>Reporting</em>}' containment reference. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @see #isSetReporting()
	 * @see #getReporting()
	 * @see #setReporting(Reporting)
	 * @generated
	 */
	void unsetReporting();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getReporting
	 * <em>Reporting</em>}' containment reference is set. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Reporting</em>' containment
	 *         reference is set.
	 * @see #unsetReporting()
	 * @see #getReporting()
	 * @see #setReporting(Reporting)
	 * @generated
	 */
	boolean isSetReporting();

	/**
	 * Returns the value of the '<em><b>Dependency Management</b></em>'
	 * containment reference. <!-- begin-user-doc --> <!-- end-user-doc --> <!--
	 * begin-model-doc --> 4.0.0
	 * 
	 * Default dependency information for projects that inherit from this one.
	 * The dependencies in this section are not immediately resolved. Instead,
	 * when a POM derived from this one declares a dependency described by a
	 * matching groupId and artifactId, the version and other values from this
	 * section are used for that dependency if they were not already specified.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Dependency Management</em>' containment
	 *         reference.
	 * @see #isSetDependencyManagement()
	 * @see #unsetDependencyManagement()
	 * @see #setDependencyManagement(DependencyManagement)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_DependencyManagement()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='dependencyManagement' namespace='##targetNamespace'"
	 * @generated
	 */
	DependencyManagement getDependencyManagement();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getDependencyManagement
	 * <em>Dependency Management</em>}' containment reference. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Dependency Management</em>'
	 *            containment reference.
	 * @see #isSetDependencyManagement()
	 * @see #unsetDependencyManagement()
	 * @see #getDependencyManagement()
	 * @generated
	 */
	void setDependencyManagement(DependencyManagement value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getDependencyManagement
	 * <em>Dependency Management</em>}' containment reference. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #isSetDependencyManagement()
	 * @see #getDependencyManagement()
	 * @see #setDependencyManagement(DependencyManagement)
	 * @generated
	 */
	void unsetDependencyManagement();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getDependencyManagement
	 * <em>Dependency Management</em>}' containment reference is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Dependency Management</em>'
	 *         containment reference is set.
	 * @see #unsetDependencyManagement()
	 * @see #getDependencyManagement()
	 * @see #setDependencyManagement(DependencyManagement)
	 * @generated
	 */
	boolean isSetDependencyManagement();

	/**
	 * Returns the value of the '<em><b>Distribution Management</b></em>'
	 * containment reference. <!-- begin-user-doc --> <!-- end-user-doc --> <!--
	 * begin-model-doc --> 4.0.0 Distribution information for a project that
	 * enables deployment of the site and artifacts to remote web servers and
	 * repositories respectively. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Distribution Management</em>' containment
	 *         reference.
	 * @see #isSetDistributionManagement()
	 * @see #unsetDistributionManagement()
	 * @see #setDistributionManagement(DistributionManagement)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_DistributionManagement()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='distributionManagement' namespace='##targetNamespace'"
	 * @generated
	 */
	DistributionManagement getDistributionManagement();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getDistributionManagement
	 * <em>Distribution Management</em>}' containment reference. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Distribution Management</em>'
	 *            containment reference.
	 * @see #isSetDistributionManagement()
	 * @see #unsetDistributionManagement()
	 * @see #getDistributionManagement()
	 * @generated
	 */
	void setDistributionManagement(DistributionManagement value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getDistributionManagement
	 * <em>Distribution Management</em>}' containment reference. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #isSetDistributionManagement()
	 * @see #getDistributionManagement()
	 * @see #setDistributionManagement(DistributionManagement)
	 * @generated
	 */
	void unsetDistributionManagement();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getDistributionManagement
	 * <em>Distribution Management</em>}' containment reference is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Distribution Management</em>'
	 *         containment reference is set.
	 * @see #unsetDistributionManagement()
	 * @see #getDistributionManagement()
	 * @see #setDistributionManagement(DistributionManagement)
	 * @generated
	 */
	boolean isSetDistributionManagement();

	/**
	 * Returns the value of the '<em><b>Properties</b></em>' containment
	 * reference list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.PropertyElement}. <!-- begin-user-doc
	 * --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * Properties that can be used throughout the POM as a substitution, and are
	 * used as filters in resources if enabled. The format is
	 * &lt;code&gt;&amp;lt;name&amp;gt;value&amp;lt;/name&amp;gt;&lt;/code&gt;.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Properties</em>' containment reference
	 *         list.
	 * @see #isSetProperties()
	 * @see #unsetProperties()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_Properties()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='properties' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<PropertyElement> getProperties();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getProperties
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
	 * {@link org.eclipse.m2e.model.edit.pom.Model#getProperties
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
	 * Returns the value of the '<em><b>Modules</b></em>' attribute list. The
	 * list contents are of type {@link java.lang.String}. <!-- begin-user-doc
	 * -->
	 * <p>
	 * If the meaning of the '<em>Modules</em>' attribute list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * 
	 * @return the value of the '<em>Modules</em>' attribute list.
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getModel_Modules()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 * @generated
	 */
	EList<String> getModules();

} // Model
