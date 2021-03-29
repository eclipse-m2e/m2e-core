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

import org.eclipse.m2e.model.edit.pom.License;
import org.eclipse.m2e.model.edit.pom.PomPackage;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>License</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.LicenseImpl#getName <em>Name </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.LicenseImpl#getUrl <em>Url</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.LicenseImpl#getDistribution <em> Distribution</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.LicenseImpl#getComments <em> Comments</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class LicenseImpl extends EObjectImpl implements License {
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
   * The default value of the '{@link #getDistribution() <em>Distribution</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getDistribution()
   * @generated
   * @ordered
   */
  protected static final String DISTRIBUTION_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getDistribution() <em>Distribution</em>} ' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getDistribution()
   * @generated
   * @ordered
   */
  protected String distribution = DISTRIBUTION_EDEFAULT;

  /**
   * The default value of the '{@link #getComments() <em>Comments</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getComments()
   * @generated
   * @ordered
   */
  protected static final String COMMENTS_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getComments() <em>Comments</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getComments()
   * @generated
   * @ordered
   */
  protected String comments = COMMENTS_EDEFAULT;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  protected LicenseImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.LICENSE;
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.LICENSE__NAME, oldName, name));
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.LICENSE__URL, oldUrl, url));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getDistribution() {
    return distribution;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setDistribution(String newDistribution) {
    String oldDistribution = distribution;
    distribution = newDistribution;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.LICENSE__DISTRIBUTION, oldDistribution,
          distribution));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getComments() {
    return comments;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setComments(String newComments) {
    String oldComments = comments;
    comments = newComments;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.LICENSE__COMMENTS, oldComments, comments));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch(featureID) {
      case PomPackage.LICENSE__NAME:
        return getName();
      case PomPackage.LICENSE__URL:
        return getUrl();
      case PomPackage.LICENSE__DISTRIBUTION:
        return getDistribution();
      case PomPackage.LICENSE__COMMENTS:
        return getComments();
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
      case PomPackage.LICENSE__NAME:
        setName((String) newValue);
        return;
      case PomPackage.LICENSE__URL:
        setUrl((String) newValue);
        return;
      case PomPackage.LICENSE__DISTRIBUTION:
        setDistribution((String) newValue);
        return;
      case PomPackage.LICENSE__COMMENTS:
        setComments((String) newValue);
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
      case PomPackage.LICENSE__NAME:
        setName(NAME_EDEFAULT);
        return;
      case PomPackage.LICENSE__URL:
        setUrl(URL_EDEFAULT);
        return;
      case PomPackage.LICENSE__DISTRIBUTION:
        setDistribution(DISTRIBUTION_EDEFAULT);
        return;
      case PomPackage.LICENSE__COMMENTS:
        setComments(COMMENTS_EDEFAULT);
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
      case PomPackage.LICENSE__NAME:
        return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
      case PomPackage.LICENSE__URL:
        return URL_EDEFAULT == null ? url != null : !URL_EDEFAULT.equals(url);
      case PomPackage.LICENSE__DISTRIBUTION:
        return DISTRIBUTION_EDEFAULT == null ? distribution != null : !DISTRIBUTION_EDEFAULT.equals(distribution);
      case PomPackage.LICENSE__COMMENTS:
        return COMMENTS_EDEFAULT == null ? comments != null : !COMMENTS_EDEFAULT.equals(comments);
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
    result.append(", url: "); //$NON-NLS-1$
    result.append(url);
    result.append(", distribution: "); //$NON-NLS-1$
    result.append(distribution);
    result.append(", comments: "); //$NON-NLS-1$
    result.append(comments);
    result.append(')');
    return result.toString();
  }

} // LicenseImpl
