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

import org.eclipse.m2e.model.edit.pom.CiManagement;
import org.eclipse.m2e.model.edit.pom.Notifier;
import org.eclipse.m2e.model.edit.pom.PomPackage;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Ci Management</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.CiManagementImpl#getSystem <em> System</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.CiManagementImpl#getUrl <em>Url </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.CiManagementImpl#getNotifiers <em>Notifiers</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class CiManagementImpl extends EObjectImpl implements CiManagement {
  /**
   * The default value of the '{@link #getSystem() <em>System</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getSystem()
   * @generated
   * @ordered
   */
  protected static final String SYSTEM_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getSystem() <em>System</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   *
   * @see #getSystem()
   * @generated
   * @ordered
   */
  protected String system = SYSTEM_EDEFAULT;

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
   * The cached value of the '{@link #getNotifiers() <em>Notifiers</em>}' containment reference list. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getNotifiers()
   * @generated
   * @ordered
   */
  protected EList<Notifier> notifiers;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  protected CiManagementImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.CI_MANAGEMENT;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getSystem() {
    return system;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setSystem(String newSystem) {
    String oldSystem = system;
    system = newSystem;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.CI_MANAGEMENT__SYSTEM, oldSystem, system));
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.CI_MANAGEMENT__URL, oldUrl, url));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<Notifier> getNotifiers() {
    if(notifiers == null) {
      notifiers = new EObjectContainmentEList.Unsettable<>(Notifier.class, this,
          PomPackage.CI_MANAGEMENT__NOTIFIERS);
    }
    return notifiers;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetNotifiers() {
    if(notifiers != null)
      ((InternalEList.Unsettable<?>) notifiers).unset();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetNotifiers() {
    return notifiers != null && ((InternalEList.Unsettable<?>) notifiers).isSet();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
    switch(featureID) {
      case PomPackage.CI_MANAGEMENT__NOTIFIERS:
        return ((InternalEList<?>) getNotifiers()).basicRemove(otherEnd, msgs);
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
      case PomPackage.CI_MANAGEMENT__SYSTEM:
        return getSystem();
      case PomPackage.CI_MANAGEMENT__URL:
        return getUrl();
      case PomPackage.CI_MANAGEMENT__NOTIFIERS:
        return getNotifiers();
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
      case PomPackage.CI_MANAGEMENT__SYSTEM:
        setSystem((String) newValue);
        return;
      case PomPackage.CI_MANAGEMENT__URL:
        setUrl((String) newValue);
        return;
      case PomPackage.CI_MANAGEMENT__NOTIFIERS:
        getNotifiers().clear();
        getNotifiers().addAll((Collection<? extends Notifier>) newValue);
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
      case PomPackage.CI_MANAGEMENT__SYSTEM:
        setSystem(SYSTEM_EDEFAULT);
        return;
      case PomPackage.CI_MANAGEMENT__URL:
        setUrl(URL_EDEFAULT);
        return;
      case PomPackage.CI_MANAGEMENT__NOTIFIERS:
        unsetNotifiers();
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
      case PomPackage.CI_MANAGEMENT__SYSTEM:
        return SYSTEM_EDEFAULT == null ? system != null : !SYSTEM_EDEFAULT.equals(system);
      case PomPackage.CI_MANAGEMENT__URL:
        return URL_EDEFAULT == null ? url != null : !URL_EDEFAULT.equals(url);
      case PomPackage.CI_MANAGEMENT__NOTIFIERS:
        return isSetNotifiers();
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
    result.append(" (system: "); //$NON-NLS-1$
    result.append(system);
    result.append(", url: "); //$NON-NLS-1$
    result.append(url);
    result.append(')');
    return result.toString();
  }

} // CiManagementImpl
