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

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;


/**
 * <!-- begin-user-doc --> The <b>Package</b> for the model. It contains accessors for the meta objects to represent
 * <ul>
 * <li>each class,</li>
 * <li>each feature of each class,</li>
 * <li>each enum,</li>
 * <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * 
 * @see org.eclipse.m2e.model.edit.pom.PomFactory
 * @model kind="package"
 * @generated
 */
public interface PomPackage extends EPackage {
  /**
   * The package name. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  String eNAME = "pom"; //$NON-NLS-1$

  /**
   * The package namespace URI. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  String eNS_URI = "http://maven.apache.org/POM/4.0.0"; //$NON-NLS-1$

  /**
   * The package namespace name. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  String eNS_PREFIX = "pom"; //$NON-NLS-1$

  /**
   * The singleton instance of the package. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  PomPackage eINSTANCE = org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl.init();

  /**
   * The meta object id for the ' {@link org.eclipse.m2e.model.edit.pom.impl.ActivationImpl <em>Activation</em>}' class.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.ActivationImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getActivation()
   * @generated
   */
  int ACTIVATION = 0;

  /**
   * The feature id for the '<em><b>Active By Default</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int ACTIVATION__ACTIVE_BY_DEFAULT = 0;

  /**
   * The feature id for the '<em><b>Jdk</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int ACTIVATION__JDK = 1;

  /**
   * The feature id for the '<em><b>Os</b></em>' containment reference. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int ACTIVATION__OS = 2;

  /**
   * The feature id for the '<em><b>Property</b></em>' containment reference. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   * @ordered
   */
  int ACTIVATION__PROPERTY = 3;

  /**
   * The feature id for the '<em><b>File</b></em>' containment reference. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int ACTIVATION__FILE = 4;

  /**
   * The number of structural features of the '<em>Activation</em>' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int ACTIVATION_FEATURE_COUNT = 5;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.ActivationFileImpl <em>Activation File</em>}
   * ' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.ActivationFileImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getActivationFile()
   * @generated
   */
  int ACTIVATION_FILE = 1;

  /**
   * The feature id for the '<em><b>Missing</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int ACTIVATION_FILE__MISSING = 0;

  /**
   * The feature id for the '<em><b>Exists</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int ACTIVATION_FILE__EXISTS = 1;

  /**
   * The number of structural features of the '<em>Activation File</em>' class. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int ACTIVATION_FILE_FEATURE_COUNT = 2;

  /**
   * The meta object id for the ' {@link org.eclipse.m2e.model.edit.pom.impl.ActivationOSImpl <em>Activation OS</em>}'
   * class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.ActivationOSImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getActivationOS()
   * @generated
   */
  int ACTIVATION_OS = 2;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int ACTIVATION_OS__NAME = 0;

  /**
   * The feature id for the '<em><b>Family</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int ACTIVATION_OS__FAMILY = 1;

  /**
   * The feature id for the '<em><b>Arch</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int ACTIVATION_OS__ARCH = 2;

  /**
   * The feature id for the '<em><b>Version</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int ACTIVATION_OS__VERSION = 3;

  /**
   * The number of structural features of the '<em>Activation OS</em>' class. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   * @ordered
   */
  int ACTIVATION_OS_FEATURE_COUNT = 4;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.ActivationPropertyImpl
   * <em>Activation Property</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.ActivationPropertyImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getActivationProperty()
   * @generated
   */
  int ACTIVATION_PROPERTY = 3;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int ACTIVATION_PROPERTY__NAME = 0;

  /**
   * The feature id for the '<em><b>Value</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int ACTIVATION_PROPERTY__VALUE = 1;

  /**
   * The number of structural features of the '<em>Activation Property</em>' class. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int ACTIVATION_PROPERTY_FEATURE_COUNT = 2;

  /**
   * The meta object id for the ' {@link org.eclipse.m2e.model.edit.pom.impl.BuildBaseImpl <em>Build Base</em>}' class.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.BuildBaseImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getBuildBase()
   * @generated
   */
  int BUILD_BASE = 5;

  /**
   * The feature id for the '<em><b>Default Goal</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD_BASE__DEFAULT_GOAL = 0;

  /**
   * The feature id for the '<em><b>Resources</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD_BASE__RESOURCES = 1;

  /**
   * The feature id for the '<em><b>Test Resources</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD_BASE__TEST_RESOURCES = 2;

  /**
   * The feature id for the '<em><b>Directory</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD_BASE__DIRECTORY = 3;

  /**
   * The feature id for the '<em><b>Final Name</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD_BASE__FINAL_NAME = 4;

  /**
   * The feature id for the '<em><b>Plugin Management</b></em>' containment reference. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD_BASE__PLUGIN_MANAGEMENT = 5;

  /**
   * The feature id for the '<em><b>Plugins</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD_BASE__PLUGINS = 6;

  /**
   * The feature id for the '<em><b>Filters</b></em>' attribute list. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD_BASE__FILTERS = 7;

  /**
   * The number of structural features of the '<em>Build Base</em>' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD_BASE_FEATURE_COUNT = 8;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.BuildImpl <em>Build</em>}' class. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.BuildImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getBuild()
   * @generated
   */
  int BUILD = 4;

  /**
   * The feature id for the '<em><b>Default Goal</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD__DEFAULT_GOAL = BUILD_BASE__DEFAULT_GOAL;

  /**
   * The feature id for the '<em><b>Resources</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD__RESOURCES = BUILD_BASE__RESOURCES;

  /**
   * The feature id for the '<em><b>Test Resources</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD__TEST_RESOURCES = BUILD_BASE__TEST_RESOURCES;

  /**
   * The feature id for the '<em><b>Directory</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD__DIRECTORY = BUILD_BASE__DIRECTORY;

  /**
   * The feature id for the '<em><b>Final Name</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD__FINAL_NAME = BUILD_BASE__FINAL_NAME;

  /**
   * The feature id for the '<em><b>Plugin Management</b></em>' containment reference. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD__PLUGIN_MANAGEMENT = BUILD_BASE__PLUGIN_MANAGEMENT;

  /**
   * The feature id for the '<em><b>Plugins</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD__PLUGINS = BUILD_BASE__PLUGINS;

  /**
   * The feature id for the '<em><b>Filters</b></em>' attribute list. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD__FILTERS = BUILD_BASE__FILTERS;

  /**
   * The feature id for the '<em><b>Source Directory</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD__SOURCE_DIRECTORY = BUILD_BASE_FEATURE_COUNT + 0;

  /**
   * The feature id for the '<em><b>Script Source Directory</b></em>' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD__SCRIPT_SOURCE_DIRECTORY = BUILD_BASE_FEATURE_COUNT + 1;

  /**
   * The feature id for the '<em><b>Test Source Directory</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   * @ordered
   */
  int BUILD__TEST_SOURCE_DIRECTORY = BUILD_BASE_FEATURE_COUNT + 2;

  /**
   * The feature id for the '<em><b>Output Directory</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD__OUTPUT_DIRECTORY = BUILD_BASE_FEATURE_COUNT + 3;

  /**
   * The feature id for the '<em><b>Test Output Directory</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   * @ordered
   */
  int BUILD__TEST_OUTPUT_DIRECTORY = BUILD_BASE_FEATURE_COUNT + 4;

  /**
   * The feature id for the '<em><b>Extensions</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD__EXTENSIONS = BUILD_BASE_FEATURE_COUNT + 5;

  /**
   * The number of structural features of the '<em>Build</em>' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int BUILD_FEATURE_COUNT = BUILD_BASE_FEATURE_COUNT + 6;

  /**
   * The meta object id for the ' {@link org.eclipse.m2e.model.edit.pom.impl.CiManagementImpl <em>Ci Management</em>}'
   * class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.CiManagementImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getCiManagement()
   * @generated
   */
  int CI_MANAGEMENT = 6;

  /**
   * The feature id for the '<em><b>System</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int CI_MANAGEMENT__SYSTEM = 0;

  /**
   * The feature id for the '<em><b>Url</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int CI_MANAGEMENT__URL = 1;

  /**
   * The feature id for the '<em><b>Notifiers</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int CI_MANAGEMENT__NOTIFIERS = 2;

  /**
   * The number of structural features of the '<em>Ci Management</em>' class. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   * @ordered
   */
  int CI_MANAGEMENT_FEATURE_COUNT = 3;

  /**
   * The meta object id for the ' {@link org.eclipse.m2e.model.edit.pom.impl.ContributorImpl <em>Contributor</em>}'
   * class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.ContributorImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getContributor()
   * @generated
   */
  int CONTRIBUTOR = 7;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int CONTRIBUTOR__NAME = 0;

  /**
   * The feature id for the '<em><b>Email</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int CONTRIBUTOR__EMAIL = 1;

  /**
   * The feature id for the '<em><b>Url</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int CONTRIBUTOR__URL = 2;

  /**
   * The feature id for the '<em><b>Organization</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int CONTRIBUTOR__ORGANIZATION = 3;

  /**
   * The feature id for the '<em><b>Organization Url</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int CONTRIBUTOR__ORGANIZATION_URL = 4;

  /**
   * The feature id for the '<em><b>Timezone</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int CONTRIBUTOR__TIMEZONE = 5;

  /**
   * The feature id for the '<em><b>Properties</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int CONTRIBUTOR__PROPERTIES = 6;

  /**
   * The feature id for the '<em><b>Roles</b></em>' attribute list. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int CONTRIBUTOR__ROLES = 7;

  /**
   * The number of structural features of the '<em>Contributor</em>' class. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   * @ordered
   */
  int CONTRIBUTOR_FEATURE_COUNT = 8;

  /**
   * The meta object id for the ' {@link org.eclipse.m2e.model.edit.pom.impl.DependencyImpl <em>Dependency</em>}' class.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.DependencyImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getDependency()
   * @generated
   */
  int DEPENDENCY = 8;

  /**
   * The feature id for the '<em><b>Group Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEPENDENCY__GROUP_ID = 0;

  /**
   * The feature id for the '<em><b>Artifact Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEPENDENCY__ARTIFACT_ID = 1;

  /**
   * The feature id for the '<em><b>Version</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEPENDENCY__VERSION = 2;

  /**
   * The feature id for the '<em><b>Type</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEPENDENCY__TYPE = 3;

  /**
   * The feature id for the '<em><b>Classifier</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEPENDENCY__CLASSIFIER = 4;

  /**
   * The feature id for the '<em><b>Scope</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEPENDENCY__SCOPE = 5;

  /**
   * The feature id for the '<em><b>System Path</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEPENDENCY__SYSTEM_PATH = 6;

  /**
   * The feature id for the '<em><b>Exclusions</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEPENDENCY__EXCLUSIONS = 7;

  /**
   * The feature id for the '<em><b>Optional</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEPENDENCY__OPTIONAL = 8;

  /**
   * The number of structural features of the '<em>Dependency</em>' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEPENDENCY_FEATURE_COUNT = 9;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.DependencyManagementImpl
   * <em>Dependency Management</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.DependencyManagementImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getDependencyManagement()
   * @generated
   */
  int DEPENDENCY_MANAGEMENT = 9;

  /**
   * The feature id for the '<em><b>Dependencies</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEPENDENCY_MANAGEMENT__DEPENDENCIES = 0;

  /**
   * The number of structural features of the '<em>Dependency Management</em>' class. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEPENDENCY_MANAGEMENT_FEATURE_COUNT = 1;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.DeploymentRepositoryImpl
   * <em>Deployment Repository</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.DeploymentRepositoryImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getDeploymentRepository()
   * @generated
   */
  int DEPLOYMENT_REPOSITORY = 10;

  /**
   * The feature id for the '<em><b>Unique Version</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEPLOYMENT_REPOSITORY__UNIQUE_VERSION = 0;

  /**
   * The feature id for the '<em><b>Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEPLOYMENT_REPOSITORY__ID = 1;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEPLOYMENT_REPOSITORY__NAME = 2;

  /**
   * The feature id for the '<em><b>Url</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEPLOYMENT_REPOSITORY__URL = 3;

  /**
   * The feature id for the '<em><b>Layout</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEPLOYMENT_REPOSITORY__LAYOUT = 4;

  /**
   * The number of structural features of the '<em>Deployment Repository</em>' class. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEPLOYMENT_REPOSITORY_FEATURE_COUNT = 5;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.DeveloperImpl <em>Developer</em>}' class.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.DeveloperImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getDeveloper()
   * @generated
   */
  int DEVELOPER = 11;

  /**
   * The feature id for the '<em><b>Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEVELOPER__ID = 0;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEVELOPER__NAME = 1;

  /**
   * The feature id for the '<em><b>Email</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEVELOPER__EMAIL = 2;

  /**
   * The feature id for the '<em><b>Url</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEVELOPER__URL = 3;

  /**
   * The feature id for the '<em><b>Organization</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEVELOPER__ORGANIZATION = 4;

  /**
   * The feature id for the '<em><b>Organization Url</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEVELOPER__ORGANIZATION_URL = 5;

  /**
   * The feature id for the '<em><b>Timezone</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEVELOPER__TIMEZONE = 6;

  /**
   * The feature id for the '<em><b>Properties</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEVELOPER__PROPERTIES = 7;

  /**
   * The feature id for the '<em><b>Roles</b></em>' attribute list. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEVELOPER__ROLES = 8;

  /**
   * The number of structural features of the '<em>Developer</em>' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DEVELOPER_FEATURE_COUNT = 9;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.DistributionManagementImpl
   * <em>Distribution Management</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.DistributionManagementImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getDistributionManagement()
   * @generated
   */
  int DISTRIBUTION_MANAGEMENT = 12;

  /**
   * The feature id for the '<em><b>Repository</b></em>' containment reference. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DISTRIBUTION_MANAGEMENT__REPOSITORY = 0;

  /**
   * The feature id for the '<em><b>Snapshot Repository</b></em>' containment reference. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DISTRIBUTION_MANAGEMENT__SNAPSHOT_REPOSITORY = 1;

  /**
   * The feature id for the '<em><b>Site</b></em>' containment reference. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DISTRIBUTION_MANAGEMENT__SITE = 2;

  /**
   * The feature id for the '<em><b>Download Url</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DISTRIBUTION_MANAGEMENT__DOWNLOAD_URL = 3;

  /**
   * The feature id for the '<em><b>Relocation</b></em>' containment reference. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DISTRIBUTION_MANAGEMENT__RELOCATION = 4;

  /**
   * The feature id for the '<em><b>Status</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DISTRIBUTION_MANAGEMENT__STATUS = 5;

  /**
   * The number of structural features of the '<em>Distribution Management</em>' class. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DISTRIBUTION_MANAGEMENT_FEATURE_COUNT = 6;

  /**
   * The meta object id for the ' {@link org.eclipse.m2e.model.edit.pom.impl.DocumentRootImpl <em>Document Root</em>}'
   * class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.DocumentRootImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getDocumentRoot()
   * @generated
   */
  int DOCUMENT_ROOT = 13;

  /**
   * The feature id for the '<em><b>Mixed</b></em>' attribute list. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DOCUMENT_ROOT__MIXED = 0;

  /**
   * The feature id for the '<em><b>XMLNS Prefix Map</b></em>' map. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DOCUMENT_ROOT__XMLNS_PREFIX_MAP = 1;

  /**
   * The feature id for the '<em><b>XSI Schema Location</b></em>' map. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int DOCUMENT_ROOT__XSI_SCHEMA_LOCATION = 2;

  /**
   * The feature id for the '<em><b>Project</b></em>' containment reference. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   * @ordered
   */
  int DOCUMENT_ROOT__PROJECT = 3;

