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
 * <em><b>Profile</b></em>'. <!-- end-user-doc -->
 * 
 * <!-- begin-model-doc --> 4.0.0
 * 
 * Modifications to the build process which is activated based on environmental
 * parameters or command line arguments.
 * 
 * <!-- end-model-doc -->
 * 
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Profile#getId <em>Id</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Profile#getActivation <em>Activation
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Profile#getBuild <em>Build</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Profile#getRepositories <em>
 * Repositories</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Profile#getPluginRepositories <em>
 * Plugin Repositories</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Profile#getDependencies <em>
 * Dependencies</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Profile#getReports <em>Reports</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Profile#getDependencyManagement <em>
 * Dependency Management</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Profile#getDistributionManagement
 * <em>Distribution Management</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Profile#getProperties <em>Properties
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Profile#getModules <em>Modules</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.Profile#getReporting <em>Reporting
 * </em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getProfile()
 * @model extendedMetaData="name='Profile' kind='elementOnly'"
 * @generated
 */
public interface Profile extends EObject {
	/**
	 * Returns the value of the '<em><b>Id</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * The identifier of this build profile. This used both for command line
	 * activation, and identifies identical profiles to merge with during
	 * inheritance. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Id</em>' attribute.
	 * @see #setId(String)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getProfile_Id()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='id' namespace='##targetNamespace'"
	 * @generated
	 */
	String getId();

	/**
	 * Sets the value of the '{@link org.eclipse.m2e.model.edit.pom.Profile#getId
	 * <em>Id</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Id</em>' attribute.
	 * @see #getId()
	 * @generated
	 */
	void setId(String value);

	/**
	 * Returns the value of the '<em><b>Activation</b></em>' containment
	 * reference. <!-- begin-user-doc --> <!-- end-user-doc --> <!--
	 * begin-model-doc --> 4.0.0 The conditional logic which will automatically
	 * trigger the inclusion of this profile. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Activation</em>' containment reference.
	 * @see #isSetActivation()
	 * @see #unsetActivation()
	 * @see #setActivation(Activation)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getProfile_Activation()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='activation' namespace='##targetNamespace'"
	 * @generated
	 */
	Activation getActivation();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getActivation
	 * <em>Activation</em>}' containment reference. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Activation</em>' containment
	 *            reference.
	 * @see #isSetActivation()
	 * @see #unsetActivation()
	 * @see #getActivation()
	 * @generated
	 */
	void setActivation(Activation value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getActivation
	 * <em>Activation</em>}' containment reference. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @see #isSetActivation()
	 * @see #getActivation()
	 * @see #setActivation(Activation)
	 * @generated
	 */
	void unsetActivation();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getActivation
	 * <em>Activation</em>}' containment reference is set. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Activation</em>' containment
	 *         reference is set.
	 * @see #unsetActivation()
	 * @see #getActivation()
	 * @see #setActivation(Activation)
	 * @generated
	 */
	boolean isSetActivation();

	/**
	 * Returns the value of the '<em><b>Build</b></em>' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc -->
	 * 4.0.0 Information required to build the project. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Build</em>' containment reference.
	 * @see #isSetBuild()
	 * @see #unsetBuild()
	 * @see #setBuild(BuildBase)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getProfile_Build()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='build' namespace='##targetNamespace'"
	 * @generated
	 */
	BuildBase getBuild();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getBuild <em>Build</em>}'
	 * containment reference. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Build</em>' containment reference.
	 * @see #isSetBuild()
	 * @see #unsetBuild()
	 * @see #getBuild()
	 * @generated
	 */
	void setBuild(BuildBase value);

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getBuild <em>Build</em>}'
	 * containment reference. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #isSetBuild()
	 * @see #getBuild()
	 * @see #setBuild(BuildBase)
	 * @generated
	 */
	void unsetBuild();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getBuild <em>Build</em>}'
	 * containment reference is set. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @return whether the value of the '<em>Build</em>' containment reference
	 *         is set.
	 * @see #unsetBuild()
	 * @see #getBuild()
	 * @see #setBuild(BuildBase)
	 * @generated
	 */
	boolean isSetBuild();

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
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getProfile_Repositories()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='repositories' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<Repository> getRepositories();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getRepositories
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
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getRepositories
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
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getProfile_PluginRepositories()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='pluginRepositories' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<Repository> getPluginRepositories();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getPluginRepositories
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
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getPluginRepositories
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
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getProfile_Dependencies()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='dependencies' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<Dependency> getDependencies();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getDependencies
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
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getDependencies
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
	 * Returns the value of the '<em><b>Reports</b></em>' containment reference
	 * list. The list contents are of type
	 * {@link org.eclipse.m2e.model.edit.pom.ReportPlugin}. <!-- begin-user-doc
	 * --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * &lt;b&gt;Deprecated&lt;/b&gt;. Now ignored by Maven.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Reports</em>' containment reference list.
	 * @see #isSetReports()
	 * @see #unsetReports()
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getProfile_Reports()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='reports' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<ReportPlugin> getReports();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getReports <em>Reports</em>}'
	 * containment reference list. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #isSetReports()
	 * @see #getReports()
	 * @generated
	 */
	void unsetReports();

	/**
	 * Returns whether the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getReports <em>Reports</em>}'
	 * containment reference list is set. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Reports</em>' containment reference
	 *         list is set.
	 * @see #unsetReports()
	 * @see #getReports()
	 * @generated
	 */
	boolean isSetReports();

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
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getProfile_DependencyManagement()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='dependencyManagement' namespace='##targetNamespace'"
	 * @generated
	 */
	DependencyManagement getDependencyManagement();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getDependencyManagement
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
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getDependencyManagement
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
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getDependencyManagement
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
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getProfile_DistributionManagement()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='distributionManagement' namespace='##targetNamespace'"
	 * @generated
	 */
	DistributionManagement getDistributionManagement();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getDistributionManagement
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
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getDistributionManagement
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
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getDistributionManagement
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
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getProfile_Properties()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='properties' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<PropertyElement> getProperties();

	/**
	 * Unsets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getProperties
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
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getProperties
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
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getProfile_Modules()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 * @generated
	 */
	EList<String> getModules();

	/**
	 * Returns the value of the '<em><b>Reporting</b></em>' reference. <!--
	 * begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Reporting</em>' reference isn't clear, there
	 * really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * 
	 * @return the value of the '<em>Reporting</em>' reference.
	 * @see #setReporting(Reporting)
	 * @see org.eclipse.m2e.model.edit.pom.PomPackage#getProfile_Reporting()
	 * @model
	 * @generated
	 */
	Reporting getReporting();

	/**
	 * Sets the value of the '
	 * {@link org.eclipse.m2e.model.edit.pom.Profile#getReporting
	 * <em>Reporting</em>}' reference. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @param value
	 *            the new value of the '<em>Reporting</em>' reference.
	 * @see #getReporting()
	 * @generated
	 */
	void setReporting(Reporting value);

} // Profile
