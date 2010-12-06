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
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.Exclusion;
import org.eclipse.m2e.model.edit.pom.PomPackage;


/**
 * <!-- begin-user-doc --> An implementation of the model object '
 * <em><b>Dependency</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.DependencyImpl#getGroupId <em>
 * Group Id</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.DependencyImpl#getArtifactId
 * <em>Artifact Id</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.DependencyImpl#getVersion <em>
 * Version</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.DependencyImpl#getType <em>Type
 * </em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.DependencyImpl#getClassifier
 * <em>Classifier</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.DependencyImpl#getScope <em>
 * Scope</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.DependencyImpl#getSystemPath
 * <em>System Path</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.DependencyImpl#getExclusions
 * <em>Exclusions</em>}</li>
 * <li>{@link org.eclipse.m2e.model.edit.pom.impl.DependencyImpl#getOptional <em>
 * Optional</em>}</li>
 * </ul>
 * </p>
 * 
 * @generated
 */
public class DependencyImpl extends EObjectImpl implements Dependency {
	/**
	 * The default value of the '{@link #getGroupId() <em>Group Id</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getGroupId()
	 * @generated
	 * @ordered
	 */
	protected static final String GROUP_ID_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getGroupId() <em>Group Id</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getGroupId()
	 * @generated
	 * @ordered
	 */
	protected String groupId = GROUP_ID_EDEFAULT;

	/**
	 * The default value of the '{@link #getArtifactId() <em>Artifact Id</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getArtifactId()
	 * @generated
	 * @ordered
	 */
	protected static final String ARTIFACT_ID_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getArtifactId() <em>Artifact Id</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getArtifactId()
	 * @generated
	 * @ordered
	 */
	protected String artifactId = ARTIFACT_ID_EDEFAULT;

	/**
	 * The default value of the '{@link #getVersion() <em>Version</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getVersion()
	 * @generated
	 * @ordered
	 */
	protected static final String VERSION_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getVersion() <em>Version</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getVersion()
	 * @generated
	 * @ordered
	 */
	protected String version = VERSION_EDEFAULT;