  /**
   * The number of structural features of the '<em>Document Root</em>' class. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   * @ordered
   */
  int DOCUMENT_ROOT_FEATURE_COUNT = 4;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.ExclusionImpl <em>Exclusion</em>}' class.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.ExclusionImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getExclusion()
   * @generated
   */
  int EXCLUSION = 14;

  /**
   * The feature id for the '<em><b>Artifact Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int EXCLUSION__ARTIFACT_ID = 0;

  /**
   * The feature id for the '<em><b>Group Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int EXCLUSION__GROUP_ID = 1;

  /**
   * The number of structural features of the '<em>Exclusion</em>' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int EXCLUSION_FEATURE_COUNT = 2;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.ExtensionImpl <em>Extension</em>}' class.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.ExtensionImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getExtension()
   * @generated
   */
  int EXTENSION = 15;

  /**
   * The feature id for the '<em><b>Group Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int EXTENSION__GROUP_ID = 0;

  /**
   * The feature id for the '<em><b>Artifact Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int EXTENSION__ARTIFACT_ID = 1;

  /**
   * The feature id for the '<em><b>Version</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int EXTENSION__VERSION = 2;

  /**
   * The number of structural features of the '<em>Extension</em>' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int EXTENSION_FEATURE_COUNT = 3;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.IssueManagementImpl
   * <em>Issue Management</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.IssueManagementImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getIssueManagement()
   * @generated
   */
  int ISSUE_MANAGEMENT = 16;

  /**
   * The feature id for the '<em><b>System</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int ISSUE_MANAGEMENT__SYSTEM = 0;

  /**
   * The feature id for the '<em><b>Url</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int ISSUE_MANAGEMENT__URL = 1;

  /**
   * The number of structural features of the '<em>Issue Management</em>' class. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int ISSUE_MANAGEMENT_FEATURE_COUNT = 2;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.LicenseImpl <em>License</em>}' class. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.LicenseImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getLicense()
   * @generated
   */
  int LICENSE = 17;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int LICENSE__NAME = 0;

  /**
   * The feature id for the '<em><b>Url</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int LICENSE__URL = 1;

  /**
   * The feature id for the '<em><b>Distribution</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int LICENSE__DISTRIBUTION = 2;

  /**
   * The feature id for the '<em><b>Comments</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int LICENSE__COMMENTS = 3;

  /**
   * The number of structural features of the '<em>License</em>' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int LICENSE_FEATURE_COUNT = 4;

  /**
   * The meta object id for the ' {@link org.eclipse.m2e.model.edit.pom.impl.MailingListImpl <em>Mailing List</em>}'
   * class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.MailingListImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getMailingList()
   * @generated
   */
  int MAILING_LIST = 18;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MAILING_LIST__NAME = 0;

  /**
   * The feature id for the '<em><b>Subscribe</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MAILING_LIST__SUBSCRIBE = 1;

  /**
   * The feature id for the '<em><b>Unsubscribe</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MAILING_LIST__UNSUBSCRIBE = 2;

  /**
   * The feature id for the '<em><b>Post</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MAILING_LIST__POST = 3;

  /**
   * The feature id for the '<em><b>Archive</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MAILING_LIST__ARCHIVE = 4;

  /**
   * The feature id for the '<em><b>Other Archives</b></em>' attribute list. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   * @ordered
   */
  int MAILING_LIST__OTHER_ARCHIVES = 5;

  /**
   * The number of structural features of the '<em>Mailing List</em>' class. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   * @ordered
   */
  int MAILING_LIST_FEATURE_COUNT = 6;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl <em>Model</em>}' class. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.ModelImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getModel()
   * @generated
   */
  int MODEL = 19;

  /**
   * The feature id for the '<em><b>Parent</b></em>' containment reference. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__PARENT = 0;

  /**
   * The feature id for the '<em><b>Model Version</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__MODEL_VERSION = 1;

  /**
   * The feature id for the '<em><b>Group Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__GROUP_ID = 2;

  /**
   * The feature id for the '<em><b>Artifact Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__ARTIFACT_ID = 3;

  /**
   * The feature id for the '<em><b>Packaging</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__PACKAGING = 4;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__NAME = 5;

  /**
   * The feature id for the '<em><b>Version</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__VERSION = 6;

  /**
   * The feature id for the '<em><b>Description</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__DESCRIPTION = 7;

  /**
   * The feature id for the '<em><b>Url</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__URL = 8;

  /**
   * The feature id for the '<em><b>Prerequisites</b></em>' containment reference. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__PREREQUISITES = 9;

  /**
   * The feature id for the '<em><b>Issue Management</b></em>' containment reference. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__ISSUE_MANAGEMENT = 10;

  /**
   * The feature id for the '<em><b>Ci Management</b></em>' containment reference. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__CI_MANAGEMENT = 11;

  /**
   * The feature id for the '<em><b>Inception Year</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__INCEPTION_YEAR = 12;

  /**
   * The feature id for the '<em><b>Mailing Lists</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__MAILING_LISTS = 13;

  /**
   * The feature id for the '<em><b>Developers</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__DEVELOPERS = 14;

  /**
   * The feature id for the '<em><b>Contributors</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__CONTRIBUTORS = 15;

  /**
   * The feature id for the '<em><b>Licenses</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__LICENSES = 16;

  /**
   * The feature id for the '<em><b>Scm</b></em>' containment reference. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__SCM = 17;

  /**
   * The feature id for the '<em><b>Organization</b></em>' containment reference. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__ORGANIZATION = 18;

  /**
   * The feature id for the '<em><b>Build</b></em>' containment reference. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__BUILD = 19;

  /**
   * The feature id for the '<em><b>Profiles</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__PROFILES = 20;

  /**
   * The feature id for the '<em><b>Repositories</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__REPOSITORIES = 21;

  /**
   * The feature id for the '<em><b>Plugin Repositories</b></em>' containment reference list. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__PLUGIN_REPOSITORIES = 22;

  /**
   * The feature id for the '<em><b>Dependencies</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__DEPENDENCIES = 23;

  /**
   * The feature id for the '<em><b>Reporting</b></em>' containment reference. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__REPORTING = 24;

  /**
   * The feature id for the '<em><b>Dependency Management</b></em>' containment reference. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__DEPENDENCY_MANAGEMENT = 25;

  /**
   * The feature id for the '<em><b>Distribution Management</b></em>' containment reference. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__DISTRIBUTION_MANAGEMENT = 26;

  /**
   * The feature id for the '<em><b>Properties</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__PROPERTIES = 27;

  /**
   * The feature id for the '<em><b>Modules</b></em>' attribute list. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL__MODULES = 28;

  /**
   * The number of structural features of the '<em>Model</em>' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int MODEL_FEATURE_COUNT = 29;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.NotifierImpl <em>Notifier</em>}' class. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.NotifierImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getNotifier()
   * @generated
   */
  int NOTIFIER = 20;

  /**
   * The feature id for the '<em><b>Type</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int NOTIFIER__TYPE = 0;

  /**
   * The feature id for the '<em><b>Send On Error</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int NOTIFIER__SEND_ON_ERROR = 1;

  /**
   * The feature id for the '<em><b>Send On Failure</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int NOTIFIER__SEND_ON_FAILURE = 2;

  /**
   * The feature id for the '<em><b>Send On Success</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int NOTIFIER__SEND_ON_SUCCESS = 3;

  /**
   * The feature id for the '<em><b>Send On Warning</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int NOTIFIER__SEND_ON_WARNING = 4;

  /**
   * The feature id for the '<em><b>Address</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int NOTIFIER__ADDRESS = 5;

  /**
   * The feature id for the '<em><b>Configuration</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int NOTIFIER__CONFIGURATION = 6;

  /**
   * The number of structural features of the '<em>Notifier</em>' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int NOTIFIER_FEATURE_COUNT = 7;

  /**
   * The meta object id for the ' {@link org.eclipse.m2e.model.edit.pom.impl.OrganizationImpl <em>Organization</em>}'
   * class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.OrganizationImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getOrganization()
   * @generated
   */
  int ORGANIZATION = 21;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int ORGANIZATION__NAME = 0;

  /**
   * The feature id for the '<em><b>Url</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int ORGANIZATION__URL = 1;

  /**
   * The number of structural features of the '<em>Organization</em>' class. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   * @ordered
   */
  int ORGANIZATION_FEATURE_COUNT = 2;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.ParentImpl <em>Parent</em>}' class. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.ParentImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getParent()
   * @generated
   */
  int PARENT = 22;

  /**
   * The feature id for the '<em><b>Artifact Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PARENT__ARTIFACT_ID = 0;

  /**
   * The feature id for the '<em><b>Group Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PARENT__GROUP_ID = 1;

  /**
   * The feature id for the '<em><b>Version</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PARENT__VERSION = 2;

  /**
   * The feature id for the '<em><b>Relative Path</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PARENT__RELATIVE_PATH = 3;

  /**
   * The number of structural features of the '<em>Parent</em>' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PARENT_FEATURE_COUNT = 4;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.PluginImpl <em>Plugin</em>}' class. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.PluginImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getPlugin()
   * @generated
   */
  int PLUGIN = 23;

  /**
   * The feature id for the '<em><b>Group Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PLUGIN__GROUP_ID = 0;

  /**
   * The feature id for the '<em><b>Artifact Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PLUGIN__ARTIFACT_ID = 1;

  /**
   * The feature id for the '<em><b>Version</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PLUGIN__VERSION = 2;

  /**
   * The feature id for the '<em><b>Extensions</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PLUGIN__EXTENSIONS = 3;

  /**
   * The feature id for the '<em><b>Executions</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PLUGIN__EXECUTIONS = 4;

  /**
   * The feature id for the '<em><b>Dependencies</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PLUGIN__DEPENDENCIES = 5;

  /**
   * The feature id for the '<em><b>Inherited</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PLUGIN__INHERITED = 6;

  /**
   * The feature id for the '<em><b>Configuration</b></em>' reference. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PLUGIN__CONFIGURATION = 7;

  /**
   * The number of structural features of the '<em>Plugin</em>' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PLUGIN_FEATURE_COUNT = 8;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.PluginExecutionImpl
   * <em>Plugin Execution</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.PluginExecutionImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getPluginExecution()
   * @generated
   */
  int PLUGIN_EXECUTION = 24;

  /**
   * The feature id for the '<em><b>Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PLUGIN_EXECUTION__ID = 0;

  /**
   * The feature id for the '<em><b>Phase</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PLUGIN_EXECUTION__PHASE = 1;

  /**
   * The feature id for the '<em><b>Inherited</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PLUGIN_EXECUTION__INHERITED = 2;

  /**
   * The feature id for the '<em><b>Goals</b></em>' attribute list. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PLUGIN_EXECUTION__GOALS = 3;

  /**
   * The feature id for the '<em><b>Configuration</b></em>' reference. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PLUGIN_EXECUTION__CONFIGURATION = 4;

  /**
   * The number of structural features of the '<em>Plugin Execution</em>' class. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PLUGIN_EXECUTION_FEATURE_COUNT = 5;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.PluginManagementImpl
   * <em>Plugin Management</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.PluginManagementImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getPluginManagement()
   * @generated
   */
  int PLUGIN_MANAGEMENT = 25;

  /**
   * The feature id for the '<em><b>Plugins</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PLUGIN_MANAGEMENT__PLUGINS = 0;

  /**
   * The number of structural features of the '<em>Plugin Management</em>' class. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PLUGIN_MANAGEMENT_FEATURE_COUNT = 1;

  /**
   * The meta object id for the ' {@link org.eclipse.m2e.model.edit.pom.impl.PrerequisitesImpl <em>Prerequisites</em>}'
   * class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.PrerequisitesImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getPrerequisites()
   * @generated
   */
  int PREREQUISITES = 26;

  /**
   * The feature id for the '<em><b>Maven</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PREREQUISITES__MAVEN = 0;

  /**
   * The number of structural features of the '<em>Prerequisites</em>' class. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   * @ordered
   */
  int PREREQUISITES_FEATURE_COUNT = 1;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.ProfileImpl <em>Profile</em>}' class. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.ProfileImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getProfile()
   * @generated
   */
  int PROFILE = 27;

  /**
   * The feature id for the '<em><b>Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PROFILE__ID = 0;

  /**
   * The feature id for the '<em><b>Activation</b></em>' containment reference. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PROFILE__ACTIVATION = 1;

  /**
   * The feature id for the '<em><b>Build</b></em>' containment reference. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PROFILE__BUILD = 2;

  /**
   * The feature id for the '<em><b>Repositories</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PROFILE__REPOSITORIES = 3;

  /**
   * The feature id for the '<em><b>Plugin Repositories</b></em>' containment reference list. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PROFILE__PLUGIN_REPOSITORIES = 4;

  /**
   * The feature id for the '<em><b>Dependencies</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PROFILE__DEPENDENCIES = 5;

  /**
   * The feature id for the '<em><b>Reports</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PROFILE__REPORTS = 6;

  /**
   * The feature id for the '<em><b>Dependency Management</b></em>' containment reference. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PROFILE__DEPENDENCY_MANAGEMENT = 7;

  /**
   * The feature id for the '<em><b>Distribution Management</b></em>' containment reference. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PROFILE__DISTRIBUTION_MANAGEMENT = 8;

  /**
   * The feature id for the '<em><b>Properties</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PROFILE__PROPERTIES = 9;

  /**
   * The feature id for the '<em><b>Modules</b></em>' attribute list. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PROFILE__MODULES = 10;

  /**
   * The feature id for the '<em><b>Reporting</b></em>' reference. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PROFILE__REPORTING = 11;

  /**
   * The number of structural features of the '<em>Profile</em>' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PROFILE_FEATURE_COUNT = 12;

  /**
   * The meta object id for the ' {@link org.eclipse.m2e.model.edit.pom.impl.RelocationImpl <em>Relocation</em>}' class.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.RelocationImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getRelocation()
   * @generated
   */
  int RELOCATION = 28;

  /**
   * The feature id for the '<em><b>Group Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int RELOCATION__GROUP_ID = 0;

  /**
   * The feature id for the '<em><b>Artifact Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int RELOCATION__ARTIFACT_ID = 1;

  /**
   * The feature id for the '<em><b>Version</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int RELOCATION__VERSION = 2;

  /**
   * The feature id for the '<em><b>Message</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int RELOCATION__MESSAGE = 3;

  /**
   * The number of structural features of the '<em>Relocation</em>' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int RELOCATION_FEATURE_COUNT = 4;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.ReportingImpl <em>Reporting</em>}' class.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.ReportingImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getReporting()
   * @generated
   */
  int REPORTING = 29;

  /**
   * The feature id for the '<em><b>Exclude Defaults</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPORTING__EXCLUDE_DEFAULTS = 0;

  /**
   * The feature id for the '<em><b>Output Directory</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPORTING__OUTPUT_DIRECTORY = 1;

  /**
   * The feature id for the '<em><b>Plugins</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPORTING__PLUGINS = 2;

  /**
   * The number of structural features of the '<em>Reporting</em>' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPORTING_FEATURE_COUNT = 3;

  /**
   * The meta object id for the ' {@link org.eclipse.m2e.model.edit.pom.impl.ReportPluginImpl <em>Report Plugin</em>}'
   * class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.ReportPluginImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getReportPlugin()
   * @generated
   */
  int REPORT_PLUGIN = 30;

  /**
   * The feature id for the '<em><b>Group Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPORT_PLUGIN__GROUP_ID = 0;

  /**
   * The feature id for the '<em><b>Artifact Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPORT_PLUGIN__ARTIFACT_ID = 1;

  /**
   * The feature id for the '<em><b>Version</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPORT_PLUGIN__VERSION = 2;

  /**
   * The feature id for the '<em><b>Inherited</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPORT_PLUGIN__INHERITED = 3;

  /**
   * The feature id for the '<em><b>Report Sets</b></em>' containment reference list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPORT_PLUGIN__REPORT_SETS = 4;

  /**
   * The feature id for the '<em><b>Configuration</b></em>' reference. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPORT_PLUGIN__CONFIGURATION = 5;

  /**
   * The number of structural features of the '<em>Report Plugin</em>' class. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   * @ordered
   */
  int REPORT_PLUGIN_FEATURE_COUNT = 6;

  /**
   * The meta object id for the ' {@link org.eclipse.m2e.model.edit.pom.impl.ReportSetImpl <em>Report Set</em>}' class.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.ReportSetImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getReportSet()
   * @generated
   */
  int REPORT_SET = 31;

  /**
   * The feature id for the '<em><b>Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPORT_SET__ID = 0;

  /**
   * The feature id for the '<em><b>Inherited</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPORT_SET__INHERITED = 1;

  /**
   * The feature id for the '<em><b>Reports</b></em>' attribute list. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPORT_SET__REPORTS = 2;

  /**
   * The feature id for the '<em><b>Configuration</b></em>' reference. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPORT_SET__CONFIGURATION = 3;

  /**
   * The number of structural features of the '<em>Report Set</em>' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPORT_SET_FEATURE_COUNT = 4;

  /**
   * The meta object id for the ' {@link org.eclipse.m2e.model.edit.pom.impl.RepositoryImpl <em>Repository</em>}' class.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.RepositoryImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getRepository()
   * @generated
   */
  int REPOSITORY = 32;

  /**
   * The feature id for the '<em><b>Releases</b></em>' containment reference. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   * @ordered
   */
  int REPOSITORY__RELEASES = 0;

  /**
   * The feature id for the '<em><b>Snapshots</b></em>' containment reference. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   * @ordered
   */
  int REPOSITORY__SNAPSHOTS = 1;

  /**
   * The feature id for the '<em><b>Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPOSITORY__ID = 2;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPOSITORY__NAME = 3;

  /**
   * The feature id for the '<em><b>Url</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPOSITORY__URL = 4;

  /**
   * The feature id for the '<em><b>Layout</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPOSITORY__LAYOUT = 5;

  /**
   * The number of structural features of the '<em>Repository</em>' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPOSITORY_FEATURE_COUNT = 6;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.RepositoryPolicyImpl
   * <em>Repository Policy</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.RepositoryPolicyImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getRepositoryPolicy()
   * @generated
   */
  int REPOSITORY_POLICY = 33;

  /**
   * The feature id for the '<em><b>Enabled</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPOSITORY_POLICY__ENABLED = 0;

  /**
   * The feature id for the '<em><b>Update Policy</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPOSITORY_POLICY__UPDATE_POLICY = 1;

  /**
   * The feature id for the '<em><b>Checksum Policy</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPOSITORY_POLICY__CHECKSUM_POLICY = 2;

  /**
   * The number of structural features of the '<em>Repository Policy</em>' class. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int REPOSITORY_POLICY_FEATURE_COUNT = 3;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.ResourceImpl <em>Resource</em>}' class. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.ResourceImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getResource()
   * @generated
   */
  int RESOURCE = 34;

  /**
   * The feature id for the '<em><b>Target Path</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int RESOURCE__TARGET_PATH = 0;

  /**
   * The feature id for the '<em><b>Filtering</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int RESOURCE__FILTERING = 1;

  /**
   * The feature id for the '<em><b>Directory</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int RESOURCE__DIRECTORY = 2;

  /**
   * The feature id for the '<em><b>Includes</b></em>' attribute list. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int RESOURCE__INCLUDES = 3;

  /**
   * The feature id for the '<em><b>Excludes</b></em>' attribute list. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int RESOURCE__EXCLUDES = 4;

  /**
   * The number of structural features of the '<em>Resource</em>' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int RESOURCE_FEATURE_COUNT = 5;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.ScmImpl <em>Scm</em>}' class. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.ScmImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getScm()
   * @generated
   */
  int SCM = 35;

  /**
   * The feature id for the '<em><b>Connection</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int SCM__CONNECTION = 0;

  /**
   * The feature id for the '<em><b>Developer Connection</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   * @ordered
   */
  int SCM__DEVELOPER_CONNECTION = 1;

  /**
   * The feature id for the '<em><b>Tag</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int SCM__TAG = 2;

  /**
   * The feature id for the '<em><b>Url</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int SCM__URL = 3;

  /**
   * The number of structural features of the '<em>Scm</em>' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int SCM_FEATURE_COUNT = 4;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.SiteImpl <em>Site</em>}' class. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.SiteImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getSite()
   * @generated
   */
  int SITE = 36;

  /**
   * The feature id for the '<em><b>Id</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int SITE__ID = 0;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int SITE__NAME = 1;

  /**
   * The feature id for the '<em><b>Url</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int SITE__URL = 2;

  /**
   * The number of structural features of the '<em>Site</em>' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int SITE_FEATURE_COUNT = 3;

  /**
   * The meta object id for the '{@link org.eclipse.m2e.model.edit.pom.impl.PropertyElementImpl
   * <em>Property Element</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.PropertyElementImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getPropertyElement()
   * @generated
   */
  int PROPERTY_ELEMENT = 37;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PROPERTY_ELEMENT__NAME = 0;

  /**
   * The feature id for the '<em><b>Value</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PROPERTY_ELEMENT__VALUE = 1;

  /**
   * The number of structural features of the '<em>Property Element</em>' class. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  int PROPERTY_ELEMENT_FEATURE_COUNT = 2;

  /**
   * The meta object id for the ' {@link org.eclipse.m2e.model.edit.pom.impl.ConfigurationImpl <em>Configuration</em>}'
   * class. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see org.eclipse.m2e.model.edit.pom.impl.ConfigurationImpl
   * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getConfiguration()
   * @generated
   */
  int CONFIGURATION = 38;

  /**
   * The number of structural features of the '<em>Configuration</em>' class. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   * @ordered
   */
  int CONFIGURATION_FEATURE_COUNT = 0;

  /**
   * Returns the meta object for class '{@link org.eclipse.m2e.model.edit.pom.Activation <em>Activation</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Activation</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Activation
   * @generated
   */
  EClass getActivation();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.Activation#getActiveByDefault
   * <em>Active By Default</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Active By Default</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Activation#getActiveByDefault()
   * @see #getActivation()
   * @generated
   */
  EAttribute getActivation_ActiveByDefault();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Activation#getJdk <em>Jdk</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Jdk</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Activation#getJdk()
   * @see #getActivation()
   * @generated
   */
  EAttribute getActivation_Jdk();

  /**
   * Returns the meta object for the containment reference ' {@link org.eclipse.m2e.model.edit.pom.Activation#getOs
   * <em>Os</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Os</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Activation#getOs()
   * @see #getActivation()
   * @generated
   */
  EReference getActivation_Os();

  /**
   * Returns the meta object for the containment reference '
   * {@link org.eclipse.m2e.model.edit.pom.Activation#getProperty <em>Property</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Property</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Activation#getProperty()
   * @see #getActivation()
   * @generated
   */
  EReference getActivation_Property();

  /**
   * Returns the meta object for the containment reference '{@link org.eclipse.m2e.model.edit.pom.Activation#getFile
   * <em>File</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>File</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Activation#getFile()
   * @see #getActivation()
   * @generated
   */
  EReference getActivation_File();

  /**
   * Returns the meta object for class '{@link org.eclipse.m2e.model.edit.pom.ActivationFile <em>Activation File</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Activation File</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ActivationFile
   * @generated
   */
  EClass getActivationFile();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.ActivationFile#getMissing
   * <em>Missing</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Missing</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ActivationFile#getMissing()
   * @see #getActivationFile()
   * @generated
   */
  EAttribute getActivationFile_Missing();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.ActivationFile#getExists
   * <em>Exists</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Exists</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ActivationFile#getExists()
   * @see #getActivationFile()
   * @generated
   */
  EAttribute getActivationFile_Exists();

  /**
   * Returns the meta object for class '{@link org.eclipse.m2e.model.edit.pom.ActivationOS <em>Activation OS</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Activation OS</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ActivationOS
   * @generated
   */
  EClass getActivationOS();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.ActivationOS#getName
   * <em>Name</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ActivationOS#getName()
   * @see #getActivationOS()
   * @generated
   */
  EAttribute getActivationOS_Name();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.ActivationOS#getFamily
   * <em>Family</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Family</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ActivationOS#getFamily()
   * @see #getActivationOS()
   * @generated
   */
  EAttribute getActivationOS_Family();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.ActivationOS#getArch
   * <em>Arch</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Arch</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ActivationOS#getArch()
   * @see #getActivationOS()
   * @generated
   */
  EAttribute getActivationOS_Arch();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.ActivationOS#getVersion
   * <em>Version</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Version</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ActivationOS#getVersion()
   * @see #getActivationOS()
   * @generated
   */
  EAttribute getActivationOS_Version();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.ActivationProperty
   * <em>Activation Property</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Activation Property</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ActivationProperty
   * @generated
   */
  EClass getActivationProperty();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.ActivationProperty#getName
   * <em>Name</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ActivationProperty#getName()
   * @see #getActivationProperty()
   * @generated
   */
  EAttribute getActivationProperty_Name();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.ActivationProperty#getValue
   * <em>Value</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Value</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ActivationProperty#getValue()
   * @see #getActivationProperty()
   * @generated
   */
  EAttribute getActivationProperty_Value();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.Build <em>Build</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Build</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Build
   * @generated
   */
  EClass getBuild();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.Build#getSourceDirectory
   * <em>Source Directory</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Source Directory</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Build#getSourceDirectory()
   * @see #getBuild()
   * @generated
   */
  EAttribute getBuild_SourceDirectory();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Build#getScriptSourceDirectory
   * <em>Script Source Directory</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Script Source Directory</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Build#getScriptSourceDirectory()
   * @see #getBuild()
   * @generated
   */
  EAttribute getBuild_ScriptSourceDirectory();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Build#getTestSourceDirectory
   * <em>Test Source Directory</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Test Source Directory</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Build#getTestSourceDirectory()
   * @see #getBuild()
   * @generated
   */
  EAttribute getBuild_TestSourceDirectory();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.Build#getOutputDirectory
   * <em>Output Directory</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Output Directory</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Build#getOutputDirectory()
   * @see #getBuild()
   * @generated
   */
  EAttribute getBuild_OutputDirectory();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Build#getTestOutputDirectory
   * <em>Test Output Directory</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Test Output Directory</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Build#getTestOutputDirectory()
   * @see #getBuild()
   * @generated
   */
  EAttribute getBuild_TestOutputDirectory();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Build#getExtensions <em>Extensions</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Extensions</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Build#getExtensions()
   * @see #getBuild()
   * @generated
   */
  EReference getBuild_Extensions();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.BuildBase <em>Build Base</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Build Base</em>'.
   * @see org.eclipse.m2e.model.edit.pom.BuildBase
   * @generated
   */
  EClass getBuildBase();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.BuildBase#getDefaultGoal
   * <em>Default Goal</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Default Goal</em>'.
   * @see org.eclipse.m2e.model.edit.pom.BuildBase#getDefaultGoal()
   * @see #getBuildBase()
   * @generated
   */
  EAttribute getBuildBase_DefaultGoal();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.BuildBase#getResources <em>Resources</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Resources</em>'.
   * @see org.eclipse.m2e.model.edit.pom.BuildBase#getResources()
   * @see #getBuildBase()
   * @generated
   */
  EReference getBuildBase_Resources();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.BuildBase#getTestResources <em>Test Resources</em>}'. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Test Resources</em>'.
   * @see org.eclipse.m2e.model.edit.pom.BuildBase#getTestResources()
   * @see #getBuildBase()
   * @generated
   */
  EReference getBuildBase_TestResources();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.BuildBase#getDirectory
   * <em>Directory</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Directory</em>'.
   * @see org.eclipse.m2e.model.edit.pom.BuildBase#getDirectory()
   * @see #getBuildBase()
   * @generated
   */
  EAttribute getBuildBase_Directory();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.BuildBase#getFinalName
   * <em>Final Name</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Final Name</em>'.
   * @see org.eclipse.m2e.model.edit.pom.BuildBase#getFinalName()
   * @see #getBuildBase()
   * @generated
   */
  EAttribute getBuildBase_FinalName();

  /**
   * Returns the meta object for the containment reference '
   * {@link org.eclipse.m2e.model.edit.pom.BuildBase#getPluginManagement <em>Plugin Management</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference ' <em>Plugin Management</em>'.
   * @see org.eclipse.m2e.model.edit.pom.BuildBase#getPluginManagement()
   * @see #getBuildBase()
   * @generated
   */
  EReference getBuildBase_PluginManagement();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.BuildBase#getPlugins <em>Plugins</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Plugins</em>'.
   * @see org.eclipse.m2e.model.edit.pom.BuildBase#getPlugins()
   * @see #getBuildBase()
   * @generated
   */
  EReference getBuildBase_Plugins();

  /**
   * Returns the meta object for the attribute list '{@link org.eclipse.m2e.model.edit.pom.BuildBase#getFilters
   * <em>Filters</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute list '<em>Filters</em>'.
   * @see org.eclipse.m2e.model.edit.pom.BuildBase#getFilters()
   * @see #getBuildBase()
   * @generated
   */
  EAttribute getBuildBase_Filters();

  /**
   * Returns the meta object for class '{@link org.eclipse.m2e.model.edit.pom.CiManagement <em>Ci Management</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Ci Management</em>'.
   * @see org.eclipse.m2e.model.edit.pom.CiManagement
   * @generated
   */
  EClass getCiManagement();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.CiManagement#getSystem
   * <em>System</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>System</em>'.
   * @see org.eclipse.m2e.model.edit.pom.CiManagement#getSystem()
   * @see #getCiManagement()
   * @generated
   */
  EAttribute getCiManagement_System();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.CiManagement#getUrl <em>Url</em>}
   * '. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Url</em>'.
   * @see org.eclipse.m2e.model.edit.pom.CiManagement#getUrl()
   * @see #getCiManagement()
   * @generated
   */
  EAttribute getCiManagement_Url();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.CiManagement#getNotifiers <em>Notifiers</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Notifiers</em>'.
   * @see org.eclipse.m2e.model.edit.pom.CiManagement#getNotifiers()
   * @see #getCiManagement()
   * @generated
   */
  EReference getCiManagement_Notifiers();

  /**
   * Returns the meta object for class '{@link org.eclipse.m2e.model.edit.pom.Contributor <em>Contributor</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Contributor</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Contributor
   * @generated
   */
  EClass getContributor();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Contributor#getName <em>Name</em>}
   * '. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Contributor#getName()
   * @see #getContributor()
   * @generated
   */
  EAttribute getContributor_Name();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Contributor#getEmail
   * <em>Email</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Email</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Contributor#getEmail()
   * @see #getContributor()
   * @generated
   */
  EAttribute getContributor_Email();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Contributor#getUrl <em>Url</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Url</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Contributor#getUrl()
   * @see #getContributor()
   * @generated
   */
  EAttribute getContributor_Url();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Contributor#getOrganization
   * <em>Organization</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Organization</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Contributor#getOrganization()
   * @see #getContributor()
   * @generated
   */
  EAttribute getContributor_Organization();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.Contributor#getOrganizationUrl
   * <em>Organization Url</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Organization Url</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Contributor#getOrganizationUrl()
   * @see #getContributor()
   * @generated
   */
  EAttribute getContributor_OrganizationUrl();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Contributor#getTimezone
   * <em>Timezone</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Timezone</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Contributor#getTimezone()
   * @see #getContributor()
   * @generated
   */
  EAttribute getContributor_Timezone();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Contributor#getProperties <em>Properties</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Properties</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Contributor#getProperties()
   * @see #getContributor()
   * @generated
   */
  EReference getContributor_Properties();

  /**
   * Returns the meta object for the attribute list '{@link org.eclipse.m2e.model.edit.pom.Contributor#getRoles
   * <em>Roles</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute list '<em>Roles</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Contributor#getRoles()
   * @see #getContributor()
   * @generated
   */
  EAttribute getContributor_Roles();

  /**
   * Returns the meta object for class '{@link org.eclipse.m2e.model.edit.pom.Dependency <em>Dependency</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Dependency</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Dependency
   * @generated
   */
  EClass getDependency();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Dependency#getGroupId
   * <em>Group Id</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Group Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Dependency#getGroupId()
   * @see #getDependency()
   * @generated
   */
  EAttribute getDependency_GroupId();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Dependency#getArtifactId
   * <em>Artifact Id</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Artifact Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Dependency#getArtifactId()
   * @see #getDependency()
   * @generated
   */
  EAttribute getDependency_ArtifactId();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Dependency#getVersion
   * <em>Version</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Version</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Dependency#getVersion()
   * @see #getDependency()
   * @generated
   */
  EAttribute getDependency_Version();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Dependency#getType <em>Type</em>}
   * '. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Type</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Dependency#getType()
   * @see #getDependency()
   * @generated
   */
  EAttribute getDependency_Type();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Dependency#getClassifier
   * <em>Classifier</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Classifier</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Dependency#getClassifier()
   * @see #getDependency()
   * @generated
   */
  EAttribute getDependency_Classifier();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Dependency#getScope
   * <em>Scope</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Scope</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Dependency#getScope()
   * @see #getDependency()
   * @generated
   */
  EAttribute getDependency_Scope();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Dependency#getSystemPath
   * <em>System Path</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>System Path</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Dependency#getSystemPath()
   * @see #getDependency()
   * @generated
   */
  EAttribute getDependency_SystemPath();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Dependency#getExclusions <em>Exclusions</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Exclusions</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Dependency#getExclusions()
   * @see #getDependency()
   * @generated
   */
  EReference getDependency_Exclusions();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Dependency#getOptional
   * <em>Optional</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Optional</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Dependency#getOptional()
   * @see #getDependency()
   * @generated
   */
  EAttribute getDependency_Optional();

  /**
   * Returns the meta object for class '{@link org.eclipse.m2e.model.edit.pom.DependencyManagement
   * <em>Dependency Management</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Dependency Management</em>'.
   * @see org.eclipse.m2e.model.edit.pom.DependencyManagement
   * @generated
   */
  EClass getDependencyManagement();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.DependencyManagement#getDependencies <em>Dependencies</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Dependencies</em>'.
   * @see org.eclipse.m2e.model.edit.pom.DependencyManagement#getDependencies()
   * @see #getDependencyManagement()
   * @generated
   */
  EReference getDependencyManagement_Dependencies();

  /**
   * Returns the meta object for class '{@link org.eclipse.m2e.model.edit.pom.DeploymentRepository
   * <em>Deployment Repository</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Deployment Repository</em>'.
   * @see org.eclipse.m2e.model.edit.pom.DeploymentRepository
   * @generated
   */
  EClass getDeploymentRepository();

  /**
   * Returns the meta object for the attribute '
   * {@link org.eclipse.m2e.model.edit.pom.DeploymentRepository#getUniqueVersion <em>Unique Version</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Unique Version</em>'.
   * @see org.eclipse.m2e.model.edit.pom.DeploymentRepository#getUniqueVersion()
   * @see #getDeploymentRepository()
   * @generated
   */
  EAttribute getDeploymentRepository_UniqueVersion();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.DeploymentRepository#getId
   * <em>Id</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.DeploymentRepository#getId()
   * @see #getDeploymentRepository()
   * @generated
   */
  EAttribute getDeploymentRepository_Id();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.DeploymentRepository#getName
   * <em>Name</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.eclipse.m2e.model.edit.pom.DeploymentRepository#getName()
   * @see #getDeploymentRepository()
   * @generated
   */
  EAttribute getDeploymentRepository_Name();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.DeploymentRepository#getUrl
   * <em>Url</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Url</em>'.
   * @see org.eclipse.m2e.model.edit.pom.DeploymentRepository#getUrl()
   * @see #getDeploymentRepository()
   * @generated
   */
  EAttribute getDeploymentRepository_Url();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.DeploymentRepository#getLayout
   * <em>Layout</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Layout</em>'.
   * @see org.eclipse.m2e.model.edit.pom.DeploymentRepository#getLayout()
   * @see #getDeploymentRepository()
   * @generated
   */
  EAttribute getDeploymentRepository_Layout();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.Developer <em>Developer</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Developer</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Developer
   * @generated
   */
  EClass getDeveloper();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.Developer#getId <em>Id</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Developer#getId()
   * @see #getDeveloper()
   * @generated
   */
  EAttribute getDeveloper_Id();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Developer#getName <em>Name</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Developer#getName()
   * @see #getDeveloper()
   * @generated
   */
  EAttribute getDeveloper_Name();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Developer#getEmail <em>Email</em>}
   * '. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Email</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Developer#getEmail()
   * @see #getDeveloper()
   * @generated
   */
  EAttribute getDeveloper_Email();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.Developer#getUrl <em>Url</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Url</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Developer#getUrl()
   * @see #getDeveloper()
   * @generated
   */
  EAttribute getDeveloper_Url();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Developer#getOrganization
   * <em>Organization</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Organization</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Developer#getOrganization()
   * @see #getDeveloper()
   * @generated
   */
  EAttribute getDeveloper_Organization();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.Developer#getOrganizationUrl
   * <em>Organization Url</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Organization Url</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Developer#getOrganizationUrl()
   * @see #getDeveloper()
   * @generated
   */
  EAttribute getDeveloper_OrganizationUrl();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Developer#getTimezone
   * <em>Timezone</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Timezone</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Developer#getTimezone()
   * @see #getDeveloper()
   * @generated
   */
  EAttribute getDeveloper_Timezone();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Developer#getProperties <em>Properties</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Properties</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Developer#getProperties()
   * @see #getDeveloper()
   * @generated
   */
  EReference getDeveloper_Properties();

  /**
   * Returns the meta object for the attribute list '{@link org.eclipse.m2e.model.edit.pom.Developer#getRoles
   * <em>Roles</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute list '<em>Roles</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Developer#getRoles()
   * @see #getDeveloper()
   * @generated
   */
  EAttribute getDeveloper_Roles();

  /**
   * Returns the meta object for class '{@link org.eclipse.m2e.model.edit.pom.DistributionManagement
   * <em>Distribution Management</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Distribution Management</em>'.
   * @see org.eclipse.m2e.model.edit.pom.DistributionManagement
   * @generated
   */
  EClass getDistributionManagement();

  /**
   * Returns the meta object for the containment reference '
   * {@link org.eclipse.m2e.model.edit.pom.DistributionManagement#getRepository <em>Repository</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Repository</em>'.
   * @see org.eclipse.m2e.model.edit.pom.DistributionManagement#getRepository()
   * @see #getDistributionManagement()
   * @generated
   */
  EReference getDistributionManagement_Repository();

  /**
   * Returns the meta object for the containment reference '
   * {@link org.eclipse.m2e.model.edit.pom.DistributionManagement#getSnapshotRepository <em>Snapshot Repository</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference ' <em>Snapshot Repository</em>'.
   * @see org.eclipse.m2e.model.edit.pom.DistributionManagement#getSnapshotRepository()
   * @see #getDistributionManagement()
   * @generated
   */
  EReference getDistributionManagement_SnapshotRepository();

  /**
   * Returns the meta object for the containment reference '
   * {@link org.eclipse.m2e.model.edit.pom.DistributionManagement#getSite <em>Site</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Site</em>'.
   * @see org.eclipse.m2e.model.edit.pom.DistributionManagement#getSite()
   * @see #getDistributionManagement()
   * @generated
   */
  EReference getDistributionManagement_Site();

  /**
   * Returns the meta object for the attribute '
   * {@link org.eclipse.m2e.model.edit.pom.DistributionManagement#getDownloadUrl <em>Download Url</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Download Url</em>'.
   * @see org.eclipse.m2e.model.edit.pom.DistributionManagement#getDownloadUrl()
   * @see #getDistributionManagement()
   * @generated
   */
  EAttribute getDistributionManagement_DownloadUrl();

  /**
   * Returns the meta object for the containment reference '
   * {@link org.eclipse.m2e.model.edit.pom.DistributionManagement#getRelocation <em>Relocation</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Relocation</em>'.
   * @see org.eclipse.m2e.model.edit.pom.DistributionManagement#getRelocation()
   * @see #getDistributionManagement()
   * @generated
   */
  EReference getDistributionManagement_Relocation();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.DistributionManagement#getStatus
   * <em>Status</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Status</em>'.
   * @see org.eclipse.m2e.model.edit.pom.DistributionManagement#getStatus()
   * @see #getDistributionManagement()
   * @generated
   */
  EAttribute getDistributionManagement_Status();

  /**
   * Returns the meta object for class '{@link org.eclipse.m2e.model.edit.pom.DocumentRoot <em>Document Root</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Document Root</em>'.
   * @see org.eclipse.m2e.model.edit.pom.DocumentRoot
   * @generated
   */
  EClass getDocumentRoot();

  /**
   * Returns the meta object for the attribute list '{@link org.eclipse.m2e.model.edit.pom.DocumentRoot#getMixed
   * <em>Mixed</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute list '<em>Mixed</em>'.
   * @see org.eclipse.m2e.model.edit.pom.DocumentRoot#getMixed()
   * @see #getDocumentRoot()
   * @generated
   */
  EAttribute getDocumentRoot_Mixed();

  /**
   * Returns the meta object for the map ' {@link org.eclipse.m2e.model.edit.pom.DocumentRoot#getXMLNSPrefixMap
   * <em>XMLNS Prefix Map</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the map '<em>XMLNS Prefix Map</em>'.
   * @see org.eclipse.m2e.model.edit.pom.DocumentRoot#getXMLNSPrefixMap()
   * @see #getDocumentRoot()
   * @generated
   */
  EReference getDocumentRoot_XMLNSPrefixMap();

  /**
   * Returns the meta object for the map ' {@link org.eclipse.m2e.model.edit.pom.DocumentRoot#getXSISchemaLocation
   * <em>XSI Schema Location</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the map '<em>XSI Schema Location</em>'.
   * @see org.eclipse.m2e.model.edit.pom.DocumentRoot#getXSISchemaLocation()
   * @see #getDocumentRoot()
   * @generated
   */
  EReference getDocumentRoot_XSISchemaLocation();

  /**
   * Returns the meta object for the containment reference '
   * {@link org.eclipse.m2e.model.edit.pom.DocumentRoot#getProject <em>Project</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Project</em>'.
   * @see org.eclipse.m2e.model.edit.pom.DocumentRoot#getProject()
   * @see #getDocumentRoot()
   * @generated
   */
  EReference getDocumentRoot_Project();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.Exclusion <em>Exclusion</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Exclusion</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Exclusion
   * @generated
   */
  EClass getExclusion();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Exclusion#getArtifactId
   * <em>Artifact Id</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Artifact Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Exclusion#getArtifactId()
   * @see #getExclusion()
   * @generated
   */
  EAttribute getExclusion_ArtifactId();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Exclusion#getGroupId
   * <em>Group Id</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Group Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Exclusion#getGroupId()
   * @see #getExclusion()
   * @generated
   */
  EAttribute getExclusion_GroupId();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.Extension <em>Extension</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Extension</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Extension
   * @generated
   */
  EClass getExtension();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Extension#getGroupId
   * <em>Group Id</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Group Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Extension#getGroupId()
   * @see #getExtension()
   * @generated
   */
  EAttribute getExtension_GroupId();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Extension#getArtifactId
   * <em>Artifact Id</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Artifact Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Extension#getArtifactId()
   * @see #getExtension()
   * @generated
   */
  EAttribute getExtension_ArtifactId();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Extension#getVersion
   * <em>Version</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Version</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Extension#getVersion()
   * @see #getExtension()
   * @generated
   */
  EAttribute getExtension_Version();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.IssueManagement
   * <em>Issue Management</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Issue Management</em>'.
   * @see org.eclipse.m2e.model.edit.pom.IssueManagement
   * @generated
   */
  EClass getIssueManagement();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.IssueManagement#getSystem
   * <em>System</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>System</em>'.
   * @see org.eclipse.m2e.model.edit.pom.IssueManagement#getSystem()
   * @see #getIssueManagement()
   * @generated
   */
  EAttribute getIssueManagement_System();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.IssueManagement#getUrl
   * <em>Url</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Url</em>'.
   * @see org.eclipse.m2e.model.edit.pom.IssueManagement#getUrl()
   * @see #getIssueManagement()
   * @generated
   */
  EAttribute getIssueManagement_Url();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.License <em>License</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>License</em>'.
   * @see org.eclipse.m2e.model.edit.pom.License
   * @generated
   */
  EClass getLicense();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.License#getName <em>Name</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.eclipse.m2e.model.edit.pom.License#getName()
   * @see #getLicense()
   * @generated
   */
  EAttribute getLicense_Name();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.License#getUrl <em>Url</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Url</em>'.
   * @see org.eclipse.m2e.model.edit.pom.License#getUrl()
   * @see #getLicense()
   * @generated
   */
  EAttribute getLicense_Url();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.License#getDistribution
   * <em>Distribution</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Distribution</em>'.
   * @see org.eclipse.m2e.model.edit.pom.License#getDistribution()
   * @see #getLicense()
   * @generated
   */
  EAttribute getLicense_Distribution();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.License#getComments
   * <em>Comments</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Comments</em>'.
   * @see org.eclipse.m2e.model.edit.pom.License#getComments()
   * @see #getLicense()
   * @generated
   */
  EAttribute getLicense_Comments();

  /**
   * Returns the meta object for class '{@link org.eclipse.m2e.model.edit.pom.MailingList <em>Mailing List</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Mailing List</em>'.
   * @see org.eclipse.m2e.model.edit.pom.MailingList
   * @generated
   */
  EClass getMailingList();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.MailingList#getName <em>Name</em>}
   * '. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.eclipse.m2e.model.edit.pom.MailingList#getName()
   * @see #getMailingList()
   * @generated
   */
  EAttribute getMailingList_Name();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.MailingList#getSubscribe
   * <em>Subscribe</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Subscribe</em>'.
   * @see org.eclipse.m2e.model.edit.pom.MailingList#getSubscribe()
   * @see #getMailingList()
   * @generated
   */
  EAttribute getMailingList_Subscribe();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.MailingList#getUnsubscribe
   * <em>Unsubscribe</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Unsubscribe</em>'.
   * @see org.eclipse.m2e.model.edit.pom.MailingList#getUnsubscribe()
   * @see #getMailingList()
   * @generated
   */
  EAttribute getMailingList_Unsubscribe();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.MailingList#getPost <em>Post</em>}
   * '. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Post</em>'.
   * @see org.eclipse.m2e.model.edit.pom.MailingList#getPost()
   * @see #getMailingList()
   * @generated
   */
  EAttribute getMailingList_Post();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.MailingList#getArchive
   * <em>Archive</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Archive</em>'.
   * @see org.eclipse.m2e.model.edit.pom.MailingList#getArchive()
   * @see #getMailingList()
   * @generated
   */
  EAttribute getMailingList_Archive();

  /**
   * Returns the meta object for the attribute list '{@link org.eclipse.m2e.model.edit.pom.MailingList#getOtherArchives
   * <em>Other Archives</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute list '<em>Other Archives</em>'.
   * @see org.eclipse.m2e.model.edit.pom.MailingList#getOtherArchives()
   * @see #getMailingList()
   * @generated
   */
  EAttribute getMailingList_OtherArchives();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.Model <em>Model</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Model</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model
   * @generated
   */
  EClass getModel();

  /**
   * Returns the meta object for the containment reference '{@link org.eclipse.m2e.model.edit.pom.Model#getParent
   * <em>Parent</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Parent</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getParent()
   * @see #getModel()
   * @generated
   */
  EReference getModel_Parent();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Model#getModelVersion
   * <em>Model Version</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Model Version</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getModelVersion()
   * @see #getModel()
   * @generated
   */
  EAttribute getModel_ModelVersion();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Model#getGroupId
   * <em>Group Id</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Group Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getGroupId()
   * @see #getModel()
   * @generated
   */
  EAttribute getModel_GroupId();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Model#getArtifactId
   * <em>Artifact Id</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Artifact Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getArtifactId()
   * @see #getModel()
   * @generated
   */
  EAttribute getModel_ArtifactId();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Model#getPackaging
   * <em>Packaging</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Packaging</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getPackaging()
   * @see #getModel()
   * @generated
   */
  EAttribute getModel_Packaging();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.Model#getName <em>Name</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getName()
   * @see #getModel()
   * @generated
   */
  EAttribute getModel_Name();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Model#getVersion <em>Version</em>}
   * '. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Version</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getVersion()
   * @see #getModel()
   * @generated
   */
  EAttribute getModel_Version();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Model#getDescription
   * <em>Description</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Description</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getDescription()
   * @see #getModel()
   * @generated
   */
  EAttribute getModel_Description();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.Model#getUrl <em>Url</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Url</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getUrl()
   * @see #getModel()
   * @generated
   */
  EAttribute getModel_Url();

  /**
   * Returns the meta object for the containment reference '
   * {@link org.eclipse.m2e.model.edit.pom.Model#getPrerequisites <em>Prerequisites</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Prerequisites</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getPrerequisites()
   * @see #getModel()
   * @generated
   */
  EReference getModel_Prerequisites();

  /**
   * Returns the meta object for the containment reference '
   * {@link org.eclipse.m2e.model.edit.pom.Model#getIssueManagement <em>Issue Management</em>}'. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference ' <em>Issue Management</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getIssueManagement()
   * @see #getModel()
   * @generated
   */
  EReference getModel_IssueManagement();

  /**
   * Returns the meta object for the containment reference '{@link org.eclipse.m2e.model.edit.pom.Model#getCiManagement
   * <em>Ci Management</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Ci Management</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getCiManagement()
   * @see #getModel()
   * @generated
   */
  EReference getModel_CiManagement();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Model#getInceptionYear
   * <em>Inception Year</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Inception Year</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getInceptionYear()
   * @see #getModel()
   * @generated
   */
  EAttribute getModel_InceptionYear();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Model#getMailingLists <em>Mailing Lists</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Mailing Lists</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getMailingLists()
   * @see #getModel()
   * @generated
   */
  EReference getModel_MailingLists();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Model#getDevelopers <em>Developers</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Developers</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getDevelopers()
   * @see #getModel()
   * @generated
   */
  EReference getModel_Developers();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Model#getContributors <em>Contributors</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Contributors</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getContributors()
   * @see #getModel()
   * @generated
   */
  EReference getModel_Contributors();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Model#getLicenses <em>Licenses</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Licenses</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getLicenses()
   * @see #getModel()
   * @generated
   */
  EReference getModel_Licenses();

  /**
   * Returns the meta object for the containment reference ' {@link org.eclipse.m2e.model.edit.pom.Model#getScm
   * <em>Scm</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Scm</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getScm()
   * @see #getModel()
   * @generated
   */
  EReference getModel_Scm();

  /**
   * Returns the meta object for the containment reference '{@link org.eclipse.m2e.model.edit.pom.Model#getOrganization
   * <em>Organization</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Organization</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getOrganization()
   * @see #getModel()
   * @generated
   */
  EReference getModel_Organization();

  /**
   * Returns the meta object for the containment reference ' {@link org.eclipse.m2e.model.edit.pom.Model#getBuild
   * <em>Build</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Build</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getBuild()
   * @see #getModel()
   * @generated
   */
  EReference getModel_Build();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Model#getProfiles <em>Profiles</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Profiles</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getProfiles()
   * @see #getModel()
   * @generated
   */
  EReference getModel_Profiles();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Model#getRepositories <em>Repositories</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Repositories</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getRepositories()
   * @see #getModel()
   * @generated
   */
  EReference getModel_Repositories();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Model#getPluginRepositories <em>Plugin Repositories</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference list ' <em>Plugin Repositories</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getPluginRepositories()
   * @see #getModel()
   * @generated
   */
  EReference getModel_PluginRepositories();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Model#getDependencies <em>Dependencies</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Dependencies</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getDependencies()
   * @see #getModel()
   * @generated
   */
  EReference getModel_Dependencies();

  /**
   * Returns the meta object for the containment reference '{@link org.eclipse.m2e.model.edit.pom.Model#getReporting
   * <em>Reporting</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Reporting</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getReporting()
   * @see #getModel()
   * @generated
   */
  EReference getModel_Reporting();

  /**
   * Returns the meta object for the containment reference '
   * {@link org.eclipse.m2e.model.edit.pom.Model#getDependencyManagement <em>Dependency Management</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Dependency Management</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getDependencyManagement()
   * @see #getModel()
   * @generated
   */
  EReference getModel_DependencyManagement();

  /**
   * Returns the meta object for the containment reference '
   * {@link org.eclipse.m2e.model.edit.pom.Model#getDistributionManagement <em>Distribution Management</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Distribution Management</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getDistributionManagement()
   * @see #getModel()
   * @generated
   */
  EReference getModel_DistributionManagement();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Model#getProperties <em>Properties</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Properties</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getProperties()
   * @see #getModel()
   * @generated
   */
  EReference getModel_Properties();

  /**
   * Returns the meta object for the attribute list '{@link org.eclipse.m2e.model.edit.pom.Model#getModules
   * <em>Modules</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute list '<em>Modules</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Model#getModules()
   * @see #getModel()
   * @generated
   */
  EAttribute getModel_Modules();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.Notifier <em>Notifier</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Notifier</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Notifier
   * @generated
   */
  EClass getNotifier();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Notifier#getType <em>Type</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Type</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Notifier#getType()
   * @see #getNotifier()
   * @generated
   */
  EAttribute getNotifier_Type();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Notifier#getSendOnError
   * <em>Send On Error</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Send On Error</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Notifier#getSendOnError()
   * @see #getNotifier()
   * @generated
   */
  EAttribute getNotifier_SendOnError();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Notifier#getSendOnFailure
   * <em>Send On Failure</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Send On Failure</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Notifier#getSendOnFailure()
   * @see #getNotifier()
   * @generated
   */
  EAttribute getNotifier_SendOnFailure();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Notifier#getSendOnSuccess
   * <em>Send On Success</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Send On Success</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Notifier#getSendOnSuccess()
   * @see #getNotifier()
   * @generated
   */
  EAttribute getNotifier_SendOnSuccess();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Notifier#getSendOnWarning
   * <em>Send On Warning</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Send On Warning</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Notifier#getSendOnWarning()
   * @see #getNotifier()
   * @generated
   */
  EAttribute getNotifier_SendOnWarning();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Notifier#getAddress
   * <em>Address</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Address</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Notifier#getAddress()
   * @see #getNotifier()
   * @generated
   */
  EAttribute getNotifier_Address();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Notifier#getConfiguration <em>Configuration</em>}'. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Configuration</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Notifier#getConfiguration()
   * @see #getNotifier()
   * @generated
   */
  EReference getNotifier_Configuration();

  /**
   * Returns the meta object for class '{@link org.eclipse.m2e.model.edit.pom.Organization <em>Organization</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Organization</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Organization
   * @generated
   */
  EClass getOrganization();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Organization#getName
   * <em>Name</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Organization#getName()
   * @see #getOrganization()
   * @generated
   */
  EAttribute getOrganization_Name();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Organization#getUrl <em>Url</em>}
   * '. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Url</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Organization#getUrl()
   * @see #getOrganization()
   * @generated
   */
  EAttribute getOrganization_Url();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.Parent <em>Parent</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Parent</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Parent
   * @generated
   */
  EClass getParent();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Parent#getArtifactId
   * <em>Artifact Id</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Artifact Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Parent#getArtifactId()
   * @see #getParent()
   * @generated
   */
  EAttribute getParent_ArtifactId();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Parent#getGroupId
   * <em>Group Id</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Group Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Parent#getGroupId()
   * @see #getParent()
   * @generated
   */
  EAttribute getParent_GroupId();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Parent#getVersion
   * <em>Version</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Version</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Parent#getVersion()
   * @see #getParent()
   * @generated
   */
  EAttribute getParent_Version();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Parent#getRelativePath
   * <em>Relative Path</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Relative Path</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Parent#getRelativePath()
   * @see #getParent()
   * @generated
   */
  EAttribute getParent_RelativePath();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.Plugin <em>Plugin</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Plugin</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Plugin
   * @generated
   */
  EClass getPlugin();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Plugin#getGroupId
   * <em>Group Id</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Group Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Plugin#getGroupId()
   * @see #getPlugin()
   * @generated
   */
  EAttribute getPlugin_GroupId();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Plugin#getArtifactId
   * <em>Artifact Id</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Artifact Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Plugin#getArtifactId()
   * @see #getPlugin()
   * @generated
   */
  EAttribute getPlugin_ArtifactId();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Plugin#getVersion
   * <em>Version</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Version</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Plugin#getVersion()
   * @see #getPlugin()
   * @generated
   */
  EAttribute getPlugin_Version();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Plugin#getExtensions
   * <em>Extensions</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Extensions</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Plugin#getExtensions()
   * @see #getPlugin()
   * @generated
   */
  EAttribute getPlugin_Extensions();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Plugin#getExecutions <em>Executions</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Executions</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Plugin#getExecutions()
   * @see #getPlugin()
   * @generated
   */
  EReference getPlugin_Executions();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Plugin#getDependencies <em>Dependencies</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Dependencies</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Plugin#getDependencies()
   * @see #getPlugin()
   * @generated
   */
  EReference getPlugin_Dependencies();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Plugin#getInherited
   * <em>Inherited</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Inherited</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Plugin#getInherited()
   * @see #getPlugin()
   * @generated
   */
  EAttribute getPlugin_Inherited();

  /**
   * Returns the meta object for the reference '{@link org.eclipse.m2e.model.edit.pom.Plugin#getConfiguration
   * <em>Configuration</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the reference '<em>Configuration</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Plugin#getConfiguration()
   * @see #getPlugin()
   * @generated
   */
  EReference getPlugin_Configuration();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.PluginExecution
   * <em>Plugin Execution</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Plugin Execution</em>'.
   * @see org.eclipse.m2e.model.edit.pom.PluginExecution
   * @generated
   */
  EClass getPluginExecution();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.PluginExecution#getId <em>Id</em>}
   * '. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.PluginExecution#getId()
   * @see #getPluginExecution()
   * @generated
   */
  EAttribute getPluginExecution_Id();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.PluginExecution#getPhase
   * <em>Phase</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Phase</em>'.
   * @see org.eclipse.m2e.model.edit.pom.PluginExecution#getPhase()
   * @see #getPluginExecution()
   * @generated
   */
  EAttribute getPluginExecution_Phase();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.PluginExecution#getInherited
   * <em>Inherited</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Inherited</em>'.
   * @see org.eclipse.m2e.model.edit.pom.PluginExecution#getInherited()
   * @see #getPluginExecution()
   * @generated
   */
  EAttribute getPluginExecution_Inherited();

  /**
   * Returns the meta object for the attribute list '{@link org.eclipse.m2e.model.edit.pom.PluginExecution#getGoals
   * <em>Goals</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute list '<em>Goals</em>'.
   * @see org.eclipse.m2e.model.edit.pom.PluginExecution#getGoals()
   * @see #getPluginExecution()
   * @generated
   */
  EAttribute getPluginExecution_Goals();

  /**
   * Returns the meta object for the reference '{@link org.eclipse.m2e.model.edit.pom.PluginExecution#getConfiguration
   * <em>Configuration</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the reference '<em>Configuration</em>'.
   * @see org.eclipse.m2e.model.edit.pom.PluginExecution#getConfiguration()
   * @see #getPluginExecution()
   * @generated
   */
  EReference getPluginExecution_Configuration();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.PluginManagement
   * <em>Plugin Management</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Plugin Management</em>'.
   * @see org.eclipse.m2e.model.edit.pom.PluginManagement
   * @generated
   */
  EClass getPluginManagement();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.PluginManagement#getPlugins <em>Plugins</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Plugins</em>'.
   * @see org.eclipse.m2e.model.edit.pom.PluginManagement#getPlugins()
   * @see #getPluginManagement()
   * @generated
   */
  EReference getPluginManagement_Plugins();

  /**
   * Returns the meta object for class '{@link org.eclipse.m2e.model.edit.pom.Prerequisites <em>Prerequisites</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Prerequisites</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Prerequisites
   * @generated
   */
  EClass getPrerequisites();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Prerequisites#getMaven
   * <em>Maven</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Maven</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Prerequisites#getMaven()
   * @see #getPrerequisites()
   * @generated
   */
  EAttribute getPrerequisites_Maven();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.Profile <em>Profile</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Profile</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Profile
   * @generated
   */
  EClass getProfile();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.Profile#getId <em>Id</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Profile#getId()
   * @see #getProfile()
   * @generated
   */
  EAttribute getProfile_Id();

  /**
   * Returns the meta object for the containment reference '{@link org.eclipse.m2e.model.edit.pom.Profile#getActivation
   * <em>Activation</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Activation</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Profile#getActivation()
   * @see #getProfile()
   * @generated
   */
  EReference getProfile_Activation();

  /**
   * Returns the meta object for the containment reference '{@link org.eclipse.m2e.model.edit.pom.Profile#getBuild
   * <em>Build</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Build</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Profile#getBuild()
   * @see #getProfile()
   * @generated
   */
  EReference getProfile_Build();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Profile#getRepositories <em>Repositories</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Repositories</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Profile#getRepositories()
   * @see #getProfile()
   * @generated
   */
  EReference getProfile_Repositories();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Profile#getPluginRepositories <em>Plugin Repositories</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference list ' <em>Plugin Repositories</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Profile#getPluginRepositories()
   * @see #getProfile()
   * @generated
   */
  EReference getProfile_PluginRepositories();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Profile#getDependencies <em>Dependencies</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Dependencies</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Profile#getDependencies()
   * @see #getProfile()
   * @generated
   */
  EReference getProfile_Dependencies();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Profile#getReports <em>Reports</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Reports</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Profile#getReports()
   * @see #getProfile()
   * @generated
   */
  EReference getProfile_Reports();

  /**
   * Returns the meta object for the containment reference '
   * {@link org.eclipse.m2e.model.edit.pom.Profile#getDependencyManagement <em>Dependency Management</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Dependency Management</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Profile#getDependencyManagement()
   * @see #getProfile()
   * @generated
   */
  EReference getProfile_DependencyManagement();

  /**
   * Returns the meta object for the containment reference '
   * {@link org.eclipse.m2e.model.edit.pom.Profile#getDistributionManagement <em>Distribution Management</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Distribution Management</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Profile#getDistributionManagement()
   * @see #getProfile()
   * @generated
   */
  EReference getProfile_DistributionManagement();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Profile#getProperties <em>Properties</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Properties</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Profile#getProperties()
   * @see #getProfile()
   * @generated
   */
  EReference getProfile_Properties();

  /**
   * Returns the meta object for the attribute list '{@link org.eclipse.m2e.model.edit.pom.Profile#getModules
   * <em>Modules</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute list '<em>Modules</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Profile#getModules()
   * @see #getProfile()
   * @generated
   */
  EAttribute getProfile_Modules();

  /**
   * Returns the meta object for the reference '{@link org.eclipse.m2e.model.edit.pom.Profile#getReporting
   * <em>Reporting</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the reference '<em>Reporting</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Profile#getReporting()
   * @see #getProfile()
   * @generated
   */
  EReference getProfile_Reporting();

  /**
   * Returns the meta object for class '{@link org.eclipse.m2e.model.edit.pom.Relocation <em>Relocation</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Relocation</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Relocation
   * @generated
   */
  EClass getRelocation();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Relocation#getGroupId
   * <em>Group Id</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Group Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Relocation#getGroupId()
   * @see #getRelocation()
   * @generated
   */
  EAttribute getRelocation_GroupId();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Relocation#getArtifactId
   * <em>Artifact Id</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Artifact Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Relocation#getArtifactId()
   * @see #getRelocation()
   * @generated
   */
  EAttribute getRelocation_ArtifactId();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Relocation#getVersion
   * <em>Version</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Version</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Relocation#getVersion()
   * @see #getRelocation()
   * @generated
   */
  EAttribute getRelocation_Version();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Relocation#getMessage
   * <em>Message</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Message</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Relocation#getMessage()
   * @see #getRelocation()
   * @generated
   */
  EAttribute getRelocation_Message();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.Reporting <em>Reporting</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Reporting</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Reporting
   * @generated
   */
  EClass getReporting();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.Reporting#getExcludeDefaults
   * <em>Exclude Defaults</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Exclude Defaults</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Reporting#getExcludeDefaults()
   * @see #getReporting()
   * @generated
   */
  EAttribute getReporting_ExcludeDefaults();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.Reporting#getOutputDirectory
   * <em>Output Directory</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Output Directory</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Reporting#getOutputDirectory()
   * @see #getReporting()
   * @generated
   */
  EAttribute getReporting_OutputDirectory();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.Reporting#getPlugins <em>Plugins</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Plugins</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Reporting#getPlugins()
   * @see #getReporting()
   * @generated
   */
  EReference getReporting_Plugins();

  /**
   * Returns the meta object for class '{@link org.eclipse.m2e.model.edit.pom.ReportPlugin <em>Report Plugin</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Report Plugin</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ReportPlugin
   * @generated
   */
  EClass getReportPlugin();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getGroupId
   * <em>Group Id</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Group Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ReportPlugin#getGroupId()
   * @see #getReportPlugin()
   * @generated
   */
  EAttribute getReportPlugin_GroupId();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getArtifactId
   * <em>Artifact Id</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Artifact Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ReportPlugin#getArtifactId()
   * @see #getReportPlugin()
   * @generated
   */
  EAttribute getReportPlugin_ArtifactId();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getVersion
   * <em>Version</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Version</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ReportPlugin#getVersion()
   * @see #getReportPlugin()
   * @generated
   */
  EAttribute getReportPlugin_Version();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getInherited
   * <em>Inherited</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Inherited</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ReportPlugin#getInherited()
   * @see #getReportPlugin()
   * @generated
   */
  EAttribute getReportPlugin_Inherited();

  /**
   * Returns the meta object for the containment reference list '
   * {@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getReportSets <em>Report Sets</em>}'. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * 
   * @return the meta object for the containment reference list '<em>Report Sets</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ReportPlugin#getReportSets()
   * @see #getReportPlugin()
   * @generated
   */
  EReference getReportPlugin_ReportSets();

  /**
   * Returns the meta object for the reference '{@link org.eclipse.m2e.model.edit.pom.ReportPlugin#getConfiguration
   * <em>Configuration</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the reference '<em>Configuration</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ReportPlugin#getConfiguration()
   * @see #getReportPlugin()
   * @generated
   */
  EReference getReportPlugin_Configuration();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.ReportSet <em>Report Set</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Report Set</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ReportSet
   * @generated
   */
  EClass getReportSet();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.ReportSet#getId <em>Id</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ReportSet#getId()
   * @see #getReportSet()
   * @generated
   */
  EAttribute getReportSet_Id();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.ReportSet#getInherited
   * <em>Inherited</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Inherited</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ReportSet#getInherited()
   * @see #getReportSet()
   * @generated
   */
  EAttribute getReportSet_Inherited();

  /**
   * Returns the meta object for the attribute list '{@link org.eclipse.m2e.model.edit.pom.ReportSet#getReports
   * <em>Reports</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute list '<em>Reports</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ReportSet#getReports()
   * @see #getReportSet()
   * @generated
   */
  EAttribute getReportSet_Reports();

  /**
   * Returns the meta object for the reference '{@link org.eclipse.m2e.model.edit.pom.ReportSet#getConfiguration
   * <em>Configuration</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the reference '<em>Configuration</em>'.
   * @see org.eclipse.m2e.model.edit.pom.ReportSet#getConfiguration()
   * @see #getReportSet()
   * @generated
   */
  EReference getReportSet_Configuration();

  /**
   * Returns the meta object for class '{@link org.eclipse.m2e.model.edit.pom.Repository <em>Repository</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Repository</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Repository
   * @generated
   */
  EClass getRepository();

  /**
   * Returns the meta object for the containment reference '
   * {@link org.eclipse.m2e.model.edit.pom.Repository#getReleases <em>Releases</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Releases</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Repository#getReleases()
   * @see #getRepository()
   * @generated
   */
  EReference getRepository_Releases();

  /**
   * Returns the meta object for the containment reference '
   * {@link org.eclipse.m2e.model.edit.pom.Repository#getSnapshots <em>Snapshots</em>}'. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @return the meta object for the containment reference '<em>Snapshots</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Repository#getSnapshots()
   * @see #getRepository()
   * @generated
   */
  EReference getRepository_Snapshots();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.Repository#getId <em>Id</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Repository#getId()
   * @see #getRepository()
   * @generated
   */
  EAttribute getRepository_Id();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Repository#getName <em>Name</em>}
   * '. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Repository#getName()
   * @see #getRepository()
   * @generated
   */
  EAttribute getRepository_Name();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Repository#getUrl <em>Url</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Url</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Repository#getUrl()
   * @see #getRepository()
   * @generated
   */
  EAttribute getRepository_Url();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Repository#getLayout
   * <em>Layout</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Layout</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Repository#getLayout()
   * @see #getRepository()
   * @generated
   */
  EAttribute getRepository_Layout();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.RepositoryPolicy
   * <em>Repository Policy</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Repository Policy</em>'.
   * @see org.eclipse.m2e.model.edit.pom.RepositoryPolicy
   * @generated
   */
  EClass getRepositoryPolicy();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.RepositoryPolicy#getEnabled
   * <em>Enabled</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Enabled</em>'.
   * @see org.eclipse.m2e.model.edit.pom.RepositoryPolicy#getEnabled()
   * @see #getRepositoryPolicy()
   * @generated
   */
  EAttribute getRepositoryPolicy_Enabled();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.RepositoryPolicy#getUpdatePolicy
   * <em>Update Policy</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Update Policy</em>'.
   * @see org.eclipse.m2e.model.edit.pom.RepositoryPolicy#getUpdatePolicy()
   * @see #getRepositoryPolicy()
   * @generated
   */
  EAttribute getRepositoryPolicy_UpdatePolicy();

  /**
   * Returns the meta object for the attribute '
   * {@link org.eclipse.m2e.model.edit.pom.RepositoryPolicy#getChecksumPolicy <em>Checksum Policy</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Checksum Policy</em>'.
   * @see org.eclipse.m2e.model.edit.pom.RepositoryPolicy#getChecksumPolicy()
   * @see #getRepositoryPolicy()
   * @generated
   */
  EAttribute getRepositoryPolicy_ChecksumPolicy();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.Resource <em>Resource</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Resource</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Resource
   * @generated
   */
  EClass getResource();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Resource#getTargetPath
   * <em>Target Path</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Target Path</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Resource#getTargetPath()
   * @see #getResource()
   * @generated
   */
  EAttribute getResource_TargetPath();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Resource#getFiltering
   * <em>Filtering</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Filtering</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Resource#getFiltering()
   * @see #getResource()
   * @generated
   */
  EAttribute getResource_Filtering();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Resource#getDirectory
   * <em>Directory</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Directory</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Resource#getDirectory()
   * @see #getResource()
   * @generated
   */
  EAttribute getResource_Directory();

  /**
   * Returns the meta object for the attribute list '{@link org.eclipse.m2e.model.edit.pom.Resource#getIncludes
   * <em>Includes</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute list '<em>Includes</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Resource#getIncludes()
   * @see #getResource()
   * @generated
   */
  EAttribute getResource_Includes();

  /**
   * Returns the meta object for the attribute list '{@link org.eclipse.m2e.model.edit.pom.Resource#getExcludes
   * <em>Excludes</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute list '<em>Excludes</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Resource#getExcludes()
   * @see #getResource()
   * @generated
   */
  EAttribute getResource_Excludes();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.Scm <em>Scm</em>}'. <!-- begin-user-doc
   * --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Scm</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Scm
   * @generated
   */
  EClass getScm();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Scm#getConnection
   * <em>Connection</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Connection</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Scm#getConnection()
   * @see #getScm()
   * @generated
   */
  EAttribute getScm_Connection();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.Scm#getDeveloperConnection
   * <em>Developer Connection</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Developer Connection</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Scm#getDeveloperConnection()
   * @see #getScm()
   * @generated
   */
  EAttribute getScm_DeveloperConnection();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.Scm#getTag <em>Tag</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Tag</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Scm#getTag()
   * @see #getScm()
   * @generated
   */
  EAttribute getScm_Tag();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.Scm#getUrl <em>Url</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Url</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Scm#getUrl()
   * @see #getScm()
   * @generated
   */
  EAttribute getScm_Url();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.Site <em>Site</em>}'. <!-- begin-user-doc
   * --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Site</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Site
   * @generated
   */
  EClass getSite();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.Site#getId <em>Id</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Id</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Site#getId()
   * @see #getSite()
   * @generated
   */
  EAttribute getSite_Id();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.Site#getName <em>Name</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Site#getName()
   * @see #getSite()
   * @generated
   */
  EAttribute getSite_Name();

  /**
   * Returns the meta object for the attribute ' {@link org.eclipse.m2e.model.edit.pom.Site#getUrl <em>Url</em>}'. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Url</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Site#getUrl()
   * @see #getSite()
   * @generated
   */
  EAttribute getSite_Url();

  /**
   * Returns the meta object for class ' {@link org.eclipse.m2e.model.edit.pom.PropertyElement
   * <em>Property Element</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Property Element</em>'.
   * @see org.eclipse.m2e.model.edit.pom.PropertyElement
   * @generated
   */
  EClass getPropertyElement();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.PropertyElement#getName
   * <em>Name</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.eclipse.m2e.model.edit.pom.PropertyElement#getName()
   * @see #getPropertyElement()
   * @generated
   */
  EAttribute getPropertyElement_Name();

  /**
   * Returns the meta object for the attribute '{@link org.eclipse.m2e.model.edit.pom.PropertyElement#getValue
   * <em>Value</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for the attribute '<em>Value</em>'.
   * @see org.eclipse.m2e.model.edit.pom.PropertyElement#getValue()
   * @see #getPropertyElement()
   * @generated
   */
  EAttribute getPropertyElement_Value();

  /**
   * Returns the meta object for class '{@link org.eclipse.m2e.model.edit.pom.Configuration <em>Configuration</em>}'.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the meta object for class '<em>Configuration</em>'.
   * @see org.eclipse.m2e.model.edit.pom.Configuration
   * @generated
   */
  EClass getConfiguration();

  /**
   * Returns the factory that creates the instances of the model. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @return the factory that creates the instances of the model.
   * @generated
   */
  PomFactory getPomFactory();

  /**
   * <!-- begin-user-doc --> Defines literals for the meta objects that represent
   * <ul>
   * <li>each class,</li>
   * <li>each feature of each class,</li>
   * <li>each enum,</li>
   * <li>and each data type</li>
   * </ul>
   * <!-- end-user-doc -->
   * 
   * @generated
   */
  interface Literals {
    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.ActivationImpl <em>Activation</em>}'
     * class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.ActivationImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getActivation()
     * @generated
     */
    EClass ACTIVATION = eINSTANCE.getActivation();

    /**
     * The meta object literal for the '<em><b>Active By Default</b></em>' attribute feature. <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * 
     * @generated
     */
    EAttribute ACTIVATION__ACTIVE_BY_DEFAULT = eINSTANCE.getActivation_ActiveByDefault();

    /**
     * The meta object literal for the '<em><b>Jdk</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute ACTIVATION__JDK = eINSTANCE.getActivation_Jdk();

    /**
     * The meta object literal for the '<em><b>Os</b></em>' containment reference feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EReference ACTIVATION__OS = eINSTANCE.getActivation_Os();

    /**
     * The meta object literal for the '<em><b>Property</b></em>' containment reference feature. <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference ACTIVATION__PROPERTY = eINSTANCE.getActivation_Property();

    /**
     * The meta object literal for the '<em><b>File</b></em>' containment reference feature. <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference ACTIVATION__FILE = eINSTANCE.getActivation_File();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.ActivationFileImpl
     * <em>Activation File</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.ActivationFileImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getActivationFile()
     * @generated
     */
    EClass ACTIVATION_FILE = eINSTANCE.getActivationFile();

    /**
     * The meta object literal for the '<em><b>Missing</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute ACTIVATION_FILE__MISSING = eINSTANCE.getActivationFile_Missing();

    /**
     * The meta object literal for the '<em><b>Exists</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute ACTIVATION_FILE__EXISTS = eINSTANCE.getActivationFile_Exists();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.ActivationOSImpl
     * <em>Activation OS</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.ActivationOSImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getActivationOS()
     * @generated
     */
    EClass ACTIVATION_OS = eINSTANCE.getActivationOS();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute ACTIVATION_OS__NAME = eINSTANCE.getActivationOS_Name();

    /**
     * The meta object literal for the '<em><b>Family</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute ACTIVATION_OS__FAMILY = eINSTANCE.getActivationOS_Family();

    /**
     * The meta object literal for the '<em><b>Arch</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute ACTIVATION_OS__ARCH = eINSTANCE.getActivationOS_Arch();

    /**
     * The meta object literal for the '<em><b>Version</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute ACTIVATION_OS__VERSION = eINSTANCE.getActivationOS_Version();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.ActivationPropertyImpl
     * <em>Activation Property</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.ActivationPropertyImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getActivationProperty()
     * @generated
     */
    EClass ACTIVATION_PROPERTY = eINSTANCE.getActivationProperty();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute ACTIVATION_PROPERTY__NAME = eINSTANCE.getActivationProperty_Name();

    /**
     * The meta object literal for the '<em><b>Value</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute ACTIVATION_PROPERTY__VALUE = eINSTANCE.getActivationProperty_Value();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.BuildImpl <em>Build</em>}' class.
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.BuildImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getBuild()
     * @generated
     */
    EClass BUILD = eINSTANCE.getBuild();

    /**
     * The meta object literal for the '<em><b>Source Directory</b></em>' attribute feature. <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * 
     * @generated
     */
    EAttribute BUILD__SOURCE_DIRECTORY = eINSTANCE.getBuild_SourceDirectory();

    /**
     * The meta object literal for the ' <em><b>Script Source Directory</b></em>' attribute feature. <!-- begin-user-doc
     * --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EAttribute BUILD__SCRIPT_SOURCE_DIRECTORY = eINSTANCE.getBuild_ScriptSourceDirectory();

    /**
     * The meta object literal for the ' <em><b>Test Source Directory</b></em>' attribute feature. <!-- begin-user-doc
     * --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EAttribute BUILD__TEST_SOURCE_DIRECTORY = eINSTANCE.getBuild_TestSourceDirectory();

    /**
     * The meta object literal for the '<em><b>Output Directory</b></em>' attribute feature. <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * 
     * @generated
     */
    EAttribute BUILD__OUTPUT_DIRECTORY = eINSTANCE.getBuild_OutputDirectory();

    /**
     * The meta object literal for the ' <em><b>Test Output Directory</b></em>' attribute feature. <!-- begin-user-doc
     * --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EAttribute BUILD__TEST_OUTPUT_DIRECTORY = eINSTANCE.getBuild_TestOutputDirectory();

    /**
     * The meta object literal for the '<em><b>Extensions</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference BUILD__EXTENSIONS = eINSTANCE.getBuild_Extensions();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.BuildBaseImpl <em>Build Base</em>}'
     * class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.BuildBaseImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getBuildBase()
     * @generated
     */
    EClass BUILD_BASE = eINSTANCE.getBuildBase();

    /**
     * The meta object literal for the '<em><b>Default Goal</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute BUILD_BASE__DEFAULT_GOAL = eINSTANCE.getBuildBase_DefaultGoal();

    /**
     * The meta object literal for the '<em><b>Resources</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference BUILD_BASE__RESOURCES = eINSTANCE.getBuildBase_Resources();

    /**
     * The meta object literal for the '<em><b>Test Resources</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference BUILD_BASE__TEST_RESOURCES = eINSTANCE.getBuildBase_TestResources();

    /**
     * The meta object literal for the '<em><b>Directory</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute BUILD_BASE__DIRECTORY = eINSTANCE.getBuildBase_Directory();

    /**
     * The meta object literal for the '<em><b>Final Name</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute BUILD_BASE__FINAL_NAME = eINSTANCE.getBuildBase_FinalName();

    /**
     * The meta object literal for the '<em><b>Plugin Management</b></em>' containment reference feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference BUILD_BASE__PLUGIN_MANAGEMENT = eINSTANCE.getBuildBase_PluginManagement();

    /**
     * The meta object literal for the '<em><b>Plugins</b></em>' containment reference list feature. <!-- begin-user-doc
     * --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference BUILD_BASE__PLUGINS = eINSTANCE.getBuildBase_Plugins();

    /**
     * The meta object literal for the '<em><b>Filters</b></em>' attribute list feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute BUILD_BASE__FILTERS = eINSTANCE.getBuildBase_Filters();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.CiManagementImpl
     * <em>Ci Management</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.CiManagementImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getCiManagement()
     * @generated
     */
    EClass CI_MANAGEMENT = eINSTANCE.getCiManagement();

    /**
     * The meta object literal for the '<em><b>System</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute CI_MANAGEMENT__SYSTEM = eINSTANCE.getCiManagement_System();

    /**
     * The meta object literal for the '<em><b>Url</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute CI_MANAGEMENT__URL = eINSTANCE.getCiManagement_Url();

    /**
     * The meta object literal for the '<em><b>Notifiers</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference CI_MANAGEMENT__NOTIFIERS = eINSTANCE.getCiManagement_Notifiers();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.ContributorImpl <em>Contributor</em>}
     * ' class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.ContributorImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getContributor()
     * @generated
     */
    EClass CONTRIBUTOR = eINSTANCE.getContributor();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute CONTRIBUTOR__NAME = eINSTANCE.getContributor_Name();

    /**
     * The meta object literal for the '<em><b>Email</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute CONTRIBUTOR__EMAIL = eINSTANCE.getContributor_Email();

    /**
     * The meta object literal for the '<em><b>Url</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute CONTRIBUTOR__URL = eINSTANCE.getContributor_Url();

    /**
     * The meta object literal for the '<em><b>Organization</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute CONTRIBUTOR__ORGANIZATION = eINSTANCE.getContributor_Organization();

    /**
     * The meta object literal for the '<em><b>Organization Url</b></em>' attribute feature. <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * 
     * @generated
     */
    EAttribute CONTRIBUTOR__ORGANIZATION_URL = eINSTANCE.getContributor_OrganizationUrl();

    /**
     * The meta object literal for the '<em><b>Timezone</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute CONTRIBUTOR__TIMEZONE = eINSTANCE.getContributor_Timezone();

    /**
     * The meta object literal for the '<em><b>Properties</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference CONTRIBUTOR__PROPERTIES = eINSTANCE.getContributor_Properties();

    /**
     * The meta object literal for the '<em><b>Roles</b></em>' attribute list feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute CONTRIBUTOR__ROLES = eINSTANCE.getContributor_Roles();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.DependencyImpl <em>Dependency</em>}'
     * class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.DependencyImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getDependency()
     * @generated
     */
    EClass DEPENDENCY = eINSTANCE.getDependency();

    /**
     * The meta object literal for the '<em><b>Group Id</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DEPENDENCY__GROUP_ID = eINSTANCE.getDependency_GroupId();

    /**
     * The meta object literal for the '<em><b>Artifact Id</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DEPENDENCY__ARTIFACT_ID = eINSTANCE.getDependency_ArtifactId();

    /**
     * The meta object literal for the '<em><b>Version</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DEPENDENCY__VERSION = eINSTANCE.getDependency_Version();

    /**
     * The meta object literal for the '<em><b>Type</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DEPENDENCY__TYPE = eINSTANCE.getDependency_Type();

    /**
     * The meta object literal for the '<em><b>Classifier</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DEPENDENCY__CLASSIFIER = eINSTANCE.getDependency_Classifier();

    /**
     * The meta object literal for the '<em><b>Scope</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DEPENDENCY__SCOPE = eINSTANCE.getDependency_Scope();

    /**
     * The meta object literal for the '<em><b>System Path</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DEPENDENCY__SYSTEM_PATH = eINSTANCE.getDependency_SystemPath();

    /**
     * The meta object literal for the '<em><b>Exclusions</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference DEPENDENCY__EXCLUSIONS = eINSTANCE.getDependency_Exclusions();

    /**
     * The meta object literal for the '<em><b>Optional</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DEPENDENCY__OPTIONAL = eINSTANCE.getDependency_Optional();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.DependencyManagementImpl
     * <em>Dependency Management</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.DependencyManagementImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getDependencyManagement()
     * @generated
     */
    EClass DEPENDENCY_MANAGEMENT = eINSTANCE.getDependencyManagement();

    /**
     * The meta object literal for the '<em><b>Dependencies</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference DEPENDENCY_MANAGEMENT__DEPENDENCIES = eINSTANCE.getDependencyManagement_Dependencies();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.DeploymentRepositoryImpl
     * <em>Deployment Repository</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.DeploymentRepositoryImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getDeploymentRepository()
     * @generated
     */
    EClass DEPLOYMENT_REPOSITORY = eINSTANCE.getDeploymentRepository();

    /**
     * The meta object literal for the '<em><b>Unique Version</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DEPLOYMENT_REPOSITORY__UNIQUE_VERSION = eINSTANCE.getDeploymentRepository_UniqueVersion();

    /**
     * The meta object literal for the '<em><b>Id</b></em>' attribute feature. <!-- begin-user-doc --> <!-- end-user-doc
     * -->
     * 
     * @generated
     */
    EAttribute DEPLOYMENT_REPOSITORY__ID = eINSTANCE.getDeploymentRepository_Id();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DEPLOYMENT_REPOSITORY__NAME = eINSTANCE.getDeploymentRepository_Name();

    /**
     * The meta object literal for the '<em><b>Url</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DEPLOYMENT_REPOSITORY__URL = eINSTANCE.getDeploymentRepository_Url();

    /**
     * The meta object literal for the '<em><b>Layout</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DEPLOYMENT_REPOSITORY__LAYOUT = eINSTANCE.getDeploymentRepository_Layout();

    /**
     * The meta object literal for the ' {@link org.eclipse.m2e.model.edit.pom.impl.DeveloperImpl <em>Developer</em>}'
     * class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.DeveloperImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getDeveloper()
     * @generated
     */
    EClass DEVELOPER = eINSTANCE.getDeveloper();

    /**
     * The meta object literal for the '<em><b>Id</b></em>' attribute feature. <!-- begin-user-doc --> <!-- end-user-doc
     * -->
     * 
     * @generated
     */
    EAttribute DEVELOPER__ID = eINSTANCE.getDeveloper_Id();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DEVELOPER__NAME = eINSTANCE.getDeveloper_Name();

    /**
     * The meta object literal for the '<em><b>Email</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DEVELOPER__EMAIL = eINSTANCE.getDeveloper_Email();

    /**
     * The meta object literal for the '<em><b>Url</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DEVELOPER__URL = eINSTANCE.getDeveloper_Url();

    /**
     * The meta object literal for the '<em><b>Organization</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DEVELOPER__ORGANIZATION = eINSTANCE.getDeveloper_Organization();

    /**
     * The meta object literal for the '<em><b>Organization Url</b></em>' attribute feature. <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * 
     * @generated
     */
    EAttribute DEVELOPER__ORGANIZATION_URL = eINSTANCE.getDeveloper_OrganizationUrl();

    /**
     * The meta object literal for the '<em><b>Timezone</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DEVELOPER__TIMEZONE = eINSTANCE.getDeveloper_Timezone();

    /**
     * The meta object literal for the '<em><b>Properties</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference DEVELOPER__PROPERTIES = eINSTANCE.getDeveloper_Properties();

    /**
     * The meta object literal for the '<em><b>Roles</b></em>' attribute list feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DEVELOPER__ROLES = eINSTANCE.getDeveloper_Roles();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.DistributionManagementImpl
     * <em>Distribution Management</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.DistributionManagementImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getDistributionManagement()
     * @generated
     */
    EClass DISTRIBUTION_MANAGEMENT = eINSTANCE.getDistributionManagement();

    /**
     * The meta object literal for the '<em><b>Repository</b></em>' containment reference feature. <!-- begin-user-doc
     * --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference DISTRIBUTION_MANAGEMENT__REPOSITORY = eINSTANCE.getDistributionManagement_Repository();

    /**
     * The meta object literal for the '<em><b>Snapshot Repository</b></em>' containment reference feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference DISTRIBUTION_MANAGEMENT__SNAPSHOT_REPOSITORY = eINSTANCE.getDistributionManagement_SnapshotRepository();

    /**
     * The meta object literal for the '<em><b>Site</b></em>' containment reference feature. <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference DISTRIBUTION_MANAGEMENT__SITE = eINSTANCE.getDistributionManagement_Site();

    /**
     * The meta object literal for the '<em><b>Download Url</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DISTRIBUTION_MANAGEMENT__DOWNLOAD_URL = eINSTANCE.getDistributionManagement_DownloadUrl();

    /**
     * The meta object literal for the '<em><b>Relocation</b></em>' containment reference feature. <!-- begin-user-doc
     * --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference DISTRIBUTION_MANAGEMENT__RELOCATION = eINSTANCE.getDistributionManagement_Relocation();

    /**
     * The meta object literal for the '<em><b>Status</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DISTRIBUTION_MANAGEMENT__STATUS = eINSTANCE.getDistributionManagement_Status();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.DocumentRootImpl
     * <em>Document Root</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.DocumentRootImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getDocumentRoot()
     * @generated
     */
    EClass DOCUMENT_ROOT = eINSTANCE.getDocumentRoot();

    /**
     * The meta object literal for the '<em><b>Mixed</b></em>' attribute list feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute DOCUMENT_ROOT__MIXED = eINSTANCE.getDocumentRoot_Mixed();

    /**
     * The meta object literal for the '<em><b>XMLNS Prefix Map</b></em>' map feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EReference DOCUMENT_ROOT__XMLNS_PREFIX_MAP = eINSTANCE.getDocumentRoot_XMLNSPrefixMap();

    /**
     * The meta object literal for the '<em><b>XSI Schema Location</b></em>' map feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EReference DOCUMENT_ROOT__XSI_SCHEMA_LOCATION = eINSTANCE.getDocumentRoot_XSISchemaLocation();

    /**
     * The meta object literal for the '<em><b>Project</b></em>' containment reference feature. <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference DOCUMENT_ROOT__PROJECT = eINSTANCE.getDocumentRoot_Project();

    /**
     * The meta object literal for the ' {@link org.eclipse.m2e.model.edit.pom.impl.ExclusionImpl <em>Exclusion</em>}'
     * class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.ExclusionImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getExclusion()
     * @generated
     */
    EClass EXCLUSION = eINSTANCE.getExclusion();

    /**
     * The meta object literal for the '<em><b>Artifact Id</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute EXCLUSION__ARTIFACT_ID = eINSTANCE.getExclusion_ArtifactId();

    /**
     * The meta object literal for the '<em><b>Group Id</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute EXCLUSION__GROUP_ID = eINSTANCE.getExclusion_GroupId();

    /**
     * The meta object literal for the ' {@link org.eclipse.m2e.model.edit.pom.impl.ExtensionImpl <em>Extension</em>}'
     * class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.ExtensionImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getExtension()
     * @generated
     */
    EClass EXTENSION = eINSTANCE.getExtension();

    /**
     * The meta object literal for the '<em><b>Group Id</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute EXTENSION__GROUP_ID = eINSTANCE.getExtension_GroupId();

    /**
     * The meta object literal for the '<em><b>Artifact Id</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute EXTENSION__ARTIFACT_ID = eINSTANCE.getExtension_ArtifactId();

    /**
     * The meta object literal for the '<em><b>Version</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute EXTENSION__VERSION = eINSTANCE.getExtension_Version();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.IssueManagementImpl
     * <em>Issue Management</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.IssueManagementImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getIssueManagement()
     * @generated
     */
    EClass ISSUE_MANAGEMENT = eINSTANCE.getIssueManagement();

    /**
     * The meta object literal for the '<em><b>System</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute ISSUE_MANAGEMENT__SYSTEM = eINSTANCE.getIssueManagement_System();

    /**
     * The meta object literal for the '<em><b>Url</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute ISSUE_MANAGEMENT__URL = eINSTANCE.getIssueManagement_Url();

    /**
     * The meta object literal for the ' {@link org.eclipse.m2e.model.edit.pom.impl.LicenseImpl <em>License</em>}'
     * class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.LicenseImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getLicense()
     * @generated
     */
    EClass LICENSE = eINSTANCE.getLicense();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute LICENSE__NAME = eINSTANCE.getLicense_Name();

    /**
     * The meta object literal for the '<em><b>Url</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute LICENSE__URL = eINSTANCE.getLicense_Url();

    /**
     * The meta object literal for the '<em><b>Distribution</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute LICENSE__DISTRIBUTION = eINSTANCE.getLicense_Distribution();

    /**
     * The meta object literal for the '<em><b>Comments</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute LICENSE__COMMENTS = eINSTANCE.getLicense_Comments();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.MailingListImpl
     * <em>Mailing List</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.MailingListImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getMailingList()
     * @generated
     */
    EClass MAILING_LIST = eINSTANCE.getMailingList();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute MAILING_LIST__NAME = eINSTANCE.getMailingList_Name();

    /**
     * The meta object literal for the '<em><b>Subscribe</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute MAILING_LIST__SUBSCRIBE = eINSTANCE.getMailingList_Subscribe();

    /**
     * The meta object literal for the '<em><b>Unsubscribe</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute MAILING_LIST__UNSUBSCRIBE = eINSTANCE.getMailingList_Unsubscribe();

    /**
     * The meta object literal for the '<em><b>Post</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute MAILING_LIST__POST = eINSTANCE.getMailingList_Post();

    /**
     * The meta object literal for the '<em><b>Archive</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute MAILING_LIST__ARCHIVE = eINSTANCE.getMailingList_Archive();

    /**
     * The meta object literal for the '<em><b>Other Archives</b></em>' attribute list feature. <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * 
     * @generated
     */
    EAttribute MAILING_LIST__OTHER_ARCHIVES = eINSTANCE.getMailingList_OtherArchives();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl <em>Model</em>}' class.
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.ModelImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getModel()
     * @generated
     */
    EClass MODEL = eINSTANCE.getModel();

    /**
     * The meta object literal for the '<em><b>Parent</b></em>' containment reference feature. <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference MODEL__PARENT = eINSTANCE.getModel_Parent();

    /**
     * The meta object literal for the '<em><b>Model Version</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute MODEL__MODEL_VERSION = eINSTANCE.getModel_ModelVersion();

    /**
     * The meta object literal for the '<em><b>Group Id</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute MODEL__GROUP_ID = eINSTANCE.getModel_GroupId();

    /**
     * The meta object literal for the '<em><b>Artifact Id</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute MODEL__ARTIFACT_ID = eINSTANCE.getModel_ArtifactId();

    /**
     * The meta object literal for the '<em><b>Packaging</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute MODEL__PACKAGING = eINSTANCE.getModel_Packaging();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute MODEL__NAME = eINSTANCE.getModel_Name();

    /**
     * The meta object literal for the '<em><b>Version</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute MODEL__VERSION = eINSTANCE.getModel_Version();

    /**
     * The meta object literal for the '<em><b>Description</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute MODEL__DESCRIPTION = eINSTANCE.getModel_Description();

    /**
     * The meta object literal for the '<em><b>Url</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute MODEL__URL = eINSTANCE.getModel_Url();

    /**
     * The meta object literal for the '<em><b>Prerequisites</b></em>' containment reference feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference MODEL__PREREQUISITES = eINSTANCE.getModel_Prerequisites();

    /**
     * The meta object literal for the '<em><b>Issue Management</b></em>' containment reference feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference MODEL__ISSUE_MANAGEMENT = eINSTANCE.getModel_IssueManagement();

    /**
     * The meta object literal for the '<em><b>Ci Management</b></em>' containment reference feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference MODEL__CI_MANAGEMENT = eINSTANCE.getModel_CiManagement();

    /**
     * The meta object literal for the '<em><b>Inception Year</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute MODEL__INCEPTION_YEAR = eINSTANCE.getModel_InceptionYear();

    /**
     * The meta object literal for the '<em><b>Mailing Lists</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference MODEL__MAILING_LISTS = eINSTANCE.getModel_MailingLists();

    /**
     * The meta object literal for the '<em><b>Developers</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference MODEL__DEVELOPERS = eINSTANCE.getModel_Developers();

    /**
     * The meta object literal for the '<em><b>Contributors</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference MODEL__CONTRIBUTORS = eINSTANCE.getModel_Contributors();

    /**
     * The meta object literal for the '<em><b>Licenses</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference MODEL__LICENSES = eINSTANCE.getModel_Licenses();

    /**
     * The meta object literal for the '<em><b>Scm</b></em>' containment reference feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EReference MODEL__SCM = eINSTANCE.getModel_Scm();

    /**
     * The meta object literal for the '<em><b>Organization</b></em>' containment reference feature. <!-- begin-user-doc
     * --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference MODEL__ORGANIZATION = eINSTANCE.getModel_Organization();

    /**
     * The meta object literal for the '<em><b>Build</b></em>' containment reference feature. <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference MODEL__BUILD = eINSTANCE.getModel_Build();

    /**
     * The meta object literal for the '<em><b>Profiles</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference MODEL__PROFILES = eINSTANCE.getModel_Profiles();

    /**
     * The meta object literal for the '<em><b>Repositories</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference MODEL__REPOSITORIES = eINSTANCE.getModel_Repositories();

    /**
     * The meta object literal for the '<em><b>Plugin Repositories</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference MODEL__PLUGIN_REPOSITORIES = eINSTANCE.getModel_PluginRepositories();

    /**
     * The meta object literal for the '<em><b>Dependencies</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference MODEL__DEPENDENCIES = eINSTANCE.getModel_Dependencies();

    /**
     * The meta object literal for the '<em><b>Reporting</b></em>' containment reference feature. <!-- begin-user-doc
     * --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference MODEL__REPORTING = eINSTANCE.getModel_Reporting();

    /**
     * The meta object literal for the '<em><b>Dependency Management</b></em>' containment reference feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference MODEL__DEPENDENCY_MANAGEMENT = eINSTANCE.getModel_DependencyManagement();

    /**
     * The meta object literal for the '<em><b>Distribution Management</b></em>' containment reference feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference MODEL__DISTRIBUTION_MANAGEMENT = eINSTANCE.getModel_DistributionManagement();

    /**
     * The meta object literal for the '<em><b>Properties</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference MODEL__PROPERTIES = eINSTANCE.getModel_Properties();

    /**
     * The meta object literal for the '<em><b>Modules</b></em>' attribute list feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute MODEL__MODULES = eINSTANCE.getModel_Modules();

    /**
     * The meta object literal for the ' {@link org.eclipse.m2e.model.edit.pom.impl.NotifierImpl <em>Notifier</em>}'
     * class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.NotifierImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getNotifier()
     * @generated
     */
    EClass NOTIFIER = eINSTANCE.getNotifier();

    /**
     * The meta object literal for the '<em><b>Type</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute NOTIFIER__TYPE = eINSTANCE.getNotifier_Type();

    /**
     * The meta object literal for the '<em><b>Send On Error</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute NOTIFIER__SEND_ON_ERROR = eINSTANCE.getNotifier_SendOnError();

    /**
     * The meta object literal for the '<em><b>Send On Failure</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute NOTIFIER__SEND_ON_FAILURE = eINSTANCE.getNotifier_SendOnFailure();

    /**
     * The meta object literal for the '<em><b>Send On Success</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute NOTIFIER__SEND_ON_SUCCESS = eINSTANCE.getNotifier_SendOnSuccess();

    /**
     * The meta object literal for the '<em><b>Send On Warning</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute NOTIFIER__SEND_ON_WARNING = eINSTANCE.getNotifier_SendOnWarning();

    /**
     * The meta object literal for the '<em><b>Address</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute NOTIFIER__ADDRESS = eINSTANCE.getNotifier_Address();

    /**
     * The meta object literal for the '<em><b>Configuration</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference NOTIFIER__CONFIGURATION = eINSTANCE.getNotifier_Configuration();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.OrganizationImpl
     * <em>Organization</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.OrganizationImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getOrganization()
     * @generated
     */
    EClass ORGANIZATION = eINSTANCE.getOrganization();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute ORGANIZATION__NAME = eINSTANCE.getOrganization_Name();

    /**
     * The meta object literal for the '<em><b>Url</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute ORGANIZATION__URL = eINSTANCE.getOrganization_Url();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.ParentImpl <em>Parent</em>}' class.
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.ParentImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getParent()
     * @generated
     */
    EClass PARENT = eINSTANCE.getParent();

    /**
     * The meta object literal for the '<em><b>Artifact Id</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute PARENT__ARTIFACT_ID = eINSTANCE.getParent_ArtifactId();

    /**
     * The meta object literal for the '<em><b>Group Id</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute PARENT__GROUP_ID = eINSTANCE.getParent_GroupId();

    /**
     * The meta object literal for the '<em><b>Version</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute PARENT__VERSION = eINSTANCE.getParent_Version();

    /**
     * The meta object literal for the '<em><b>Relative Path</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute PARENT__RELATIVE_PATH = eINSTANCE.getParent_RelativePath();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.PluginImpl <em>Plugin</em>}' class.
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.PluginImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getPlugin()
     * @generated
     */
    EClass PLUGIN = eINSTANCE.getPlugin();

    /**
     * The meta object literal for the '<em><b>Group Id</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute PLUGIN__GROUP_ID = eINSTANCE.getPlugin_GroupId();

    /**
     * The meta object literal for the '<em><b>Artifact Id</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute PLUGIN__ARTIFACT_ID = eINSTANCE.getPlugin_ArtifactId();

    /**
     * The meta object literal for the '<em><b>Version</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute PLUGIN__VERSION = eINSTANCE.getPlugin_Version();

    /**
     * The meta object literal for the '<em><b>Extensions</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute PLUGIN__EXTENSIONS = eINSTANCE.getPlugin_Extensions();

    /**
     * The meta object literal for the '<em><b>Executions</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference PLUGIN__EXECUTIONS = eINSTANCE.getPlugin_Executions();

    /**
     * The meta object literal for the '<em><b>Dependencies</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference PLUGIN__DEPENDENCIES = eINSTANCE.getPlugin_Dependencies();

    /**
     * The meta object literal for the '<em><b>Inherited</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute PLUGIN__INHERITED = eINSTANCE.getPlugin_Inherited();

    /**
     * The meta object literal for the '<em><b>Configuration</b></em>' reference feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EReference PLUGIN__CONFIGURATION = eINSTANCE.getPlugin_Configuration();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.PluginExecutionImpl
     * <em>Plugin Execution</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.PluginExecutionImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getPluginExecution()
     * @generated
     */
    EClass PLUGIN_EXECUTION = eINSTANCE.getPluginExecution();

    /**
     * The meta object literal for the '<em><b>Id</b></em>' attribute feature. <!-- begin-user-doc --> <!-- end-user-doc
     * -->
     * 
     * @generated
     */
    EAttribute PLUGIN_EXECUTION__ID = eINSTANCE.getPluginExecution_Id();

    /**
     * The meta object literal for the '<em><b>Phase</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute PLUGIN_EXECUTION__PHASE = eINSTANCE.getPluginExecution_Phase();

    /**
     * The meta object literal for the '<em><b>Inherited</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute PLUGIN_EXECUTION__INHERITED = eINSTANCE.getPluginExecution_Inherited();

    /**
     * The meta object literal for the '<em><b>Goals</b></em>' attribute list feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute PLUGIN_EXECUTION__GOALS = eINSTANCE.getPluginExecution_Goals();

    /**
     * The meta object literal for the '<em><b>Configuration</b></em>' reference feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EReference PLUGIN_EXECUTION__CONFIGURATION = eINSTANCE.getPluginExecution_Configuration();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.PluginManagementImpl
     * <em>Plugin Management</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.PluginManagementImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getPluginManagement()
     * @generated
     */
    EClass PLUGIN_MANAGEMENT = eINSTANCE.getPluginManagement();

    /**
     * The meta object literal for the '<em><b>Plugins</b></em>' containment reference list feature. <!-- begin-user-doc
     * --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference PLUGIN_MANAGEMENT__PLUGINS = eINSTANCE.getPluginManagement_Plugins();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.PrerequisitesImpl
     * <em>Prerequisites</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.PrerequisitesImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getPrerequisites()
     * @generated
     */
    EClass PREREQUISITES = eINSTANCE.getPrerequisites();

    /**
     * The meta object literal for the '<em><b>Maven</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute PREREQUISITES__MAVEN = eINSTANCE.getPrerequisites_Maven();

    /**
     * The meta object literal for the ' {@link org.eclipse.m2e.model.edit.pom.impl.ProfileImpl <em>Profile</em>}'
     * class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.ProfileImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getProfile()
     * @generated
     */
    EClass PROFILE = eINSTANCE.getProfile();

    /**
     * The meta object literal for the '<em><b>Id</b></em>' attribute feature. <!-- begin-user-doc --> <!-- end-user-doc
     * -->
     * 
     * @generated
     */
    EAttribute PROFILE__ID = eINSTANCE.getProfile_Id();

    /**
     * The meta object literal for the '<em><b>Activation</b></em>' containment reference feature. <!-- begin-user-doc
     * --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference PROFILE__ACTIVATION = eINSTANCE.getProfile_Activation();

    /**
     * The meta object literal for the '<em><b>Build</b></em>' containment reference feature. <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference PROFILE__BUILD = eINSTANCE.getProfile_Build();

    /**
     * The meta object literal for the '<em><b>Repositories</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference PROFILE__REPOSITORIES = eINSTANCE.getProfile_Repositories();

    /**
     * The meta object literal for the '<em><b>Plugin Repositories</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference PROFILE__PLUGIN_REPOSITORIES = eINSTANCE.getProfile_PluginRepositories();

    /**
     * The meta object literal for the '<em><b>Dependencies</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference PROFILE__DEPENDENCIES = eINSTANCE.getProfile_Dependencies();

    /**
     * The meta object literal for the '<em><b>Reports</b></em>' containment reference list feature. <!-- begin-user-doc
     * --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference PROFILE__REPORTS = eINSTANCE.getProfile_Reports();

    /**
     * The meta object literal for the '<em><b>Dependency Management</b></em>' containment reference feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference PROFILE__DEPENDENCY_MANAGEMENT = eINSTANCE.getProfile_DependencyManagement();

    /**
     * The meta object literal for the '<em><b>Distribution Management</b></em>' containment reference feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference PROFILE__DISTRIBUTION_MANAGEMENT = eINSTANCE.getProfile_DistributionManagement();

    /**
     * The meta object literal for the '<em><b>Properties</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference PROFILE__PROPERTIES = eINSTANCE.getProfile_Properties();

    /**
     * The meta object literal for the '<em><b>Modules</b></em>' attribute list feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute PROFILE__MODULES = eINSTANCE.getProfile_Modules();

    /**
     * The meta object literal for the '<em><b>Reporting</b></em>' reference feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EReference PROFILE__REPORTING = eINSTANCE.getProfile_Reporting();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.RelocationImpl <em>Relocation</em>}'
     * class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.RelocationImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getRelocation()
     * @generated
     */
    EClass RELOCATION = eINSTANCE.getRelocation();

    /**
     * The meta object literal for the '<em><b>Group Id</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute RELOCATION__GROUP_ID = eINSTANCE.getRelocation_GroupId();

    /**
     * The meta object literal for the '<em><b>Artifact Id</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute RELOCATION__ARTIFACT_ID = eINSTANCE.getRelocation_ArtifactId();

    /**
     * The meta object literal for the '<em><b>Version</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute RELOCATION__VERSION = eINSTANCE.getRelocation_Version();

    /**
     * The meta object literal for the '<em><b>Message</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute RELOCATION__MESSAGE = eINSTANCE.getRelocation_Message();

    /**
     * The meta object literal for the ' {@link org.eclipse.m2e.model.edit.pom.impl.ReportingImpl <em>Reporting</em>}'
     * class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.ReportingImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getReporting()
     * @generated
     */
    EClass REPORTING = eINSTANCE.getReporting();

    /**
     * The meta object literal for the '<em><b>Exclude Defaults</b></em>' attribute feature. <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * 
     * @generated
     */
    EAttribute REPORTING__EXCLUDE_DEFAULTS = eINSTANCE.getReporting_ExcludeDefaults();

    /**
     * The meta object literal for the '<em><b>Output Directory</b></em>' attribute feature. <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * 
     * @generated
     */
    EAttribute REPORTING__OUTPUT_DIRECTORY = eINSTANCE.getReporting_OutputDirectory();

    /**
     * The meta object literal for the '<em><b>Plugins</b></em>' containment reference list feature. <!-- begin-user-doc
     * --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference REPORTING__PLUGINS = eINSTANCE.getReporting_Plugins();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.ReportPluginImpl
     * <em>Report Plugin</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.ReportPluginImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getReportPlugin()
     * @generated
     */
    EClass REPORT_PLUGIN = eINSTANCE.getReportPlugin();

    /**
     * The meta object literal for the '<em><b>Group Id</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute REPORT_PLUGIN__GROUP_ID = eINSTANCE.getReportPlugin_GroupId();

    /**
     * The meta object literal for the '<em><b>Artifact Id</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute REPORT_PLUGIN__ARTIFACT_ID = eINSTANCE.getReportPlugin_ArtifactId();

    /**
     * The meta object literal for the '<em><b>Version</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute REPORT_PLUGIN__VERSION = eINSTANCE.getReportPlugin_Version();

    /**
     * The meta object literal for the '<em><b>Inherited</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute REPORT_PLUGIN__INHERITED = eINSTANCE.getReportPlugin_Inherited();

    /**
     * The meta object literal for the '<em><b>Report Sets</b></em>' containment reference list feature. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference REPORT_PLUGIN__REPORT_SETS = eINSTANCE.getReportPlugin_ReportSets();

    /**
     * The meta object literal for the '<em><b>Configuration</b></em>' reference feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EReference REPORT_PLUGIN__CONFIGURATION = eINSTANCE.getReportPlugin_Configuration();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.ReportSetImpl <em>Report Set</em>}'
     * class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.ReportSetImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getReportSet()
     * @generated
     */
    EClass REPORT_SET = eINSTANCE.getReportSet();

    /**
     * The meta object literal for the '<em><b>Id</b></em>' attribute feature. <!-- begin-user-doc --> <!-- end-user-doc
     * -->
     * 
     * @generated
     */
    EAttribute REPORT_SET__ID = eINSTANCE.getReportSet_Id();

    /**
     * The meta object literal for the '<em><b>Inherited</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute REPORT_SET__INHERITED = eINSTANCE.getReportSet_Inherited();

    /**
     * The meta object literal for the '<em><b>Reports</b></em>' attribute list feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute REPORT_SET__REPORTS = eINSTANCE.getReportSet_Reports();

    /**
     * The meta object literal for the '<em><b>Configuration</b></em>' reference feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EReference REPORT_SET__CONFIGURATION = eINSTANCE.getReportSet_Configuration();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.RepositoryImpl <em>Repository</em>}'
     * class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.RepositoryImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getRepository()
     * @generated
     */
    EClass REPOSITORY = eINSTANCE.getRepository();

    /**
     * The meta object literal for the '<em><b>Releases</b></em>' containment reference feature. <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference REPOSITORY__RELEASES = eINSTANCE.getRepository_Releases();

    /**
     * The meta object literal for the '<em><b>Snapshots</b></em>' containment reference feature. <!-- begin-user-doc
     * --> <!-- end-user-doc -->
     * 
     * @generated
     */
    EReference REPOSITORY__SNAPSHOTS = eINSTANCE.getRepository_Snapshots();

    /**
     * The meta object literal for the '<em><b>Id</b></em>' attribute feature. <!-- begin-user-doc --> <!-- end-user-doc
     * -->
     * 
     * @generated
     */
    EAttribute REPOSITORY__ID = eINSTANCE.getRepository_Id();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute REPOSITORY__NAME = eINSTANCE.getRepository_Name();

    /**
     * The meta object literal for the '<em><b>Url</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute REPOSITORY__URL = eINSTANCE.getRepository_Url();

    /**
     * The meta object literal for the '<em><b>Layout</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute REPOSITORY__LAYOUT = eINSTANCE.getRepository_Layout();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.RepositoryPolicyImpl
     * <em>Repository Policy</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.RepositoryPolicyImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getRepositoryPolicy()
     * @generated
     */
    EClass REPOSITORY_POLICY = eINSTANCE.getRepositoryPolicy();

    /**
     * The meta object literal for the '<em><b>Enabled</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute REPOSITORY_POLICY__ENABLED = eINSTANCE.getRepositoryPolicy_Enabled();

    /**
     * The meta object literal for the '<em><b>Update Policy</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute REPOSITORY_POLICY__UPDATE_POLICY = eINSTANCE.getRepositoryPolicy_UpdatePolicy();

    /**
     * The meta object literal for the '<em><b>Checksum Policy</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute REPOSITORY_POLICY__CHECKSUM_POLICY = eINSTANCE.getRepositoryPolicy_ChecksumPolicy();

    /**
     * The meta object literal for the ' {@link org.eclipse.m2e.model.edit.pom.impl.ResourceImpl <em>Resource</em>}'
     * class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.ResourceImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getResource()
     * @generated
     */
    EClass RESOURCE = eINSTANCE.getResource();

    /**
     * The meta object literal for the '<em><b>Target Path</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute RESOURCE__TARGET_PATH = eINSTANCE.getResource_TargetPath();

    /**
     * The meta object literal for the '<em><b>Filtering</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute RESOURCE__FILTERING = eINSTANCE.getResource_Filtering();

    /**
     * The meta object literal for the '<em><b>Directory</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute RESOURCE__DIRECTORY = eINSTANCE.getResource_Directory();

    /**
     * The meta object literal for the '<em><b>Includes</b></em>' attribute list feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute RESOURCE__INCLUDES = eINSTANCE.getResource_Includes();

    /**
     * The meta object literal for the '<em><b>Excludes</b></em>' attribute list feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute RESOURCE__EXCLUDES = eINSTANCE.getResource_Excludes();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.ScmImpl <em>Scm</em>}' class. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.ScmImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getScm()
     * @generated
     */
    EClass SCM = eINSTANCE.getScm();

    /**
     * The meta object literal for the '<em><b>Connection</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute SCM__CONNECTION = eINSTANCE.getScm_Connection();

    /**
     * The meta object literal for the '<em><b>Developer Connection</b></em>' attribute feature. <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * 
     * @generated
     */
    EAttribute SCM__DEVELOPER_CONNECTION = eINSTANCE.getScm_DeveloperConnection();

    /**
     * The meta object literal for the '<em><b>Tag</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute SCM__TAG = eINSTANCE.getScm_Tag();

    /**
     * The meta object literal for the '<em><b>Url</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute SCM__URL = eINSTANCE.getScm_Url();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.SiteImpl <em>Site</em>}' class. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.SiteImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getSite()
     * @generated
     */
    EClass SITE = eINSTANCE.getSite();

    /**
     * The meta object literal for the '<em><b>Id</b></em>' attribute feature. <!-- begin-user-doc --> <!-- end-user-doc
     * -->
     * 
     * @generated
     */
    EAttribute SITE__ID = eINSTANCE.getSite_Id();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute SITE__NAME = eINSTANCE.getSite_Name();

    /**
     * The meta object literal for the '<em><b>Url</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute SITE__URL = eINSTANCE.getSite_Url();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.PropertyElementImpl
     * <em>Property Element</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.PropertyElementImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getPropertyElement()
     * @generated
     */
    EClass PROPERTY_ELEMENT = eINSTANCE.getPropertyElement();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute PROPERTY_ELEMENT__NAME = eINSTANCE.getPropertyElement_Name();

    /**
     * The meta object literal for the '<em><b>Value</b></em>' attribute feature. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    EAttribute PROPERTY_ELEMENT__VALUE = eINSTANCE.getPropertyElement_Value();

    /**
     * The meta object literal for the '{@link org.eclipse.m2e.model.edit.pom.impl.ConfigurationImpl
     * <em>Configuration</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see org.eclipse.m2e.model.edit.pom.impl.ConfigurationImpl
     * @see org.eclipse.m2e.model.edit.pom.impl.PomPackageImpl#getConfiguration()
     * @generated
     */
    EClass CONFIGURATION = eINSTANCE.getConfiguration();

  }

} // PomPackage
