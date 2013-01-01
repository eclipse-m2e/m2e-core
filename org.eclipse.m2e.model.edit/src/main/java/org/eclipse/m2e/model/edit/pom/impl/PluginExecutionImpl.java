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
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.util.EDataTypeEList;

import org.eclipse.m2e.model.edit.pom.Configuration;
import org.eclipse.m2e.model.edit.pom.PluginExecution;
import org.eclipse.m2e.model.edit.pom.PomPackage;


/**
 * <!-- begin-user-doc --> An implementation of the model object ' <em><b>Plugin Execution</b></em>'. <!-- end-user-doc
 * -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.PluginExecutionImpl#getId <em>Id </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.PluginExecutionImpl#getPhase <em>Phase</em>}</li>
 * <li>
 * {@link org.eclipse.m2e.model.edit.pom.impl.PluginExecutionImpl#getInherited <em>Inherited</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.PluginExecutionImpl#getGoals <em>Goals</em>}</li>
 * <li>
 * {@link org.eclipse.m2e.model.edit.pom.impl.PluginExecutionImpl#getConfiguration <em>Configuration</em>}</li>
 * </ul>
 * </p>
 * 
 * @generated
 */
public class PluginExecutionImpl extends EObjectImpl implements PluginExecution {
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
   * This is true if the Id attribute has been set. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   * @ordered
   */
  protected boolean idESet;

  /**
   * The default value of the '{@link #getPhase() <em>Phase</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @see #getPhase()
   * @generated
   * @ordered
   */
  protected static final String PHASE_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getPhase() <em>Phase</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @see #getPhase()
   * @generated
   * @ordered
   */
  protected String phase = PHASE_EDEFAULT;

  /**
   * The default value of the '{@link #getInherited() <em>Inherited</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getInherited()
   * @generated
   * @ordered
   */
  protected static final String INHERITED_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getInherited() <em>Inherited</em>}' attribute. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getInherited()
   * @generated
   * @ordered
   */
  protected String inherited = INHERITED_EDEFAULT;

  /**
   * The cached value of the '{@link #getGoals() <em>Goals</em>}' attribute list. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @see #getGoals()
   * @generated
   * @ordered
   */
  protected EList<String> goals;

  /**
   * The cached value of the '{@link #getConfiguration() <em>Configuration</em>}' reference. <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * 
   * @see #getConfiguration()
   * @generated
   * @ordered
   */
  protected Configuration configuration;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected PluginExecutionImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  protected EClass eStaticClass() {
    return PomPackage.Literals.PLUGIN_EXECUTION;
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
    boolean oldIdESet = idESet;
    idESet = true;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PLUGIN_EXECUTION__ID, oldId, id, !oldIdESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void unsetId() {
    String oldId = id;
    boolean oldIdESet = idESet;
    id = ID_EDEFAULT;
    idESet = false;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.UNSET, PomPackage.PLUGIN_EXECUTION__ID, oldId, ID_EDEFAULT,
          oldIdESet));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public boolean isSetId() {
    return idESet;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getPhase() {
    return phase;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setPhase(String newPhase) {
    String oldPhase = phase;
    phase = newPhase;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PLUGIN_EXECUTION__PHASE, oldPhase, phase));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getInherited() {
    return inherited;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setInherited(String newInherited) {
    String oldInherited = inherited;
    inherited = newInherited;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PLUGIN_EXECUTION__INHERITED, oldInherited,
          inherited));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public EList<String> getGoals() {
    if(goals == null) {
      goals = new EDataTypeEList<String>(String.class, this, PomPackage.PLUGIN_EXECUTION__GOALS);
    }
    return goals;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public Configuration getConfiguration() {
    if(configuration != null && configuration.eIsProxy()) {
      InternalEObject oldConfiguration = (InternalEObject) configuration;
      configuration = (Configuration) eResolveProxy(oldConfiguration);
      if(configuration != oldConfiguration) {
        if(eNotificationRequired())
          eNotify(new ENotificationImpl(this, Notification.RESOLVE, PomPackage.PLUGIN_EXECUTION__CONFIGURATION,
              oldConfiguration, configuration));
      }
    }
    return configuration;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public Configuration basicGetConfiguration() {
    return configuration;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setConfiguration(Configuration newConfiguration) {
    Configuration oldConfiguration = configuration;
    configuration = newConfiguration;
    if(eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PomPackage.PLUGIN_EXECUTION__CONFIGURATION,
          oldConfiguration, configuration));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch(featureID) {
      case PomPackage.PLUGIN_EXECUTION__ID:
        return getId();
      case PomPackage.PLUGIN_EXECUTION__PHASE:
        return getPhase();
      case PomPackage.PLUGIN_EXECUTION__INHERITED:
        return getInherited();
      case PomPackage.PLUGIN_EXECUTION__GOALS:
        return getGoals();
      case PomPackage.PLUGIN_EXECUTION__CONFIGURATION:
        if(resolve)
          return getConfiguration();
        return basicGetConfiguration();
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
      case PomPackage.PLUGIN_EXECUTION__ID:
        setId((String) newValue);
        return;
      case PomPackage.PLUGIN_EXECUTION__PHASE:
        setPhase((String) newValue);
        return;
      case PomPackage.PLUGIN_EXECUTION__INHERITED:
        setInherited((String) newValue);
        return;
      case PomPackage.PLUGIN_EXECUTION__GOALS:
        getGoals().clear();
        getGoals().addAll((Collection<? extends String>) newValue);
        return;
      case PomPackage.PLUGIN_EXECUTION__CONFIGURATION:
        setConfiguration((Configuration) newValue);
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
      case PomPackage.PLUGIN_EXECUTION__ID:
        unsetId();
        return;
      case PomPackage.PLUGIN_EXECUTION__PHASE:
        setPhase(PHASE_EDEFAULT);
        return;
      case PomPackage.PLUGIN_EXECUTION__INHERITED:
        setInherited(INHERITED_EDEFAULT);
        return;
      case PomPackage.PLUGIN_EXECUTION__GOALS:
        getGoals().clear();
        return;
      case PomPackage.PLUGIN_EXECUTION__CONFIGURATION:
        setConfiguration((Configuration) null);
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
      case PomPackage.PLUGIN_EXECUTION__ID:
        return isSetId();
      case PomPackage.PLUGIN_EXECUTION__PHASE:
        return PHASE_EDEFAULT == null ? phase != null : !PHASE_EDEFAULT.equals(phase);
      case PomPackage.PLUGIN_EXECUTION__INHERITED:
        return INHERITED_EDEFAULT == null ? inherited != null : !INHERITED_EDEFAULT.equals(inherited);
      case PomPackage.PLUGIN_EXECUTION__GOALS:
        return goals != null && !goals.isEmpty();
      case PomPackage.PLUGIN_EXECUTION__CONFIGURATION:
        return configuration != null;
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
    if(idESet)
      result.append(id);
    else
      result.append("<unset>"); //$NON-NLS-1$
    result.append(", phase: "); //$NON-NLS-1$
    result.append(phase);
    result.append(", inherited: "); //$NON-NLS-1$
    result.append(inherited);
    result.append(", goals: "); //$NON-NLS-1$
    result.append(goals);
    result.append(')');
    return result.toString();
  }

} // PluginExecutionImpl
