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
import org.eclipse.emf.ecore.util.EDataTypeEList;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.m2e.model.edit.pom.Developer;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.PropertyElement;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Developer</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.DeveloperImpl#getId <em>Id</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.DeveloperImpl#getName <em>Name </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.DeveloperImpl#getEmail <em>Email </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.DeveloperImpl#getUrl <em>Url </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.DeveloperImpl#getOrganization <em>Organization</em>}</li>
 * <li>
 * {@link org.eclipse.m2e.model.edit.pom.impl.DeveloperImpl#getOrganizationUrl <em>Organization Url</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.DeveloperImpl#getTimezone <em> Timezone</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.DeveloperImpl#getProperties <em> Properties</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.DeveloperImpl#getRoles <em>Roles </em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class DeveloperImpl extends EObjectImpl implements Developer {
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
   * The default value of the '{@link #getEmail() <em>Email</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   *
   * @see #getEmail()
   * @generated
   * @ordered
   */
  protected static final String EMAIL_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getEmail() <em>Email</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   *
   * @see #getEmail()
   * @generated
   * @ordered
   */
  protected String email = EMAIL_EDEFAULT;

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
   * The default value of the '{@link #getOrganization() <em>Organization</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getOrganization()
   * @generated
   * @ordered
   */
  protected static final String ORGANIZATION_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getOrganization() <em>Organization</em>} ' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getOrganization()
   * @generated
   * @ordered
   */
  protected String organization = ORGANIZATION_EDEFAULT;

  /**
   * The default value of the '{@link #getOrganizationUrl() <em>Organization Url</em>}' attribute. <!-- begin-user-doc
   * --> <!-- end-user-doc -->
   *
   * @see #getOrganizationUrl()
   * @generated
   * @ordered
   */
  protected static final String ORGANIZATION_URL_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getOrganizationUrl() <em>Organization Url</em>}' attribute. <!-- begin-user-doc
   * --> <!-- end-user-doc -->
   *
   * @see #getOrganizationUrl()
   * @generated
   * @ordered
   */
  protected String organizationUrl = ORGANIZATION_URL_EDEFAULT;

  /**
   * The default value of the '{@link #getTimezone() <em>Timezone</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getTimezone()
   * @generated
   * @ordered
   */
  protected static final String TIMEZONE_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getTimezone() <em>Timezone</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getTimezone()
   * @generated
   * @ordered
   */
  protected String timezone = TIMEZONE_EDEFAULT;

  /**
   * The cached value of the '{@link #getProperties() <em>Properties</em>}' containment reference list. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getProperties()
   * @generated
   * @ordered
   */
  protected EList<PropertyElement> properties;

  /**
   * The cached value of the '{@link #getRoles() <em>Roles</em>}' attribute list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getRoles()
   * @generated
   * @ordered
   */
  protected EList<String> roles;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  protected DeveloperImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.DEVELOPER;
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
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.DEVELOPER__ID, oldId, id));
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.DEVELOPER__NAME, oldName, name));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getEmail() {
    return email;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setEmail(String newEmail) {
    String oldEmail = email;
    email = newEmail;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.DEVELOPER__EMAIL, oldEmail, email));
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.DEVELOPER__URL, oldUrl, url));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getOrganization() {
    return organization;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setOrganization(String newOrganization) {
    String oldOrganization = organization;
    organization = newOrganization;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.DEVELOPER__ORGANIZATION, oldOrganization,
          organization));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getOrganizationUrl() {
    return organizationUrl;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setOrganizationUrl(String newOrganizationUrl) {
    String oldOrganizationUrl = organizationUrl;
    organizationUrl = newOrganizationUrl;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.DEVELOPER__ORGANIZATION_URL, oldOrganizationUrl,
          organizationUrl));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getTimezone() {
    return timezone;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setTimezone(String newTimezone) {
    String oldTimezone = timezone;
    timezone = newTimezone;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.DEVELOPER__TIMEZONE, oldTimezone, timezone));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<PropertyElement> getProperties() {
    if(properties == null) {
      properties = new EObjectContainmentEList.Unsettable<>(PropertyElement.class, this,
          PomPackage.DEVELOPER__PROPERTIES);
    }
    return properties;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetProperties() {
    if(properties != null)
      ((InternalEList.Unsettable<?>) properties).unset();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetProperties() {
    return properties != null && ((InternalEList.Unsettable<?>) properties).isSet();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<String> getRoles() {
    if(roles == null) {
      roles = new EDataTypeEList.Unsettable<>(String.class, this, PomPackage.DEVELOPER__ROLES);
    }
    return roles;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetRoles() {
    if(roles != null)
      ((InternalEList.Unsettable<?>) roles).unset();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetRoles() {
    return roles != null && ((InternalEList.Unsettable<?>) roles).isSet();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
    switch(featureID) {
      case PomPackage.DEVELOPER__PROPERTIES:
        return ((InternalEList<?>) getProperties()).basicRemove(otherEnd, msgs);
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
      case PomPackage.DEVELOPER__ID:
        return getId();
      case PomPackage.DEVELOPER__NAME:
        return getName();
      case PomPackage.DEVELOPER__EMAIL:
        return getEmail();
      case PomPackage.DEVELOPER__URL:
        return getUrl();
      case PomPackage.DEVELOPER__ORGANIZATION:
        return getOrganization();
      case PomPackage.DEVELOPER__ORGANIZATION_URL:
        return getOrganizationUrl();
      case PomPackage.DEVELOPER__TIMEZONE:
        return getTimezone();
      case PomPackage.DEVELOPER__PROPERTIES:
        return getProperties();
      case PomPackage.DEVELOPER__ROLES:
        return getRoles();
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
      case PomPackage.DEVELOPER__ID:
        setId((String) newValue);
        return;
      case PomPackage.DEVELOPER__NAME:
        setName((String) newValue);
        return;
      case PomPackage.DEVELOPER__EMAIL:
        setEmail((String) newValue);
        return;
      case PomPackage.DEVELOPER__URL:
        setUrl((String) newValue);
        return;
      case PomPackage.DEVELOPER__ORGANIZATION:
        setOrganization((String) newValue);
        return;
      case PomPackage.DEVELOPER__ORGANIZATION_URL:
        setOrganizationUrl((String) newValue);
        return;
      case PomPackage.DEVELOPER__TIMEZONE:
        setTimezone((String) newValue);
        return;
      case PomPackage.DEVELOPER__PROPERTIES:
        getProperties().clear();
        getProperties().addAll((Collection<? extends PropertyElement>) newValue);
        return;
      case PomPackage.DEVELOPER__ROLES:
        getRoles().clear();
        getRoles().addAll((Collection<? extends String>) newValue);
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
      case PomPackage.DEVELOPER__ID:
        setId(ID_EDEFAULT);
        return;
      case PomPackage.DEVELOPER__NAME:
        setName(NAME_EDEFAULT);
        return;
      case PomPackage.DEVELOPER__EMAIL:
        setEmail(EMAIL_EDEFAULT);
        return;
      case PomPackage.DEVELOPER__URL:
        setUrl(URL_EDEFAULT);
        return;
      case PomPackage.DEVELOPER__ORGANIZATION:
        setOrganization(ORGANIZATION_EDEFAULT);
        return;
      case PomPackage.DEVELOPER__ORGANIZATION_URL:
        setOrganizationUrl(ORGANIZATION_URL_EDEFAULT);
        return;
      case PomPackage.DEVELOPER__TIMEZONE:
        setTimezone(TIMEZONE_EDEFAULT);
        return;
      case PomPackage.DEVELOPER__PROPERTIES:
        unsetProperties();
        return;
      case PomPackage.DEVELOPER__ROLES:
        unsetRoles();
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
      case PomPackage.DEVELOPER__ID:
        return ID_EDEFAULT == null ? id != null : !ID_EDEFAULT.equals(id);
      case PomPackage.DEVELOPER__NAME:
        return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
      case PomPackage.DEVELOPER__EMAIL:
        return EMAIL_EDEFAULT == null ? email != null : !EMAIL_EDEFAULT.equals(email);
      case PomPackage.DEVELOPER__URL:
        return URL_EDEFAULT == null ? url != null : !URL_EDEFAULT.equals(url);
      case PomPackage.DEVELOPER__ORGANIZATION:
        return ORGANIZATION_EDEFAULT == null ? organization != null : !ORGANIZATION_EDEFAULT.equals(organization);
      case PomPackage.DEVELOPER__ORGANIZATION_URL:
        return ORGANIZATION_URL_EDEFAULT == null ? organizationUrl != null : !ORGANIZATION_URL_EDEFAULT
            .equals(organizationUrl);
      case PomPackage.DEVELOPER__TIMEZONE:
        return TIMEZONE_EDEFAULT == null ? timezone != null : !TIMEZONE_EDEFAULT.equals(timezone);
      case PomPackage.DEVELOPER__PROPERTIES:
        return isSetProperties();
      case PomPackage.DEVELOPER__ROLES:
        return isSetRoles();
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
    result.append(id);
    result.append(", name: "); //$NON-NLS-1$
    result.append(name);
    result.append(", email: "); //$NON-NLS-1$
    result.append(email);
    result.append(", url: "); //$NON-NLS-1$
    result.append(url);
    result.append(", organization: "); //$NON-NLS-1$
    result.append(organization);
    result.append(", organizationUrl: "); //$NON-NLS-1$
    result.append(organizationUrl);
    result.append(", timezone: "); //$NON-NLS-1$
    result.append(timezone);
    result.append(", roles: "); //$NON-NLS-1$
    result.append(roles);
    result.append(')');
    return result.toString();
  }

} // DeveloperImpl
