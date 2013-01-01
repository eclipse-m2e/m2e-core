/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Sonatype, Inc. - modified to support versioned IUs
 *******************************************************************************/

package org.eclipse.m2e.internal.discovery.strategy;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.discovery.AbstractCatalogSource;
import org.eclipse.equinox.internal.p2.discovery.DiscoveryCore;
import org.eclipse.equinox.internal.p2.discovery.compatibility.ConnectorDiscoveryExtensionReader;
import org.eclipse.equinox.internal.p2.discovery.compatibility.RemoteBundleDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Certification;
import org.eclipse.equinox.internal.p2.discovery.model.ValidationException;
import org.eclipse.osgi.util.NLS;

import org.eclipse.m2e.internal.discovery.Messages;


@SuppressWarnings("restriction")
public class M2ERemoteBundleDiscoveryStrategy extends RemoteBundleDiscoveryStrategy {

  protected void processExtensions(IProgressMonitor monitor, IExtension[] extensions) {
    monitor.beginTask(Messages.BundleDiscoveryStrategy_task_processing_extensions, extensions.length == 0 ? 1
        : extensions.length);
    try {
      M2EConnectorDiscoveryExtensionReader extensionReader = new M2EConnectorDiscoveryExtensionReader();

      for(IExtension extension : extensions) {
        AbstractCatalogSource discoverySource = computeDiscoverySource(extension.getContributor());
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(monitor.isCanceled()) {
            return;
          }
          try {
            if(ConnectorDiscoveryExtensionReader.CONNECTOR_DESCRIPTOR.equals(element.getName())) {
              CatalogItem descriptor = extensionReader.readConnectorDescriptor(element, CatalogItem.class);
              descriptor.setSource(discoverySource);
              items.add(descriptor);
            } else if(ConnectorDiscoveryExtensionReader.CONNECTOR_CATEGORY.equals(element.getName())) {
              CatalogCategory category = extensionReader.readConnectorCategory(element, CatalogCategory.class);
              category.setSource(discoverySource);
              if(!discoverySource.getPolicy().isPermitCategories()) {
                LogHelper.log(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, NLS.bind(
                    Messages.BundleDiscoveryStrategy_categoryDisallowed,
                    new Object[] {category.getName(), category.getId(), element.getContributor().getName()}), null));
              } else {
                categories.add(category);
              }
            } else if(ConnectorDiscoveryExtensionReader.CERTIFICATION.equals(element.getName())) {
              Certification certification = extensionReader.readCertification(element, Certification.class);
              certification.setSource(discoverySource);
              certifications.add(certification);
            } else {
              throw new ValidationException(NLS.bind(Messages.BundleDiscoveryStrategy_unexpected_element,
                  element.getName()));
            }
          } catch(ValidationException e) {
            LogHelper.log(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, NLS.bind(
                Messages.BundleDiscoveryStrategy_3, element.getContributor().getName(), e.getMessage()), e));
          }
        }
        monitor.worked(1);
      }

      tags.addAll(extensionReader.getTags());
    } finally {
      monitor.done();
    }
  }
}
