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

import java.util.List;

import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * Parent node for all artifact repositories and mirrors defined in settings.xml.
 * 
 * @author dyocum
 */
public class GlobalRepositoriesNode extends AbstractRepositoriesNode {

  public String getName() {
    return Messages.GlobalRepositoriesNode_name;
  }

  protected List<IRepository> getRepositories() {
    return repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_SETTINGS);
  }

}
