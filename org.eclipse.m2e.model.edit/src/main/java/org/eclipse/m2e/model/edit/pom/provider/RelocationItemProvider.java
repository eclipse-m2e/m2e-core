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

package org.eclipse.m2e.model.edit.pom.provider;

import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.ResourceLocator;
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

import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.Relocation;


/**
 * This is the item provider adapter for a {@link org.eclipse.m2e.model.edit.pom.Relocation} object. <!-- begin-user-doc
 * --> <!-- end-user-doc -->
 *
 * @generated
 */
public class RelocationItemProvider extends ItemProviderAdapter implements IEditingDomainItemProvider,
    IStructuredItemContentProvider, ITreeItemContentProvider, IItemLabelProvider, IItemPropertySource {
  /**
   * This constructs an instance from a factory and a notifier. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  public RelocationItemProvider(AdapterFactory adapterFactory) {
    super(adapterFactory);
  }

  /**
   * This returns the property descriptors for the adapted class. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public List<IItemPropertyDescriptor> getPropertyDescriptors(Object object) {
    if(itemPropertyDescriptors == null) {
      super.getPropertyDescriptors(object);

      addGroupIdPropertyDescriptor(object);
      addArtifactIdPropertyDescriptor(object);
      addVersionPropertyDescriptor(object);
      addMessagePropertyDescriptor(object);
    }
    return itemPropertyDescriptors;
  }

  /**
   * This adds a property descriptor for the Group Id feature. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  protected void addGroupIdPropertyDescriptor(Object object) {
    itemPropertyDescriptors.add(createItemPropertyDescriptor(
        ((ComposeableAdapterFactory) adapterFactory).getRootAdapterFactory(), getResourceLocator(),
        getString("_UI_Relocation_groupId_feature"),
        getString("_UI_PropertyDescriptor_description", "_UI_Relocation_groupId_feature", "_UI_Relocation_type"),
        PomPackage.Literals.RELOCATION__GROUP_ID, true, false, false, ItemPropertyDescriptor.GENERIC_VALUE_IMAGE, null,
        null));
  }

  /**
   * This adds a property descriptor for the Artifact Id feature. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  protected void addArtifactIdPropertyDescriptor(Object object) {
    itemPropertyDescriptors.add(createItemPropertyDescriptor(
        ((ComposeableAdapterFactory) adapterFactory).getRootAdapterFactory(), getResourceLocator(),
        getString("_UI_Relocation_artifactId_feature"),
        getString("_UI_PropertyDescriptor_description", "_UI_Relocation_artifactId_feature", "_UI_Relocation_type"),
        PomPackage.Literals.RELOCATION__ARTIFACT_ID, true, false, false, ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
        null, null));
  }

  /**
   * This adds a property descriptor for the Version feature. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  protected void addVersionPropertyDescriptor(Object object) {
    itemPropertyDescriptors.add(createItemPropertyDescriptor(
        ((ComposeableAdapterFactory) adapterFactory).getRootAdapterFactory(), getResourceLocator(),
        getString("_UI_Relocation_version_feature"),
        getString("_UI_PropertyDescriptor_description", "_UI_Relocation_version_feature", "_UI_Relocation_type"),
        PomPackage.Literals.RELOCATION__VERSION, true, false, false, ItemPropertyDescriptor.GENERIC_VALUE_IMAGE, null,
        null));
  }

  /**
   * This adds a property descriptor for the Message feature. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  protected void addMessagePropertyDescriptor(Object object) {
    itemPropertyDescriptors.add(createItemPropertyDescriptor(
        ((ComposeableAdapterFactory) adapterFactory).getRootAdapterFactory(), getResourceLocator(),
        getString("_UI_Relocation_message_feature"),
        getString("_UI_PropertyDescriptor_description", "_UI_Relocation_message_feature", "_UI_Relocation_type"),
        PomPackage.Literals.RELOCATION__MESSAGE, true, false, false, ItemPropertyDescriptor.GENERIC_VALUE_IMAGE, null,
        null));
  }

  /**
   * This returns Relocation.gif. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public Object getImage(Object object) {
    return overlayImage(object, getResourceLocator().getImage("full/obj16/Relocation"));
  }

  /**
   * This returns the label text for the adapted class. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public String getText(Object object) {
    String label = ((Relocation) object).getGroupId();
    return label == null || label.length() == 0 ? getString("_UI_Relocation_type") : getString("_UI_Relocation_type")
        + " " + label;
  }

  /**
   * This handles model notifications by calling {@link #updateChildren} to update any cached children and by creating a
   * viewer notification, which it passes to {@link #fireNotifyChanged}. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public void notifyChanged(Notification notification) {
    updateChildren(notification);

    switch(notification.getFeatureID(Relocation.class)) {
      case PomPackage.RELOCATION__GROUP_ID:
      case PomPackage.RELOCATION__ARTIFACT_ID:
      case PomPackage.RELOCATION__VERSION:
      case PomPackage.RELOCATION__MESSAGE:
        fireNotifyChanged(new ViewerNotification(notification, notification.getNotifier(), false, true));
        return;
    }
    super.notifyChanged(notification);
  }

  /**
   * This adds {@link org.eclipse.emf.edit.command.CommandParameter}s describing the children that can be created under
   * this object. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  protected void collectNewChildDescriptors(Collection<Object> newChildDescriptors, Object object) {
    super.collectNewChildDescriptors(newChildDescriptors, object);
  }

  /**
   * Return the resource locator for this item provider's resources. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @generated
   */
  @Override
  public ResourceLocator getResourceLocator() {
    return PomEditPlugin.INSTANCE;
  }

}