	/**
	 * The default value of the '{@link #getType() <em>Type</em>}' attribute.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getType()
	 * @generated
	 * @ordered
	 */
	protected static final String TYPE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getType() <em>Type</em>}' attribute.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getType()
	 * @generated
	 * @ordered
	 */
	protected String type = TYPE_EDEFAULT;

	/**
	 * This is true if the Type attribute has been set. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */
	protected boolean typeESet;

	/**
	 * The default value of the '{@link #getClassifier() <em>Classifier</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getClassifier()
	 * @generated
	 * @ordered
	 */
	protected static final String CLASSIFIER_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getClassifier() <em>Classifier</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getClassifier()
	 * @generated
	 * @ordered
	 */
	protected String classifier = CLASSIFIER_EDEFAULT;

	/**
	 * The default value of the '{@link #getScope() <em>Scope</em>}' attribute.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getScope()
	 * @generated
	 * @ordered
	 */
	protected static final String SCOPE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getScope() <em>Scope</em>}' attribute.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getScope()
	 * @generated
	 * @ordered
	 */
	protected String scope = SCOPE_EDEFAULT;

	/**
	 * The default value of the '{@link #getSystemPath() <em>System Path</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getSystemPath()
	 * @generated
	 * @ordered
	 */
	protected static final String SYSTEM_PATH_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getSystemPath() <em>System Path</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getSystemPath()
	 * @generated
	 * @ordered
	 */
	protected String systemPath = SYSTEM_PATH_EDEFAULT;

	/**
	 * The cached value of the '{@link #getExclusions() <em>Exclusions</em>}'
	 * containment reference list. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getExclusions()
	 * @generated
	 * @ordered
	 */
	protected EList<Exclusion> exclusions;

	/**
	 * The default value of the '{@link #getOptional() <em>Optional</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getOptional()
	 * @generated
	 * @ordered
	 */
	protected static final String OPTIONAL_EDEFAULT = "false"; //$NON-NLS-1$

	/**
	 * The cached value of the '{@link #getOptional() <em>Optional</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getOptional()
	 * @generated
	 * @ordered
	 */
	protected String optional = OPTIONAL_EDEFAULT;

	/**
	 * This is true if the Optional attribute has been set. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */
	protected boolean optionalESet;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected DependencyImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return PomPackage.Literals.DEPENDENCY;
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
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET,
					PomPackage.DEPENDENCY__GROUP_ID, oldGroupId, groupId));
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
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET,
					PomPackage.DEPENDENCY__ARTIFACT_ID, oldArtifactId,
					artifactId));
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
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET,
					PomPackage.DEPENDENCY__VERSION, oldVersion, version));
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public String getType() {
		return type;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public void setType(String newType) {
		String oldType = type;
		type = newType;
		boolean oldTypeESet = typeESet;
		typeESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET,
					PomPackage.DEPENDENCY__TYPE, oldType, type, !oldTypeESet));
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public void unsetType() {
		String oldType = type;
		boolean oldTypeESet = typeESet;
		type = TYPE_EDEFAULT;
		typeESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET,
					PomPackage.DEPENDENCY__TYPE, oldType, TYPE_EDEFAULT,
					oldTypeESet));
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public boolean isSetType() {
		return typeESet;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public String getClassifier() {
		return classifier;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public void setClassifier(String newClassifier) {
		String oldClassifier = classifier;
		classifier = newClassifier;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET,
					PomPackage.DEPENDENCY__CLASSIFIER, oldClassifier,
					classifier));
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public String getScope() {
		return scope;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public void setScope(String newScope) {
		String oldScope = scope;
		scope = newScope;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET,
					PomPackage.DEPENDENCY__SCOPE, oldScope, scope));
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public String getSystemPath() {
		return systemPath;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public void setSystemPath(String newSystemPath) {
		String oldSystemPath = systemPath;
		systemPath = newSystemPath;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET,
					PomPackage.DEPENDENCY__SYSTEM_PATH, oldSystemPath,
					systemPath));
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public EList<Exclusion> getExclusions() {
		if (exclusions == null) {
			exclusions = new EObjectContainmentEList.Unsettable<Exclusion>(
					Exclusion.class, this, PomPackage.DEPENDENCY__EXCLUSIONS);
		}
		return exclusions;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public void unsetExclusions() {
		if (exclusions != null)
			((InternalEList.Unsettable<?>) exclusions).unset();
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public boolean isSetExclusions() {
		return exclusions != null
				&& ((InternalEList.Unsettable<?>) exclusions).isSet();
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public String getOptional() {
		return optional;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public void setOptional(String newOptional) {
		String oldOptional = optional;
		optional = newOptional;
		boolean oldOptionalESet = optionalESet;
		optionalESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET,
					PomPackage.DEPENDENCY__OPTIONAL, oldOptional, optional,
					!oldOptionalESet));
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public void unsetOptional() {
		String oldOptional = optional;
		boolean oldOptionalESet = optionalESet;
		optional = OPTIONAL_EDEFAULT;
		optionalESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET,
					PomPackage.DEPENDENCY__OPTIONAL, oldOptional,
					OPTIONAL_EDEFAULT, oldOptionalESet));
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public boolean isSetOptional() {
		return optionalESet;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd,
			int featureID, NotificationChain msgs) {
		switch (featureID) {
		case PomPackage.DEPENDENCY__EXCLUSIONS:
			return ((InternalEList<?>) getExclusions()).basicRemove(otherEnd,
					msgs);
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
		switch (featureID) {
		case PomPackage.DEPENDENCY__GROUP_ID:
			return getGroupId();
		case PomPackage.DEPENDENCY__ARTIFACT_ID:
			return getArtifactId();
		case PomPackage.DEPENDENCY__VERSION:
			return getVersion();
		case PomPackage.DEPENDENCY__TYPE:
			return getType();
		case PomPackage.DEPENDENCY__CLASSIFIER:
			return getClassifier();
		case PomPackage.DEPENDENCY__SCOPE:
			return getScope();
		case PomPackage.DEPENDENCY__SYSTEM_PATH:
			return getSystemPath();
		case PomPackage.DEPENDENCY__EXCLUSIONS:
			return getExclusions();
		case PomPackage.DEPENDENCY__OPTIONAL:
			return getOptional();
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
		switch (featureID) {
		case PomPackage.DEPENDENCY__GROUP_ID:
			setGroupId((String) newValue);
			return;
		case PomPackage.DEPENDENCY__ARTIFACT_ID:
			setArtifactId((String) newValue);
			return;
		case PomPackage.DEPENDENCY__VERSION:
			setVersion((String) newValue);
			return;
		case PomPackage.DEPENDENCY__TYPE:
			setType((String) newValue);
			return;
		case PomPackage.DEPENDENCY__CLASSIFIER:
			setClassifier((String) newValue);
			return;
		case PomPackage.DEPENDENCY__SCOPE:
			setScope((String) newValue);
			return;
		case PomPackage.DEPENDENCY__SYSTEM_PATH:
			setSystemPath((String) newValue);
			return;
		case PomPackage.DEPENDENCY__EXCLUSIONS:
			getExclusions().clear();
			getExclusions().addAll((Collection<? extends Exclusion>) newValue);
			return;
		case PomPackage.DEPENDENCY__OPTIONAL:
			setOptional((String) newValue);
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
		switch (featureID) {
		case PomPackage.DEPENDENCY__GROUP_ID:
			setGroupId(GROUP_ID_EDEFAULT);
			return;
		case PomPackage.DEPENDENCY__ARTIFACT_ID:
			setArtifactId(ARTIFACT_ID_EDEFAULT);
			return;
		case PomPackage.DEPENDENCY__VERSION:
			setVersion(VERSION_EDEFAULT);
			return;
		case PomPackage.DEPENDENCY__TYPE:
			unsetType();
			return;
		case PomPackage.DEPENDENCY__CLASSIFIER:
			setClassifier(CLASSIFIER_EDEFAULT);
			return;
		case PomPackage.DEPENDENCY__SCOPE:
			setScope(SCOPE_EDEFAULT);
			return;
		case PomPackage.DEPENDENCY__SYSTEM_PATH:
			setSystemPath(SYSTEM_PATH_EDEFAULT);
			return;
		case PomPackage.DEPENDENCY__EXCLUSIONS:
			unsetExclusions();
			return;
		case PomPackage.DEPENDENCY__OPTIONAL:
			unsetOptional();
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
		switch (featureID) {
		case PomPackage.DEPENDENCY__GROUP_ID:
			return GROUP_ID_EDEFAULT == null ? groupId != null
					: !GROUP_ID_EDEFAULT.equals(groupId);
		case PomPackage.DEPENDENCY__ARTIFACT_ID:
			return ARTIFACT_ID_EDEFAULT == null ? artifactId != null
					: !ARTIFACT_ID_EDEFAULT.equals(artifactId);
		case PomPackage.DEPENDENCY__VERSION:
			return VERSION_EDEFAULT == null ? version != null
					: !VERSION_EDEFAULT.equals(version);
		case PomPackage.DEPENDENCY__TYPE:
			return isSetType();
		case PomPackage.DEPENDENCY__CLASSIFIER:
			return CLASSIFIER_EDEFAULT == null ? classifier != null
					: !CLASSIFIER_EDEFAULT.equals(classifier);
		case PomPackage.DEPENDENCY__SCOPE:
			return SCOPE_EDEFAULT == null ? scope != null : !SCOPE_EDEFAULT
					.equals(scope);
		case PomPackage.DEPENDENCY__SYSTEM_PATH:
			return SYSTEM_PATH_EDEFAULT == null ? systemPath != null
					: !SYSTEM_PATH_EDEFAULT.equals(systemPath);
		case PomPackage.DEPENDENCY__EXCLUSIONS:
			return isSetExclusions();
		case PomPackage.DEPENDENCY__OPTIONAL:
			return isSetOptional();
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
		if (eIsProxy())
			return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (groupId: "); //$NON-NLS-1$
		result.append(groupId);
		result.append(", artifactId: "); //$NON-NLS-1$
		result.append(artifactId);
		result.append(", version: "); //$NON-NLS-1$
		result.append(version);
		result.append(", type: "); //$NON-NLS-1$
		if (typeESet)
			result.append(type);
		else
			result.append("<unset>"); //$NON-NLS-1$
		result.append(", classifier: "); //$NON-NLS-1$
		result.append(classifier);
		result.append(", scope: "); //$NON-NLS-1$
		result.append(scope);
		result.append(", systemPath: "); //$NON-NLS-1$
		result.append(systemPath);
		result.append(", optional: "); //$NON-NLS-1$
		if (optionalESet)
			result.append(optional);
		else
			result.append("<unset>"); //$NON-NLS-1$
		result.append(')');
		return result.toString();
	}

} // DependencyImpl
