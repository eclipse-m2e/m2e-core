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
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.m2e.model.edit.pom.Activation;
import org.eclipse.m2e.model.edit.pom.ActivationFile;
import org.eclipse.m2e.model.edit.pom.ActivationOS;
import org.eclipse.m2e.model.edit.pom.ActivationProperty;
import org.eclipse.m2e.model.edit.pom.PomPackage;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Activation</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>
 * {@link org.eclipse.m2e.model.edit.pom.impl.ActivationImpl#getActiveByDefault <em>Active By Default</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ActivationImpl#getJdk <em>Jdk </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ActivationImpl#getOs <em>Os </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ActivationImpl#getProperty <em> Property</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ActivationImpl#getFile <em>File </em>}</li>
 * </ul>
 * </p>
 * 
 * @generated
 */
public class ActivationImpl extends EObjectImpl implements Activation {
  /**
   * The default value of the '{@link #getActiveByDefault() <em>Active By Default</em>}' attribute. <!-- begin-user-doc
   * --> <!-- end-user-doc -->
   * 
   * @see #getActiveByDefault()
   * @generated
   * @ordered
   */
  protected static final String ACTIVE_BY_DEFAULT_EDEFAULT = "false"; //$NON-NLS-1$

  /**
   * The cached value of the '{@link #getActiveByDefault() <em>Active By Default</em>}' attribute. <!-- begin-user-doc
   * --> <!-- end-user-doc -->
   * 
   * @see #getActiveByDefault()
   * @generated
   * @ordered
   */
  protected String activeByDefault = ACTIVE_BY_DEFAULT_EDEFAULT;

  /**
   * This is true if the Active By Default attribute has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  protected boolean activeByDefaultESet;

  /**
   * The default value of the '{@link #getJdk() <em>Jdk</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getJdk()
   * @generated
   * @ordered
   */
  protected static final String JDK_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getJdk() <em>Jdk</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getJdk()
   * @generated
   * @ordered
   */
  protected String jdk = JDK_EDEFAULT;

  /**
   * The cached value of the '{@link #getOs() <em>Os</em>}' containment reference. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getOs()
   * @generated
   * @ordered
   */
  protected ActivationOS os;

  /**
   * This is true if the Os containment reference has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  protected boolean osESet;

  /**
   * The cached value of the '{@link #getProperty() <em>Property</em>}' containment reference. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * 
   * @see #getProperty()
   * @generated
   * @ordered
   */
  protected ActivationProperty property;

  /**
   * This is true if the Property containment reference has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  protected boolean propertyESet;

  /**
   * The cached value of the '{@link #getFile() <em>File</em>}' containment reference. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getFile()
   * @generated
   * @ordered
   */
  protected ActivationFile file;

  /**
   * This is true if the File containment reference has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  protected boolean fileESet;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected ActivationImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.ACTIVATION;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getActiveByDefault() {
    return activeByDefault;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setActiveByDefault(String newActiveByDefault) {
    String oldActiveByDefault = activeByDefault;
    activeByDefault = newActiveByDefault;
    boolean oldActiveByDefaultESet = activeByDefaultESet;
    activeByDefaultESet = true;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.ACTIVATION__ACTIVE_BY_DEFAULT,
          oldActiveByDefault, activeByDefault, !oldActiveByDefaultESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void unsetActiveByDefault() {
    String oldActiveByDefault = activeByDefault;
    boolean oldActiveByDefaultESet = activeByDefaultESet;
    activeByDefault = ACTIVE_BY_DEFAULT_EDEFAULT;
    activeByDefaultESet = false;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.ACTIVATION__ACTIVE_BY_DEFAULT,
          oldActiveByDefault, ACTIVE_BY_DEFAULT_EDEFAULT, oldActiveByDefaultESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public boolean isSetActiveByDefault() {
    return activeByDefaultESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getJdk() {
    return jdk;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setJdk(String newJdk) {
    String oldJdk = jdk;
    jdk = newJdk;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.ACTIVATION__JDK, oldJdk, jdk));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public ActivationOS getOs() {
    return os;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public NotificationChain basicSetOs(ActivationOS newOs, NotificationChain msgs) {
    ActivationOS oldOs = os;
    os = newOs;
    boolean oldOsESet = osESet;
    osESet = true;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, PomPackage.ACTIVATION__OS, oldOs,
          newOs, !oldOsESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setOs(ActivationOS newOs) {
    if(newOs != os) {
      NotificationChain msgs = null;
      if(os != null)
        msgs = ((InternalEObject) os).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - PomPackage.ACTIVATION__OS, null,
            msgs);
      if(newOs != null)
        msgs = ((InternalEObject) newOs).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - PomPackage.ACTIVATION__OS, null,
            msgs);
      msgs = basicSetOs(newOs, msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldOsESet = osESet;
      osESet = true;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.ACTIVATION__OS, newOs, newOs, !oldOsESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public NotificationChain basicUnsetOs(NotificationChain msgs) {
    ActivationOS oldOs = os;
    os = null;
    boolean oldOsESet = osESet;
    osESet = false;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.UNSET, PomPackage.ACTIVATION__OS,
          oldOs, null, oldOsESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void unsetOs() {
    if(os != null) {
      NotificationChain msgs = null;
      msgs = ((InternalEObject) os)
          .eInverseRemove(this, EOPPOSITE_FEATURE_BASE - PomPackage.ACTIVATION__OS, null, msgs);
      msgs = basicUnsetOs(msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldOsESet = osESet;
      osESet = false;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.ACTIVATION__OS, null, null, oldOsESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public boolean isSetOs() {
    return osESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public ActivationProperty getProperty() {
    return property;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public NotificationChain basicSetProperty(ActivationProperty newProperty, NotificationChain msgs) {
    ActivationProperty oldProperty = property;
    property = newProperty;
    boolean oldPropertyESet = propertyESet;
    propertyESet = true;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, PomPackage.ACTIVATION__PROPERTY,
          oldProperty, newProperty, !oldPropertyESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setProperty(ActivationProperty newProperty) {
    if(newProperty != property) {
      NotificationChain msgs = null;
      if(property != null)
        msgs = ((InternalEObject) property).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.ACTIVATION__PROPERTY, null, msgs);
      if(newProperty != null)
        msgs = ((InternalEObject) newProperty).eInverseAdd(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.ACTIVATION__PROPERTY, null, msgs);
      msgs = basicSetProperty(newProperty, msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldPropertyESet = propertyESet;
      propertyESet = true;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.ACTIVATION__PROPERTY, newProperty,
            newProperty, !oldPropertyESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public NotificationChain basicUnsetProperty(NotificationChain msgs) {
    ActivationProperty oldProperty = property;
    property = null;
    boolean oldPropertyESet = propertyESet;
    propertyESet = false;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.UNSET, PomPackage.ACTIVATION__PROPERTY,
          oldProperty, null, oldPropertyESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void unsetProperty() {
    if(property != null) {
      NotificationChain msgs = null;
      msgs = ((InternalEObject) property).eInverseRemove(this,
          EOPPOSITE_FEATURE_BASE - PomPackage.ACTIVATION__PROPERTY, null, msgs);
      msgs = basicUnsetProperty(msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldPropertyESet = propertyESet;
      propertyESet = false;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.ACTIVATION__PROPERTY, null, null,
            oldPropertyESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public boolean isSetProperty() {
    return propertyESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public ActivationFile getFile() {
    return file;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public NotificationChain basicSetFile(ActivationFile newFile, NotificationChain msgs) {
    ActivationFile oldFile = file;
    file = newFile;
    boolean oldFileESet = fileESet;
    fileESet = true;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, PomPackage.ACTIVATION__FILE,
          oldFile, newFile, !oldFileESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setFile(ActivationFile newFile) {
    if(newFile != file) {
      NotificationChain msgs = null;
      if(file != null)
        msgs = ((InternalEObject) file).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - PomPackage.ACTIVATION__FILE,
            null, msgs);
      if(newFile != null)
        msgs = ((InternalEObject) newFile).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - PomPackage.ACTIVATION__FILE,
            null, msgs);
      msgs = basicSetFile(newFile, msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldFileESet = fileESet;
      fileESet = true;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.ACTIVATION__FILE, newFile, newFile,
            !oldFileESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public NotificationChain basicUnsetFile(NotificationChain msgs) {
    ActivationFile oldFile = file;
    file = null;
    boolean oldFileESet = fileESet;
    fileESet = false;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.UNSET, PomPackage.ACTIVATION__FILE,
          oldFile, null, oldFileESet);
      if(msgs == null)
        msgs = notification;
      else
        msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void unsetFile() {
    if(file != null) {
      NotificationChain msgs = null;
      msgs = ((InternalEObject) file).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - PomPackage.ACTIVATION__FILE, null,
          msgs);
      msgs = basicUnsetFile(msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldFileESet = fileESet;
      fileESet = false;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.ACTIVATION__FILE, null, null, oldFileESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public boolean isSetFile() {
    return fileESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
    switch(featureID) {
      case PomPackage.ACTIVATION__OS:
        return basicUnsetOs(msgs);
      case PomPackage.ACTIVATION__PROPERTY:
        return basicUnsetProperty(msgs);
      case PomPackage.ACTIVATION__FILE:
        return basicUnsetFile(msgs);
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
      case PomPackage.ACTIVATION__ACTIVE_BY_DEFAULT:
        return getActiveByDefault();
      case PomPackage.ACTIVATION__JDK:
        return getJdk();
      case PomPackage.ACTIVATION__OS:
        return getOs();
      case PomPackage.ACTIVATION__PROPERTY:
        return getProperty();
      case PomPackage.ACTIVATION__FILE:
        return getFile();
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
      case PomPackage.ACTIVATION__ACTIVE_BY_DEFAULT:
        setActiveByDefault((String) newValue);
        return;
      case PomPackage.ACTIVATION__JDK:
        setJdk((String) newValue);
        return;
      case PomPackage.ACTIVATION__OS:
        setOs((ActivationOS) newValue);
        return;
      case PomPackage.ACTIVATION__PROPERTY:
        setProperty((ActivationProperty) newValue);
        return;
      case PomPackage.ACTIVATION__FILE:
        setFile((ActivationFile) newValue);
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
      case PomPackage.ACTIVATION__ACTIVE_BY_DEFAULT:
        unsetActiveByDefault();
        return;
      case PomPackage.ACTIVATION__JDK:
        setJdk(JDK_EDEFAULT);
        return;
      case PomPackage.ACTIVATION__OS:
        unsetOs();
        return;
      case PomPackage.ACTIVATION__PROPERTY:
        unsetProperty();
        return;
      case PomPackage.ACTIVATION__FILE:
        unsetFile();
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
      case PomPackage.ACTIVATION__ACTIVE_BY_DEFAULT:
        return isSetActiveByDefault();
      case PomPackage.ACTIVATION__JDK:
        return JDK_EDEFAULT == null ? jdk != null : !JDK_EDEFAULT.equals(jdk);
      case PomPackage.ACTIVATION__OS:
        return isSetOs();
      case PomPackage.ACTIVATION__PROPERTY:
        return isSetProperty();
      case PomPackage.ACTIVATION__FILE:
        return isSetFile();
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
    result.append(" (activeByDefault: "); //$NON-NLS-1$
    if(activeByDefaultESet)
      result.append(activeByDefault);
    else
      result.append("<unset>"); //$NON-NLS-1$
    result.append(", jdk: "); //$NON-NLS-1$
    result.append(jdk);
    result.append(')');
    return result.toString();
  }

} // ActivationImpl
