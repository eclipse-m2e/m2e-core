/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Sonatype, Inc. - support for versioned IUs
 *******************************************************************************/

package org.eclipse.m2e.internal.discovery.strategy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.equinox.internal.p2.discovery.compatibility.ConnectorDiscoveryExtensionReader;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.FeatureFilter;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.equinox.internal.p2.discovery.model.Overview;
import org.eclipse.equinox.internal.p2.discovery.model.Tag;
import org.eclipse.equinox.internal.p2.discovery.model.ValidationException;

import org.eclipse.m2e.internal.discovery.Messages;


/*
 * ConnectorDiscoveryExtensionReader assumes that IUs installed will be features and that the user 
 * always wants the latest version.
 * 
 * In the m2e use case the user may need a specific version of the IU which need not be a feature.
 */
@SuppressWarnings("restriction")
public class M2EConnectorDiscoveryExtensionReader extends ConnectorDiscoveryExtensionReader {

  private Map<String, Tag> tagById = new HashMap<String, Tag>();

  public Set<Tag> getTags() {
    return new HashSet<Tag>(tagById.values());
  }

  private Tag getTag(String id) {
    if(id == null) {
      return null;
    }
    // first, look for known tag
    Tag result = tagById.get(id);
    if(result != null) {
      return result;
    }
    // second, search default tags
    for(Tag tag : DEFAULT_TAGS) {
      if(tag.getValue().equals(id)) {
        tagById.put(id, tag);
        return tag;
      }
    }
    // third, create new tag
    result = new Tag(id, id);
    tagById.put(id, result);
    return result;
  }

  public <T extends CatalogItem> T readConnectorDescriptor(IConfigurationElement element, Class<T> clazz)
      throws ValidationException {
    T connectorDescriptor;
    try {
      connectorDescriptor = clazz.newInstance();
    } catch(Exception e) {
      throw new IllegalStateException(e);
    }

    try {
      String kinds = element.getAttribute("kind"); //$NON-NLS-1$
      if(kinds != null) {
        String[] akinds = kinds.split("\\s*,\\s*"); //$NON-NLS-1$
        for(String kind : akinds) {
          Tag tag = getTag(kind);
          if(tag != null) {
            connectorDescriptor.addTag(tag);
          }
        }
      }
    } catch(IllegalArgumentException e) {
      throw new ValidationException(Messages.ConnectorDiscoveryExtensionReader_unexpected_value_kind);
    }
    connectorDescriptor.setName(element.getAttribute("name")); //$NON-NLS-1$
    connectorDescriptor.setProvider(element.getAttribute("provider")); //$NON-NLS-1$
    connectorDescriptor.setLicense(element.getAttribute("license")); //$NON-NLS-1$
    connectorDescriptor.setDescription(element.getAttribute("description")); //$NON-NLS-1$
    connectorDescriptor.setSiteUrl(element.getAttribute("siteUrl")); //$NON-NLS-1$
    connectorDescriptor.setId(element.getAttribute("id")); //$NON-NLS-1$
    connectorDescriptor.setCategoryId(element.getAttribute("categoryId")); //$NON-NLS-1$
    connectorDescriptor.setCertificationId(element.getAttribute("certificationId")); //$NON-NLS-1$
    connectorDescriptor.setPlatformFilter(element.getAttribute("platformFilter")); //$NON-NLS-1$
    connectorDescriptor.setGroupId(element.getAttribute("groupId")); //$NON-NLS-1$

    IConfigurationElement[] children = element.getChildren("iu"); //$NON-NLS-1$
    if(children.length > 0) {
      for(IConfigurationElement child : children) {
        connectorDescriptor.getInstallableUnits().add(child.getAttribute("id")); //$NON-NLS-1$
      }
    } else {
      // TODO Should an exception be thrown here?
      // no particular iu specified, use connector id
      connectorDescriptor.getInstallableUnits().add(connectorDescriptor.getId());
    }
    for(IConfigurationElement child : element.getChildren("featureFilter")) { //$NON-NLS-1$
      FeatureFilter featureFilterItem = readFeatureFilter(child);
      featureFilterItem.setItem(connectorDescriptor);
      connectorDescriptor.getFeatureFilter().add(featureFilterItem);
    }
    for(IConfigurationElement child : element.getChildren("icon")) { //$NON-NLS-1$
      Icon iconItem = readIcon(child);
      if(connectorDescriptor.getIcon() != null) {
        throw new ValidationException(Messages.ConnectorDiscoveryExtensionReader_unexpected_element_icon);
      }
      connectorDescriptor.setIcon(iconItem);
    }
    for(IConfigurationElement child : element.getChildren("overview")) { //$NON-NLS-1$
      Overview overviewItem = readOverview(child);
      overviewItem.setItem(connectorDescriptor);
      if(connectorDescriptor.getOverview() != null) {
        throw new ValidationException(Messages.ConnectorDiscoveryExtensionReader_unexpected_element_overview);
      }
      connectorDescriptor.setOverview(overviewItem);
    }

    connectorDescriptor.validate();

    return connectorDescriptor;
  }
}
