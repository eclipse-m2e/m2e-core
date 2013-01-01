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

package org.eclipse.m2e.model.edit.pom.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.Prerequisites;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Prerequisites</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.PrerequisitesImpl#getMaven <em> Maven</em>}</li>
 * </ul>
 * </p>
 * 
 * @generated
 */
public class PrerequisitesImpl extends EObjectImpl implements Prerequisites {
  /**
   * The default value of the '{@link #getMaven() <em>Maven</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @see #getMaven()
   * @generated
   * @ordered
   */
  protected static final String MAVEN_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getMaven() <em>Maven</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @see #getMaven()
   * @generated
   * @ordered
   */
  protected String maven = MAVEN_EDEFAULT;

  /**
   * This is true if the Maven attribute has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  protected boolean mavenESet;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected PrerequisitesImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.PREREQUISITES;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getMaven() {
    return maven;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setMaven(String newMaven) {
    String oldMaven = maven;
    maven = newMaven;
    boolean oldMavenESet = mavenESet;
    mavenESet = true;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PREREQUISITES__MAVEN, oldMaven, maven,
          !oldMavenESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void unsetMaven() {
    String oldMaven = maven;
    boolean oldMavenESet = mavenESet;
    maven = MAVEN_EDEFAULT;
    mavenESet = false;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.PREREQUISITES__MAVEN, oldMaven,
          MAVEN_EDEFAULT, oldMavenESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public boolean isSetMaven() {
    return mavenESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch(featureID) {
      case PomPackage.PREREQUISITES__MAVEN:
        return getMaven();
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
      case PomPackage.PREREQUISITES__MAVEN:
        setMaven((String) newValue);
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
      case PomPackage.PREREQUISITES__MAVEN:
        unsetMaven();
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
      case PomPackage.PREREQUISITES__MAVEN:
        return isSetMaven();
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
    result.append(" (maven: "); //$NON-NLS-1$
    if(mavenESet)
      result.append(maven);
    else
      result.append("<unset>"); //$NON-NLS-1$
    result.append(')');
    return result.toString();
  }

} // PrerequisitesImpl
