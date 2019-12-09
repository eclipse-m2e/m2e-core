/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.internal.index.filter;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.IMavenConstants;


/**
 * ArtifactFilterManager
 * 
 * @author igor
 */
public class ArtifactFilterManager {
  private static final Logger log = LoggerFactory.getLogger(ArtifactFilterManager.class);

  public IStatus filter(IProject project, ArtifactKey artifact) {
    for(IArtifactFilter filter : getFilters()) {
      IStatus status = filter.filter(project, artifact);
      if(status != null && status.matches(IStatus.ERROR)) {
        // TODO should we aggregate multiple not-OK statuses?
        return status;
      }
    }
    return Status.OK_STATUS;
  }

  private List<IArtifactFilter> getFilters() {
    ArrayList<IArtifactFilter> filters = new ArrayList<IArtifactFilter>();

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint filtersExtensionPoint = registry.getExtensionPoint(IMavenConstants.PLUGIN_ID + ".artifactFilters");
    if(filtersExtensionPoint != null) {
      IExtension[] filtersExtensions = filtersExtensionPoint.getExtensions();
      for(IExtension extension : filtersExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if("artifactFilter".equals(element.getName())) {
            try {
              filters.add((IArtifactFilter) element.createExecutableExtension("class"));
            } catch(CoreException ex) {
              log.warn("Could not instantiate extension", ex);
            }
          }
        }
      }
    }

    return filters;
  }

}
