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

package org.eclipse.m2e.core.ui.internal.views.nodes;

import org.eclipse.m2e.core.internal.index.nexus.NexusIndex;
import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * LocalRepsoitoryNode
 *
 * @author dyocum
 */
public class RepositoryNode extends AbstractIndexedRepositoryNode {

  private final IRepository repository;

  public RepositoryNode(NexusIndex index) {
    super(index);
    this.repository = index.getRepository();
  }

  @Override
  public String getName() {
    StringBuilder sb = new StringBuilder();
    sb.append(repository.getId());
    sb.append(" (").append(repository.getUrl()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
    if(repository.getMirrorOf() != null) {
      sb.append(" [mirrorOf=").append(repository.getMirrorOf()).append("]"); //$NON-NLS-2$
    }
    if(repository.getMirrorId() != null) {
      sb.append(" [mirrored by ").append(repository.getMirrorId()).append("]"); //$NON-NLS-2$
    }
    if(isUpdating()) {
      sb.append(Messages.RepositoryNode_updating);
    }
    return sb.toString();
  }

  @Override
  public String getRepositoryUrl() {
    return repository.getUrl();
  }

  public String getRepoName() {
    return repository.toString();
  }

}
