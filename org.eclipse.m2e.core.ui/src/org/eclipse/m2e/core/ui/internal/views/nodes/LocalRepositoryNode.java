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
 * LocalRepositoryNode
 *
 * @author igor
 */
public class LocalRepositoryNode extends AbstractIndexedRepositoryNode {

  public LocalRepositoryNode(NexusIndex index) {
    super(index);
  }

  @Override
  public String getName() {
    IRepository repository = index.getRepository();
    StringBuilder sb = new StringBuilder();
    sb.append(Messages.LocalRepositoryNode_local);
    if(repository.getBasedir() != null) {
      sb.append(" (").append(repository.getBasedir().getAbsolutePath()).append(')'); //$NON-NLS-1$
    }
    return sb.toString();
  }
}
