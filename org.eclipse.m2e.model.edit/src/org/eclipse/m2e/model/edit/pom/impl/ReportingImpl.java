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

import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.ReportPlugin;
import org.eclipse.m2e.model.edit.pom.Reporting;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Reporting</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>
 * {@link org.eclipse.m2e.model.edit.pom.impl.ReportingImpl#getExcludeDefaults <em>Exclude Defaults</em>}</li>
 * <li>
 * {@link org.eclipse.m2e.model.edit.pom.impl.ReportingImpl#getOutputDirectory <em>Output Directory</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.ReportingImpl#getPlugins <em> Plugins</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ReportingImpl extends EObjectImpl implements Reporting {
  /**
   * The default value of the '{@link #getExcludeDefaults() <em>Exclude Defaults</em>}' attribute. <!-- begin-user-doc
   * --> <!-- end-user-doc -->
   *
   * @see #getExcludeDefaults()
   * @generated
   * @ordered
   */
  protected static final String EXCLUDE_DEFAULTS_EDEFAULT = "false"; //$NON-NLS-1$

  /**
   * The cached value of the '{@link #getExcludeDefaults() <em>Exclude Defaults</em>}' attribute. <!-- begin-user-doc
   * --> <!-- end-user-doc -->
   *
   * @see #getExcludeDefaults()
   * @generated
   * @ordered
   */
  protected String excludeDefaults = EXCLUDE_DEFAULTS_EDEFAULT;

  /**
   * This is true if the Exclude Defaults attribute has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   * @ordered
   */
  protected boolean excludeDefaultsESet;

  /**
   * The default value of the '{@link #getOutputDirectory() <em>Output Directory</em>}' attribute. <!-- begin-user-doc
   * --> <!-- end-user-doc -->
   *
   * @see #getOutputDirectory()
   * @generated
   * @ordered
   */
  protected static final String OUTPUT_DIRECTORY_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getOutputDirectory() <em>Output Directory</em>}' attribute. <!-- begin-user-doc
   * --> <!-- end-user-doc -->
   *
   * @see #getOutputDirectory()
   * @generated
   * @ordered
   */
  protected String outputDirectory = OUTPUT_DIRECTORY_EDEFAULT;

  /**
   * The cached value of the '{@link #getPlugins() <em>Plugins</em>}' containment reference list. <!-- begin-user-doc
   * --> <!-- end-user-doc -->
   *
   * @see #getPlugins()
   * @generated
   * @ordered
   */
  protected EList<ReportPlugin> plugins;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  protected ReportingImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.REPORTING;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getExcludeDefaults() {
    return excludeDefaults;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setExcludeDefaults(String newExcludeDefaults) {
    String oldExcludeDefaults = excludeDefaults;
    excludeDefaults = newExcludeDefaults;
    boolean oldExcludeDefaultsESet = excludeDefaultsESet;
    excludeDefaultsESet = true;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.REPORTING__EXCLUDE_DEFAULTS, oldExcludeDefaults,
          excludeDefaults, !oldExcludeDefaultsESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetExcludeDefaults() {
    String oldExcludeDefaults = excludeDefaults;
    boolean oldExcludeDefaultsESet = excludeDefaultsESet;
    excludeDefaults = EXCLUDE_DEFAULTS_EDEFAULT;
    excludeDefaultsESet = false;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.REPORTING__EXCLUDE_DEFAULTS,
          oldExcludeDefaults, EXCLUDE_DEFAULTS_EDEFAULT, oldExcludeDefaultsESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetExcludeDefaults() {
    return excludeDefaultsESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getOutputDirectory() {
    return outputDirectory;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setOutputDirectory(String newOutputDirectory) {
    String oldOutputDirectory = outputDirectory;
    outputDirectory = newOutputDirectory;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.REPORTING__OUTPUT_DIRECTORY, oldOutputDirectory,
          outputDirectory));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<ReportPlugin> getPlugins() {
    if(plugins == null) {
      plugins = new EObjectContainmentEList.Unsettable<>(ReportPlugin.class, this,
          PomPackage.REPORTING__PLUGINS);
    }
    return plugins;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetPlugins() {
    if(plugins != null)
      ((InternalEList.Unsettable<?>) plugins).unset();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetPlugins() {
    return plugins != null && ((InternalEList.Unsettable<?>) plugins).isSet();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
    switch(featureID) {
      case PomPackage.REPORTING__PLUGINS:
        return ((InternalEList<?>) getPlugins()).basicRemove(otherEnd, msgs);
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
      case PomPackage.REPORTING__EXCLUDE_DEFAULTS:
        return getExcludeDefaults();
      case PomPackage.REPORTING__OUTPUT_DIRECTORY:
        return getOutputDirectory();
      case PomPackage.REPORTING__PLUGINS:
        return getPlugins();
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
      case PomPackage.REPORTING__EXCLUDE_DEFAULTS:
        setExcludeDefaults((String) newValue);
        return;
      case PomPackage.REPORTING__OUTPUT_DIRECTORY:
        setOutputDirectory((String) newValue);
        return;
      case PomPackage.REPORTING__PLUGINS:
        getPlugins().clear();
        getPlugins().addAll((Collection<? extends ReportPlugin>) newValue);
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
      case PomPackage.REPORTING__EXCLUDE_DEFAULTS:
        unsetExcludeDefaults();
        return;
      case PomPackage.REPORTING__OUTPUT_DIRECTORY:
        setOutputDirectory(OUTPUT_DIRECTORY_EDEFAULT);
        return;
      case PomPackage.REPORTING__PLUGINS:
        unsetPlugins();
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
      case PomPackage.REPORTING__EXCLUDE_DEFAULTS:
        return isSetExcludeDefaults();
      case PomPackage.REPORTING__OUTPUT_DIRECTORY:
        return OUTPUT_DIRECTORY_EDEFAULT == null ? outputDirectory != null : !OUTPUT_DIRECTORY_EDEFAULT
            .equals(outputDirectory);
      case PomPackage.REPORTING__PLUGINS:
        return isSetPlugins();
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
    result.append(" (excludeDefaults: "); //$NON-NLS-1$
    if(excludeDefaultsESet)
      result.append(excludeDefaults);
    else
      result.append("<unset>"); //$NON-NLS-1$
    result.append(", outputDirectory: "); //$NON-NLS-1$
    result.append(outputDirectory);
    result.append(')');
    return result.toString();
  }

} // ReportingImpl
