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

import org.eclipse.swt.graphics.Image;

import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.m2e.core.ui.internal.MavenImages;


/**
 * AbstractIndexedRepository
 *
 * @author igor
 */
public abstract class RemoteRepositoryNode implements IMavenRepositoryNode {
  protected static final Object[] NO_CHILDREN = new Object[0];

  protected final IRepository repository;

  protected RemoteRepositoryNode(IRepository repository) {
    this.repository = repository;
  }

  @Override
  public Object[] getChildren() {
    return NO_CHILDREN;
  }

  @Override
  public Image getImage() {
    return MavenImages.IMG_INDEX;
  }

  @Override
  public boolean hasChildren() {
    return false;
  }

  @Override
  public boolean isUpdating() {
    return false;
  }


  public String getRepositoryUrl() {
    return repository.getUrl();
  }

}
