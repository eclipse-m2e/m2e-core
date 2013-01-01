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

import org.eclipse.m2e.model.edit.pom.Parent;
import org.eclipse.m2e.model.edit.pom.PomPackage;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Parent</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ParentImpl#getArtifactId <em> Artifact Id</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ParentImpl#getGroupId <em>Group Id</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ParentImpl#getVersion <em> Version</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ParentImpl#getRelativePath <em> Relative Path</em>}</li>
 * </ul>
 * </p>
 * 
 * @generated
 */
public class ParentImpl extends EObjectImpl implements Parent {
  /**
   * The default value of the '{@link #getArtifactId() <em>Artifact Id</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getArtifactId()
   * @generated
   * @ordered
   */
  protected static final String ARTIFACT_ID_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getArtifactId() <em>Artifact Id</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getArtifactId()
   * @generated
   * @ordered
   */
  protected String artifactId = ARTIFACT_ID_EDEFAULT;

  /**
   * The default value of the '{@link #getGroupId() <em>Group Id</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getGroupId()
   * @generated
   * @ordered
   */
  protected static final String GROUP_ID_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getGroupId() <em>Group Id</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getGroupId()
   * @generated
   * @ordered
   */
  protected String groupId = GROUP_ID_EDEFAULT;

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
   * The default value of the '{@link #getRelativePath() <em>Relative Path</em>}' attribute. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * 
   * @see #getRelativePath()
   * @generated
   * @ordered
   */
  protected static final String RELATIVE_PATH_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getRelativePath() <em>Relative Path</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getRelativePath()
   * @generated
   * @ordered
   */
  protected String relativePath = RELATIVE_PATH_EDEFAULT;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected ParentImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.PARENT;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getArtifactId() {
    return artifactId;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setArtifactId(String newArtifactId) {
    String oldArtifactId = artifactId;
    artifactId = newArtifactId;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PARENT__ARTIFACT_ID, oldArtifactId, artifactId));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setGroupId(String newGroupId) {
    String oldGroupId = groupId;
    groupId = newGroupId;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PARENT__GROUP_ID, oldGroupId, groupId));
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PARENT__VERSION, oldVersion, version));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getRelativePath() {
    return relativePath;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setRelativePath(String newRelativePath) {
    String oldRelativePath = relativePath;
    relativePath = newRelativePath;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PARENT__RELATIVE_PATH, oldRelativePath,
          relativePath));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch(featureID) {
      case PomPackage.PARENT__ARTIFACT_ID:
        return getArtifactId();
      case PomPackage.PARENT__GROUP_ID:
        return getGroupId();
      case PomPackage.PARENT__VERSION:
        return getVersion();
      case PomPackage.PARENT__RELATIVE_PATH:
        return getRelativePath();
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
      case PomPackage.PARENT__ARTIFACT_ID:
        setArtifactId((String) newValue);
        return;
      case PomPackage.PARENT__GROUP_ID:
        setGroupId((String) newValue);
        return;
      case PomPackage.PARENT__VERSION:
        setVersion((String) newValue);
        return;
      case PomPackage.PARENT__RELATIVE_PATH:
        setRelativePath((String) newValue);
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
      case PomPackage.PARENT__ARTIFACT_ID:
        setArtifactId(ARTIFACT_ID_EDEFAULT);
        return;
      case PomPackage.PARENT__GROUP_ID:
        setGroupId(GROUP_ID_EDEFAULT);
        return;
      case PomPackage.PARENT__VERSION:
        setVersion(VERSION_EDEFAULT);
        return;
      case PomPackage.PARENT__RELATIVE_PATH:
        setRelativePath(RELATIVE_PATH_EDEFAULT);
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
      case PomPackage.PARENT__ARTIFACT_ID:
        return ARTIFACT_ID_EDEFAULT == null ? artifactId != null : !ARTIFACT_ID_EDEFAULT.equals(artifactId);
      case PomPackage.PARENT__GROUP_ID:
        return GROUP_ID_EDEFAULT == null ? groupId != null : !GROUP_ID_EDEFAULT.equals(groupId);
      case PomPackage.PARENT__VERSION:
        return VERSION_EDEFAULT == null ? version != null : !VERSION_EDEFAULT.equals(version);
      case PomPackage.PARENT__RELATIVE_PATH:
        return RELATIVE_PATH_EDEFAULT == null ? relativePath != null : !RELATIVE_PATH_EDEFAULT.equals(relativePath);
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
    result.append(" (artifactId: "); //$NON-NLS-1$
    result.append(artifactId);
    result.append(", groupId: "); //$NON-NLS-1$
    result.append(groupId);
    result.append(", version: "); //$NON-NLS-1$
    result.append(version);
    result.append(", relativePath: "); //$NON-NLS-1$
    result.append(relativePath);
    result.append(')');
    return result.toString();
  }

} // ParentImpl
