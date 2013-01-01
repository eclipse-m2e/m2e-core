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

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.util.EDataTypeEList;

import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.Resource;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Resource</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ResourceImpl#getTargetPath <em> Target Path</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ResourceImpl#getFiltering <em> Filtering</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ResourceImpl#getDirectory <em> Directory</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ResourceImpl#getIncludes <em> Includes</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ResourceImpl#getExcludes <em> Excludes</em>}</li>
 * </ul>
 * </p>
 * 
 * @generated
 */
public class ResourceImpl extends EObjectImpl implements Resource {
  /**
   * The default value of the '{@link #getTargetPath() <em>Target Path</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getTargetPath()
   * @generated
   * @ordered
   */
  protected static final String TARGET_PATH_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getTargetPath() <em>Target Path</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getTargetPath()
   * @generated
   * @ordered
   */
  protected String targetPath = TARGET_PATH_EDEFAULT;

  /**
   * The default value of the '{@link #getFiltering() <em>Filtering</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getFiltering()
   * @generated
   * @ordered
   */
  protected static final String FILTERING_EDEFAULT = "false"; //$NON-NLS-1$

  /**
   * The cached value of the '{@link #getFiltering() <em>Filtering</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getFiltering()
   * @generated
   * @ordered
   */
  protected String filtering = FILTERING_EDEFAULT;

  /**
   * This is true if the Filtering attribute has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  protected boolean filteringESet;

  /**
   * The default value of the '{@link #getDirectory() <em>Directory</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getDirectory()
   * @generated
   * @ordered
   */
  protected static final String DIRECTORY_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getDirectory() <em>Directory</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getDirectory()
   * @generated
   * @ordered
   */
  protected String directory = DIRECTORY_EDEFAULT;

  /**
   * The cached value of the '{@link #getIncludes() <em>Includes</em>}' attribute list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getIncludes()
   * @generated
   * @ordered
   */
  protected EList<String> includes;

  /**
   * The cached value of the '{@link #getExcludes() <em>Excludes</em>}' attribute list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getExcludes()
   * @generated
   * @ordered
   */
  protected EList<String> excludes;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected ResourceImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.RESOURCE;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getTargetPath() {
    return targetPath;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setTargetPath(String newTargetPath) {
    String oldTargetPath = targetPath;
    targetPath = newTargetPath;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.RESOURCE__TARGET_PATH, oldTargetPath, targetPath));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getFiltering() {
    return filtering;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setFiltering(String newFiltering) {
    String oldFiltering = filtering;
    filtering = newFiltering;
    boolean oldFilteringESet = filteringESet;
    filteringESet = true;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.RESOURCE__FILTERING, oldFiltering, filtering,
          !oldFilteringESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void unsetFiltering() {
    String oldFiltering = filtering;
    boolean oldFilteringESet = filteringESet;
    filtering = FILTERING_EDEFAULT;
    filteringESet = false;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.RESOURCE__FILTERING, oldFiltering,
          FILTERING_EDEFAULT, oldFilteringESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public boolean isSetFiltering() {
    return filteringESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getDirectory() {
    return directory;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setDirectory(String newDirectory) {
    String oldDirectory = directory;
    directory = newDirectory;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.RESOURCE__DIRECTORY, oldDirectory, directory));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public EList<String> getIncludes() {
    if(includes == null) {
      includes = new EDataTypeEList<String>(String.class, this, PomPackage.RESOURCE__INCLUDES);
    }
    return includes;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public EList<String> getExcludes() {
    if(excludes == null) {
      excludes = new EDataTypeEList<String>(String.class, this, PomPackage.RESOURCE__EXCLUDES);
    }
    return excludes;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch(featureID) {
      case PomPackage.RESOURCE__TARGET_PATH:
        return getTargetPath();
      case PomPackage.RESOURCE__FILTERING:
        return getFiltering();
      case PomPackage.RESOURCE__DIRECTORY:
        return getDirectory();
      case PomPackage.RESOURCE__INCLUDES:
        return getIncludes();
      case PomPackage.RESOURCE__EXCLUDES:
        return getExcludes();
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
      case PomPackage.RESOURCE__TARGET_PATH:
        setTargetPath((String) newValue);
        return;
      case PomPackage.RESOURCE__FILTERING:
        setFiltering((String) newValue);
        return;
      case PomPackage.RESOURCE__DIRECTORY:
        setDirectory((String) newValue);
        return;
      case PomPackage.RESOURCE__INCLUDES:
        getIncludes().clear();
        getIncludes().addAll((Collection<? extends String>) newValue);
        return;
      case PomPackage.RESOURCE__EXCLUDES:
        getExcludes().clear();
        getExcludes().addAll((Collection<? extends String>) newValue);
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
      case PomPackage.RESOURCE__TARGET_PATH:
        setTargetPath(TARGET_PATH_EDEFAULT);
        return;
      case PomPackage.RESOURCE__FILTERING:
        unsetFiltering();
        return;
      case PomPackage.RESOURCE__DIRECTORY:
        setDirectory(DIRECTORY_EDEFAULT);
        return;
      case PomPackage.RESOURCE__INCLUDES:
        getIncludes().clear();
        return;
      case PomPackage.RESOURCE__EXCLUDES:
        getExcludes().clear();
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
      case PomPackage.RESOURCE__TARGET_PATH:
        return TARGET_PATH_EDEFAULT == null ? targetPath != null : !TARGET_PATH_EDEFAULT.equals(targetPath);
      case PomPackage.RESOURCE__FILTERING:
        return isSetFiltering();
      case PomPackage.RESOURCE__DIRECTORY:
        return DIRECTORY_EDEFAULT == null ? directory != null : !DIRECTORY_EDEFAULT.equals(directory);
      case PomPackage.RESOURCE__INCLUDES:
        return includes != null && !includes.isEmpty();
      case PomPackage.RESOURCE__EXCLUDES:
        return excludes != null && !excludes.isEmpty();
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
    result.append(" (targetPath: "); //$NON-NLS-1$
    result.append(targetPath);
    result.append(", filtering: "); //$NON-NLS-1$
    if(filteringESet)
      result.append(filtering);
    else
      result.append("<unset>"); //$NON-NLS-1$
    result.append(", directory: "); //$NON-NLS-1$
    result.append(directory);
    result.append(", includes: "); //$NON-NLS-1$
    result.append(includes);
    result.append(", excludes: "); //$NON-NLS-1$
    result.append(excludes);
    result.append(')');
    return result.toString();
  }

} // ResourceImpl
