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

package org.eclipse.m2e.core.internal.index.nexus;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.repository.IRepositoryDiscoverer;
import org.eclipse.m2e.core.internal.repository.RepositoryInfo;
import org.eclipse.m2e.core.internal.repository.RepositoryRegistry;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;


/**
 * IndexesExtensionReader
 *
 * @author igor
 */
public class IndexesExtensionReader implements IRepositoryDiscoverer {

  private static final String EXTENSION_INDEXES = IMavenConstants.PLUGIN_ID + ".indexes"; //$NON-NLS-1$

  private static final String ELEMENT_INDEX = "index"; //$NON-NLS-1$

  private static final String ATTR_INDEX_ID = "indexId"; //$NON-NLS-1$

//  private static final String ATTR_INDEX_ARCHIVE = "archive";

  private static final String ATTR_REPOSITORY_URL = "repositoryUrl"; //$NON-NLS-1$

//  private static final String ATTR_UPDATE_URL = "updateUrl";

  private static final String ATTR_IS_SHORT = "isShort"; //$NON-NLS-1$

  private final NexusIndexManager indexManager;

  public IndexesExtensionReader(NexusIndexManager indexManager) {
    this.indexManager = indexManager;
  }

  @Override
  public void addRepositories(RepositoryRegistry registry, IProgressMonitor monitor) throws CoreException {
    IExtensionPoint indexesExtensionPoint = Platform.getExtensionRegistry().getExtensionPoint(EXTENSION_INDEXES);
    if(indexesExtensionPoint != null) {
      IExtension[] indexesExtensions = indexesExtensionPoint.getExtensions();
      for(IExtension extension : indexesExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(ELEMENT_INDEX.equals(element.getName())) {
            processIndexElement(registry, element, monitor);
          }
        }
      }
    }
  }

  private void processIndexElement(RepositoryRegistry registry, IConfigurationElement element, IProgressMonitor monitor)
      throws CoreException {
    String indexId = element.getAttribute(ATTR_INDEX_ID);
    String repositoryUrl = element.getAttribute(ATTR_REPOSITORY_URL);
    boolean isShort = Boolean.parseBoolean(element.getAttribute(ATTR_IS_SHORT));

//    String indexUpdateUrl = element.getAttribute(ATTR_UPDATE_URL);
//    String archive = element.getAttribute(ATTR_INDEX_ARCHIVE);

    RepositoryInfo repository = new RepositoryInfo(indexId, repositoryUrl, IRepositoryRegistry.SCOPE_UNKNOWN, null);
    registry.addRepository(repository, monitor);

    // for consistency, always process indexes using our background thread
    indexManager
        .setIndexDetails(repository, isShort ? NexusIndex.DETAILS_MIN : NexusIndex.DETAILS_FULL, null/*async*/);
  }

}
