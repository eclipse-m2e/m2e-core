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
import org.eclipse.m2e.model.edit.pom.Scm;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Scm</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ScmImpl#getConnection <em> Connection</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ScmImpl#getDeveloperConnection <em>Developer Connection</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ScmImpl#getTag <em>Tag</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ScmImpl#getUrl <em>Url</em>}</li>
 * </ul>
 * </p>
 * 
 * @generated
 */
public class ScmImpl extends EObjectImpl implements Scm {
  /**
   * The default value of the '{@link #getConnection() <em>Connection</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getConnection()
   * @generated
   * @ordered
   */
  protected static final String CONNECTION_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getConnection() <em>Connection</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getConnection()
   * @generated
   * @ordered
   */
  protected String connection = CONNECTION_EDEFAULT;

  /**
   * The default value of the '{@link #getDeveloperConnection() <em>Developer Connection</em>}' attribute. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getDeveloperConnection()
   * @generated
   * @ordered
   */
  protected static final String DEVELOPER_CONNECTION_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getDeveloperConnection() <em>Developer Connection</em>}' attribute. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getDeveloperConnection()
   * @generated
   * @ordered
   */
  protected String developerConnection = DEVELOPER_CONNECTION_EDEFAULT;

  /**
   * The default value of the '{@link #getTag() <em>Tag</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getTag()
   * @generated
   * @ordered
   */
  protected static final String TAG_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getTag() <em>Tag</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getTag()
   * @generated
   * @ordered
   */
  protected String tag = TAG_EDEFAULT;

  /**
   * This is true if the Tag attribute has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  protected boolean tagESet;

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
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected ScmImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.SCM;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getConnection() {
    return connection;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setConnection(String newConnection) {
    String oldConnection = connection;
    connection = newConnection;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.SCM__CONNECTION, oldConnection, connection));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getDeveloperConnection() {
    return developerConnection;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setDeveloperConnection(String newDeveloperConnection) {
    String oldDeveloperConnection = developerConnection;
    developerConnection = newDeveloperConnection;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.SCM__DEVELOPER_CONNECTION,
          oldDeveloperConnection, developerConnection));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getTag() {
    return tag;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setTag(String newTag) {
    String oldTag = tag;
    tag = newTag;
    boolean oldTagESet = tagESet;
    tagESet = true;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.SCM__TAG, oldTag, tag, !oldTagESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void unsetTag() {
    String oldTag = tag;
    boolean oldTagESet = tagESet;
    tag = TAG_EDEFAULT;
    tagESet = false;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.SCM__TAG, oldTag, TAG_EDEFAULT, oldTagESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public boolean isSetTag() {
    return tagESet;
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.SCM__URL, oldUrl, url));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch(featureID) {
      case PomPackage.SCM__CONNECTION:
        return getConnection();
      case PomPackage.SCM__DEVELOPER_CONNECTION:
        return getDeveloperConnection();
      case PomPackage.SCM__TAG:
        return getTag();
      case PomPackage.SCM__URL:
        return getUrl();
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
      case PomPackage.SCM__CONNECTION:
        setConnection((String) newValue);
        return;
      case PomPackage.SCM__DEVELOPER_CONNECTION:
        setDeveloperConnection((String) newValue);
        return;
      case PomPackage.SCM__TAG:
        setTag((String) newValue);
        return;
      case PomPackage.SCM__URL:
        setUrl((String) newValue);
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
      case PomPackage.SCM__CONNECTION:
        setConnection(CONNECTION_EDEFAULT);
        return;
      case PomPackage.SCM__DEVELOPER_CONNECTION:
        setDeveloperConnection(DEVELOPER_CONNECTION_EDEFAULT);
        return;
      case PomPackage.SCM__TAG:
        unsetTag();
        return;
      case PomPackage.SCM__URL:
        setUrl(URL_EDEFAULT);
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
      case PomPackage.SCM__CONNECTION:
        return CONNECTION_EDEFAULT == null ? connection != null : !CONNECTION_EDEFAULT.equals(connection);
      case PomPackage.SCM__DEVELOPER_CONNECTION:
        return DEVELOPER_CONNECTION_EDEFAULT == null ? developerConnection != null : !DEVELOPER_CONNECTION_EDEFAULT
            .equals(developerConnection);
      case PomPackage.SCM__TAG:
        return isSetTag();
      case PomPackage.SCM__URL:
        return URL_EDEFAULT == null ? url != null : !URL_EDEFAULT.equals(url);
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
    result.append(" (connection: "); //$NON-NLS-1$
    result.append(connection);
    result.append(", developerConnection: "); //$NON-NLS-1$
    result.append(developerConnection);
    result.append(", tag: "); //$NON-NLS-1$
    if(tagESet)
      result.append(tag);
    else
      result.append("<unset>"); //$NON-NLS-1$
    result.append(", url: "); //$NON-NLS-1$
    result.append(url);
    result.append(')');
    return result.toString();
  }

} // ScmImpl
