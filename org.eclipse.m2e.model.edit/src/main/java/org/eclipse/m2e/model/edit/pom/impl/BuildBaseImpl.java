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

import org.eclipse.m2e.model.edit.pom.BuildBase;
import org.eclipse.m2e.model.edit.pom.Plugin;
import org.eclipse.m2e.model.edit.pom.PluginManagement;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.Resource;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Build Base</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.BuildBaseImpl#getDefaultGoal <em>Default Goal</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.BuildBaseImpl#getResources <em> Resources</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.BuildBaseImpl#getTestResources <em>Test Resources</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.BuildBaseImpl#getDirectory <em> Directory</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.BuildBaseImpl#getFinalName <em> Final Name</em>}</li>
 * <li>
 * {@link org.eclipse.m2e.model.edit.pom.impl.BuildBaseImpl#getPluginManagement <em>Plugin Management</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.BuildBaseImpl#getPlugins <em> Plugins</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.BuildBaseImpl#getFilters <em> Filters</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class BuildBaseImpl extends EObjectImpl implements BuildBase {
  /**
   * The default value of the '{@link #getDefaultGoal() <em>Default Goal</em>} ' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getDefaultGoal()
   * @generated
   * @ordered
   */
  protected static final String DEFAULT_GOAL_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getDefaultGoal() <em>Default Goal</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getDefaultGoal()
   * @generated
   * @ordered
   */
  protected String defaultGoal = DEFAULT_GOAL_EDEFAULT;

  /**
   * The cached value of the '{@link #getResources() <em>Resources</em>}' containment reference list. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getResources()
   * @generated
   * @ordered
   */
  protected EList<Resource> resources;

  /**
   * The cached value of the '{@link #getTestResources() <em>Test Resources</em>}' containment reference list. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getTestResources()
   * @generated
   * @ordered
   */
  protected EList<Resource> testResources;

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
   * The default value of the '{@link #getFinalName() <em>Final Name</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getFinalName()
   * @generated
   * @ordered
   */
  protected static final String FINAL_NAME_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getFinalName() <em>Final Name</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getFinalName()
   * @generated
   * @ordered
   */
  protected String finalName = FINAL_NAME_EDEFAULT;

  /**
   * The cached value of the '{@link #getPluginManagement() <em>Plugin Management</em>}' containment reference. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   *
   * @see #getPluginManagement()
   * @generated
   * @ordered
   */
  protected PluginManagement pluginManagement;

  /**
   * This is true if the Plugin Management containment reference has been set. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   *
   * @generated
   * @ordered
   */
  protected boolean pluginManagementESet;

  /**
   * The cached value of the '{@link #getPlugins() <em>Plugins</em>}' containment reference list. <!-- begin-user-doc
   * --> <!-- end-user-doc -->
   *
   * @see #getPlugins()
   * @generated
   * @ordered
   */
  protected EList<Plugin> plugins;

  /**
   * The cached value of the '{@link #getFilters() <em>Filters</em>}' attribute list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   *
   * @see #getFilters()
   * @generated
   * @ordered
   */
  protected EList<String> filters;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  protected BuildBaseImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.BUILD_BASE;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getDefaultGoal() {
    return defaultGoal;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setDefaultGoal(String newDefaultGoal) {
    String oldDefaultGoal = defaultGoal;
    defaultGoal = newDefaultGoal;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.BUILD_BASE__DEFAULT_GOAL, oldDefaultGoal,
          defaultGoal));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<Resource> getResources() {
    if(resources == null) {
      resources = new EObjectContainmentEList.Unsettable<>(Resource.class, this,
          PomPackage.BUILD_BASE__RESOURCES);
    }
    return resources;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetResources() {
    if(resources != null)
      ((InternalEList.Unsettable<?>) resources).unset();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetResources() {
    return resources != null && ((InternalEList.Unsettable<?>) resources).isSet();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<Resource> getTestResources() {
    if(testResources == null) {
      testResources = new EObjectContainmentEList.Unsettable<>(Resource.class, this,
          PomPackage.BUILD_BASE__TEST_RESOURCES);
    }
    return testResources;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void unsetTestResources() {
    if(testResources != null)
      ((InternalEList.Unsettable<?>) testResources).unset();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetTestResources() {
    return testResources != null && ((InternalEList.Unsettable<?>) testResources).isSet();
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
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.BUILD_BASE__DIRECTORY, oldDirectory, directory));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public String getFinalName() {
    return finalName;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public void setFinalName(String newFinalName) {
    String oldFinalName = finalName;
    finalName = newFinalName;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.BUILD_BASE__FINAL_NAME, oldFinalName, finalName));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public PluginManagement getPluginManagement() {
    return pluginManagement;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicSetPluginManagement(PluginManagement newPluginManagement, NotificationChain msgs) {
    PluginManagement oldPluginManagement = pluginManagement;
    pluginManagement = newPluginManagement;
    boolean oldPluginManagementESet = pluginManagementESet;
    pluginManagementESet = true;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET,
          PomPackage.BUILD_BASE__PLUGIN_MANAGEMENT, oldPluginManagement, newPluginManagement, !oldPluginManagementESet);
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
  public void setPluginManagement(PluginManagement newPluginManagement) {
    if(newPluginManagement != pluginManagement) {
      NotificationChain msgs = null;
      if(pluginManagement != null)
        msgs = ((InternalEObject) pluginManagement).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.BUILD_BASE__PLUGIN_MANAGEMENT, null, msgs);
      if(newPluginManagement != null)
        msgs = ((InternalEObject) newPluginManagement).eInverseAdd(this, EOPPOSITE_FEATURE_BASE
            - PomPackage.BUILD_BASE__PLUGIN_MANAGEMENT, null, msgs);
      msgs = basicSetPluginManagement(newPluginManagement, msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldPluginManagementESet = pluginManagementESet;
      pluginManagementESet = true;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.BUILD_BASE__PLUGIN_MANAGEMENT,
            newPluginManagement, newPluginManagement, !oldPluginManagementESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public NotificationChain basicUnsetPluginManagement(NotificationChain msgs) {
    PluginManagement oldPluginManagement = pluginManagement;
    pluginManagement = null;
    boolean oldPluginManagementESet = pluginManagementESet;
    pluginManagementESet = false;
    if(eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.UNSET,
          PomPackage.BUILD_BASE__PLUGIN_MANAGEMENT, oldPluginManagement, null, oldPluginManagementESet);
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
  public void unsetPluginManagement() {
    if(pluginManagement != null) {
      NotificationChain msgs = null;
      msgs = ((InternalEObject) pluginManagement).eInverseRemove(this, EOPPOSITE_FEATURE_BASE
          - PomPackage.BUILD_BASE__PLUGIN_MANAGEMENT, null, msgs);
      msgs = basicUnsetPluginManagement(msgs);
      if(msgs != null)
        msgs.dispatch();
    } else {
      boolean oldPluginManagementESet = pluginManagementESet;
      pluginManagementESet = false;
      if(eNotificationRequired())
        eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.BUILD_BASE__PLUGIN_MANAGEMENT, null, null,
            oldPluginManagementESet));
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public boolean isSetPluginManagement() {
    return pluginManagementESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public EList<Plugin> getPlugins() {
    if(plugins == null) {
      plugins = new EObjectContainmentEList.Unsettable<>(Plugin.class, this, PomPackage.BUILD_BASE__PLUGINS);
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
  public EList<String> getFilters() {
    if(filters == null) {
      filters = new EDataTypeEList<>(String.class, this, PomPackage.BUILD_BASE__FILTERS);
    }
    return filters;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
    switch(featureID) {
      case PomPackage.BUILD_BASE__RESOURCES:
        return ((InternalEList<?>) getResources()).basicRemove(otherEnd, msgs);
      case PomPackage.BUILD_BASE__TEST_RESOURCES:
        return ((InternalEList<?>) getTestResources()).basicRemove(otherEnd, msgs);
      case PomPackage.BUILD_BASE__PLUGIN_MANAGEMENT:
        return basicUnsetPluginManagement(msgs);
      case PomPackage.BUILD_BASE__PLUGINS:
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
      case PomPackage.BUILD_BASE__DEFAULT_GOAL:
        return getDefaultGoal();
      case PomPackage.BUILD_BASE__RESOURCES:
        return getResources();
      case PomPackage.BUILD_BASE__TEST_RESOURCES:
        return getTestResources();
      case PomPackage.BUILD_BASE__DIRECTORY:
        return getDirectory();
      case PomPackage.BUILD_BASE__FINAL_NAME:
        return getFinalName();
      case PomPackage.BUILD_BASE__PLUGIN_MANAGEMENT:
        return getPluginManagement();
      case PomPackage.BUILD_BASE__PLUGINS:
        return getPlugins();
      case PomPackage.BUILD_BASE__FILTERS:
        return getFilters();
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
      case PomPackage.BUILD_BASE__DEFAULT_GOAL:
        setDefaultGoal((String) newValue);
        return;
      case PomPackage.BUILD_BASE__RESOURCES:
        getResources().clear();
        getResources().addAll((Collection<? extends Resource>) newValue);
        return;
      case PomPackage.BUILD_BASE__TEST_RESOURCES:
        getTestResources().clear();
        getTestResources().addAll((Collection<? extends Resource>) newValue);
        return;
      case PomPackage.BUILD_BASE__DIRECTORY:
        setDirectory((String) newValue);
        return;
      case PomPackage.BUILD_BASE__FINAL_NAME:
        setFinalName((String) newValue);
        return;
      case PomPackage.BUILD_BASE__PLUGIN_MANAGEMENT:
        setPluginManagement((PluginManagement) newValue);
        return;
      case PomPackage.BUILD_BASE__PLUGINS:
        getPlugins().clear();
        getPlugins().addAll((Collection<? extends Plugin>) newValue);
        return;
      case PomPackage.BUILD_BASE__FILTERS:
        getFilters().clear();
        getFilters().addAll((Collection<? extends String>) newValue);
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
      case PomPackage.BUILD_BASE__DEFAULT_GOAL:
        setDefaultGoal(DEFAULT_GOAL_EDEFAULT);
        return;
      case PomPackage.BUILD_BASE__RESOURCES:
        unsetResources();
        return;
      case PomPackage.BUILD_BASE__TEST_RESOURCES:
        unsetTestResources();
        return;
      case PomPackage.BUILD_BASE__DIRECTORY:
        setDirectory(DIRECTORY_EDEFAULT);
        return;
      case PomPackage.BUILD_BASE__FINAL_NAME:
        setFinalName(FINAL_NAME_EDEFAULT);
        return;
      case PomPackage.BUILD_BASE__PLUGIN_MANAGEMENT:
        unsetPluginManagement();
        return;
      case PomPackage.BUILD_BASE__PLUGINS:
        unsetPlugins();
        return;
      case PomPackage.BUILD_BASE__FILTERS:
        getFilters().clear();
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
      case PomPackage.BUILD_BASE__DEFAULT_GOAL:
        return DEFAULT_GOAL_EDEFAULT == null ? defaultGoal != null : !DEFAULT_GOAL_EDEFAULT.equals(defaultGoal);
      case PomPackage.BUILD_BASE__RESOURCES:
        return isSetResources();
      case PomPackage.BUILD_BASE__TEST_RESOURCES:
        return isSetTestResources();
      case PomPackage.BUILD_BASE__DIRECTORY:
        return DIRECTORY_EDEFAULT == null ? directory != null : !DIRECTORY_EDEFAULT.equals(directory);
      case PomPackage.BUILD_BASE__FINAL_NAME:
        return FINAL_NAME_EDEFAULT == null ? finalName != null : !FINAL_NAME_EDEFAULT.equals(finalName);
      case PomPackage.BUILD_BASE__PLUGIN_MANAGEMENT:
        return isSetPluginManagement();
      case PomPackage.BUILD_BASE__PLUGINS:
        return isSetPlugins();
      case PomPackage.BUILD_BASE__FILTERS:
        return filters != null && !filters.isEmpty();
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
    result.append(" (defaultGoal: "); //$NON-NLS-1$
    result.append(defaultGoal);
    result.append(", directory: "); //$NON-NLS-1$
    result.append(directory);
    result.append(", finalName: "); //$NON-NLS-1$
    result.append(finalName);
    result.append(", filters: "); //$NON-NLS-1$
    result.append(filters);
    result.append(')');
    return result.toString();
  }

} // BuildBaseImpl
