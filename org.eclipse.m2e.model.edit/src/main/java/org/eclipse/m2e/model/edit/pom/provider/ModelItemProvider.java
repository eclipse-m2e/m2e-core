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

package org.eclipse.m2e.model.edit.pom.provider;

import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.ResourceLocator;

import org.eclipse.emf.ecore.EStructuralFeature;

import org.eclipse.emf.edit.provider.ComposeableAdapterFactory;
import org.eclipse.emf.edit.provider.IEditingDomainItemProvider;
import org.eclipse.emf.edit.provider.IItemLabelProvider;
import org.eclipse.emf.edit.provider.IItemPropertyDescriptor;
import org.eclipse.emf.edit.provider.IItemPropertySource;
import org.eclipse.emf.edit.provider.IStructuredItemContentProvider;
import org.eclipse.emf.edit.provider.ITreeItemContentProvider;
import org.eclipse.emf.edit.provider.ItemPropertyDescriptor;
import org.eclipse.emf.edit.provider.ItemProviderAdapter;
import org.eclipse.emf.edit.provider.ViewerNotification;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.PomPackage;


/**
 * This is the item provider adapter for a
 * {@link org.eclipse.m2e.model.edit.pom.Model} object. <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * 
 * @generated
 */
public class ModelItemProvider extends ItemProviderAdapter implements
		IEditingDomainItemProvider, IStructuredItemContentProvider,
		ITreeItemContentProvider, IItemLabelProvider, IItemPropertySource {
	/**
	 * This constructs an instance from a factory and a notifier. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public ModelItemProvider(AdapterFactory adapterFactory) {
		super(adapterFactory);
	}

	/**
	 * This returns the property descriptors for the adapted class. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public List<IItemPropertyDescriptor> getPropertyDescriptors(Object object) {
		if (itemPropertyDescriptors == null) {
			super.getPropertyDescriptors(object);

			addModelVersionPropertyDescriptor(object);
			addGroupIdPropertyDescriptor(object);
			addArtifactIdPropertyDescriptor(object);
			addPackagingPropertyDescriptor(object);
			addNamePropertyDescriptor(object);
			addVersionPropertyDescriptor(object);
			addDescriptionPropertyDescriptor(object);
			addUrlPropertyDescriptor(object);
			addInceptionYearPropertyDescriptor(object);
		}
		return itemPropertyDescriptors;
	}

	/**
	 * This adds a property descriptor for the Model Version feature. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected void addModelVersionPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(
				((ComposeableAdapterFactory) adapterFactory)
						.getRootAdapterFactory(), getResourceLocator(),
				getString("_UI_Model_modelVersion_feature"), getString(
						"_UI_PropertyDescriptor_description",
						"_UI_Model_modelVersion_feature", "_UI_Model_type"),
				PomPackage.Literals.MODEL__MODEL_VERSION, true, false, false,
				ItemPropertyDescriptor.GENERIC_VALUE_IMAGE, null, null));
	}

	/**
	 * This adds a property descriptor for the Group Id feature. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected void addGroupIdPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(
				((ComposeableAdapterFactory) adapterFactory)
						.getRootAdapterFactory(), getResourceLocator(),
				getString("_UI_Model_groupId_feature"), getString(
						"_UI_PropertyDescriptor_description",
						"_UI_Model_groupId_feature", "_UI_Model_type"),
				PomPackage.Literals.MODEL__GROUP_ID, true, false, false,
				ItemPropertyDescriptor.GENERIC_VALUE_IMAGE, null, null));
	}

	/**
	 * This adds a property descriptor for the Artifact Id feature. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected void addArtifactIdPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(
				((ComposeableAdapterFactory) adapterFactory)
						.getRootAdapterFactory(), getResourceLocator(),
				getString("_UI_Model_artifactId_feature"), getString(
						"_UI_PropertyDescriptor_description",
						"_UI_Model_artifactId_feature", "_UI_Model_type"),
				PomPackage.Literals.MODEL__ARTIFACT_ID, true, false, false,
				ItemPropertyDescriptor.GENERIC_VALUE_IMAGE, null, null));
	}

	/**
	 * This adds a property descriptor for the Packaging feature. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected void addPackagingPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(
				((ComposeableAdapterFactory) adapterFactory)
						.getRootAdapterFactory(), getResourceLocator(),
				getString("_UI_Model_packaging_feature"), getString(
						"_UI_PropertyDescriptor_description",
						"_UI_Model_packaging_feature", "_UI_Model_type"),
				PomPackage.Literals.MODEL__PACKAGING, true, false, false,
				ItemPropertyDescriptor.GENERIC_VALUE_IMAGE, null, null));
	}

	/**
	 * This adds a property descriptor for the Name feature. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected void addNamePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(
				((ComposeableAdapterFactory) adapterFactory)
						.getRootAdapterFactory(), getResourceLocator(),
				getString("_UI_Model_name_feature"), getString(
						"_UI_PropertyDescriptor_description",
						"_UI_Model_name_feature", "_UI_Model_type"),
				PomPackage.Literals.MODEL__NAME, true, false, false,
				ItemPropertyDescriptor.GENERIC_VALUE_IMAGE, null, null));
	}

	/**
	 * This adds a property descriptor for the Version feature. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected void addVersionPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(
				((ComposeableAdapterFactory) adapterFactory)
						.getRootAdapterFactory(), getResourceLocator(),
				getString("_UI_Model_version_feature"), getString(
						"_UI_PropertyDescriptor_description",
						"_UI_Model_version_feature", "_UI_Model_type"),
				PomPackage.Literals.MODEL__VERSION, true, false, false,
				ItemPropertyDescriptor.GENERIC_VALUE_IMAGE, null, null));
	}

	/**
	 * This adds a property descriptor for the Description feature. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected void addDescriptionPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(
				((ComposeableAdapterFactory) adapterFactory)
						.getRootAdapterFactory(), getResourceLocator(),
				getString("_UI_Model_description_feature"), getString(
						"_UI_PropertyDescriptor_description",
						"_UI_Model_description_feature", "_UI_Model_type"),
				PomPackage.Literals.MODEL__DESCRIPTION, true, false, false,
				ItemPropertyDescriptor.GENERIC_VALUE_IMAGE, null, null));
	}

	/**
	 * This adds a property descriptor for the Url feature. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected void addUrlPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(
				((ComposeableAdapterFactory) adapterFactory)
						.getRootAdapterFactory(), getResourceLocator(),
				getString("_UI_Model_url_feature"), getString(
						"_UI_PropertyDescriptor_description",
						"_UI_Model_url_feature", "_UI_Model_type"),
				PomPackage.Literals.MODEL__URL, true, false, false,
				ItemPropertyDescriptor.GENERIC_VALUE_IMAGE, null, null));
	}

	/**
	 * This adds a property descriptor for the Inception Year feature. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected void addInceptionYearPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(
				((ComposeableAdapterFactory) adapterFactory)
						.getRootAdapterFactory(), getResourceLocator(),
				getString("_UI_Model_inceptionYear_feature"), getString(
						"_UI_PropertyDescriptor_description",
						"_UI_Model_inceptionYear_feature", "_UI_Model_type"),
				PomPackage.Literals.MODEL__INCEPTION_YEAR, true, false, false,
				ItemPropertyDescriptor.GENERIC_VALUE_IMAGE, null, null));
	}

	/**
	 * This specifies how to implement {@link #getChildren} and is used to
	 * deduce an appropriate feature for an
	 * {@link org.eclipse.emf.edit.command.AddCommand},
	 * {@link org.eclipse.emf.edit.command.RemoveCommand} or
	 * {@link org.eclipse.emf.edit.command.MoveCommand} in
	 * {@link #createCommand}. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public Collection<? extends EStructuralFeature> getChildrenFeatures(
			Object object) {
		if (childrenFeatures == null) {
			super.getChildrenFeatures(object);
			childrenFeatures.add(PomPackage.Literals.MODEL__PARENT);
			childrenFeatures.add(PomPackage.Literals.MODEL__PREREQUISITES);
			childrenFeatures.add(PomPackage.Literals.MODEL__ISSUE_MANAGEMENT);
			childrenFeatures.add(PomPackage.Literals.MODEL__CI_MANAGEMENT);
			childrenFeatures.add(PomPackage.Literals.MODEL__MAILING_LISTS);
			childrenFeatures.add(PomPackage.Literals.MODEL__DEVELOPERS);
			childrenFeatures.add(PomPackage.Literals.MODEL__CONTRIBUTORS);
			childrenFeatures.add(PomPackage.Literals.MODEL__LICENSES);
			childrenFeatures.add(PomPackage.Literals.MODEL__SCM);
			childrenFeatures.add(PomPackage.Literals.MODEL__ORGANIZATION);
			childrenFeatures.add(PomPackage.Literals.MODEL__BUILD);
			childrenFeatures.add(PomPackage.Literals.MODEL__PROFILES);
			childrenFeatures.add(PomPackage.Literals.MODEL__REPOSITORIES);
			childrenFeatures
					.add(PomPackage.Literals.MODEL__PLUGIN_REPOSITORIES);
			childrenFeatures.add(PomPackage.Literals.MODEL__DEPENDENCIES);
			childrenFeatures.add(PomPackage.Literals.MODEL__REPORTING);
			childrenFeatures
					.add(PomPackage.Literals.MODEL__DEPENDENCY_MANAGEMENT);
			childrenFeatures
					.add(PomPackage.Literals.MODEL__DISTRIBUTION_MANAGEMENT);
			childrenFeatures.add(PomPackage.Literals.MODEL__PROPERTIES);
			childrenFeatures.add(PomPackage.Literals.MODEL__MODULES);
		}
		return childrenFeatures;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	protected EStructuralFeature getChildFeature(Object object, Object child) {
		// Check the type of the specified child object and return the proper
		// feature to use for
		// adding (see {@link AddCommand}) it as a child.

		return super.getChildFeature(object, child);
	}

	/**
	 * This returns Model.gif. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public Object getImage(Object object) {
		return overlayImage(object, getResourceLocator().getImage(
				"full/obj16/Model"));
	}

	/**
	 * This returns the label text for the adapted class. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public String getText(Object object) {
		String label = ((Model) object).getName();
		return label == null || label.length() == 0 ? getString("_UI_Model_type")
				: getString("_UI_Model_type") + " " + label;
	}

	/**
	 * This handles model notifications by calling {@link #updateChildren} to
	 * update any cached children and by creating a viewer notification, which
	 * it passes to {@link #fireNotifyChanged}. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public void notifyChanged(Notification notification) {
		updateChildren(notification);

		switch (notification.getFeatureID(Model.class)) {
		case PomPackage.MODEL__MODEL_VERSION:
		case PomPackage.MODEL__GROUP_ID:
		case PomPackage.MODEL__ARTIFACT_ID:
		case PomPackage.MODEL__PACKAGING:
		case PomPackage.MODEL__NAME:
		case PomPackage.MODEL__VERSION:
		case PomPackage.MODEL__DESCRIPTION:
		case PomPackage.MODEL__URL:
		case PomPackage.MODEL__INCEPTION_YEAR:
			fireNotifyChanged(new ViewerNotification(notification, notification
					.getNotifier(), false, true));
			return;
		case PomPackage.MODEL__PARENT:
		case PomPackage.MODEL__PREREQUISITES:
		case PomPackage.MODEL__ISSUE_MANAGEMENT:
		case PomPackage.MODEL__CI_MANAGEMENT:
		case PomPackage.MODEL__MAILING_LISTS:
		case PomPackage.MODEL__DEVELOPERS:
		case PomPackage.MODEL__CONTRIBUTORS:
		case PomPackage.MODEL__LICENSES:
		case PomPackage.MODEL__SCM:
		case PomPackage.MODEL__ORGANIZATION:
		case PomPackage.MODEL__BUILD:
		case PomPackage.MODEL__PROFILES:
		case PomPackage.MODEL__REPOSITORIES:
		case PomPackage.MODEL__PLUGIN_REPOSITORIES:
		case PomPackage.MODEL__DEPENDENCIES:
		case PomPackage.MODEL__REPORTING:
		case PomPackage.MODEL__DEPENDENCY_MANAGEMENT:
		case PomPackage.MODEL__DISTRIBUTION_MANAGEMENT:
		case PomPackage.MODEL__PROPERTIES:
		case PomPackage.MODEL__MODULES:
			fireNotifyChanged(new ViewerNotification(notification, notification
					.getNotifier(), true, false));
			return;
		}
		super.notifyChanged(notification);
	}

	/**
	 * This adds {@link org.eclipse.emf.edit.command.CommandParameter}s
	 * describing the children that can be created under this object. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	protected void collectNewChildDescriptors(
			Collection<Object> newChildDescriptors, Object object) {
		super.collectNewChildDescriptors(newChildDescriptors, object);

		newChildDescriptors.add(createChildParameter(
				PomPackage.Literals.MODEL__PARENT, PomFactory.eINSTANCE
						.createParent()));

		newChildDescriptors.add(createChildParameter(
				PomPackage.Literals.MODEL__PREREQUISITES, PomFactory.eINSTANCE
						.createPrerequisites()));

		newChildDescriptors.add(createChildParameter(
				PomPackage.Literals.MODEL__ISSUE_MANAGEMENT,
				PomFactory.eINSTANCE.createIssueManagement()));

		newChildDescriptors.add(createChildParameter(
				PomPackage.Literals.MODEL__CI_MANAGEMENT, PomFactory.eINSTANCE
						.createCiManagement()));

		newChildDescriptors.add(createChildParameter(
				PomPackage.Literals.MODEL__MAILING_LISTS, PomFactory.eINSTANCE
						.createMailingList()));

		newChildDescriptors.add(createChildParameter(
				PomPackage.Literals.MODEL__DEVELOPERS, PomFactory.eINSTANCE
						.createDeveloper()));

		newChildDescriptors.add(createChildParameter(
				PomPackage.Literals.MODEL__CONTRIBUTORS, PomFactory.eINSTANCE
						.createContributor()));

		newChildDescriptors.add(createChildParameter(
				PomPackage.Literals.MODEL__LICENSES, PomFactory.eINSTANCE
						.createLicense()));

		newChildDescriptors.add(createChildParameter(
				PomPackage.Literals.MODEL__SCM, PomFactory.eINSTANCE
						.createScm()));

		newChildDescriptors.add(createChildParameter(
				PomPackage.Literals.MODEL__ORGANIZATION, PomFactory.eINSTANCE
						.createOrganization()));

		newChildDescriptors.add(createChildParameter(
				PomPackage.Literals.MODEL__BUILD, PomFactory.eINSTANCE
						.createBuild()));

		newChildDescriptors.add(createChildParameter(
				PomPackage.Literals.MODEL__PROFILES, PomFactory.eINSTANCE
						.createProfile()));

		newChildDescriptors.add(createChildParameter(
				PomPackage.Literals.MODEL__REPOSITORIES, PomFactory.eINSTANCE
						.createRepository()));

		newChildDescriptors.add(createChildParameter(
				PomPackage.Literals.MODEL__PLUGIN_REPOSITORIES,
				PomFactory.eINSTANCE.createRepository()));

		newChildDescriptors.add(createChildParameter(
				PomPackage.Literals.MODEL__DEPENDENCIES, PomFactory.eINSTANCE
						.createDependency()));

		newChildDescriptors.add(createChildParameter(
				PomPackage.Literals.MODEL__REPORTING, PomFactory.eINSTANCE
						.createReporting()));

		newChildDescriptors.add(createChildParameter(
				PomPackage.Literals.MODEL__DEPENDENCY_MANAGEMENT,
				PomFactory.eINSTANCE.createDependencyManagement()));

		newChildDescriptors.add(createChildParameter(
				PomPackage.Literals.MODEL__DISTRIBUTION_MANAGEMENT,
				PomFactory.eINSTANCE.createDistributionManagement()));

		newChildDescriptors.add(createChildParameter(
				PomPackage.Literals.MODEL__PROPERTIES, PomFactory.eINSTANCE
						.createPropertyElement()));

		newChildDescriptors.add(createChildParameter(
				PomPackage.Literals.MODEL__MODULES, ""));
	}

	/**
	 * This returns the label text for
	 * {@link org.eclipse.emf.edit.command.CreateChildCommand}. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public String getCreateChildText(Object owner, Object feature,
			Object child, Collection<?> selection) {
		Object childFeature = feature;
		Object childObject = child;

		boolean qualify = childFeature == PomPackage.Literals.MODEL__REPOSITORIES
				|| childFeature == PomPackage.Literals.MODEL__PLUGIN_REPOSITORIES;

		if (qualify) {
			return getString("_UI_CreateChild_text2", new Object[] {
					getTypeText(childObject), getFeatureText(childFeature),
					getTypeText(owner) });
		}
		return super.getCreateChildText(owner, feature, child, selection);
	}

	/**
	 * Return the resource locator for this item provider's resources. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public ResourceLocator getResourceLocator() {
		return PomEditPlugin.INSTANCE;
	}

}
