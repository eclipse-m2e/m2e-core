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

import org.eclipse.m2e.model.edit.pom.ActivationOS;
import org.eclipse.m2e.model.edit.pom.PomPackage;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Activation OS</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ActivationOSImpl#getName <em> Name</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ActivationOSImpl#getFamily <em> Family</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ActivationOSImpl#getArch <em> Arch</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ActivationOSImpl#getVersion <em> Version</em>}</li>
 * </ul>
 * </p>
 * 
 * @generated
 */
public class ActivationOSImpl extends EObjectImpl implements ActivationOS {
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
   * The default value of the '{@link #getFamily() <em>Family</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getFamily()
   * @generated
   * @ordered
   */
  protected static final String FAMILY_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getFamily() <em>Family</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @see #getFamily()
   * @generated
   * @ordered
   */
  protected String family = FAMILY_EDEFAULT;

  /**
   * The default value of the '{@link #getArch() <em>Arch</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @see #getArch()
   * @generated
   * @ordered
   */
  protected static final String ARCH_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getArch() <em>Arch</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getArch()
   * @generated
   * @ordered
   */
  protected String arch = ARCH_EDEFAULT;

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
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected ActivationOSImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.ACTIVATION_OS;
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.ACTIVATION_OS__NAME, oldName, name));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getFamily() {
    return family;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setFamily(String newFamily) {
    String oldFamily = family;
    family = newFamily;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.ACTIVATION_OS__FAMILY, oldFamily, family));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getArch() {
    return arch;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setArch(String newArch) {
    String oldArch = arch;
    arch = newArch;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.ACTIVATION_OS__ARCH, oldArch, arch));
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.ACTIVATION_OS__VERSION, oldVersion, version));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch(featureID) {
      case PomPackage.ACTIVATION_OS__NAME:
        return getName();
      case PomPackage.ACTIVATION_OS__FAMILY:
        return getFamily();
      case PomPackage.ACTIVATION_OS__ARCH:
        return getArch();
      case PomPackage.ACTIVATION_OS__VERSION:
        return getVersion();
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
      case PomPackage.ACTIVATION_OS__NAME:
        setName((String) newValue);
        return;
      case PomPackage.ACTIVATION_OS__FAMILY:
        setFamily((String) newValue);
        return;
      case PomPackage.ACTIVATION_OS__ARCH:
        setArch((String) newValue);
        return;
      case PomPackage.ACTIVATION_OS__VERSION:
        setVersion((String) newValue);
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
      case PomPackage.ACTIVATION_OS__NAME:
        setName(NAME_EDEFAULT);
        return;
      case PomPackage.ACTIVATION_OS__FAMILY:
        setFamily(FAMILY_EDEFAULT);
        return;
      case PomPackage.ACTIVATION_OS__ARCH:
        setArch(ARCH_EDEFAULT);
        return;
      case PomPackage.ACTIVATION_OS__VERSION:
        setVersion(VERSION_EDEFAULT);
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
      case PomPackage.ACTIVATION_OS__NAME:
        return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
      case PomPackage.ACTIVATION_OS__FAMILY:
        return FAMILY_EDEFAULT == null ? family != null : !FAMILY_EDEFAULT.equals(family);
      case PomPackage.ACTIVATION_OS__ARCH:
        return ARCH_EDEFAULT == null ? arch != null : !ARCH_EDEFAULT.equals(arch);
      case PomPackage.ACTIVATION_OS__VERSION:
        return VERSION_EDEFAULT == null ? version != null : !VERSION_EDEFAULT.equals(version);
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
    result.append(", family: "); //$NON-NLS-1$
    result.append(family);
    result.append(", arch: "); //$NON-NLS-1$
    result.append(arch);
    result.append(", version: "); //$NON-NLS-1$
    result.append(version);
    result.append(')');
    return result.toString();
  }

} // ActivationOSImpl
