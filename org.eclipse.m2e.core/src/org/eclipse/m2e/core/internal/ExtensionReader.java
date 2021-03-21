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

package org.eclipse.m2e.core.internal;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import org.eclipse.m2e.core.internal.archetype.ArchetypeCatalogFactory;
import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;


/**
 * Extension reader
 * 
 * @author Eugene Kuleshov
 */
public class ExtensionReader {
  private static final Logger log = LoggerFactory.getLogger(ExtensionReader.class);

  public static final String EXTENSION_ARCHETYPES = IMavenConstants.PLUGIN_ID + ".archetypeCatalogs"; //$NON-NLS-1$

  public static final String EXTENSION_PROJECT_CHANGED_EVENT_LISTENERS = IMavenConstants.PLUGIN_ID
      + ".mavenProjectChangedListeners"; //$NON-NLS-1$

  public static final String EXTENSION_INCREMENTAL_BUILD_FRAMEWORKS = IMavenConstants.PLUGIN_ID
      + ".incrementalBuildFrameworks"; //$NON-NLS-1$

  private static final String ELEMENT_LOCAL_ARCHETYPE = "local"; //$NON-NLS-1$

  private static final String ELEMENT_REMOTE_ARCHETYPE = "remote"; //$NON-NLS-1$

  private static final String ATTR_NAME = "name"; //$NON-NLS-1$

  private static final String ATTR_URL = "url"; //$NON-NLS-1$

  private static final String ATTR_DESCRIPTION = "description"; //$NON-NLS-1$

  private static final String ELEMENT_LISTENER = "listener"; //$NON-NLS-1$

  public static List<ArchetypeCatalogFactory> readArchetypeExtensions() {
    List<ArchetypeCatalogFactory> archetypeCatalogs = new ArrayList<>();

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint archetypesExtensionPoint = registry.getExtensionPoint(EXTENSION_ARCHETYPES);
    if(archetypesExtensionPoint != null) {
      IExtension[] archetypesExtensions = archetypesExtensionPoint.getExtensions();
      for(IExtension extension : archetypesExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        IContributor contributor = extension.getContributor();
        for(IConfigurationElement element : elements) {
          ArchetypeCatalogFactory factory = readArchetypeCatalogs(element, contributor);
          // archetypeManager.addArchetypeCatalogFactory(factory);
          archetypeCatalogs.add(factory);
        }
      }
    }
    return archetypeCatalogs;
  }

  private static ArchetypeCatalogFactory readArchetypeCatalogs(IConfigurationElement element,
      IContributor contributor) {
    if(ELEMENT_LOCAL_ARCHETYPE.equals(element.getName())) {
      String name = element.getAttribute(ATTR_NAME);
      if(name != null) {
        Bundle[] bundles = Platform.getBundles(contributor.getName(), null);
        URL catalogUrl = null;
        for(Bundle bundle : bundles) {
          catalogUrl = bundle.getEntry(name);
          if(catalogUrl != null) {
            String description = element.getAttribute(ATTR_DESCRIPTION);
            String url = catalogUrl.toString();
            return new ArchetypeCatalogFactory.LocalCatalogFactory(url, description, false);
          }
        }
        log.error("Unable to find Archetype catalog " + name + " in " + contributor.getName());
      }
    } else if(ELEMENT_REMOTE_ARCHETYPE.equals(element.getName())) {
      String url = element.getAttribute(ATTR_URL);
      if(url != null) {
        String description = element.getAttribute(ATTR_DESCRIPTION);
        return new ArchetypeCatalogFactory.RemoteCatalogFactory(url, description, false);
      }
    }
    return null;
  }

  public static List<IMavenProjectChangedListener> readProjectChangedEventListenerExtentions() {
    ArrayList<IMavenProjectChangedListener> listeners = new ArrayList<>();

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint mappingsExtensionPoint = registry.getExtensionPoint(EXTENSION_PROJECT_CHANGED_EVENT_LISTENERS);
    if(mappingsExtensionPoint != null) {
      IExtension[] mappingsExtensions = mappingsExtensionPoint.getExtensions();
      for(IExtension extension : mappingsExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_LISTENER)) {
            try {
              listeners.add((IMavenProjectChangedListener) element.createExecutableExtension("class")); //$NON-NLS-1$
            } catch(CoreException ex) {
              log.error(ex.getMessage(), ex);
            }
          }
        }
      }
    }

    return listeners;
  }

  public static List<IIncrementalBuildFramework> readIncrementalBuildFrameworks() {
    ArrayList<IIncrementalBuildFramework> frameworks = new ArrayList<>();

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint mappingsExtensionPoint = registry.getExtensionPoint(EXTENSION_INCREMENTAL_BUILD_FRAMEWORKS);
    if(mappingsExtensionPoint != null) {
      IExtension[] mappingsExtensions = mappingsExtensionPoint.getExtensions();
      for(IExtension extension : mappingsExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals("framework")) {
            try {
              frameworks.add((IIncrementalBuildFramework) element.createExecutableExtension("class")); //$NON-NLS-1$
            } catch(CoreException ex) {
              log.error(ex.getMessage(), ex);
            }
          }
        }
      }
    }

    return frameworks;
  }
}
