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

import java.util.List;

import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * CustomRepositoriesNode
 *
 * @author igor
 */
public class CustomRepositoriesNode extends AbstractRepositoriesNode {

  @Override
  protected List<IRepository> getRepositories() {
    return repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_UNKNOWN);
  }

  @Override
  public String getName() {
    return Messages.CustomRepositoriesNode_name;
  }

}
