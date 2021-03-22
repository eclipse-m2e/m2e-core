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

import org.eclipse.m2e.model.edit.pom.Notifier;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.PropertyElement;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Notifier</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.NotifierImpl#getType <em>Type </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.NotifierImpl#getSendOnError <em> Send On Error</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.NotifierImpl#getSendOnFailure <em>Send On Failure</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.NotifierImpl#getSendOnSuccess <em>Send On Success</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.NotifierImpl#getSendOnWarning <em>Send On Warning</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.NotifierImpl#getAddress <em> Address</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.NotifierImpl#getConfiguration <em>Configuration</em>}</li>
 * </ul>
 * </p>
 * 
 * @generated
 */
public class NotifierImpl extends EObjectImpl implements Notifier {
  /**
   * The default value of the '{@link #getType() <em>Type</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @see #getType()
   * @generated
   * @ordered
   */
  protected static final String TYPE_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getType() <em>Type</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getType()
   * @generated
   * @ordered
   */
  protected String type = TYPE_EDEFAULT;

  /**
   * This is true if the Type attribute has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  protected boolean typeESet;

  /**
   * The default value of the '{@link #getSendOnError() <em>Send On Error</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getSendOnError()
   * @generated
   * @ordered
   */
  protected static final String SEND_ON_ERROR_EDEFAULT = "true"; //$NON-NLS-1$

  /**
   * The cached value of the '{@link #getSendOnError() <em>Send On Error</em>} ' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getSendOnError()
   * @generated
   * @ordered
   */
  protected String sendOnError = SEND_ON_ERROR_EDEFAULT;

  /**
   * This is true if the Send On Error attribute has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  protected boolean sendOnErrorESet;

  /**
   * The default value of the '{@link #getSendOnFailure() <em>Send On Failure</em>}' attribute. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * 
   * @see #getSendOnFailure()
   * @generated
   * @ordered
   */
  protected static final String SEND_ON_FAILURE_EDEFAULT = "true"; //$NON-NLS-1$

  /**
   * The cached value of the '{@link #getSendOnFailure() <em>Send On Failure</em>}' attribute. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * 
   * @see #getSendOnFailure()
   * @generated
   * @ordered
   */
  protected String sendOnFailure = SEND_ON_FAILURE_EDEFAULT;

  /**
   * This is true if the Send On Failure attribute has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  protected boolean sendOnFailureESet;

  /**
   * The default value of the '{@link #getSendOnSuccess() <em>Send On Success</em>}' attribute. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * 
   * @see #getSendOnSuccess()
   * @generated
   * @ordered
   */
  protected static final String SEND_ON_SUCCESS_EDEFAULT = "true"; //$NON-NLS-1$

  /**
   * The cached value of the '{@link #getSendOnSuccess() <em>Send On Success</em>}' attribute. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * 
   * @see #getSendOnSuccess()
   * @generated
   * @ordered
   */
  protected String sendOnSuccess = SEND_ON_SUCCESS_EDEFAULT;

  /**
   * This is true if the Send On Success attribute has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  protected boolean sendOnSuccessESet;

  /**
   * The default value of the '{@link #getSendOnWarning() <em>Send On Warning</em>}' attribute. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * 
   * @see #getSendOnWarning()
   * @generated
   * @ordered
   */
  protected static final String SEND_ON_WARNING_EDEFAULT = "true"; //$NON-NLS-1$

  /**
   * The cached value of the '{@link #getSendOnWarning() <em>Send On Warning</em>}' attribute. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * 
   * @see #getSendOnWarning()
   * @generated
   * @ordered
   */
  protected String sendOnWarning = SEND_ON_WARNING_EDEFAULT;

  /**
   * This is true if the Send On Warning attribute has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  protected boolean sendOnWarningESet;

  /**
   * The default value of the '{@link #getAddress() <em>Address</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getAddress()
   * @generated
   * @ordered
   */
  protected static final String ADDRESS_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getAddress() <em>Address</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getAddress()
   * @generated
   * @ordered
   */
  protected String address = ADDRESS_EDEFAULT;

