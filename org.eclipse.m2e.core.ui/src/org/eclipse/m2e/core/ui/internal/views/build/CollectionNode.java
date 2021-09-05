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

import java.util.Collection;


class CollectionNode<T extends Node> implements Node {

  private final String name;

  private final Collection<T> members;

  public CollectionNode(String name, Collection<T> members) {
    this.name = name;
    this.members = members;
  }

  public Collection<T> getMembers() {
    return members;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getBuildCount() {
    int result = 0;
    for(Node member : members) {
      if(result < member.getBuildCount()) {
        result = member.getBuildCount();
      }
    }
    return result;
  }
}
