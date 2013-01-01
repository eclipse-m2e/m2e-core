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
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.Repository;
import org.eclipse.m2e.model.edit.pom.RepositoryPolicy;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Repository</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.RepositoryImpl#getReleases <em> Releases</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.RepositoryImpl#getSnapshots <em> Snapshots</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.RepositoryImpl#getId <em>Id </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.RepositoryImpl#getName <em>Name </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.RepositoryImpl#getUrl <em>Url </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.RepositoryImpl#getLayout <em> Layout</em>}</li>
 * </ul>
 * </p>
 * 
 * @generated
 */
public class RepositoryImpl extends EObjectImpl implements Repository {
  /**
   * The cached value of the '{@link #getReleases() <em>Releases</em>}' containment reference. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * 
   * @see #getReleases()
   * @generated
   * @ordered
   */
  protected RepositoryPolicy releases;

  /**
   * This is true if the Releases containment reference has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  protected boolean releasesESet;

  /**
   * The cached value of the '{@link #getSnapshots() <em>Snapshots</em>}' containment reference. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * 
   * @see #getSnapshots()
   * @generated
   * @ordered
   */
  protected RepositoryPolicy snapshots;

  /**
   * This is true if the Snapshots containment reference has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  protected boolean snapshotsESet;

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
   * The default value of the '{@link #getLayout() <em>Layout</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getLayout()
   * @generated
   * @ordered
   */
  protected static final String LAYOUT_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getLayout() <em>Layout</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @see #getLayout()
   * @generated
   * @ordered
   */
  protected String layout = LAYOUT_EDEFAULT;

