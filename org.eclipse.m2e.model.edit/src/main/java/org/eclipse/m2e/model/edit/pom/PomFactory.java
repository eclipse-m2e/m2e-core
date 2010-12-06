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

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc --> The <b>Factory</b> for the model. It provides a
 * create method for each non-abstract class of the model. <!-- end-user-doc -->
 * 
 * @see org.eclipse.m2e.model.edit.pom.PomPackage
 * @generated
 */
public interface PomFactory extends EFactory {
	/**
	 * The singleton instance of the factory. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated
	 */
	PomFactory eINSTANCE = org.eclipse.m2e.model.edit.pom.impl.PomFactoryImpl
			.init();

	/**
	 * Returns a new object of class '<em>Activation</em>'. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Activation</em>'.
	 * @generated
	 */
	Activation createActivation();

	/**
	 * Returns a new object of class '<em>Activation File</em>'. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Activation File</em>'.
	 * @generated
	 */
	ActivationFile createActivationFile();

	/**
	 * Returns a new object of class '<em>Activation OS</em>'. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Activation OS</em>'.
	 * @generated
	 */
	ActivationOS createActivationOS();

	/**
	 * Returns a new object of class '<em>Activation Property</em>'. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Activation Property</em>'.
	 * @generated
	 */
	ActivationProperty createActivationProperty();

	/**
	 * Returns a new object of class '<em>Build</em>'. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Build</em>'.
	 * @generated
	 */
	Build createBuild();

	/**
	 * Returns a new object of class '<em>Build Base</em>'. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Build Base</em>'.
	 * @generated
	 */
	BuildBase createBuildBase();

	/**
	 * Returns a new object of class '<em>Ci Management</em>'. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Ci Management</em>'.
	 * @generated
	 */
	CiManagement createCiManagement();

	/**
	 * Returns a new object of class '<em>Contributor</em>'. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Contributor</em>'.
	 * @generated
	 */
	Contributor createContributor();

	/**
	 * Returns a new object of class '<em>Dependency</em>'. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Dependency</em>'.
	 * @generated
	 */
	Dependency createDependency();

	/**
	 * Returns a new object of class '<em>Dependency Management</em>'. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Dependency Management</em>'.
	 * @generated
	 */
	DependencyManagement createDependencyManagement();

	/**
	 * Returns a new object of class '<em>Deployment Repository</em>'. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Deployment Repository</em>'.
	 * @generated
	 */
	DeploymentRepository createDeploymentRepository();

	/**
	 * Returns a new object of class '<em>Developer</em>'. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Developer</em>'.
	 * @generated
	 */
	Developer createDeveloper();

	/**
	 * Returns a new object of class '<em>Distribution Management</em>'. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Distribution Management</em>'.
	 * @generated
	 */
	DistributionManagement createDistributionManagement();

	/**
	 * Returns a new object of class '<em>Document Root</em>'. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Document Root</em>'.
	 * @generated
	 */
	DocumentRoot createDocumentRoot();

	/**
	 * Returns a new object of class '<em>Exclusion</em>'. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Exclusion</em>'.
	 * @generated
	 */
	Exclusion createExclusion();

	/**
	 * Returns a new object of class '<em>Extension</em>'. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Extension</em>'.
	 * @generated
	 */
	Extension createExtension();

	/**
	 * Returns a new object of class '<em>Issue Management</em>'. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Issue Management</em>'.
	 * @generated
	 */
	IssueManagement createIssueManagement();

	/**
	 * Returns a new object of class '<em>License</em>'. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>License</em>'.
	 * @generated
	 */
	License createLicense();

	/**
	 * Returns a new object of class '<em>Mailing List</em>'. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Mailing List</em>'.
	 * @generated
	 */
	MailingList createMailingList();

	/**
	 * Returns a new object of class '<em>Model</em>'. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Model</em>'.
	 * @generated
	 */
	Model createModel();

	/**
	 * Returns a new object of class '<em>Notifier</em>'. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Notifier</em>'.
	 * @generated
	 */
	Notifier createNotifier();

	/**
	 * Returns a new object of class '<em>Organization</em>'. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Organization</em>'.
	 * @generated
	 */
	Organization createOrganization();

	/**
	 * Returns a new object of class '<em>Parent</em>'. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Parent</em>'.
	 * @generated
	 */
	Parent createParent();

	/**
	 * Returns a new object of class '<em>Plugin</em>'. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Plugin</em>'.
	 * @generated
	 */
	Plugin createPlugin();

	/**
	 * Returns a new object of class '<em>Plugin Execution</em>'. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Plugin Execution</em>'.
	 * @generated
	 */
	PluginExecution createPluginExecution();

	/**
	 * Returns a new object of class '<em>Plugin Management</em>'. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Plugin Management</em>'.
	 * @generated
	 */
	PluginManagement createPluginManagement();

	/**
	 * Returns a new object of class '<em>Prerequisites</em>'. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Prerequisites</em>'.
	 * @generated
	 */
	Prerequisites createPrerequisites();

	/**
	 * Returns a new object of class '<em>Profile</em>'. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Profile</em>'.
	 * @generated
	 */
	Profile createProfile();

	/**
	 * Returns a new object of class '<em>Relocation</em>'. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Relocation</em>'.
	 * @generated
	 */
	Relocation createRelocation();

	/**
	 * Returns a new object of class '<em>Reporting</em>'. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Reporting</em>'.
	 * @generated
	 */
	Reporting createReporting();

	/**
	 * Returns a new object of class '<em>Report Plugin</em>'. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Report Plugin</em>'.
	 * @generated
	 */
	ReportPlugin createReportPlugin();

	/**
	 * Returns a new object of class '<em>Report Set</em>'. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Report Set</em>'.
	 * @generated
	 */
	ReportSet createReportSet();

	/**
	 * Returns a new object of class '<em>Repository</em>'. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Repository</em>'.
	 * @generated
	 */
	Repository createRepository();

	/**
	 * Returns a new object of class '<em>Repository Policy</em>'. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Repository Policy</em>'.
	 * @generated
	 */
	RepositoryPolicy createRepositoryPolicy();

	/**
	 * Returns a new object of class '<em>Resource</em>'. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Resource</em>'.
	 * @generated
	 */
	Resource createResource();

	/**
	 * Returns a new object of class '<em>Scm</em>'. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Scm</em>'.
	 * @generated
	 */
	Scm createScm();

	/**
	 * Returns a new object of class '<em>Site</em>'. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Site</em>'.
	 * @generated
	 */
	Site createSite();

	/**
	 * Returns a new object of class '<em>Property Element</em>'. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Property Element</em>'.
	 * @generated
	 */
	PropertyElement createPropertyElement();

	/**
	 * Returns a new object of class '<em>Configuration</em>'. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return a new object of class '<em>Configuration</em>'.
	 * @generated
	 */
	Configuration createConfiguration();

	/**
	 * Returns the package supported by this factory. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @return the package supported by this factory.
	 * @generated
	 */
	PomPackage getPomPackage();

} // PomFactory
