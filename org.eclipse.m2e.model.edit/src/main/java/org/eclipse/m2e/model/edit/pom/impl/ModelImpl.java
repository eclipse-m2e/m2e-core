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

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.util.EDataTypeEList;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.m2e.model.edit.pom.Build;
import org.eclipse.m2e.model.edit.pom.CiManagement;
import org.eclipse.m2e.model.edit.pom.Contributor;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.DependencyManagement;
import org.eclipse.m2e.model.edit.pom.Developer;
import org.eclipse.m2e.model.edit.pom.DistributionManagement;
import org.eclipse.m2e.model.edit.pom.IssueManagement;
import org.eclipse.m2e.model.edit.pom.License;
import org.eclipse.m2e.model.edit.pom.MailingList;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.Organization;
import org.eclipse.m2e.model.edit.pom.Parent;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.Prerequisites;
import org.eclipse.m2e.model.edit.pom.Profile;
import org.eclipse.m2e.model.edit.pom.PropertyElement;
import org.eclipse.m2e.model.edit.pom.Reporting;
import org.eclipse.m2e.model.edit.pom.Repository;
import org.eclipse.m2e.model.edit.pom.Scm;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Model</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getParent <em>Parent </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getModelVersion <em> Model Version</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getGroupId <em>Group Id</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getArtifactId <em> Artifact Id</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getPackaging <em> Packaging</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getName <em>Name</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getVersion <em>Version </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getDescription <em> Description</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getUrl <em>Url</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getPrerequisites <em> Prerequisites</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getIssueManagement <em>Issue Management</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getCiManagement <em>Ci Management</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getInceptionYear <em> Inception Year</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getMailingLists <em> Mailing Lists</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getDevelopers <em> Developers</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getContributors <em> Contributors</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getLicenses <em> Licenses</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getScm <em>Scm</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getOrganization <em> Organization</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getBuild <em>Build </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getProfiles <em> Profiles</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getRepositories <em> Repositories</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getPluginRepositories <em>Plugin Repositories</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getDependencies <em> Dependencies</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getReporting <em> Reporting</em>}</li>
 * <li>
 * {@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getDependencyManagement <em>Dependency Management</em>}</li>
 * <li>
 * {@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getDistributionManagement <em>Distribution Management</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getProperties <em> Properties</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ModelImpl#getModules <em>Modules </em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ModelImpl extends EObjectImpl implements Model {
  /**
   * The cached value of the '{@link #getParent() <em>Parent</em>}' containment reference. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getParent()
   * @generated
   * @ordered
   */
  protected Parent parent;

  /**
   * This is true if the Parent containment reference has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   * @ordered
   */
  protected boolean parentESet;

  /**
   * The default value of the '{@link #getModelVersion() <em>Model Version</em>}' attribute. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   *
   * @see #getModelVersion()
   * @generated
   * @ordered
   */
  protected static final String MODEL_VERSION_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getModelVersion() <em>Model Version</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getModelVersion()
   * @generated
   * @ordered
   */
  protected String modelVersion = MODEL_VERSION_EDEFAULT;

  /**
   * The default value of the '{@link #getGroupId() <em>Group Id</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getGroupId()
   * @generated
   * @ordered
   */
  protected static final String GROUP_ID_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getGroupId() <em>Group Id</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getGroupId()
   * @generated
   * @ordered
   */
  protected String groupId = GROUP_ID_EDEFAULT;

  /**
   * The default value of the '{@link #getArtifactId() <em>Artifact Id</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getArtifactId()
   * @generated
   * @ordered
   */
  protected static final String ARTIFACT_ID_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getArtifactId() <em>Artifact Id</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getArtifactId()
   * @generated
   * @ordered
   */
  protected String artifactId = ARTIFACT_ID_EDEFAULT;

  /**
   * The default value of the '{@link #getPackaging() <em>Packaging</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getPackaging()
   * @generated
   * @ordered
   */
  protected static final String PACKAGING_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getPackaging() <em>Packaging</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getPackaging()
   * @generated
   * @ordered
   */
  protected String packaging = PACKAGING_EDEFAULT;

  /**
   * This is true if the Packaging attribute has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   * @ordered
   */
  protected boolean packagingESet;

  /**
   * The default value of the '{@link #getName() <em>Name</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   *
   * @see #getName()
   * @generated
   * @ordered
   */
  protected static final String NAME_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getName() <em>Name</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getName()
   * @generated
   * @ordered
   */
  protected String name = NAME_EDEFAULT;

  /**
   * The default value of the '{@link #getVersion() <em>Version</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getVersion()
   * @generated
   * @ordered
   */
  protected static final String VERSION_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getVersion() <em>Version</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getVersion()
   * @generated
   * @ordered
   */
  protected String version = VERSION_EDEFAULT;

  /**
   * The default value of the '{@link #getDescription() <em>Description</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getDescription()
   * @generated
   * @ordered
   */
  protected static final String DESCRIPTION_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getDescription() <em>Description</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getDescription()
   * @generated
   * @ordered
   */
  protected String description = DESCRIPTION_EDEFAULT;

  /**
   * The default value of the '{@link #getUrl() <em>Url</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getUrl()
   * @generated
   * @ordered
   */
  protected static final String URL_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getUrl() <em>Url</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getUrl()
   * @generated
   * @ordered
   */
  protected String url = URL_EDEFAULT;

  /**
   * The cached value of the '{@link #getPrerequisites() <em>Prerequisites</em>}' containment reference. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getPrerequisites()
   * @generated
   * @ordered
   */
  protected Prerequisites prerequisites;

  /**
   * This is true if the Prerequisites containment reference has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   * @ordered
   */
  protected boolean prerequisitesESet;

  /**
   * The cached value of the '{@link #getIssueManagement() <em>Issue Management</em>}' containment reference. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getIssueManagement()
   * @generated
   * @ordered
   */
  protected IssueManagement issueManagement;

  /**
   * This is true if the Issue Management containment reference has been set. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   *
   * @generated
   * @ordered
   */
  protected boolean issueManagementESet;

  /**
   * The cached value of the '{@link #getCiManagement() <em>Ci Management</em>}' containment reference. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getCiManagement()
   * @generated
   * @ordered
   */
  protected CiManagement ciManagement;

  /**
   * This is true if the Ci Management containment reference has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   * @ordered
   */
  protected boolean ciManagementESet;

  /**
   * The default value of the '{@link #getInceptionYear() <em>Inception Year</em>}' attribute. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   *
   * @see #getInceptionYear()
   * @generated
   * @ordered
   */
  protected static final String INCEPTION_YEAR_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getInceptionYear() <em>Inception Year</em>}' attribute. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   *
   * @see #getInceptionYear()
   * @generated
   * @ordered
   */
  protected String inceptionYear = INCEPTION_YEAR_EDEFAULT;

  /**
   * The cached value of the '{@link #getMailingLists() <em>Mailing Lists</em>}' containment reference list. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getMailingLists()
   * @generated
   * @ordered
   */
  protected EList<MailingList> mailingLists;

  /**
   * The cached value of the '{@link #getDevelopers() <em>Developers</em>}' containment reference list. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getDevelopers()
   * @generated
   * @ordered
   */
  protected EList<Developer> developers;

  /**
   * The cached value of the '{@link #getContributors() <em>Contributors</em>} ' containment reference list. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getContributors()
   * @generated
   * @ordered
   */
  protected EList<Contributor> contributors;

  /**
   * The cached value of the '{@link #getLicenses() <em>Licenses</em>}' containment reference list. <!-- begin-user-doc
   * --> <!-- end-user-doc -->
   *
   * @see #getLicenses()
   * @generated
   * @ordered
   */
  protected EList<License> licenses;

  /**
   * The cached value of the '{@link #getScm() <em>Scm</em>}' containment reference. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getScm()
   * @generated
   * @ordered
   */
  protected Scm scm;

  /**
   * This is true if the Scm containment reference has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   * @ordered
   */
  protected boolean scmESet;

  /**
   * The cached value of the '{@link #getOrganization() <em>Organization</em>} ' containment reference. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getOrganization()
   * @generated
   * @ordered
   */
  protected Organization organization;

  /**
   * This is true if the Organization containment reference has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   * @ordered
   */
  protected boolean organizationESet;

  /**
   * The cached value of the '{@link #getBuild() <em>Build</em>}' containment reference. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getBuild()
   * @generated
   * @ordered
   */
  protected Build build;

  /**
   * This is true if the Build containment reference has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   * @ordered
   */
  protected boolean buildESet;

  /**
   * The cached value of the '{@link #getProfiles() <em>Profiles</em>}' containment reference list. <!-- begin-user-doc
   * --> <!-- end-user-doc -->
   *
   * @see #getProfiles()
   * @generated
   * @ordered
   */
  protected EList<Profile> profiles;

  /**
   * The cached value of the '{@link #getRepositories() <em>Repositories</em>} ' containment reference list. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getRepositories()
   * @generated
   * @ordered
   */
  protected EList<Repository> repositories;

  /**
   * The cached value of the '{@link #getPluginRepositories() <em>Plugin Repositories</em>}' containment reference list.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getPluginRepositories()
   * @generated
   * @ordered
   */
  protected EList<Repository> pluginRepositories;

  /**
   * The cached value of the '{@link #getDependencies() <em>Dependencies</em>} ' containment reference list. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getDependencies()
   * @generated
   * @ordered
   */
  protected EList<Dependency> dependencies;

  /**
   * The cached value of the '{@link #getReporting() <em>Reporting</em>}' containment reference. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   *
   * @see #getReporting()
   * @generated
   * @ordered
   */
  protected Reporting reporting;

  /**
   * This is true if the Reporting containment reference has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   * @ordered
   */
  protected boolean reportingESet;

  /**
   * The cached value of the '{@link #getDependencyManagement() <em>Dependency Management</em>}' containment reference.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getDependencyManagement()
   * @generated
   * @ordered
   */
  protected DependencyManagement dependencyManagement;

  /**
   * This is true if the Dependency Management containment reference has been set. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @generated
   * @ordered
   */
  protected boolean dependencyManagementESet;

  /**
   * The cached value of the '{@link #getDistributionManagement() <em>Distribution Management</em>}' containment
   * reference. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getDistributionManagement()
   * @generated
   * @ordered
   */
  protected DistributionManagement distributionManagement;

  /**
   * This is true if the Distribution Management containment reference has been set. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @generated
   * @ordered
   */
  protected boolean distributionManagementESet;

  /**
   * The cached value of the '{@link #getProperties() <em>Properties</em>}' containment reference list. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getProperties()
   * @generated
   * @ordered
   */
  protected EList<PropertyElement> properties;

  /**
   * The cached value of the '{@link #getModules() <em>Modules</em>}' attribute list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getModules()
   * @generated
   * @ordered
   */
  protected EList<String> modules;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  protected ModelImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.MODEL;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Parent getParent() {
    return parent;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicSetParent(Parent newParent, NotificationChain msgs) {
    Parent oldParent = parent;
    parent = newParent;
    boolean oldParentESet = parentESet;
    parentESet = true;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__PARENT,
          oldParent, newParent, !oldParentESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setParent(Parent newParent) {
    if(newParent != parent) {
      NotificationChain msgs = null;
      if(parent != null)
        msgs = ((InternalEObject) parent).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - PomPackage.MODEL__PARENT, null,
            msgs);
      if(newParent != null)
        msgs = ((InternalEObject) newParent).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - PomPackage.MODEL__PARENT, null,
            msgs);
      msgs = basicSetParent(newParent, msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldParentESet = parentESet;
      parentESet = true;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__PARENT, newParent, newParent,
            !oldParentESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicUnsetParent(NotificationChain msgs) {
    Parent oldParent = parent;
    parent = null;
    boolean oldParentESet = parentESet;
    parentESet = false;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.UNSET, PomPackage.MODEL__PARENT,
          oldParent, null, oldParentESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetParent() {
    if(parent != null) {
      NotificationChain msgs = null;
      msgs = ((InternalEObject) parent).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - PomPackage.MODEL__PARENT, null,
          msgs);
      msgs = basicUnsetParent(msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldParentESet = parentESet;
      parentESet = false;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.MODEL__PARENT, null, null, oldParentESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetParent() {
    return parentESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getModelVersion() {
    return modelVersion;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setModelVersion(String newModelVersion) {
    String oldModelVersion = modelVersion;
    modelVersion = newModelVersion;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__MODEL_VERSION, oldModelVersion,
          modelVersion));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setGroupId(String newGroupId) {
    String oldGroupId = groupId;
    groupId = newGroupId;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__GROUP_ID, oldGroupId, groupId));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getArtifactId() {
    return artifactId;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setArtifactId(String newArtifactId) {
    String oldArtifactId = artifactId;
    artifactId = newArtifactId;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__ARTIFACT_ID, oldArtifactId, artifactId));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getPackaging() {
    return packaging;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setPackaging(String newPackaging) {
    String oldPackaging = packaging;
    packaging = newPackaging;
    boolean oldPackagingESet = packagingESet;
    packagingESet = true;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__PACKAGING, oldPackaging, packaging,
          !oldPackagingESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetPackaging() {
    String oldPackaging = packaging;
    boolean oldPackagingESet = packagingESet;
    packaging = PACKAGING_EDEFAULT;
    packagingESet = false;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.MODEL__PACKAGING, oldPackaging,
          PACKAGING_EDEFAULT, oldPackagingESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetPackaging() {
    return packagingESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getName() {
    return name;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setName(String newName) {
    String oldName = name;
    name = newName;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__NAME, oldName, name));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getVersion() {
    return version;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setVersion(String newVersion) {
    String oldVersion = version;
    version = newVersion;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__VERSION, oldVersion, version));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getDescription() {
    return description;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setDescription(String newDescription) {
    String oldDescription = description;
    description = newDescription;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__DESCRIPTION, oldDescription, description));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getUrl() {
    return url;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setUrl(String newUrl) {
    String oldUrl = url;
    url = newUrl;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__URL, oldUrl, url));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Prerequisites getPrerequisites() {
    return prerequisites;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicSetPrerequisites(Prerequisites newPrerequisites, NotificationChain msgs) {
    Prerequisites oldPrerequisites = prerequisites;
    prerequisites = newPrerequisites;
    boolean oldPrerequisitesESet = prerequisitesESet;
    prerequisitesESet = true;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__PREREQUISITES,
          oldPrerequisites, newPrerequisites, !oldPrerequisitesESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setPrerequisites(Prerequisites newPrerequisites) {
    if(newPrerequisites != prerequisites) {
      NotificationChain msgs = null;
      if(prerequisites != null)
        msgs = ((InternalEObject) prerequisites).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.MODEL__PREREQUISITES, null, msgs);
      if(newPrerequisites != null)
        msgs = ((InternalEObject) newPrerequisites).eInverseAdd(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.MODEL__PREREQUISITES, null, msgs);
      msgs = basicSetPrerequisites(newPrerequisites, msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldPrerequisitesESet = prerequisitesESet;
      prerequisitesESet = true;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__PREREQUISITES, newPrerequisites,
            newPrerequisites, !oldPrerequisitesESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicUnsetPrerequisites(NotificationChain msgs) {
    Prerequisites oldPrerequisites = prerequisites;
    prerequisites = null;
    boolean oldPrerequisitesESet = prerequisitesESet;
    prerequisitesESet = false;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.UNSET, PomPackage.MODEL__PREREQUISITES,
          oldPrerequisites, null, oldPrerequisitesESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetPrerequisites() {
    if(prerequisites != null) {
      NotificationChain msgs = null;
      msgs = ((InternalEObject) prerequisites).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
          - PomPackage.MODEL__PREREQUISITES, null, msgs);
      msgs = basicUnsetPrerequisites(msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldPrerequisitesESet = prerequisitesESet;
      prerequisitesESet = false;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.MODEL__PREREQUISITES, null, null,
            oldPrerequisitesESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetPrerequisites() {
    return prerequisitesESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public IssueManagement getIssueManagement() {
    return issueManagement;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicSetIssueManagement(IssueManagement newIssueManagement, NotificationChain msgs) {
    IssueManagement oldIssueManagement = issueManagement;
    issueManagement = newIssueManagement;
    boolean oldIssueManagementESet = issueManagementESet;
    issueManagementESet = true;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET,
          PomPackage.MODEL__ISSUE_MANAGEMENT, oldIssueManagement, newIssueManagement, !oldIssueManagementESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setIssueManagement(IssueManagement newIssueManagement) {
    if(newIssueManagement != issueManagement) {
      NotificationChain msgs = null;
      if(issueManagement != null)
        msgs = ((InternalEObject) issueManagement).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.MODEL__ISSUE_MANAGEMENT, null, msgs);
      if(newIssueManagement != null)
        msgs = ((InternalEObject) newIssueManagement).eInverseAdd(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.MODEL__ISSUE_MANAGEMENT, null, msgs);
      msgs = basicSetIssueManagement(newIssueManagement, msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldIssueManagementESet = issueManagementESet;
      issueManagementESet = true;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__ISSUE_MANAGEMENT, newIssueManagement,
            newIssueManagement, !oldIssueManagementESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicUnsetIssueManagement(NotificationChain msgs) {
    IssueManagement oldIssueManagement = issueManagement;
    issueManagement = null;
    boolean oldIssueManagementESet = issueManagementESet;
    issueManagementESet = false;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.UNSET,
          PomPackage.MODEL__ISSUE_MANAGEMENT, oldIssueManagement, null, oldIssueManagementESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetIssueManagement() {
    if(issueManagement != null) {
      NotificationChain msgs = null;
      msgs = ((InternalEObject) issueManagement).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
          - PomPackage.MODEL__ISSUE_MANAGEMENT, null, msgs);
      msgs = basicUnsetIssueManagement(msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldIssueManagementESet = issueManagementESet;
      issueManagementESet = false;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.MODEL__ISSUE_MANAGEMENT, null, null,
            oldIssueManagementESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetIssueManagement() {
    return issueManagementESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public CiManagement getCiManagement() {
    return ciManagement;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicSetCiManagement(CiManagement newCiManagement, NotificationChain msgs) {
    CiManagement oldCiManagement = ciManagement;
    ciManagement = newCiManagement;
    boolean oldCiManagementESet = ciManagementESet;
    ciManagementESet = true;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__CI_MANAGEMENT,
          oldCiManagement, newCiManagement, !oldCiManagementESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setCiManagement(CiManagement newCiManagement) {
    if(newCiManagement != ciManagement) {
      NotificationChain msgs = null;
      if(ciManagement != null)
        msgs = ((InternalEObject) ciManagement).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.MODEL__CI_MANAGEMENT, null, msgs);
      if(newCiManagement != null)
        msgs = ((InternalEObject) newCiManagement).eInverseAdd(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.MODEL__CI_MANAGEMENT, null, msgs);
      msgs = basicSetCiManagement(newCiManagement, msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldCiManagementESet = ciManagementESet;
      ciManagementESet = true;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__CI_MANAGEMENT, newCiManagement,
            newCiManagement, !oldCiManagementESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicUnsetCiManagement(NotificationChain msgs) {
    CiManagement oldCiManagement = ciManagement;
    ciManagement = null;
    boolean oldCiManagementESet = ciManagementESet;
    ciManagementESet = false;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.UNSET, PomPackage.MODEL__CI_MANAGEMENT,
          oldCiManagement, null, oldCiManagementESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetCiManagement() {
    if(ciManagement != null) {
      NotificationChain msgs = null;
      msgs = ((InternalEObject) ciManagement).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
          - PomPackage.MODEL__CI_MANAGEMENT, null, msgs);
      msgs = basicUnsetCiManagement(msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldCiManagementESet = ciManagementESet;
      ciManagementESet = false;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.MODEL__CI_MANAGEMENT, null, null,
            oldCiManagementESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetCiManagement() {
    return ciManagementESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getInceptionYear() {
    return inceptionYear;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setInceptionYear(String newInceptionYear) {
    String oldInceptionYear = inceptionYear;
    inceptionYear = newInceptionYear;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__INCEPTION_YEAR, oldInceptionYear,
          inceptionYear));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<MailingList> getMailingLists() {
    if(mailingLists == null) {
      mailingLists = new EObjectContainmentEList.Unsettable<>(MailingList.class, this,
          PomPackage.MODEL__MAILING_LISTS);
    }
    return mailingLists;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetMailingLists() {
    if(mailingLists != null)
      ((InternalEList.Unsettable<?>) mailingLists).unset();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetMailingLists() {
    return mailingLists != null && ((InternalEList.Unsettable<?>) mailingLists).isSet();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<Developer> getDevelopers() {
    if(developers == null) {
      developers = new EObjectContainmentEList.Unsettable<>(Developer.class, this,
          PomPackage.MODEL__DEVELOPERS);
    }
    return developers;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetDevelopers() {
    if(developers != null)
      ((InternalEList.Unsettable<?>) developers).unset();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetDevelopers() {
    return developers != null && ((InternalEList.Unsettable<?>) developers).isSet();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<Contributor> getContributors() {
    if(contributors == null) {
      contributors = new EObjectContainmentEList.Unsettable<>(Contributor.class, this,
          PomPackage.MODEL__CONTRIBUTORS);
    }
    return contributors;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetContributors() {
    if(contributors != null)
      ((InternalEList.Unsettable<?>) contributors).unset();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetContributors() {
    return contributors != null && ((InternalEList.Unsettable<?>) contributors).isSet();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<License> getLicenses() {
    if(licenses == null) {
      licenses = new EObjectContainmentEList<>(License.class, this, PomPackage.MODEL__LICENSES);
    }
    return licenses;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Scm getScm() {
    return scm;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicSetScm(Scm newScm, NotificationChain msgs) {
    Scm oldScm = scm;
    scm = newScm;
    boolean oldScmESet = scmESet;
    scmESet = true;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__SCM, oldScm,
          newScm, !oldScmESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setScm(Scm newScm) {
    if(newScm != scm) {
      NotificationChain msgs = null;
      if(scm != null)
        msgs = ((InternalEObject) scm).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - PomPackage.MODEL__SCM, null, msgs);
      if(newScm != null)
        msgs = ((InternalEObject) newScm).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - PomPackage.MODEL__SCM, null, msgs);
      msgs = basicSetScm(newScm, msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldScmESet = scmESet;
      scmESet = true;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__SCM, newScm, newScm, !oldScmESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicUnsetScm(NotificationChain msgs) {
    Scm oldScm = scm;
    scm = null;
    boolean oldScmESet = scmESet;
    scmESet = false;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.UNSET, PomPackage.MODEL__SCM, oldScm,
          null, oldScmESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetScm() {
    if(scm != null) {
      NotificationChain msgs = null;
      msgs = ((InternalEObject) scm).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - PomPackage.MODEL__SCM, null, msgs);
      msgs = basicUnsetScm(msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldScmESet = scmESet;
      scmESet = false;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.MODEL__SCM, null, null, oldScmESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetScm() {
    return scmESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Organization getOrganization() {
    return organization;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicSetOrganization(Organization newOrganization, NotificationChain msgs) {
    Organization oldOrganization = organization;
    organization = newOrganization;
    boolean oldOrganizationESet = organizationESet;
    organizationESet = true;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__ORGANIZATION,
          oldOrganization, newOrganization, !oldOrganizationESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setOrganization(Organization newOrganization) {
    if(newOrganization != organization) {
      NotificationChain msgs = null;
      if(organization != null)
        msgs = ((InternalEObject) organization).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.MODEL__ORGANIZATION, null, msgs);
      if(newOrganization != null)
        msgs = ((InternalEObject) newOrganization).eInverseAdd(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.MODEL__ORGANIZATION, null, msgs);
      msgs = basicSetOrganization(newOrganization, msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldOrganizationESet = organizationESet;
      organizationESet = true;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__ORGANIZATION, newOrganization,
            newOrganization, !oldOrganizationESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicUnsetOrganization(NotificationChain msgs) {
    Organization oldOrganization = organization;
    organization = null;
    boolean oldOrganizationESet = organizationESet;
    organizationESet = false;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.UNSET, PomPackage.MODEL__ORGANIZATION,
          oldOrganization, null, oldOrganizationESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetOrganization() {
    if(organization != null) {
      NotificationChain msgs = null;
      msgs = ((InternalEObject) organization).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
          - PomPackage.MODEL__ORGANIZATION, null, msgs);
      msgs = basicUnsetOrganization(msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldOrganizationESet = organizationESet;
      organizationESet = false;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.MODEL__ORGANIZATION, null, null,
            oldOrganizationESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetOrganization() {
    return organizationESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Build getBuild() {
    return build;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicSetBuild(Build newBuild, NotificationChain msgs) {
    Build oldBuild = build;
    build = newBuild;
    boolean oldBuildESet = buildESet;
    buildESet = true;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__BUILD, oldBuild,
          newBuild, !oldBuildESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setBuild(Build newBuild) {
    if(newBuild != build) {
      NotificationChain msgs = null;
      if(build != null)
        msgs = ((InternalEObject) build).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - PomPackage.MODEL__BUILD, null,
            msgs);
      if(newBuild != null)
        msgs = ((InternalEObject) newBuild).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - PomPackage.MODEL__BUILD, null,
            msgs);
      msgs = basicSetBuild(newBuild, msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldBuildESet = buildESet;
      buildESet = true;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__BUILD, newBuild, newBuild,
            !oldBuildESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicUnsetBuild(NotificationChain msgs) {
    Build oldBuild = build;
    build = null;
    boolean oldBuildESet = buildESet;
    buildESet = false;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.UNSET, PomPackage.MODEL__BUILD,
          oldBuild, null, oldBuildESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetBuild() {
    if(build != null) {
      NotificationChain msgs = null;
      msgs = ((InternalEObject) build).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - PomPackage.MODEL__BUILD, null,
          msgs);
      msgs = basicUnsetBuild(msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldBuildESet = buildESet;
      buildESet = false;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.MODEL__BUILD, null, null, oldBuildESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetBuild() {
    return buildESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<Profile> getProfiles() {
    if(profiles == null) {
      profiles = new EObjectContainmentEList.Unsettable<>(Profile.class, this, PomPackage.MODEL__PROFILES);
    }
    return profiles;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetProfiles() {
    if(profiles != null)
      ((InternalEList.Unsettable<?>) profiles).unset();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetProfiles() {
    return profiles != null && ((InternalEList.Unsettable<?>) profiles).isSet();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<Repository> getRepositories() {
    if(repositories == null) {
      repositories = new EObjectContainmentEList.Unsettable<>(Repository.class, this,
          PomPackage.MODEL__REPOSITORIES);
    }
    return repositories;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetRepositories() {
    if(repositories != null)
      ((InternalEList.Unsettable<?>) repositories).unset();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetRepositories() {
    return repositories != null && ((InternalEList.Unsettable<?>) repositories).isSet();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<Repository> getPluginRepositories() {
    if(pluginRepositories == null) {
      pluginRepositories = new EObjectContainmentEList.Unsettable<>(Repository.class, this,
          PomPackage.MODEL__PLUGIN_REPOSITORIES);
    }
    return pluginRepositories;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetPluginRepositories() {
    if(pluginRepositories != null)
      ((InternalEList.Unsettable<?>) pluginRepositories).unset();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetPluginRepositories() {
    return pluginRepositories != null && ((InternalEList.Unsettable<?>) pluginRepositories).isSet();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<Dependency> getDependencies() {
    if(dependencies == null) {
      dependencies = new EObjectContainmentEList.Unsettable<>(Dependency.class, this,
          PomPackage.MODEL__DEPENDENCIES);
    }
    return dependencies;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetDependencies() {
    if(dependencies != null)
      ((InternalEList.Unsettable<?>) dependencies).unset();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetDependencies() {
    return dependencies != null && ((InternalEList.Unsettable<?>) dependencies).isSet();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Reporting getReporting() {
    return reporting;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicSetReporting(Reporting newReporting, NotificationChain msgs) {
    Reporting oldReporting = reporting;
    reporting = newReporting;
    boolean oldReportingESet = reportingESet;
    reportingESet = true;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__REPORTING,
          oldReporting, newReporting, !oldReportingESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setReporting(Reporting newReporting) {
    if(newReporting != reporting) {
      NotificationChain msgs = null;
      if(reporting != null)
        msgs = ((InternalEObject) reporting).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - PomPackage.MODEL__REPORTING,
            null, msgs);
      if(newReporting != null)
        msgs = ((InternalEObject) newReporting).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - PomPackage.MODEL__REPORTING,
            null, msgs);
      msgs = basicSetReporting(newReporting, msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldReportingESet = reportingESet;
      reportingESet = true;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__REPORTING, newReporting, newReporting,
            !oldReportingESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicUnsetReporting(NotificationChain msgs) {
    Reporting oldReporting = reporting;
    reporting = null;
    boolean oldReportingESet = reportingESet;
    reportingESet = false;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.UNSET, PomPackage.MODEL__REPORTING,
          oldReporting, null, oldReportingESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetReporting() {
    if(reporting != null) {
      NotificationChain msgs = null;
      msgs = ((InternalEObject) reporting).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - PomPackage.MODEL__REPORTING,
          null, msgs);
      msgs = basicUnsetReporting(msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldReportingESet = reportingESet;
      reportingESet = false;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.MODEL__REPORTING, null, null,
            oldReportingESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetReporting() {
    return reportingESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public DependencyManagement getDependencyManagement() {
    return dependencyManagement;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicSetDependencyManagement(DependencyManagement newDependencyManagement,
      NotificationChain msgs) {
    DependencyManagement oldDependencyManagement = dependencyManagement;
    dependencyManagement = newDependencyManagement;
    boolean oldDependencyManagementESet = dependencyManagementESet;
    dependencyManagementESet = true;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET,
          PomPackage.MODEL__DEPENDENCY_MANAGEMENT, oldDependencyManagement, newDependencyManagement,
          !oldDependencyManagementESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setDependencyManagement(DependencyManagement newDependencyManagement) {
    if(newDependencyManagement != dependencyManagement) {
      NotificationChain msgs = null;
      if(dependencyManagement != null)
        msgs = ((InternalEObject) dependencyManagement).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.MODEL__DEPENDENCY_MANAGEMENT, null, msgs);
      if(newDependencyManagement != null)
        msgs = ((InternalEObject) newDependencyManagement).eInverseAdd(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.MODEL__DEPENDENCY_MANAGEMENT, null, msgs);
      msgs = basicSetDependencyManagement(newDependencyManagement, msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldDependencyManagementESet = dependencyManagementESet;
      dependencyManagementESet = true;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__DEPENDENCY_MANAGEMENT,
            newDependencyManagement, newDependencyManagement, !oldDependencyManagementESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicUnsetDependencyManagement(NotificationChain msgs) {
    DependencyManagement oldDependencyManagement = dependencyManagement;
    dependencyManagement = null;
    boolean oldDependencyManagementESet = dependencyManagementESet;
    dependencyManagementESet = false;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.UNSET,
          PomPackage.MODEL__DEPENDENCY_MANAGEMENT, oldDependencyManagement, null, oldDependencyManagementESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetDependencyManagement() {
    if(dependencyManagement != null) {
      NotificationChain msgs = null;
      msgs = ((InternalEObject) dependencyManagement).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
          - PomPackage.MODEL__DEPENDENCY_MANAGEMENT, null, msgs);
      msgs = basicUnsetDependencyManagement(msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldDependencyManagementESet = dependencyManagementESet;
      dependencyManagementESet = false;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.MODEL__DEPENDENCY_MANAGEMENT, null, null,
            oldDependencyManagementESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetDependencyManagement() {
    return dependencyManagementESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public DistributionManagement getDistributionManagement() {
    return distributionManagement;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicSetDistributionManagement(DistributionManagement newDistributionManagement,
      NotificationChain msgs) {
    DistributionManagement oldDistributionManagement = distributionManagement;
    distributionManagement = newDistributionManagement;
    boolean oldDistributionManagementESet = distributionManagementESet;
    distributionManagementESet = true;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET,
          PomPackage.MODEL__DISTRIBUTION_MANAGEMENT, oldDistributionManagement, newDistributionManagement,
          !oldDistributionManagementESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setDistributionManagement(DistributionManagement newDistributionManagement) {
    if(newDistributionManagement != distributionManagement) {
      NotificationChain msgs = null;
      if(distributionManagement != null)
        msgs = ((InternalEObject) distributionManagement).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.MODEL__DISTRIBUTION_MANAGEMENT, null, msgs);
      if(newDistributionManagement != null)
        msgs = ((InternalEObject) newDistributionManagement).eInverseAdd(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.MODEL__DISTRIBUTION_MANAGEMENT, null, msgs);
      msgs = basicSetDistributionManagement(newDistributionManagement, msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldDistributionManagementESet = distributionManagementESet;
      distributionManagementESet = true;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MODEL__DISTRIBUTION_MANAGEMENT,
            newDistributionManagement, newDistributionManagement, !oldDistributionManagementESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicUnsetDistributionManagement(NotificationChain msgs) {
    DistributionManagement oldDistributionManagement = distributionManagement;
    distributionManagement = null;
    boolean oldDistributionManagementESet = distributionManagementESet;
    distributionManagementESet = false;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.UNSET,
          PomPackage.MODEL__DISTRIBUTION_MANAGEMENT, oldDistributionManagement, null, oldDistributionManagementESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetDistributionManagement() {
    if(distributionManagement != null) {
      NotificationChain msgs = null;
      msgs = ((InternalEObject) distributionManagement).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
          - PomPackage.MODEL__DISTRIBUTION_MANAGEMENT, null, msgs);
      msgs = basicUnsetDistributionManagement(msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldDistributionManagementESet = distributionManagementESet;
      distributionManagementESet = false;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.MODEL__DISTRIBUTION_MANAGEMENT, null, null,
            oldDistributionManagementESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetDistributionManagement() {
    return distributionManagementESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<PropertyElement> getProperties() {
    if(properties == null) {
      properties = new EObjectContainmentEList.Unsettable<>(PropertyElement.class, this,
          PomPackage.MODEL__PROPERTIES);
    }
    return properties;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetProperties() {
    if(properties != null)
      ((InternalEList.Unsettable<?>) properties).unset();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetProperties() {
    return properties != null && ((InternalEList.Unsettable<?>) properties).isSet();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<String> getModules() {
    if(modules == null) {
      modules = new EDataTypeEList<>(String.class, this, PomPackage.MODEL__MODULES);
    }
    return modules;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
    switch(featureID) {
      case PomPackage.MODEL__PARENT:
        return basicUnsetParent(msgs);
      case PomPackage.MODEL__PREREQUISITES:
        return basicUnsetPrerequisites(msgs);
      case PomPackage.MODEL__ISSUE_MANAGEMENT:
        return basicUnsetIssueManagement(msgs);
      case PomPackage.MODEL__CI_MANAGEMENT:
        return basicUnsetCiManagement(msgs);
      case PomPackage.MODEL__MAILING_LISTS:
        return ((InternalEList<?>) getMailingLists()).basicRemove(otherEnd, msgs);
      case PomPackage.MODEL__DEVELOPERS:
        return ((InternalEList<?>) getDevelopers()).basicRemove(otherEnd, msgs);
      case PomPackage.MODEL__CONTRIBUTORS:
        return ((InternalEList<?>) getContributors()).basicRemove(otherEnd, msgs);
      case PomPackage.MODEL__LICENSES:
        return ((InternalEList<?>) getLicenses()).basicRemove(otherEnd, msgs);
      case PomPackage.MODEL__SCM:
        return basicUnsetScm(msgs);
      case PomPackage.MODEL__ORGANIZATION:
        return basicUnsetOrganization(msgs);
      case PomPackage.MODEL__BUILD:
        return basicUnsetBuild(msgs);
      case PomPackage.MODEL__PROFILES:
        return ((InternalEList<?>) getProfiles()).basicRemove(otherEnd, msgs);
      case PomPackage.MODEL__REPOSITORIES:
        return ((InternalEList<?>) getRepositories()).basicRemove(otherEnd, msgs);
      case PomPackage.MODEL__PLUGIN_REPOSITORIES:
        return ((InternalEList<?>) getPluginRepositories()).basicRemove(otherEnd, msgs);
      case PomPackage.MODEL__DEPENDENCIES:
        return ((InternalEList<?>) getDependencies()).basicRemove(otherEnd, msgs);
      case PomPackage.MODEL__REPORTING:
        return basicUnsetReporting(msgs);
      case PomPackage.MODEL__DEPENDENCY_MANAGEMENT:
        return basicUnsetDependencyManagement(msgs);
      case PomPackage.MODEL__DISTRIBUTION_MANAGEMENT:
        return basicUnsetDistributionManagement(msgs);
      case PomPackage.MODEL__PROPERTIES:
        return ((InternalEList<?>) getProperties()).basicRemove(otherEnd, msgs);
    }
    return super.eInverseRemove(otherEnd, featureID, msgs);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch(featureID) {
      case PomPackage.MODEL__PARENT:
        return getParent();
      case PomPackage.MODEL__MODEL_VERSION:
        return getModelVersion();
      case PomPackage.MODEL__GROUP_ID:
        return getGroupId();
      case PomPackage.MODEL__ARTIFACT_ID:
        return getArtifactId();
      case PomPackage.MODEL__PACKAGING:
        return getPackaging();
      case PomPackage.MODEL__NAME:
        return getName();
      case PomPackage.MODEL__VERSION:
        return getVersion();
      case PomPackage.MODEL__DESCRIPTION:
        return getDescription();
      case PomPackage.MODEL__URL:
        return getUrl();
      case PomPackage.MODEL__PREREQUISITES:
        return getPrerequisites();
      case PomPackage.MODEL__ISSUE_MANAGEMENT:
        return getIssueManagement();
      case PomPackage.MODEL__CI_MANAGEMENT:
        return getCiManagement();
      case PomPackage.MODEL__INCEPTION_YEAR:
        return getInceptionYear();
      case PomPackage.MODEL__MAILING_LISTS:
        return getMailingLists();
      case PomPackage.MODEL__DEVELOPERS:
        return getDevelopers();
      case PomPackage.MODEL__CONTRIBUTORS:
        return getContributors();
      case PomPackage.MODEL__LICENSES:
        return getLicenses();
      case PomPackage.MODEL__SCM:
        return getScm();
      case PomPackage.MODEL__ORGANIZATION:
        return getOrganization();
      case PomPackage.MODEL__BUILD:
        return getBuild();
      case PomPackage.MODEL__PROFILES:
        return getProfiles();
      case PomPackage.MODEL__REPOSITORIES:
        return getRepositories();
      case PomPackage.MODEL__PLUGIN_REPOSITORIES:
        return getPluginRepositories();
      case PomPackage.MODEL__DEPENDENCIES:
        return getDependencies();
      case PomPackage.MODEL__REPORTING:
        return getReporting();
      case PomPackage.MODEL__DEPENDENCY_MANAGEMENT:
        return getDependencyManagement();
      case PomPackage.MODEL__DISTRIBUTION_MANAGEMENT:
        return getDistributionManagement();
      case PomPackage.MODEL__PROPERTIES:
        return getProperties();
      case PomPackage.MODEL__MODULES:
        return getModules();
    }
    return super.eGet(featureID, resolve, coreType);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @SuppressWarnings("unchecked")
  @Override
  public void eSet(int featureID, Object newValue) {
    switch(featureID) {
      case PomPackage.MODEL__PARENT:
        setParent((Parent) newValue);
        return;
      case PomPackage.MODEL__MODEL_VERSION:
        setModelVersion((String) newValue);
        return;
      case PomPackage.MODEL__GROUP_ID:
        setGroupId((String) newValue);
        return;
      case PomPackage.MODEL__ARTIFACT_ID:
        setArtifactId((String) newValue);
        return;
      case PomPackage.MODEL__PACKAGING:
        setPackaging((String) newValue);
        return;
      case PomPackage.MODEL__NAME:
        setName((String) newValue);
        return;
      case PomPackage.MODEL__VERSION:
        setVersion((String) newValue);
        return;
      case PomPackage.MODEL__DESCRIPTION:
        setDescription((String) newValue);
        return;
      case PomPackage.MODEL__URL:
        setUrl((String) newValue);
        return;
      case PomPackage.MODEL__PREREQUISITES:
        setPrerequisites((Prerequisites) newValue);
        return;
      case PomPackage.MODEL__ISSUE_MANAGEMENT:
        setIssueManagement((IssueManagement) newValue);
        return;
      case PomPackage.MODEL__CI_MANAGEMENT:
        setCiManagement((CiManagement) newValue);
        return;
      case PomPackage.MODEL__INCEPTION_YEAR:
        setInceptionYear((String) newValue);
        return;
      case PomPackage.MODEL__MAILING_LISTS:
        getMailingLists().clear();
        getMailingLists().addAll((Collection<? extends MailingList>) newValue);
        return;
      case PomPackage.MODEL__DEVELOPERS:
        getDevelopers().clear();
        getDevelopers().addAll((Collection<? extends Developer>) newValue);
        return;
      case PomPackage.MODEL__CONTRIBUTORS:
        getContributors().clear();
        getContributors().addAll((Collection<? extends Contributor>) newValue);
        return;
      case PomPackage.MODEL__LICENSES:
        getLicenses().clear();
        getLicenses().addAll((Collection<? extends License>) newValue);
        return;
      case PomPackage.MODEL__SCM:
        setScm((Scm) newValue);
        return;
      case PomPackage.MODEL__ORGANIZATION:
        setOrganization((Organization) newValue);
        return;
      case PomPackage.MODEL__BUILD:
        setBuild((Build) newValue);
        return;
      case PomPackage.MODEL__PROFILES:
        getProfiles().clear();
        getProfiles().addAll((Collection<? extends Profile>) newValue);
        return;
      case PomPackage.MODEL__REPOSITORIES:
        getRepositories().clear();
        getRepositories().addAll((Collection<? extends Repository>) newValue);
        return;
      case PomPackage.MODEL__PLUGIN_REPOSITORIES:
        getPluginRepositories().clear();
        getPluginRepositories().addAll((Collection<? extends Repository>) newValue);
        return;
      case PomPackage.MODEL__DEPENDENCIES:
        getDependencies().clear();
        getDependencies().addAll((Collection<? extends Dependency>) newValue);
        return;
      case PomPackage.MODEL__REPORTING:
        setReporting((Reporting) newValue);
        return;
      case PomPackage.MODEL__DEPENDENCY_MANAGEMENT:
        setDependencyManagement((DependencyManagement) newValue);
        return;
      case PomPackage.MODEL__DISTRIBUTION_MANAGEMENT:
        setDistributionManagement((DistributionManagement) newValue);
        return;
      case PomPackage.MODEL__PROPERTIES:
        getProperties().clear();
        getProperties().addAll((Collection<? extends PropertyElement>) newValue);
        return;
      case PomPackage.MODEL__MODULES:
        getModules().clear();
        getModules().addAll((Collection<? extends String>) newValue);
        return;
    }
    super.eSet(featureID, newValue);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public void eUnset(int featureID) {
    switch(featureID) {
      case PomPackage.MODEL__PARENT:
        unsetParent();
        return;
      case PomPackage.MODEL__MODEL_VERSION:
        setModelVersion(MODEL_VERSION_EDEFAULT);
        return;
      case PomPackage.MODEL__GROUP_ID:
        setGroupId(GROUP_ID_EDEFAULT);
        return;
      case PomPackage.MODEL__ARTIFACT_ID:
        setArtifactId(ARTIFACT_ID_EDEFAULT);
        return;
      case PomPackage.MODEL__PACKAGING:
        unsetPackaging();
        return;
      case PomPackage.MODEL__NAME:
        setName(NAME_EDEFAULT);
        return;
      case PomPackage.MODEL__VERSION:
        setVersion(VERSION_EDEFAULT);
        return;
      case PomPackage.MODEL__DESCRIPTION:
        setDescription(DESCRIPTION_EDEFAULT);
        return;
      case PomPackage.MODEL__URL:
        setUrl(URL_EDEFAULT);
        return;
      case PomPackage.MODEL__PREREQUISITES:
        unsetPrerequisites();
        return;
      case PomPackage.MODEL__ISSUE_MANAGEMENT:
        unsetIssueManagement();
        return;
      case PomPackage.MODEL__CI_MANAGEMENT:
        unsetCiManagement();
        return;
      case PomPackage.MODEL__INCEPTION_YEAR:
        setInceptionYear(INCEPTION_YEAR_EDEFAULT);
        return;
      case PomPackage.MODEL__MAILING_LISTS:
        unsetMailingLists();
        return;
      case PomPackage.MODEL__DEVELOPERS:
        unsetDevelopers();
        return;
      case PomPackage.MODEL__CONTRIBUTORS:
        unsetContributors();
        return;
      case PomPackage.MODEL__LICENSES:
        getLicenses().clear();
        return;
      case PomPackage.MODEL__SCM:
        unsetScm();
        return;
      case PomPackage.MODEL__ORGANIZATION:
        unsetOrganization();
        return;
      case PomPackage.MODEL__BUILD:
        unsetBuild();
        return;
      case PomPackage.MODEL__PROFILES:
        unsetProfiles();
        return;
      case PomPackage.MODEL__REPOSITORIES:
        unsetRepositories();
        return;
      case PomPackage.MODEL__PLUGIN_REPOSITORIES:
        unsetPluginRepositories();
        return;
      case PomPackage.MODEL__DEPENDENCIES:
        unsetDependencies();
        return;
      case PomPackage.MODEL__REPORTING:
        unsetReporting();
        return;
      case PomPackage.MODEL__DEPENDENCY_MANAGEMENT:
        unsetDependencyManagement();
        return;
      case PomPackage.MODEL__DISTRIBUTION_MANAGEMENT:
        unsetDistributionManagement();
        return;
      case PomPackage.MODEL__PROPERTIES:
        unsetProperties();
        return;
      case PomPackage.MODEL__MODULES:
        getModules().clear();
        return;
    }
    super.eUnset(featureID);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public boolean eIsSet(int featureID) {
    switch(featureID) {
      case PomPackage.MODEL__PARENT:
        return isSetParent();
      case PomPackage.MODEL__MODEL_VERSION:
        return MODEL_VERSION_EDEFAULT == null ? modelVersion != null : !MODEL_VERSION_EDEFAULT.equals(modelVersion);
      case PomPackage.MODEL__GROUP_ID:
        return GROUP_ID_EDEFAULT == null ? groupId != null : !GROUP_ID_EDEFAULT.equals(groupId);
      case PomPackage.MODEL__ARTIFACT_ID:
        return ARTIFACT_ID_EDEFAULT == null ? artifactId != null : !ARTIFACT_ID_EDEFAULT.equals(artifactId);
      case PomPackage.MODEL__PACKAGING:
        return isSetPackaging();
      case PomPackage.MODEL__NAME:
        return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
      case PomPackage.MODEL__VERSION:
        return VERSION_EDEFAULT == null ? version != null : !VERSION_EDEFAULT.equals(version);
      case PomPackage.MODEL__DESCRIPTION:
        return DESCRIPTION_EDEFAULT == null ? description != null : !DESCRIPTION_EDEFAULT.equals(description);
      case PomPackage.MODEL__URL:
        return URL_EDEFAULT == null ? url != null : !URL_EDEFAULT.equals(url);
      case PomPackage.MODEL__PREREQUISITES:
        return isSetPrerequisites();
      case PomPackage.MODEL__ISSUE_MANAGEMENT:
        return isSetIssueManagement();
      case PomPackage.MODEL__CI_MANAGEMENT:
        return isSetCiManagement();
      case PomPackage.MODEL__INCEPTION_YEAR:
        return INCEPTION_YEAR_EDEFAULT == null ? inceptionYear != null : !INCEPTION_YEAR_EDEFAULT.equals(inceptionYear);
      case PomPackage.MODEL__MAILING_LISTS:
        return isSetMailingLists();
      case PomPackage.MODEL__DEVELOPERS:
        return isSetDevelopers();
      case PomPackage.MODEL__CONTRIBUTORS:
        return isSetContributors();
      case PomPackage.MODEL__LICENSES:
        return licenses != null && !licenses.isEmpty();
      case PomPackage.MODEL__SCM:
        return isSetScm();
      case PomPackage.MODEL__ORGANIZATION:
        return isSetOrganization();
      case PomPackage.MODEL__BUILD:
        return isSetBuild();
      case PomPackage.MODEL__PROFILES:
        return isSetProfiles();
      case PomPackage.MODEL__REPOSITORIES:
        return isSetRepositories();
      case PomPackage.MODEL__PLUGIN_REPOSITORIES:
        return isSetPluginRepositories();
      case PomPackage.MODEL__DEPENDENCIES:
        return isSetDependencies();
      case PomPackage.MODEL__REPORTING:
        return isSetReporting();
      case PomPackage.MODEL__DEPENDENCY_MANAGEMENT:
        return isSetDependencyManagement();
      case PomPackage.MODEL__DISTRIBUTION_MANAGEMENT:
        return isSetDistributionManagement();
      case PomPackage.MODEL__PROPERTIES:
        return isSetProperties();
      case PomPackage.MODEL__MODULES:
        return modules != null && !modules.isEmpty();
    }
    return super.eIsSet(featureID);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public String toString() {
    if(eIsProxy())
      return super.toString();

    StringBuffer result = new StringBuffer(super.toString());
    result.append(" (modelVersion: "); //$NON-NLS-1$
    result.append(modelVersion);
    result.append(", groupId: "); //$NON-NLS-1$
    result.append(groupId);
    result.append(", artifactId: "); //$NON-NLS-1$
    result.append(artifactId);
    result.append(", packaging: "); //$NON-NLS-1$
    if(packagingESet)
      result.append(packaging);
    else
      result.append("<unset>"); //$NON-NLS-1$
    result.append(", name: "); //$NON-NLS-1$
    result.append(name);
    result.append(", version: "); //$NON-NLS-1$
    result.append(version);
    result.append(", description: "); //$NON-NLS-1$
    result.append(description);
    result.append(", url: "); //$NON-NLS-1$
    result.append(url);
    result.append(", inceptionYear: "); //$NON-NLS-1$
    result.append(inceptionYear);
    result.append(", modules: "); //$NON-NLS-1$
    result.append(modules);
    result.append(')');
    return result.toString();
  }

} // ModelImpl
