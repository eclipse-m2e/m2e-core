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

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.m2e.model.edit.pom.ActivationProperty;
import org.eclipse.m2e.model.edit.pom.PomPackage;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Activation Property</b></em>'. <!--
 * end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ActivationPropertyImpl#getName <em>Name</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ActivationPropertyImpl#getValue <em>Value</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ActivationPropertyImpl extends EObjectImpl implements ActivationProperty {
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
   * The default value of the '{@link #getValue() <em>Value</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   *
   * @see #getValue()
   * @generated
   * @ordered
   */
  protected static final String VALUE_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getValue() <em>Value</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   *
   * @see #getValue()
   * @generated
   * @ordered
   */
  protected String value = VALUE_EDEFAULT;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  protected ActivationPropertyImpl() {
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.ACTIVATION_PROPERTY;
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.ACTIVATION_PROPERTY__NAME, oldName, name));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getValue() {
    return value;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setValue(String newValue) {
    String oldValue = value;
    value = newValue;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.ACTIVATION_PROPERTY__VALUE, oldValue, value));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch(featureID) {
      case PomPackage.ACTIVATION_PROPERTY__NAME:
        return getName();
      case PomPackage.ACTIVATION_PROPERTY__VALUE:
        return getValue();
    }
    return super.eGet(featureID, resolve, coreType);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public void eSet(int featureID, Object newValue) {
    switch(featureID) {
      case PomPackage.ACTIVATION_PROPERTY__NAME:
        setName((String) newValue);
        return;
      case PomPackage.ACTIVATION_PROPERTY__VALUE:
        setValue((String) newValue);
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
      case PomPackage.ACTIVATION_PROPERTY__NAME:
        setName(NAME_EDEFAULT);
        return;
      case PomPackage.ACTIVATION_PROPERTY__VALUE:
        setValue(VALUE_EDEFAULT);
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
      case PomPackage.ACTIVATION_PROPERTY__NAME:
        return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
      case PomPackage.ACTIVATION_PROPERTY__VALUE:
        return VALUE_EDEFAULT == null ? value != null : !VALUE_EDEFAULT.equals(value);
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
    result.append(" (name: "); //$NON-NLS-1$
    result.append(name);
    result.append(", value: "); //$NON-NLS-1$
    result.append(value);
    result.append(')');
    return result.toString();
  }

} // ActivationPropertyImpl
