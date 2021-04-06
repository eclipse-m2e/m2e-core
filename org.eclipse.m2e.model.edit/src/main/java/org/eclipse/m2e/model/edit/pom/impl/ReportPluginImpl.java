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
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.ReportPlugin;
import org.eclipse.m2e.model.edit.pom.ReportSet;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Report Plugin</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ReportPluginImpl#getGroupId <em> Group Id</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ReportPluginImpl#getArtifactId <em>Artifact Id</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ReportPluginImpl#getVersion <em> Version</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ReportPluginImpl#getInherited <em>Inherited</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ReportPluginImpl#getReportSets <em>Report Sets</em>}</li>
 * <li>
 * {@link org.eclipse.m2e.model.edit.pom.impl.ReportPluginImpl#getConfiguration <em>Configuration</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ReportPluginImpl extends EObjectImpl implements ReportPlugin {
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
   * The cached value of the '{@link #getReportSets() <em>Report Sets</em>}' containment reference list. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getReportSets()
   * @generated
   * @ordered
   */
  protected EList<ReportSet> reportSets;

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
  protected ReportPluginImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.REPORT_PLUGIN;
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.REPORT_PLUGIN__GROUP_ID, oldGroupId, groupId,
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
      eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.REPORT_PLUGIN__GROUP_ID, oldGroupId,
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.REPORT_PLUGIN__ARTIFACT_ID, oldArtifactId,
          artifactId));
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.REPORT_PLUGIN__VERSION, oldVersion, version));
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.REPORT_PLUGIN__INHERITED, oldInherited,
          inherited));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<ReportSet> getReportSets() {
    if(reportSets == null) {
      reportSets = new EObjectContainmentEList.Unsettable<>(ReportSet.class, this,
          PomPackage.REPORT_PLUGIN__REPORT_SETS);
    }
    return reportSets;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetReportSets() {
    if(reportSets != null)
      ((InternalEList.Unsettable<?>) reportSets).unset();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetReportSets() {
    return reportSets != null && ((InternalEList.Unsettable<?>) reportSets).isSet();
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
          eNotify(new ENotificationImpl(this, Notification.RESOLVE, PomPackage.REPORT_PLUGIN__CONFIGURATION,
              oldConfiguration, configuration));
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.REPORT_PLUGIN__CONFIGURATION, oldConfiguration,
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
      case PomPackage.REPORT_PLUGIN__REPORT_SETS:
        return ((InternalEList<?>) getReportSets()).basicRemove(otherEnd, msgs);
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
      case PomPackage.REPORT_PLUGIN__GROUP_ID:
        return getGroupId();
      case PomPackage.REPORT_PLUGIN__ARTIFACT_ID:
        return getArtifactId();
      case PomPackage.REPORT_PLUGIN__VERSION:
        return getVersion();
      case PomPackage.REPORT_PLUGIN__INHERITED:
        return getInherited();
      case PomPackage.REPORT_PLUGIN__REPORT_SETS:
        return getReportSets();
      case PomPackage.REPORT_PLUGIN__CONFIGURATION:
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
      case PomPackage.REPORT_PLUGIN__GROUP_ID:
        setGroupId((String) newValue);
        return;
      case PomPackage.REPORT_PLUGIN__ARTIFACT_ID:
        setArtifactId((String) newValue);
        return;
      case PomPackage.REPORT_PLUGIN__VERSION:
        setVersion((String) newValue);
        return;
      case PomPackage.REPORT_PLUGIN__INHERITED:
        setInherited((String) newValue);
        return;
      case PomPackage.REPORT_PLUGIN__REPORT_SETS:
        getReportSets().clear();
        getReportSets().addAll((Collection<? extends ReportSet>) newValue);
        return;
      case PomPackage.REPORT_PLUGIN__CONFIGURATION:
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
      case PomPackage.REPORT_PLUGIN__GROUP_ID:
        unsetGroupId();
        return;
      case PomPackage.REPORT_PLUGIN__ARTIFACT_ID:
        setArtifactId(ARTIFACT_ID_EDEFAULT);
        return;
      case PomPackage.REPORT_PLUGIN__VERSION:
        setVersion(VERSION_EDEFAULT);
        return;
      case PomPackage.REPORT_PLUGIN__INHERITED:
        setInherited(INHERITED_EDEFAULT);
        return;
      case PomPackage.REPORT_PLUGIN__REPORT_SETS:
        unsetReportSets();
        return;
      case PomPackage.REPORT_PLUGIN__CONFIGURATION:
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
      case PomPackage.REPORT_PLUGIN__GROUP_ID:
        return isSetGroupId();
      case PomPackage.REPORT_PLUGIN__ARTIFACT_ID:
        return ARTIFACT_ID_EDEFAULT == null ? artifactId != null : !ARTIFACT_ID_EDEFAULT.equals(artifactId);
      case PomPackage.REPORT_PLUGIN__VERSION:
        return VERSION_EDEFAULT == null ? version != null : !VERSION_EDEFAULT.equals(version);
      case PomPackage.REPORT_PLUGIN__INHERITED:
        return INHERITED_EDEFAULT == null ? inherited != null : !INHERITED_EDEFAULT.equals(inherited);
      case PomPackage.REPORT_PLUGIN__REPORT_SETS:
        return isSetReportSets();
      case PomPackage.REPORT_PLUGIN__CONFIGURATION:
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
    result.append(", inherited: "); //$NON-NLS-1$
    result.append(inherited);
    result.append(')');
    return result.toString();
  }

} // ReportPluginImpl