  /**
   * The cached value of the '{@link #getConfiguration() <em>Configuration</em>}' containment reference list. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getConfiguration()
   * @generated
   * @ordered
   */
  protected EList<PropertyElement> configuration;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected NotifierImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.NOTIFIER;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getType() {
    return type;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setType(String newType) {
    String oldType = type;
    type = newType;
    boolean oldTypeESet = typeESet;
    typeESet = true;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.NOTIFIER__TYPE, oldType, type, !oldTypeESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void unsetType() {
    String oldType = type;
    boolean oldTypeESet = typeESet;
    type = TYPE_EDEFAULT;
    typeESet = false;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.NOTIFIER__TYPE, oldType, TYPE_EDEFAULT,
          oldTypeESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public boolean isSetType() {
    return typeESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getSendOnError() {
    return sendOnError;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setSendOnError(String newSendOnError) {
    String oldSendOnError = sendOnError;
    sendOnError = newSendOnError;
    boolean oldSendOnErrorESet = sendOnErrorESet;
    sendOnErrorESet = true;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.NOTIFIER__SEND_ON_ERROR, oldSendOnError,
          sendOnError, !oldSendOnErrorESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void unsetSendOnError() {
    String oldSendOnError = sendOnError;
    boolean oldSendOnErrorESet = sendOnErrorESet;
    sendOnError = SEND_ON_ERROR_EDEFAULT;
    sendOnErrorESet = false;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.NOTIFIER__SEND_ON_ERROR, oldSendOnError,
          SEND_ON_ERROR_EDEFAULT, oldSendOnErrorESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public boolean isSetSendOnError() {
    return sendOnErrorESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getSendOnFailure() {
    return sendOnFailure;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setSendOnFailure(String newSendOnFailure) {
    String oldSendOnFailure = sendOnFailure;
    sendOnFailure = newSendOnFailure;
    boolean oldSendOnFailureESet = sendOnFailureESet;
    sendOnFailureESet = true;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.NOTIFIER__SEND_ON_FAILURE, oldSendOnFailure,
          sendOnFailure, !oldSendOnFailureESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void unsetSendOnFailure() {
    String oldSendOnFailure = sendOnFailure;
    boolean oldSendOnFailureESet = sendOnFailureESet;
    sendOnFailure = SEND_ON_FAILURE_EDEFAULT;
    sendOnFailureESet = false;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.NOTIFIER__SEND_ON_FAILURE, oldSendOnFailure,
          SEND_ON_FAILURE_EDEFAULT, oldSendOnFailureESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public boolean isSetSendOnFailure() {
    return sendOnFailureESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getSendOnSuccess() {
    return sendOnSuccess;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setSendOnSuccess(String newSendOnSuccess) {
    String oldSendOnSuccess = sendOnSuccess;
    sendOnSuccess = newSendOnSuccess;
    boolean oldSendOnSuccessESet = sendOnSuccessESet;
    sendOnSuccessESet = true;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.NOTIFIER__SEND_ON_SUCCESS, oldSendOnSuccess,
          sendOnSuccess, !oldSendOnSuccessESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void unsetSendOnSuccess() {
    String oldSendOnSuccess = sendOnSuccess;
    boolean oldSendOnSuccessESet = sendOnSuccessESet;
    sendOnSuccess = SEND_ON_SUCCESS_EDEFAULT;
    sendOnSuccessESet = false;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.NOTIFIER__SEND_ON_SUCCESS, oldSendOnSuccess,
          SEND_ON_SUCCESS_EDEFAULT, oldSendOnSuccessESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public boolean isSetSendOnSuccess() {
    return sendOnSuccessESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getSendOnWarning() {
    return sendOnWarning;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setSendOnWarning(String newSendOnWarning) {
    String oldSendOnWarning = sendOnWarning;
    sendOnWarning = newSendOnWarning;
    boolean oldSendOnWarningESet = sendOnWarningESet;
    sendOnWarningESet = true;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.NOTIFIER__SEND_ON_WARNING, oldSendOnWarning,
          sendOnWarning, !oldSendOnWarningESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void unsetSendOnWarning() {
    String oldSendOnWarning = sendOnWarning;
    boolean oldSendOnWarningESet = sendOnWarningESet;
    sendOnWarning = SEND_ON_WARNING_EDEFAULT;
    sendOnWarningESet = false;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.NOTIFIER__SEND_ON_WARNING, oldSendOnWarning,
          SEND_ON_WARNING_EDEFAULT, oldSendOnWarningESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public boolean isSetSendOnWarning() {
    return sendOnWarningESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getAddress() {
    return address;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setAddress(String newAddress) {
    String oldAddress = address;
    address = newAddress;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.NOTIFIER__ADDRESS, oldAddress, address));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public EList<PropertyElement> getConfiguration() {
    if(configuration == null) {
      configuration = new EObjectContainmentEList.Unsettable<>(PropertyElement.class, this,
          PomPackage.NOTIFIER__CONFIGURATION);
    }
    return configuration;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void unsetConfiguration() {
    if(configuration != null)
      ((InternalEList.Unsettable<?>) configuration).unset();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public boolean isSetConfiguration() {
    return configuration != null && ((InternalEList.Unsettable<?>) configuration).isSet();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
    switch(featureID) {
      case PomPackage.NOTIFIER__CONFIGURATION:
        return ((InternalEList<?>) getConfiguration()).basicRemove(otherEnd, msgs);
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
      case PomPackage.NOTIFIER__TYPE:
        return getType();
      case PomPackage.NOTIFIER__SEND_ON_ERROR:
        return getSendOnError();
      case PomPackage.NOTIFIER__SEND_ON_FAILURE:
        return getSendOnFailure();
      case PomPackage.NOTIFIER__SEND_ON_SUCCESS:
        return getSendOnSuccess();
      case PomPackage.NOTIFIER__SEND_ON_WARNING:
        return getSendOnWarning();
      case PomPackage.NOTIFIER__ADDRESS:
        return getAddress();
      case PomPackage.NOTIFIER__CONFIGURATION:
        return getConfiguration();
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
      case PomPackage.NOTIFIER__TYPE:
        setType((String) newValue);
        return;
      case PomPackage.NOTIFIER__SEND_ON_ERROR:
        setSendOnError((String) newValue);
        return;
      case PomPackage.NOTIFIER__SEND_ON_FAILURE:
        setSendOnFailure((String) newValue);
        return;
      case PomPackage.NOTIFIER__SEND_ON_SUCCESS:
        setSendOnSuccess((String) newValue);
        return;
      case PomPackage.NOTIFIER__SEND_ON_WARNING:
        setSendOnWarning((String) newValue);
        return;
      case PomPackage.NOTIFIER__ADDRESS:
        setAddress((String) newValue);
        return;
      case PomPackage.NOTIFIER__CONFIGURATION:
        getConfiguration().clear();
        getConfiguration().addAll((Collection<? extends PropertyElement>) newValue);
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
      case PomPackage.NOTIFIER__TYPE:
        unsetType();
        return;
      case PomPackage.NOTIFIER__SEND_ON_ERROR:
        unsetSendOnError();
        return;
      case PomPackage.NOTIFIER__SEND_ON_FAILURE:
        unsetSendOnFailure();
        return;
      case PomPackage.NOTIFIER__SEND_ON_SUCCESS:
        unsetSendOnSuccess();
        return;
      case PomPackage.NOTIFIER__SEND_ON_WARNING:
        unsetSendOnWarning();
        return;
      case PomPackage.NOTIFIER__ADDRESS:
        setAddress(ADDRESS_EDEFAULT);
        return;
      case PomPackage.NOTIFIER__CONFIGURATION:
        unsetConfiguration();
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
      case PomPackage.NOTIFIER__TYPE:
        return isSetType();
      case PomPackage.NOTIFIER__SEND_ON_ERROR:
        return isSetSendOnError();
      case PomPackage.NOTIFIER__SEND_ON_FAILURE:
        return isSetSendOnFailure();
      case PomPackage.NOTIFIER__SEND_ON_SUCCESS:
        return isSetSendOnSuccess();
      case PomPackage.NOTIFIER__SEND_ON_WARNING:
        return isSetSendOnWarning();
      case PomPackage.NOTIFIER__ADDRESS:
        return ADDRESS_EDEFAULT == null ? address != null : !ADDRESS_EDEFAULT.equals(address);
      case PomPackage.NOTIFIER__CONFIGURATION:
        return isSetConfiguration();
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
    result.append(" (type: "); //$NON-NLS-1$
    if(typeESet)
      result.append(type);
    else
      result.append("<unset>"); //$NON-NLS-1$
    result.append(", sendOnError: "); //$NON-NLS-1$
    if(sendOnErrorESet)
      result.append(sendOnError);
    else
      result.append("<unset>"); //$NON-NLS-1$
    result.append(", sendOnFailure: "); //$NON-NLS-1$
    if(sendOnFailureESet)
      result.append(sendOnFailure);
    else
      result.append("<unset>"); //$NON-NLS-1$
    result.append(", sendOnSuccess: "); //$NON-NLS-1$
    if(sendOnSuccessESet)
      result.append(sendOnSuccess);
    else
      result.append("<unset>"); //$NON-NLS-1$
    result.append(", sendOnWarning: "); //$NON-NLS-1$
    if(sendOnWarningESet)
      result.append(sendOnWarning);
    else
      result.append("<unset>"); //$NON-NLS-1$
    result.append(", address: "); //$NON-NLS-1$
    result.append(address);
    result.append(')');
    return result.toString();
  }

} // NotifierImpl
