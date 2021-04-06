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
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.util.EDataTypeEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.m2e.model.edit.pom.MailingList;
import org.eclipse.m2e.model.edit.pom.PomPackage;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Mailing List</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.MailingListImpl#getName <em>Name </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.MailingListImpl#getSubscribe <em>Subscribe</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.MailingListImpl#getUnsubscribe <em>Unsubscribe</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.MailingListImpl#getPost <em>Post </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.MailingListImpl#getArchive <em> Archive</em>}</li>
 * <li>
 * {@link org.eclipse.m2e.model.edit.pom.impl.MailingListImpl#getOtherArchives <em>Other Archives</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class MailingListImpl extends EObjectImpl implements MailingList {
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
   * The default value of the '{@link #getSubscribe() <em>Subscribe</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getSubscribe()
   * @generated
   * @ordered
   */
  protected static final String SUBSCRIBE_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getSubscribe() <em>Subscribe</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getSubscribe()
   * @generated
   * @ordered
   */
  protected String subscribe = SUBSCRIBE_EDEFAULT;

  /**
   * The default value of the '{@link #getUnsubscribe() <em>Unsubscribe</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getUnsubscribe()
   * @generated
   * @ordered
   */
  protected static final String UNSUBSCRIBE_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getUnsubscribe() <em>Unsubscribe</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getUnsubscribe()
   * @generated
   * @ordered
   */
  protected String unsubscribe = UNSUBSCRIBE_EDEFAULT;

  /**
   * The default value of the '{@link #getPost() <em>Post</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   *
   * @see #getPost()
   * @generated
   * @ordered
   */
  protected static final String POST_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getPost() <em>Post</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getPost()
   * @generated
   * @ordered
   */
  protected String post = POST_EDEFAULT;

  /**
   * The default value of the '{@link #getArchive() <em>Archive</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getArchive()
   * @generated
   * @ordered
   */
  protected static final String ARCHIVE_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getArchive() <em>Archive</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getArchive()
   * @generated
   * @ordered
   */
  protected String archive = ARCHIVE_EDEFAULT;

  /**
   * The cached value of the '{@link #getOtherArchives() <em>Other Archives</em>}' attribute list. <!-- begin-user-doc
   * --> <!-- end-user-doc -->
   *
   * @see #getOtherArchives()
   * @generated
   * @ordered
   */
  protected EList<String> otherArchives;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  protected MailingListImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.MAILING_LIST;
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MAILING_LIST__NAME, oldName, name));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getSubscribe() {
    return subscribe;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setSubscribe(String newSubscribe) {
    String oldSubscribe = subscribe;
    subscribe = newSubscribe;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MAILING_LIST__SUBSCRIBE, oldSubscribe, subscribe));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getUnsubscribe() {
    return unsubscribe;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setUnsubscribe(String newUnsubscribe) {
    String oldUnsubscribe = unsubscribe;
    unsubscribe = newUnsubscribe;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MAILING_LIST__UNSUBSCRIBE, oldUnsubscribe,
          unsubscribe));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getPost() {
    return post;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setPost(String newPost) {
    String oldPost = post;
    post = newPost;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MAILING_LIST__POST, oldPost, post));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getArchive() {
    return archive;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setArchive(String newArchive) {
    String oldArchive = archive;
    archive = newArchive;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.MAILING_LIST__ARCHIVE, oldArchive, archive));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<String> getOtherArchives() {
    if(otherArchives == null) {
      otherArchives = new EDataTypeEList.Unsettable<>(String.class, this, PomPackage.MAILING_LIST__OTHER_ARCHIVES);
    }
    return otherArchives;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetOtherArchives() {
    if(otherArchives != null)
      ((InternalEList.Unsettable<?>) otherArchives).unset();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetOtherArchives() {
    return otherArchives != null && ((InternalEList.Unsettable<?>) otherArchives).isSet();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch(featureID) {
      case PomPackage.MAILING_LIST__NAME:
        return getName();
      case PomPackage.MAILING_LIST__SUBSCRIBE:
        return getSubscribe();
      case PomPackage.MAILING_LIST__UNSUBSCRIBE:
        return getUnsubscribe();
      case PomPackage.MAILING_LIST__POST:
        return getPost();
      case PomPackage.MAILING_LIST__ARCHIVE:
        return getArchive();
      case PomPackage.MAILING_LIST__OTHER_ARCHIVES:
        return getOtherArchives();
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
      case PomPackage.MAILING_LIST__NAME:
        setName((String) newValue);
        return;
      case PomPackage.MAILING_LIST__SUBSCRIBE:
        setSubscribe((String) newValue);
        return;
      case PomPackage.MAILING_LIST__UNSUBSCRIBE:
        setUnsubscribe((String) newValue);
        return;
      case PomPackage.MAILING_LIST__POST:
        setPost((String) newValue);
        return;
      case PomPackage.MAILING_LIST__ARCHIVE:
        setArchive((String) newValue);
        return;
      case PomPackage.MAILING_LIST__OTHER_ARCHIVES:
        getOtherArchives().clear();
        getOtherArchives().addAll((Collection<? extends String>) newValue);
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
      case PomPackage.MAILING_LIST__NAME:
        setName(NAME_EDEFAULT);
        return;
      case PomPackage.MAILING_LIST__SUBSCRIBE:
        setSubscribe(SUBSCRIBE_EDEFAULT);
        return;
      case PomPackage.MAILING_LIST__UNSUBSCRIBE:
        setUnsubscribe(UNSUBSCRIBE_EDEFAULT);
        return;
      case PomPackage.MAILING_LIST__POST:
        setPost(POST_EDEFAULT);
        return;
      case PomPackage.MAILING_LIST__ARCHIVE:
        setArchive(ARCHIVE_EDEFAULT);
        return;
      case PomPackage.MAILING_LIST__OTHER_ARCHIVES:
        unsetOtherArchives();
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
      case PomPackage.MAILING_LIST__NAME:
        return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
      case PomPackage.MAILING_LIST__SUBSCRIBE:
        return SUBSCRIBE_EDEFAULT == null ? subscribe != null : !SUBSCRIBE_EDEFAULT.equals(subscribe);
      case PomPackage.MAILING_LIST__UNSUBSCRIBE:
        return UNSUBSCRIBE_EDEFAULT == null ? unsubscribe != null : !UNSUBSCRIBE_EDEFAULT.equals(unsubscribe);
      case PomPackage.MAILING_LIST__POST:
        return POST_EDEFAULT == null ? post != null : !POST_EDEFAULT.equals(post);
      case PomPackage.MAILING_LIST__ARCHIVE:
        return ARCHIVE_EDEFAULT == null ? archive != null : !ARCHIVE_EDEFAULT.equals(archive);
      case PomPackage.MAILING_LIST__OTHER_ARCHIVES:
        return isSetOtherArchives();
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
    result.append(", subscribe: "); //$NON-NLS-1$
    result.append(subscribe);
    result.append(", unsubscribe: "); //$NON-NLS-1$
    result.append(unsubscribe);
    result.append(", post: "); //$NON-NLS-1$
    result.append(post);
    result.append(", archive: "); //$NON-NLS-1$
    result.append(archive);
    result.append(", otherArchives: "); //$NON-NLS-1$
    result.append(otherArchives);
    result.append(')');
    return result.toString();
  }

} // MailingListImpl