  /**
   * This is true if the Layout attribute has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  protected boolean layoutESet;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected RepositoryImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.REPOSITORY;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public RepositoryPolicy getReleases() {
    return releases;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public NotificationChain basicSetReleases(RepositoryPolicy newReleases, NotificationChain msgs) {
    RepositoryPolicy oldReleases = releases;
    releases = newReleases;
    boolean oldReleasesESet = releasesESet;
    releasesESet = true;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, PomPackage.REPOSITORY__RELEASES,
          oldReleases, newReleases, !oldReleasesESet);
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
  public void setReleases(RepositoryPolicy newReleases) {
    if(newReleases != releases) {
      NotificationChain msgs = null;
      if(releases != null)
        msgs = ((InternalEObject) releases).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.REPOSITORY__RELEASES, null, msgs);
      if(newReleases != null)
        msgs = ((InternalEObject) newReleases).eInverseAdd(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.REPOSITORY__RELEASES, null, msgs);
      msgs = basicSetReleases(newReleases, msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldReleasesESet = releasesESet;
      releasesESet = true;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.REPOSITORY__RELEASES, newReleases,
            newReleases, !oldReleasesESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public NotificationChain basicUnsetReleases(NotificationChain msgs) {
    RepositoryPolicy oldReleases = releases;
    releases = null;
    boolean oldReleasesESet = releasesESet;
    releasesESet = false;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.UNSET, PomPackage.REPOSITORY__RELEASES,
          oldReleases, null, oldReleasesESet);
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
  public void unsetReleases() {
    if(releases != null) {
      NotificationChain msgs = null;
      msgs = ((InternalEObject) releases).eInverseRemove(this,
          EOPPOSITE_FEATURE_BASE - PomPackage.REPOSITORY__RELEASES, null, msgs);
      msgs = basicUnsetReleases(msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldReleasesESet = releasesESet;
      releasesESet = false;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.REPOSITORY__RELEASES, null, null,
            oldReleasesESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public boolean isSetReleases() {
    return releasesESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public RepositoryPolicy getSnapshots() {
    return snapshots;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public NotificationChain basicSetSnapshots(RepositoryPolicy newSnapshots, NotificationChain msgs) {
    RepositoryPolicy oldSnapshots = snapshots;
    snapshots = newSnapshots;
    boolean oldSnapshotsESet = snapshotsESet;
    snapshotsESet = true;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, PomPackage.REPOSITORY__SNAPSHOTS,
          oldSnapshots, newSnapshots, !oldSnapshotsESet);
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
  public void setSnapshots(RepositoryPolicy newSnapshots) {
    if(newSnapshots != snapshots) {
      NotificationChain msgs = null;
      if(snapshots != null)
        msgs = ((InternalEObject) snapshots).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.REPOSITORY__SNAPSHOTS, null, msgs);
      if(newSnapshots != null)
        msgs = ((InternalEObject) newSnapshots).eInverseAdd(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.REPOSITORY__SNAPSHOTS, null, msgs);
      msgs = basicSetSnapshots(newSnapshots, msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldSnapshotsESet = snapshotsESet;
      snapshotsESet = true;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.REPOSITORY__SNAPSHOTS, newSnapshots,
            newSnapshots, !oldSnapshotsESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public NotificationChain basicUnsetSnapshots(NotificationChain msgs) {
    RepositoryPolicy oldSnapshots = snapshots;
    snapshots = null;
    boolean oldSnapshotsESet = snapshotsESet;
    snapshotsESet = false;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.UNSET,
          PomPackage.REPOSITORY__SNAPSHOTS, oldSnapshots, null, oldSnapshotsESet);
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
  public void unsetSnapshots() {
    if(snapshots != null) {
      NotificationChain msgs = null;
      msgs = ((InternalEObject) snapshots).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
          - PomPackage.REPOSITORY__SNAPSHOTS, null, msgs);
      msgs = basicUnsetSnapshots(msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldSnapshotsESet = snapshotsESet;
      snapshotsESet = false;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.REPOSITORY__SNAPSHOTS, null, null,
            oldSnapshotsESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public boolean isSetSnapshots() {
    return snapshotsESet;
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.REPOSITORY__ID, oldId, id));
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.REPOSITORY__NAME, oldName, name));
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.REPOSITORY__URL, oldUrl, url));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getLayout() {
    return layout;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setLayout(String newLayout) {
    String oldLayout = layout;
    layout = newLayout;
    boolean oldLayoutESet = layoutESet;
    layoutESet = true;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.REPOSITORY__LAYOUT, oldLayout, layout,
          !oldLayoutESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void unsetLayout() {
    String oldLayout = layout;
    boolean oldLayoutESet = layoutESet;
    layout = LAYOUT_EDEFAULT;
    layoutESet = false;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.REPOSITORY__LAYOUT, oldLayout,
          LAYOUT_EDEFAULT, oldLayoutESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public boolean isSetLayout() {
    return layoutESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
    switch(featureID) {
      case PomPackage.REPOSITORY__RELEASES:
        return basicUnsetReleases(msgs);
      case PomPackage.REPOSITORY__SNAPSHOTS:
        return basicUnsetSnapshots(msgs);
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
      case PomPackage.REPOSITORY__RELEASES:
        return getReleases();
      case PomPackage.REPOSITORY__SNAPSHOTS:
        return getSnapshots();
      case PomPackage.REPOSITORY__ID:
        return getId();
      case PomPackage.REPOSITORY__NAME:
        return getName();
      case PomPackage.REPOSITORY__URL:
        return getUrl();
      case PomPackage.REPOSITORY__LAYOUT:
        return getLayout();
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
      case PomPackage.REPOSITORY__RELEASES:
        setReleases((RepositoryPolicy) newValue);
        return;
      case PomPackage.REPOSITORY__SNAPSHOTS:
        setSnapshots((RepositoryPolicy) newValue);
        return;
      case PomPackage.REPOSITORY__ID:
        setId((String) newValue);
        return;
      case PomPackage.REPOSITORY__NAME:
        setName((String) newValue);
        return;
      case PomPackage.REPOSITORY__URL:
        setUrl((String) newValue);
        return;
      case PomPackage.REPOSITORY__LAYOUT:
        setLayout((String) newValue);
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
      case PomPackage.REPOSITORY__RELEASES:
        unsetReleases();
        return;
      case PomPackage.REPOSITORY__SNAPSHOTS:
        unsetSnapshots();
        return;
      case PomPackage.REPOSITORY__ID:
        setId(ID_EDEFAULT);
        return;
      case PomPackage.REPOSITORY__NAME:
        setName(NAME_EDEFAULT);
        return;
      case PomPackage.REPOSITORY__URL:
        setUrl(URL_EDEFAULT);
        return;
      case PomPackage.REPOSITORY__LAYOUT:
        unsetLayout();
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
      case PomPackage.REPOSITORY__RELEASES:
        return isSetReleases();
      case PomPackage.REPOSITORY__SNAPSHOTS:
        return isSetSnapshots();
      case PomPackage.REPOSITORY__ID:
        return ID_EDEFAULT == null ? id != null : !ID_EDEFAULT.equals(id);
      case PomPackage.REPOSITORY__NAME:
        return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
      case PomPackage.REPOSITORY__URL:
        return URL_EDEFAULT == null ? url != null : !URL_EDEFAULT.equals(url);
      case PomPackage.REPOSITORY__LAYOUT:
        return isSetLayout();
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
    result.append(", url: "); //$NON-NLS-1$
    result.append(url);
    result.append(", layout: "); //$NON-NLS-1$
    if(layoutESet)
      result.append(layout);
    else
      result.append("<unset>"); //$NON-NLS-1$
    result.append(')');
    return result.toString();
  }

} // RepositoryImpl
