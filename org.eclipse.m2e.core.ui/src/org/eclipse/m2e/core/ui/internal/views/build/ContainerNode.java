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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;


abstract class ContainerNode implements Node {
  private final String name;

  private final Map<IPath, ResourceNode> resources = new HashMap<>();

  protected ContainerNode(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  public synchronized ResourceNode addResource(IPath path) {
    ResourceNode child = resources.get(path);
    if(child == null) {
      child = new ResourceNode(path);
      resources.put(path, child);
    }
    return child;
  }

  public synchronized Collection<ResourceNode> getResources() {
    return new ArrayList<>(resources.values());
  }

}
