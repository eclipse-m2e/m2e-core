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
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.m2e.model.edit.pom.Configuration;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.Plugin;
import org.eclipse.m2e.model.edit.pom.PluginExecution;
import org.eclipse.m2e.model.edit.pom.PomPackage;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Plugin</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.PluginImpl#getGroupId <em>Group Id</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.PluginImpl#getArtifactId <em> Artifact Id</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.PluginImpl#getVersion <em> Version</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.PluginImpl#getExtensions <em> Extensions</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.PluginImpl#getExecutions <em> Executions</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.PluginImpl#getDependencies <em> Dependencies</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.PluginImpl#getInherited <em> Inherited</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.PluginImpl#getConfiguration <em> Configuration</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class PluginImpl extends EObjectImpl implements Plugin {
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
   * This is true if the Group Id attribute has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   * @ordered
   */
  protected boolean groupIdESet;

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
   * The default value of the '{@link #getExtensions() <em>Extensions</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getExtensions()
   * @generated
   * @ordered
   */
  protected static final String EXTENSIONS_EDEFAULT = "false"; //$NON-NLS-1$

  /**
   * The cached value of the '{@link #getExtensions() <em>Extensions</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getExtensions()
   * @generated
   * @ordered
   */
  protected String extensions = EXTENSIONS_EDEFAULT;

  /**
   * This is true if the Extensions attribute has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   * @ordered
   */
  protected boolean extensionsESet;

  /**
   * The cached value of the '{@link #getExecutions() <em>Executions</em>}' containment reference list. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getExecutions()
   * @generated
   * @ordered
   */
  protected EList<PluginExecution> executions;

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
   * The default value of the '{@link #getInherited() <em>Inherited</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getInherited()
   * @generated
   * @ordered
   */
  protected static final String INHERITED_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getInherited() <em>Inherited</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getInherited()
   * @generated
   * @ordered
   */
  protected String inherited = INHERITED_EDEFAULT;

  /**
   * The cached value of the '{@link #getConfiguration() <em>Configuration</em>}' reference. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   *
   * @see #getConfiguration()
   * @generated
   * @ordered
   */
  protected Configuration configuration;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  protected PluginImpl() {
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.PLUGIN;
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
    boolean oldGroupIdESet = groupIdESet;
    groupIdESet = true;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PLUGIN__GROUP_ID, oldGroupId, groupId,
          !oldGroupIdESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetGroupId() {
    String oldGroupId = groupId;
    boolean oldGroupIdESet = groupIdESet;
    groupId = GROUP_ID_EDEFAULT;
    groupIdESet = false;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.PLUGIN__GROUP_ID, oldGroupId,
          GROUP_ID_EDEFAULT, oldGroupIdESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetGroupId() {
    return groupIdESet;
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PLUGIN__ARTIFACT_ID, oldArtifactId, artifactId));
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PLUGIN__VERSION, oldVersion, version));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getExtensions() {
    return extensions;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setExtensions(String newExtensions) {
    String oldExtensions = extensions;
    extensions = newExtensions;
    boolean oldExtensionsESet = extensionsESet;
    extensionsESet = true;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PLUGIN__EXTENSIONS, oldExtensions, extensions,
          !oldExtensionsESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetExtensions() {
    String oldExtensions = extensions;
    boolean oldExtensionsESet = extensionsESet;
    extensions = EXTENSIONS_EDEFAULT;
    extensionsESet = false;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.PLUGIN__EXTENSIONS, oldExtensions,
          EXTENSIONS_EDEFAULT, oldExtensionsESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetExtensions() {
    return extensionsESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<PluginExecution> getExecutions() {
    if(executions == null) {
      executions = new EObjectContainmentEList.Unsettable<>(PluginExecution.class, this,
          PomPackage.PLUGIN__EXECUTIONS);
    }
    return executions;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetExecutions() {
    if(executions != null)
      ((InternalEList.Unsettable<?>) executions).unset();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetExecutions() {
    return executions != null && ((InternalEList.Unsettable<?>) executions).isSet();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<Dependency> getDependencies() {
    if(dependencies == null) {
      dependencies = new EObjectContainmentEList.Unsettable<>(Dependency.class, this,
          PomPackage.PLUGIN__DEPENDENCIES);
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
  public String getInherited() {
    return inherited;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setInherited(String newInherited) {
    String oldInherited = inherited;
    inherited = newInherited;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PLUGIN__INHERITED, oldInherited, inherited));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Configuration getConfiguration() {
    if(configuration != null && configuration.eIsProxy()) {
      InternalEObject oldConfiguration = (InternalEObject) configuration;
      configuration = (Configuration) eResolveProxy(oldConfiguration);
      if(configuration != oldConfiguration) {
        if(eNotificationRequired())
          eNotify(new ENotificationImpl(this, Notification.RESOLVE, PomPackage.PLUGIN__CONFIGURATION, oldConfiguration,
              configuration));
      }
    }
    return configuration;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public Configuration basicGetConfiguration() {
    return configuration;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setConfiguration(Configuration newConfiguration) {
    Configuration oldConfiguration = configuration;
    configuration = newConfiguration;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PLUGIN__CONFIGURATION, oldConfiguration,
          configuration));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
    switch(featureID) {
      case PomPackage.PLUGIN__EXECUTIONS:
        return ((InternalEList<?>) getExecutions()).basicRemove(otherEnd, msgs);
      case PomPackage.PLUGIN__DEPENDENCIES:
        return ((InternalEList<?>) getDependencies()).basicRemove(otherEnd, msgs);
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
      case PomPackage.PLUGIN__GROUP_ID:
        return getGroupId();
      case PomPackage.PLUGIN__ARTIFACT_ID:
        return getArtifactId();
      case PomPackage.PLUGIN__VERSION:
        return getVersion();
      case PomPackage.PLUGIN__EXTENSIONS:
        return getExtensions();
      case PomPackage.PLUGIN__EXECUTIONS:
        return getExecutions();
      case PomPackage.PLUGIN__DEPENDENCIES:
        return getDependencies();
      case PomPackage.PLUGIN__INHERITED:
        return getInherited();
      case PomPackage.PLUGIN__CONFIGURATION:
        if(resolve)
          return getConfiguration();
        return basicGetConfiguration();
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
      case PomPackage.PLUGIN__GROUP_ID:
        setGroupId((String) newValue);
        return;
      case PomPackage.PLUGIN__ARTIFACT_ID:
        setArtifactId((String) newValue);
        return;
      case PomPackage.PLUGIN__VERSION:
        setVersion((String) newValue);
        return;
      case PomPackage.PLUGIN__EXTENSIONS:
        setExtensions((String) newValue);
        return;
      case PomPackage.PLUGIN__EXECUTIONS:
        getExecutions().clear();
        getExecutions().addAll((Collection<? extends PluginExecution>) newValue);
        return;
      case PomPackage.PLUGIN__DEPENDENCIES:
        getDependencies().clear();
        getDependencies().addAll((Collection<? extends Dependency>) newValue);
        return;
      case PomPackage.PLUGIN__INHERITED:
        setInherited((String) newValue);
        return;
      case PomPackage.PLUGIN__CONFIGURATION:
        setConfiguration((Configuration) newValue);
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
      case PomPackage.PLUGIN__GROUP_ID:
        unsetGroupId();
        return;
      case PomPackage.PLUGIN__ARTIFACT_ID:
        setArtifactId(ARTIFACT_ID_EDEFAULT);
        return;
      case PomPackage.PLUGIN__VERSION:
        setVersion(VERSION_EDEFAULT);
        return;
      case PomPackage.PLUGIN__EXTENSIONS:
        unsetExtensions();
        return;
      case PomPackage.PLUGIN__EXECUTIONS:
        unsetExecutions();
        return;
      case PomPackage.PLUGIN__DEPENDENCIES:
        unsetDependencies();
        return;
      case PomPackage.PLUGIN__INHERITED:
        setInherited(INHERITED_EDEFAULT);
        return;
      case PomPackage.PLUGIN__CONFIGURATION:
        setConfiguration((Configuration) null);
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
      case PomPackage.PLUGIN__GROUP_ID:
        return isSetGroupId();
      case PomPackage.PLUGIN__ARTIFACT_ID:
        return ARTIFACT_ID_EDEFAULT == null ? artifactId != null : !ARTIFACT_ID_EDEFAULT.equals(artifactId);
      case PomPackage.PLUGIN__VERSION:
        return VERSION_EDEFAULT == null ? version != null : !VERSION_EDEFAULT.equals(version);
      case PomPackage.PLUGIN__EXTENSIONS:
        return isSetExtensions();
      case PomPackage.PLUGIN__EXECUTIONS:
        return isSetExecutions();
      case PomPackage.PLUGIN__DEPENDENCIES:
        return isSetDependencies();
      case PomPackage.PLUGIN__INHERITED:
        return INHERITED_EDEFAULT == null ? inherited != null : !INHERITED_EDEFAULT.equals(inherited);
      case PomPackage.PLUGIN__CONFIGURATION:
        return configuration != null;
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
    result.append(" (groupId: "); //$NON-NLS-1$
    if(groupIdESet)
      result.append(groupId);
    else
      result.append("<unset>"); //$NON-NLS-1$
    result.append(", artifactId: "); //$NON-NLS-1$
    result.append(artifactId);
    result.append(", version: "); //$NON-NLS-1$
    result.append(version);
    result.append(", extensions: "); //$NON-NLS-1$
    if(extensionsESet)
      result.append(extensions);
    else
      result.append("<unset>"); //$NON-NLS-1$
    result.append(", inherited: "); //$NON-NLS-1$
    result.append(inherited);
    result.append(')');
    return result.toString();
  }

} // PluginImpl
