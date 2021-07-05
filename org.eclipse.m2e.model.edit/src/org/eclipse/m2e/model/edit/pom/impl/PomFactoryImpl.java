/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.model.edit.pom.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EFactoryImpl;
import org.eclipse.emf.ecore.plugin.EcorePlugin;

import org.eclipse.m2e.model.edit.pom.Activation;
import org.eclipse.m2e.model.edit.pom.ActivationFile;
import org.eclipse.m2e.model.edit.pom.ActivationOS;
import org.eclipse.m2e.model.edit.pom.ActivationProperty;
import org.eclipse.m2e.model.edit.pom.Build;
import org.eclipse.m2e.model.edit.pom.BuildBase;
import org.eclipse.m2e.model.edit.pom.CiManagement;
import org.eclipse.m2e.model.edit.pom.Configuration;
import org.eclipse.m2e.model.edit.pom.Contributor;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.DependencyManagement;
import org.eclipse.m2e.model.edit.pom.DeploymentRepository;
import org.eclipse.m2e.model.edit.pom.Developer;
import org.eclipse.m2e.model.edit.pom.DistributionManagement;
import org.eclipse.m2e.model.edit.pom.DocumentRoot;
import org.eclipse.m2e.model.edit.pom.Exclusion;
import org.eclipse.m2e.model.edit.pom.Extension;
import org.eclipse.m2e.model.edit.pom.IssueManagement;
import org.eclipse.m2e.model.edit.pom.License;
import org.eclipse.m2e.model.edit.pom.MailingList;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.Notifier;
import org.eclipse.m2e.model.edit.pom.Organization;
import org.eclipse.m2e.model.edit.pom.Parent;
import org.eclipse.m2e.model.edit.pom.Plugin;
import org.eclipse.m2e.model.edit.pom.PluginExecution;
import org.eclipse.m2e.model.edit.pom.PluginManagement;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.Prerequisites;
import org.eclipse.m2e.model.edit.pom.Profile;
import org.eclipse.m2e.model.edit.pom.PropertyElement;
import org.eclipse.m2e.model.edit.pom.Relocation;
import org.eclipse.m2e.model.edit.pom.ReportPlugin;
import org.eclipse.m2e.model.edit.pom.ReportSet;
import org.eclipse.m2e.model.edit.pom.Reporting;
import org.eclipse.m2e.model.edit.pom.Repository;
import org.eclipse.m2e.model.edit.pom.RepositoryPolicy;
import org.eclipse.m2e.model.edit.pom.Resource;
import org.eclipse.m2e.model.edit.pom.Scm;
import org.eclipse.m2e.model.edit.pom.Site;


/**
 * <!-- begin-user-doc --> An implementation of the model <b>Factory</b>. <!-- end-user-doc -->
 *
 * @generated
 */
