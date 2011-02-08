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

package org.eclipse.m2e.core.ui.internal.views.nodes;

import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.index.NexusIndex;

/**
 * WorkspaceRepositoryNode
 *
 * @author igor
 */
public class WorkspaceRepositoryNode extends AbstractIndexedRepositoryNode {

  public WorkspaceRepositoryNode(NexusIndex index) {
    super(index);
  }

  public String getName() {
    return Messages.WorkspaceRepositoryNode_name;
  }

}
