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
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.util.EDataTypeEList;

import org.eclipse.m2e.model.edit.pom.Configuration;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.ReportSet;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Report Set</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ReportSetImpl#getId <em>Id</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ReportSetImpl#getInherited <em> Inherited</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ReportSetImpl#getReports <em> Reports</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ReportSetImpl#getConfiguration <em>Configuration</em>}</li>
 * </ul>
 * </p>
 * 
 * @generated
 */
public class ReportSetImpl extends EObjectImpl implements ReportSet {
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
   * This is true if the Id attribute has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  protected boolean idESet;

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
   * The cached value of the '{@link #getReports() <em>Reports</em>}' attribute list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getReports()
   * @generated
   * @ordered
   */
  protected EList<String> reports;

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
  protected ReportSetImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.REPORT_SET;
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
    boolean oldIdESet = idESet;
    idESet = true;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.REPORT_SET__ID, oldId, id, !oldIdESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void unsetId() {
    String oldId = id;
    boolean oldIdESet = idESet;
    id = ID_EDEFAULT;
    idESet = false;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.REPORT_SET__ID, oldId, ID_EDEFAULT, oldIdESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public boolean isSetId() {
    return idESet;
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.REPORT_SET__INHERITED, oldInherited, inherited));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public EList<String> getReports() {
    if(reports == null) {
      reports = new EDataTypeEList<>(String.class, this, PomPackage.REPORT_SET__REPORTS);
    }
    return reports;
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
          eNotify(new ENotificationImpl(this, Notification.RESOLVE, PomPackage.REPORT_SET__CONFIGURATION,
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.REPORT_SET__CONFIGURATION, oldConfiguration,
          configuration));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch(featureID) {
      case PomPackage.REPORT_SET__ID:
        return getId();
      case PomPackage.REPORT_SET__INHERITED:
        return getInherited();
      case PomPackage.REPORT_SET__REPORTS:
        return getReports();
      case PomPackage.REPORT_SET__CONFIGURATION:
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
      case PomPackage.REPORT_SET__ID:
        setId((String) newValue);
        return;
      case PomPackage.REPORT_SET__INHERITED:
        setInherited((String) newValue);
        return;
      case PomPackage.REPORT_SET__REPORTS:
        getReports().clear();
        getReports().addAll((Collection<? extends String>) newValue);
        return;
      case PomPackage.REPORT_SET__CONFIGURATION:
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
      case PomPackage.REPORT_SET__ID:
        unsetId();
        return;
      case PomPackage.REPORT_SET__INHERITED:
        setInherited(INHERITED_EDEFAULT);
        return;
      case PomPackage.REPORT_SET__REPORTS:
        getReports().clear();
        return;
      case PomPackage.REPORT_SET__CONFIGURATION:
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
      case PomPackage.REPORT_SET__ID:
        return isSetId();
      case PomPackage.REPORT_SET__INHERITED:
        return INHERITED_EDEFAULT == null ? inherited != null : !INHERITED_EDEFAULT.equals(inherited);
      case PomPackage.REPORT_SET__REPORTS:
        return reports != null && !reports.isEmpty();
      case PomPackage.REPORT_SET__CONFIGURATION:
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
    result.append(" (id: "); //$NON-NLS-1$
    if(idESet)
      result.append(id);
    else
      result.append("<unset>"); //$NON-NLS-1$
    result.append(", inherited: "); //$NON-NLS-1$
    result.append(inherited);
    result.append(", reports: "); //$NON-NLS-1$
    result.append(reports);
    result.append(')');
    return result.toString();
  }

} // ReportSetImpl
