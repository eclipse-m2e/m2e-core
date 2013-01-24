/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

  public String getName() {
    return name;
  }

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
