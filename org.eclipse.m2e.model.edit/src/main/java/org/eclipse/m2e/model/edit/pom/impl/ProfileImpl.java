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

import org.eclipse.m2e.model.edit.pom.Activation;
import org.eclipse.m2e.model.edit.pom.BuildBase;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.DependencyManagement;
import org.eclipse.m2e.model.edit.pom.DistributionManagement;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.Profile;
import org.eclipse.m2e.model.edit.pom.PropertyElement;
import org.eclipse.m2e.model.edit.pom.ReportPlugin;
import org.eclipse.m2e.model.edit.pom.Reporting;
import org.eclipse.m2e.model.edit.pom.Repository;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Profile</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ProfileImpl#getId <em>Id</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ProfileImpl#getActivation <em> Activation</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ProfileImpl#getBuild <em>Build </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ProfileImpl#getRepositories <em> Repositories</em>}</li>
 * <li>
 * {@link org.eclipse.m2e.model.edit.pom.impl.ProfileImpl#getPluginRepositories <em>Plugin Repositories</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ProfileImpl#getDependencies <em> Dependencies</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ProfileImpl#getReports <em> Reports</em>}</li>
 * <li>
 * {@link org.eclipse.m2e.model.edit.pom.impl.ProfileImpl#getDependencyManagement <em>Dependency Management</em>}</li>
 * <li>
 * {@link org.eclipse.m2e.model.edit.pom.impl.ProfileImpl#getDistributionManagement <em>Distribution Management
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ProfileImpl#getProperties <em> Properties</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ProfileImpl#getModules <em> Modules</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ProfileImpl#getReporting <em> Reporting</em>}</li>
 * </ul>
 * </p>
 * 
 * @generated
 */
public class ProfileImpl extends EObjectImpl implements Profile {
  /**
   * The default value of the '{@link #getId() <em>Id</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getId()
   * @generated
   * @ordered
   */
  protected static final String ID_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getId() <em>Id</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getId()
   * @generated
   * @ordered
   */
  protected String id = ID_EDEFAULT;

  /**
   * The cached value of the '{@link #getActivation() <em>Activation</em>}' containment reference. <!-- begin-user-doc
   * --> <!-- end-user-doc -->
   * 
   * @see #getActivation()
   * @generated
   * @ordered
   */
  protected Activation activation;

  /**
   * This is true if the Activation containment reference has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  protected boolean activationESet;

  /**
   * The cached value of the '{@link #getBuild() <em>Build</em>}' containment reference. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getBuild()
   * @generated
   * @ordered
   */
  protected BuildBase build;

  /**
   * This is true if the Build containment reference has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  protected boolean buildESet;

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
   * The cached value of the '{@link #getReports() <em>Reports</em>}' containment reference list. <!-- begin-user-doc
   * --> <!-- end-user-doc -->
   * 
   * @see #getReports()
   * @generated
   * @ordered
   */
  protected EList<ReportPlugin> reports;

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
   * The cached value of the '{@link #getReporting() <em>Reporting</em>}' reference. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getReporting()
   * @generated
   * @ordered
   */
  protected Reporting reporting;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected ProfileImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.PROFILE;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getId() {
    return id;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setId(String newId) {
    String oldId = id;
    id = newId;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PROFILE__ID, oldId, id));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public Activation getActivation() {
    return activation;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public NotificationChain basicSetActivation(Activation newActivation, NotificationChain msgs) {
    Activation oldActivation = activation;
    activation = newActivation;
    boolean oldActivationESet = activationESet;
    activationESet = true;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, PomPackage.PROFILE__ACTIVATION,
          oldActivation, newActivation, !oldActivationESet);
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
  public void setActivation(Activation newActivation) {
    if(newActivation != activation) {
      NotificationChain msgs = null;
      if(activation != null)
        msgs = ((InternalEObject) activation).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.PROFILE__ACTIVATION, null, msgs);
      if(newActivation != null)
        msgs = ((InternalEObject) newActivation).eInverseAdd(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.PROFILE__ACTIVATION, null, msgs);
      msgs = basicSetActivation(newActivation, msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldActivationESet = activationESet;
      activationESet = true;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PROFILE__ACTIVATION, newActivation,
            newActivation, !oldActivationESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public NotificationChain basicUnsetActivation(NotificationChain msgs) {
    Activation oldActivation = activation;
    activation = null;
    boolean oldActivationESet = activationESet;
    activationESet = false;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.UNSET, PomPackage.PROFILE__ACTIVATION,
          oldActivation, null, oldActivationESet);
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
  public void unsetActivation() {
    if(activation != null) {
      NotificationChain msgs = null;
      msgs = ((InternalEObject) activation).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
          - PomPackage.PROFILE__ACTIVATION, null, msgs);
      msgs = basicUnsetActivation(msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldActivationESet = activationESet;
      activationESet = false;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.PROFILE__ACTIVATION, null, null,
            oldActivationESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public boolean isSetActivation() {
    return activationESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public BuildBase getBuild() {
    return build;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public NotificationChain basicSetBuild(BuildBase newBuild, NotificationChain msgs) {
    BuildBase oldBuild = build;
    build = newBuild;
    boolean oldBuildESet = buildESet;
    buildESet = true;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, PomPackage.PROFILE__BUILD,
          oldBuild, newBuild, !oldBuildESet);
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
  public void setBuild(BuildBase newBuild) {
    if(newBuild != build) {
      NotificationChain msgs = null;
      if(build != null)
        msgs = ((InternalEObject) build).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - PomPackage.PROFILE__BUILD, null,
            msgs);
      if(newBuild != null)
        msgs = ((InternalEObject) newBuild).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - PomPackage.PROFILE__BUILD, null,
            msgs);
      msgs = basicSetBuild(newBuild, msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldBuildESet = buildESet;
      buildESet = true;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PROFILE__BUILD, newBuild, newBuild,
            !oldBuildESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public NotificationChain basicUnsetBuild(NotificationChain msgs) {
    BuildBase oldBuild = build;
    build = null;
    boolean oldBuildESet = buildESet;
    buildESet = false;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.UNSET, PomPackage.PROFILE__BUILD,
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
      msgs = ((InternalEObject) build).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - PomPackage.PROFILE__BUILD, null,
          msgs);
      msgs = basicUnsetBuild(msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldBuildESet = buildESet;
      buildESet = false;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.PROFILE__BUILD, null, null, oldBuildESet));
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
  public EList<Repository> getRepositories() {
    if(repositories == null) {
      repositories = new EObjectContainmentEList.Unsettable<>(Repository.class, this,
          PomPackage.PROFILE__REPOSITORIES);
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
          PomPackage.PROFILE__PLUGIN_REPOSITORIES);
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
          PomPackage.PROFILE__DEPENDENCIES);
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
  public EList<ReportPlugin> getReports() {
    if(reports == null) {
      reports = new EObjectContainmentEList.Unsettable<>(ReportPlugin.class, this,
          PomPackage.PROFILE__REPORTS);
    }
    return reports;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void unsetReports() {
    if(reports != null)
      ((InternalEList.Unsettable<?>) reports).unset();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public boolean isSetReports() {
    return reports != null && ((InternalEList.Unsettable<?>) reports).isSet();
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
          PomPackage.PROFILE__DEPENDENCY_MANAGEMENT, oldDependencyManagement, newDependencyManagement,
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
            - PomPackage.PROFILE__DEPENDENCY_MANAGEMENT, null, msgs);
      if(newDependencyManagement != null)
        msgs = ((InternalEObject) newDependencyManagement).eInverseAdd(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.PROFILE__DEPENDENCY_MANAGEMENT, null, msgs);
      msgs = basicSetDependencyManagement(newDependencyManagement, msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldDependencyManagementESet = dependencyManagementESet;
      dependencyManagementESet = true;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PROFILE__DEPENDENCY_MANAGEMENT,
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
          PomPackage.PROFILE__DEPENDENCY_MANAGEMENT, oldDependencyManagement, null, oldDependencyManagementESet);
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
          - PomPackage.PROFILE__DEPENDENCY_MANAGEMENT, null, msgs);
      msgs = basicUnsetDependencyManagement(msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldDependencyManagementESet = dependencyManagementESet;
      dependencyManagementESet = false;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.PROFILE__DEPENDENCY_MANAGEMENT, null, null,
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
          PomPackage.PROFILE__DISTRIBUTION_MANAGEMENT, oldDistributionManagement, newDistributionManagement,
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
            - PomPackage.PROFILE__DISTRIBUTION_MANAGEMENT, null, msgs);
      if(newDistributionManagement != null)
        msgs = ((InternalEObject) newDistributionManagement).eInverseAdd(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.PROFILE__DISTRIBUTION_MANAGEMENT, null, msgs);
      msgs = basicSetDistributionManagement(newDistributionManagement, msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldDistributionManagementESet = distributionManagementESet;
      distributionManagementESet = true;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PROFILE__DISTRIBUTION_MANAGEMENT,
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
          PomPackage.PROFILE__DISTRIBUTION_MANAGEMENT, oldDistributionManagement, null, oldDistributionManagementESet);
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
          - PomPackage.PROFILE__DISTRIBUTION_MANAGEMENT, null, msgs);
      msgs = basicUnsetDistributionManagement(msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldDistributionManagementESet = distributionManagementESet;
      distributionManagementESet = false;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.PROFILE__DISTRIBUTION_MANAGEMENT, null,
            null, oldDistributionManagementESet));
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
          PomPackage.PROFILE__PROPERTIES);
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
      modules = new EDataTypeEList<>(String.class, this, PomPackage.PROFILE__MODULES);
    }
    return modules;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public Reporting getReporting() {
    if(reporting != null && reporting.eIsProxy()) {
      InternalEObject oldReporting = (InternalEObject) reporting;
      reporting = (Reporting) eResolveProxy(oldReporting);
      if(reporting != oldReporting) {
        if(eNotificationRequired())
          eNotify(new ENotificationImpl(this, Notification.RESOLVE, PomPackage.PROFILE__REPORTING, oldReporting,
              reporting));
      }
    }
    return reporting;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public Reporting basicGetReporting() {
    return reporting;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setReporting(Reporting newReporting) {
    Reporting oldReporting = reporting;
    reporting = newReporting;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PROFILE__REPORTING, oldReporting, reporting));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
    switch(featureID) {
      case PomPackage.PROFILE__ACTIVATION:
        return basicUnsetActivation(msgs);
      case PomPackage.PROFILE__BUILD:
        return basicUnsetBuild(msgs);
      case PomPackage.PROFILE__REPOSITORIES:
        return ((InternalEList<?>) getRepositories()).basicRemove(otherEnd, msgs);
      case PomPackage.PROFILE__PLUGIN_REPOSITORIES:
        return ((InternalEList<?>) getPluginRepositories()).basicRemove(otherEnd, msgs);
      case PomPackage.PROFILE__DEPENDENCIES:
        return ((InternalEList<?>) getDependencies()).basicRemove(otherEnd, msgs);
      case PomPackage.PROFILE__REPORTS:
        return ((InternalEList<?>) getReports()).basicRemove(otherEnd, msgs);
      case PomPackage.PROFILE__DEPENDENCY_MANAGEMENT:
        return basicUnsetDependencyManagement(msgs);
      case PomPackage.PROFILE__DISTRIBUTION_MANAGEMENT:
        return basicUnsetDistributionManagement(msgs);
      case PomPackage.PROFILE__PROPERTIES:
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
      case PomPackage.PROFILE__ID:
        return getId();
      case PomPackage.PROFILE__ACTIVATION:
        return getActivation();
      case PomPackage.PROFILE__BUILD:
        return getBuild();
      case PomPackage.PROFILE__REPOSITORIES:
        return getRepositories();
      case PomPackage.PROFILE__PLUGIN_REPOSITORIES:
        return getPluginRepositories();
      case PomPackage.PROFILE__DEPENDENCIES:
        return getDependencies();
      case PomPackage.PROFILE__REPORTS:
        return getReports();
      case PomPackage.PROFILE__DEPENDENCY_MANAGEMENT:
        return getDependencyManagement();
      case PomPackage.PROFILE__DISTRIBUTION_MANAGEMENT:
        return getDistributionManagement();
      case PomPackage.PROFILE__PROPERTIES:
        return getProperties();
      case PomPackage.PROFILE__MODULES:
        return getModules();
      case PomPackage.PROFILE__REPORTING:
        if(resolve)
          return getReporting();
        return basicGetReporting();
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
      case PomPackage.PROFILE__ID:
        setId((String) newValue);
        return;
      case PomPackage.PROFILE__ACTIVATION:
        setActivation((Activation) newValue);
        return;
      case PomPackage.PROFILE__BUILD:
        setBuild((BuildBase) newValue);
        return;
      case PomPackage.PROFILE__REPOSITORIES:
        getRepositories().clear();
        getRepositories().addAll((Collection<? extends Repository>) newValue);
        return;
      case PomPackage.PROFILE__PLUGIN_REPOSITORIES:
        getPluginRepositories().clear();
        getPluginRepositories().addAll((Collection<? extends Repository>) newValue);
        return;
      case PomPackage.PROFILE__DEPENDENCIES:
        getDependencies().clear();
        getDependencies().addAll((Collection<? extends Dependency>) newValue);
        return;
      case PomPackage.PROFILE__REPORTS:
        getReports().clear();
        getReports().addAll((Collection<? extends ReportPlugin>) newValue);
        return;
      case PomPackage.PROFILE__DEPENDENCY_MANAGEMENT:
        setDependencyManagement((DependencyManagement) newValue);
        return;
      case PomPackage.PROFILE__DISTRIBUTION_MANAGEMENT:
        setDistributionManagement((DistributionManagement) newValue);
        return;
      case PomPackage.PROFILE__PROPERTIES:
        getProperties().clear();
        getProperties().addAll((Collection<? extends PropertyElement>) newValue);
        return;
      case PomPackage.PROFILE__MODULES:
        getModules().clear();
        getModules().addAll((Collection<? extends String>) newValue);
        return;
      case PomPackage.PROFILE__REPORTING:
        setReporting((Reporting) newValue);
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
      case PomPackage.PROFILE__ID:
        setId(ID_EDEFAULT);
        return;
      case PomPackage.PROFILE__ACTIVATION:
        unsetActivation();
        return;
      case PomPackage.PROFILE__BUILD:
        unsetBuild();
        return;
      case PomPackage.PROFILE__REPOSITORIES:
        unsetRepositories();
        return;
      case PomPackage.PROFILE__PLUGIN_REPOSITORIES:
        unsetPluginRepositories();
        return;
      case PomPackage.PROFILE__DEPENDENCIES:
        unsetDependencies();
        return;
      case PomPackage.PROFILE__REPORTS:
        unsetReports();
        return;
      case PomPackage.PROFILE__DEPENDENCY_MANAGEMENT:
        unsetDependencyManagement();
        return;
      case PomPackage.PROFILE__DISTRIBUTION_MANAGEMENT:
        unsetDistributionManagement();
        return;
      case PomPackage.PROFILE__PROPERTIES:
        unsetProperties();
        return;
      case PomPackage.PROFILE__MODULES:
        getModules().clear();
        return;
      case PomPackage.PROFILE__REPORTING:
        setReporting((Reporting) null);
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
      case PomPackage.PROFILE__ID:
        return ID_EDEFAULT == null ? id != null : !ID_EDEFAULT.equals(id);
      case PomPackage.PROFILE__ACTIVATION:
        return isSetActivation();
      case PomPackage.PROFILE__BUILD:
        return isSetBuild();
      case PomPackage.PROFILE__REPOSITORIES:
        return isSetRepositories();
      case PomPackage.PROFILE__PLUGIN_REPOSITORIES:
        return isSetPluginRepositories();
      case PomPackage.PROFILE__DEPENDENCIES:
        return isSetDependencies();
      case PomPackage.PROFILE__REPORTS:
        return isSetReports();
      case PomPackage.PROFILE__DEPENDENCY_MANAGEMENT:
        return isSetDependencyManagement();
      case PomPackage.PROFILE__DISTRIBUTION_MANAGEMENT:
        return isSetDistributionManagement();
      case PomPackage.PROFILE__PROPERTIES:
        return isSetProperties();
      case PomPackage.PROFILE__MODULES:
        return modules != null && !modules.isEmpty();
      case PomPackage.PROFILE__REPORTING:
        return reporting != null;
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
    result.append(" (id: "); //$NON-NLS-1$
    result.append(id);
    result.append(", modules: "); //$NON-NLS-1$
    result.append(modules);
    result.append(')');
    return result.toString();
  }

} // ProfileImpl
