/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.views.build;

import org.eclipse.core.runtime.IPath;


class ResourceNode implements Node {

  private final IPath path;

  private int buildCount;

  public ResourceNode(IPath path) {
    this.path = path;
  }

  @Override
  public String getName() {
    return path.toPortableString();
  }

  public IPath getPath() {
    return path;
  }

  @Override
  public synchronized int getBuildCount() {
    return buildCount;
  }

  public void setBuildCount(int buildCount) {
    this.buildCount = buildCount;
  }
}