public class PomFactoryImpl extends EFactoryImpl implements PomFactory {
  /**
   * Creates the default factory implementation. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public static PomFactory init() {
    try {
      PomFactory thePomFactory = (PomFactory) EPackage.Registry.INSTANCE
          .getEFactory("http://maven.apache.org/POM/4.0.0"); //$NON-NLS-1$
      if(thePomFactory != null) {
        return thePomFactory;
      }
    } catch(Exception exception) {
      EcorePlugin.INSTANCE.log(exception);
    }
    return new PomFactoryImpl();
  }

  /**
   * Creates an instance of the factory. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public PomFactoryImpl() {
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public EObject create(EClass eClass) {
    switch(eClass.getClassifierID()) {
      case PomPackage.ACTIVATION:
        return createActivation();
      case PomPackage.ACTIVATION_FILE:
        return createActivationFile();
      case PomPackage.ACTIVATION_OS:
        return createActivationOS();
      case PomPackage.ACTIVATION_PROPERTY:
        return createActivationProperty();
      case PomPackage.BUILD:
        return createBuild();
      case PomPackage.BUILD_BASE:
        return createBuildBase();
      case PomPackage.CI_MANAGEMENT:
        return createCiManagement();
      case PomPackage.CONTRIBUTOR:
        return createContributor();
      case PomPackage.DEPENDENCY:
        return createDependency();
      case PomPackage.DEPENDENCY_MANAGEMENT:
        return createDependencyManagement();
      case PomPackage.DEPLOYMENT_REPOSITORY:
        return createDeploymentRepository();
      case PomPackage.DEVELOPER:
        return createDeveloper();
      case PomPackage.DISTRIBUTION_MANAGEMENT:
        return createDistributionManagement();
      case PomPackage.DOCUMENT_ROOT:
        return createDocumentRoot();
      case PomPackage.EXCLUSION:
        return createExclusion();
      case PomPackage.EXTENSION:
        return createExtension();
      case PomPackage.ISSUE_MANAGEMENT:
        return createIssueManagement();
      case PomPackage.LICENSE:
        return createLicense();
      case PomPackage.MAILING_LIST:
        return createMailingList();
      case PomPackage.MODEL:
        return createModel();
      case PomPackage.NOTIFIER:
        return createNotifier();
      case PomPackage.ORGANIZATION:
        return createOrganization();
      case PomPackage.PARENT:
        return createParent();
      case PomPackage.PLUGIN:
        return createPlugin();
      case PomPackage.PLUGIN_EXECUTION:
        return createPluginExecution();
      case PomPackage.PLUGIN_MANAGEMENT:
        return createPluginManagement();
      case PomPackage.PREREQUISITES:
        return createPrerequisites();
      case PomPackage.PROFILE:
        return createProfile();
      case PomPackage.RELOCATION:
        return createRelocation();
      case PomPackage.REPORTING:
        return createReporting();
      case PomPackage.REPORT_PLUGIN:
        return createReportPlugin();
      case PomPackage.REPORT_SET:
        return createReportSet();
      case PomPackage.REPOSITORY:
        return createRepository();
      case PomPackage.REPOSITORY_POLICY:
        return createRepositoryPolicy();
      case PomPackage.RESOURCE:
        return createResource();
      case PomPackage.SCM:
        return createScm();
      case PomPackage.SITE:
        return createSite();
      case PomPackage.PROPERTY_ELEMENT:
        return createPropertyElement();
      case PomPackage.CONFIGURATION:
        return createConfiguration();
      default:
        throw new IllegalArgumentException("The class '" + eClass.getName() //$NON-NLS-1$
            + "' is not a valid classifier"); //$NON-NLS-1$
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Activation createActivation() {
    ActivationImpl activation = new ActivationImpl();
    return activation;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public ActivationFile createActivationFile() {
    ActivationFileImpl activationFile = new ActivationFileImpl();
    return activationFile;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public ActivationOS createActivationOS() {
    ActivationOSImpl activationOS = new ActivationOSImpl();
    return activationOS;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public ActivationProperty createActivationProperty() {
    ActivationPropertyImpl activationProperty = new ActivationPropertyImpl();
    return activationProperty;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Build createBuild() {
    BuildImpl build = new BuildImpl();
    return build;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public BuildBase createBuildBase() {
    BuildBaseImpl buildBase = new BuildBaseImpl();
    return buildBase;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public CiManagement createCiManagement() {
    CiManagementImpl ciManagement = new CiManagementImpl();
    return ciManagement;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Contributor createContributor() {
    ContributorImpl contributor = new ContributorImpl();
    return contributor;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Dependency createDependency() {
    DependencyImpl dependency = new DependencyImpl();
    return dependency;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public DependencyManagement createDependencyManagement() {
    DependencyManagementImpl dependencyManagement = new DependencyManagementImpl();
    return dependencyManagement;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public DeploymentRepository createDeploymentRepository() {
    DeploymentRepositoryImpl deploymentRepository = new DeploymentRepositoryImpl();
    return deploymentRepository;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Developer createDeveloper() {
    DeveloperImpl developer = new DeveloperImpl();
    return developer;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public DistributionManagement createDistributionManagement() {
    DistributionManagementImpl distributionManagement = new DistributionManagementImpl();
    return distributionManagement;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public DocumentRoot createDocumentRoot() {
    DocumentRootImpl documentRoot = new DocumentRootImpl();
    return documentRoot;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Exclusion createExclusion() {
    ExclusionImpl exclusion = new ExclusionImpl();
    return exclusion;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Extension createExtension() {
    ExtensionImpl extension = new ExtensionImpl();
    return extension;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public IssueManagement createIssueManagement() {
    IssueManagementImpl issueManagement = new IssueManagementImpl();
    return issueManagement;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public License createLicense() {
    LicenseImpl license = new LicenseImpl();
    return license;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public MailingList createMailingList() {
    MailingListImpl mailingList = new MailingListImpl();
    return mailingList;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Model createModel() {
    ModelImpl model = new ModelImpl();
    return model;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Notifier createNotifier() {
    NotifierImpl notifier = new NotifierImpl();
    return notifier;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Organization createOrganization() {
    OrganizationImpl organization = new OrganizationImpl();
    return organization;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Parent createParent() {
    ParentImpl parent = new ParentImpl();
    return parent;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Plugin createPlugin() {
    PluginImpl plugin = new PluginImpl();
    return plugin;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public PluginExecution createPluginExecution() {
    PluginExecutionImpl pluginExecution = new PluginExecutionImpl();
    return pluginExecution;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public PluginManagement createPluginManagement() {
    PluginManagementImpl pluginManagement = new PluginManagementImpl();
    return pluginManagement;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Prerequisites createPrerequisites() {
    PrerequisitesImpl prerequisites = new PrerequisitesImpl();
    return prerequisites;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Profile createProfile() {
    ProfileImpl profile = new ProfileImpl();
    return profile;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Relocation createRelocation() {
    RelocationImpl relocation = new RelocationImpl();
    return relocation;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Reporting createReporting() {
    ReportingImpl reporting = new ReportingImpl();
    return reporting;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public ReportPlugin createReportPlugin() {
    ReportPluginImpl reportPlugin = new ReportPluginImpl();
    return reportPlugin;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public ReportSet createReportSet() {
    ReportSetImpl reportSet = new ReportSetImpl();
    return reportSet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Repository createRepository() {
    RepositoryImpl repository = new RepositoryImpl();
    return repository;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public RepositoryPolicy createRepositoryPolicy() {
    RepositoryPolicyImpl repositoryPolicy = new RepositoryPolicyImpl();
    return repositoryPolicy;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Resource createResource() {
    ResourceImpl resource = new ResourceImpl();
    return resource;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Scm createScm() {
    ScmImpl scm = new ScmImpl();
    return scm;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Site createSite() {
    SiteImpl site = new SiteImpl();
    return site;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public PropertyElement createPropertyElement() {
    PropertyElementImpl propertyElement = new PropertyElementImpl();
    return propertyElement;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Configuration createConfiguration() {
    ConfigurationImpl configuration = new ConfigurationImpl();
    return configuration;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public PomPackage getPomPackage() {
    return (PomPackage) getEPackage();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @deprecated
   * @generated
   */
  @Deprecated
  public static PomPackage getPackage() {
    return PomPackage.eINSTANCE;
  }

} // PomFactoryImpl
