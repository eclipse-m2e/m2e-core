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
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.m2e.model.edit.pom.Build;
import org.eclipse.m2e.model.edit.pom.Extension;
import org.eclipse.m2e.model.edit.pom.PomPackage;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Build</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.BuildImpl#getSourceDirectory <em>Source Directory</em>}</li>
 * <li>
 * {@link org.eclipse.m2e.model.edit.pom.impl.BuildImpl#getScriptSourceDirectory <em>Script Source Directory</em>}</li>
 * <li>
 * {@link org.eclipse.m2e.model.edit.pom.impl.BuildImpl#getTestSourceDirectory <em>Test Source Directory</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.BuildImpl#getOutputDirectory <em>Output Directory</em>}</li>
 * <li>
 * {@link org.eclipse.m2e.model.edit.pom.impl.BuildImpl#getTestOutputDirectory <em>Test Output Directory</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.BuildImpl#getExtensions <em> Extensions</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class BuildImpl extends BuildBaseImpl implements Build {
  /**
   * The default value of the '{@link #getSourceDirectory() <em>Source Directory</em>}' attribute. <!-- begin-user-doc
   * --> <!-- end-user-doc -->
   *
   * @see #getSourceDirectory()
   * @generated
   * @ordered
   */
  protected static final String SOURCE_DIRECTORY_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getSourceDirectory() <em>Source Directory</em>}' attribute. <!-- begin-user-doc
   * --> <!-- end-user-doc -->
   *
   * @see #getSourceDirectory()
   * @generated
   * @ordered
   */
  protected String sourceDirectory = SOURCE_DIRECTORY_EDEFAULT;

  /**
   * The default value of the '{@link #getScriptSourceDirectory() <em>Script Source Directory</em>}' attribute. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getScriptSourceDirectory()
   * @generated
   * @ordered
   */
  protected static final String SCRIPT_SOURCE_DIRECTORY_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getScriptSourceDirectory() <em>Script Source Directory</em>}' attribute. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getScriptSourceDirectory()
   * @generated
   * @ordered
   */
  protected String scriptSourceDirectory = SCRIPT_SOURCE_DIRECTORY_EDEFAULT;

  /**
   * The default value of the '{@link #getTestSourceDirectory() <em>Test Source Directory</em>}' attribute. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getTestSourceDirectory()
   * @generated
   * @ordered
   */
  protected static final String TEST_SOURCE_DIRECTORY_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getTestSourceDirectory() <em>Test Source Directory</em>}' attribute. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getTestSourceDirectory()
   * @generated
   * @ordered
   */
  protected String testSourceDirectory = TEST_SOURCE_DIRECTORY_EDEFAULT;

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
   * The default value of the '{@link #getTestOutputDirectory() <em>Test Output Directory</em>}' attribute. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getTestOutputDirectory()
   * @generated
   * @ordered
   */
  protected static final String TEST_OUTPUT_DIRECTORY_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getTestOutputDirectory() <em>Test Output Directory</em>}' attribute. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getTestOutputDirectory()
   * @generated
   * @ordered
   */
  protected String testOutputDirectory = TEST_OUTPUT_DIRECTORY_EDEFAULT;

  /**
   * The cached value of the '{@link #getExtensions() <em>Extensions</em>}' containment reference list. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getExtensions()
   * @generated
   * @ordered
   */
  protected EList<Extension> extensions;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  protected BuildImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.BUILD;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getSourceDirectory() {
    return sourceDirectory;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setSourceDirectory(String newSourceDirectory) {
    String oldSourceDirectory = sourceDirectory;
    sourceDirectory = newSourceDirectory;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.BUILD__SOURCE_DIRECTORY, oldSourceDirectory,
          sourceDirectory));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getScriptSourceDirectory() {
    return scriptSourceDirectory;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setScriptSourceDirectory(String newScriptSourceDirectory) {
    String oldScriptSourceDirectory = scriptSourceDirectory;
    scriptSourceDirectory = newScriptSourceDirectory;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.BUILD__SCRIPT_SOURCE_DIRECTORY,
          oldScriptSourceDirectory, scriptSourceDirectory));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getTestSourceDirectory() {
    return testSourceDirectory;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setTestSourceDirectory(String newTestSourceDirectory) {
    String oldTestSourceDirectory = testSourceDirectory;
    testSourceDirectory = newTestSourceDirectory;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.BUILD__TEST_SOURCE_DIRECTORY,
          oldTestSourceDirectory, testSourceDirectory));
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.BUILD__OUTPUT_DIRECTORY, oldOutputDirectory,
          outputDirectory));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getTestOutputDirectory() {
    return testOutputDirectory;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setTestOutputDirectory(String newTestOutputDirectory) {
    String oldTestOutputDirectory = testOutputDirectory;
    testOutputDirectory = newTestOutputDirectory;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.BUILD__TEST_OUTPUT_DIRECTORY,
          oldTestOutputDirectory, testOutputDirectory));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<Extension> getExtensions() {
    if(extensions == null) {
      extensions = new EObjectContainmentEList.Unsettable<>(Extension.class, this,
          PomPackage.BUILD__EXTENSIONS);
    }
    return extensions;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetExtensions() {
    if(extensions != null)
      ((InternalEList.Unsettable<?>) extensions).unset();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetExtensions() {
    return extensions != null && ((InternalEList.Unsettable<?>) extensions).isSet();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
    switch(featureID) {
      case PomPackage.BUILD__EXTENSIONS:
        return ((InternalEList<?>) getExtensions()).basicRemove(otherEnd, msgs);
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
      case PomPackage.BUILD__SOURCE_DIRECTORY:
        return getSourceDirectory();
      case PomPackage.BUILD__SCRIPT_SOURCE_DIRECTORY:
        return getScriptSourceDirectory();
      case PomPackage.BUILD__TEST_SOURCE_DIRECTORY:
        return getTestSourceDirectory();
      case PomPackage.BUILD__OUTPUT_DIRECTORY:
        return getOutputDirectory();
      case PomPackage.BUILD__TEST_OUTPUT_DIRECTORY:
        return getTestOutputDirectory();
      case PomPackage.BUILD__EXTENSIONS:
        return getExtensions();
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
      case PomPackage.BUILD__SOURCE_DIRECTORY:
        setSourceDirectory((String) newValue);
        return;
      case PomPackage.BUILD__SCRIPT_SOURCE_DIRECTORY:
        setScriptSourceDirectory((String) newValue);
        return;
      case PomPackage.BUILD__TEST_SOURCE_DIRECTORY:
        setTestSourceDirectory((String) newValue);
        return;
      case PomPackage.BUILD__OUTPUT_DIRECTORY:
        setOutputDirectory((String) newValue);
        return;
      case PomPackage.BUILD__TEST_OUTPUT_DIRECTORY:
        setTestOutputDirectory((String) newValue);
        return;
      case PomPackage.BUILD__EXTENSIONS:
        getExtensions().clear();
        getExtensions().addAll((Collection<? extends Extension>) newValue);
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
      case PomPackage.BUILD__SOURCE_DIRECTORY:
        setSourceDirectory(SOURCE_DIRECTORY_EDEFAULT);
        return;
      case PomPackage.BUILD__SCRIPT_SOURCE_DIRECTORY:
        setScriptSourceDirectory(SCRIPT_SOURCE_DIRECTORY_EDEFAULT);
        return;
      case PomPackage.BUILD__TEST_SOURCE_DIRECTORY:
        setTestSourceDirectory(TEST_SOURCE_DIRECTORY_EDEFAULT);
        return;
      case PomPackage.BUILD__OUTPUT_DIRECTORY:
        setOutputDirectory(OUTPUT_DIRECTORY_EDEFAULT);
        return;
      case PomPackage.BUILD__TEST_OUTPUT_DIRECTORY:
        setTestOutputDirectory(TEST_OUTPUT_DIRECTORY_EDEFAULT);
        return;
      case PomPackage.BUILD__EXTENSIONS:
        unsetExtensions();
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
      case PomPackage.BUILD__SOURCE_DIRECTORY:
        return SOURCE_DIRECTORY_EDEFAULT == null ? sourceDirectory != null : !SOURCE_DIRECTORY_EDEFAULT
            .equals(sourceDirectory);
      case PomPackage.BUILD__SCRIPT_SOURCE_DIRECTORY:
        return SCRIPT_SOURCE_DIRECTORY_EDEFAULT == null ? scriptSourceDirectory != null
            : !SCRIPT_SOURCE_DIRECTORY_EDEFAULT.equals(scriptSourceDirectory);
      case PomPackage.BUILD__TEST_SOURCE_DIRECTORY:
        return TEST_SOURCE_DIRECTORY_EDEFAULT == null ? testSourceDirectory != null : !TEST_SOURCE_DIRECTORY_EDEFAULT
            .equals(testSourceDirectory);
      case PomPackage.BUILD__OUTPUT_DIRECTORY:
        return OUTPUT_DIRECTORY_EDEFAULT == null ? outputDirectory != null : !OUTPUT_DIRECTORY_EDEFAULT
            .equals(outputDirectory);
      case PomPackage.BUILD__TEST_OUTPUT_DIRECTORY:
        return TEST_OUTPUT_DIRECTORY_EDEFAULT == null ? testOutputDirectory != null : !TEST_OUTPUT_DIRECTORY_EDEFAULT
            .equals(testOutputDirectory);
      case PomPackage.BUILD__EXTENSIONS:
        return isSetExtensions();
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
    result.append(" (sourceDirectory: "); //$NON-NLS-1$
    result.append(sourceDirectory);
    result.append(", scriptSourceDirectory: "); //$NON-NLS-1$
    result.append(scriptSourceDirectory);
    result.append(", testSourceDirectory: "); //$NON-NLS-1$
    result.append(testSourceDirectory);
    result.append(", outputDirectory: "); //$NON-NLS-1$
    result.append(outputDirectory);
    result.append(", testOutputDirectory: "); //$NON-NLS-1$
    result.append(testOutputDirectory);
    result.append(')');
    return result.toString();
  }

} // BuildImpl
