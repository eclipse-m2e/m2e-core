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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

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

  private static final String ELEMENT_LISTENER = "listener"; //$NON-NLS-1$

  public static List<IMavenProjectChangedListener> readProjectChangedEventListenerExtentions() {
    ArrayList<IMavenProjectChangedListener> listeners = new ArrayList<>();

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint mappingsExtensionPoint = registry.getExtensionPoint(EXTENSION_PROJECT_CHANGED_EVENT_LISTENERS);
    if(mappingsExtensionPoint != null) {
      IExtension[] mappingsExtensions = mappingsExtensionPoint.getExtensions();
      for(IExtension extension : mappingsExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(ELEMENT_LISTENER.equals(element.getName())) {
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
          if("framework".equals(element.getName())) {
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
