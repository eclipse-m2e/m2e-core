/*******************************************************************************
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Takari, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.preferences.launch;

public abstract class ClasspathEntryNode {

  private ClassRealmNode realm;

  public ClasspathEntryNode(ClassRealmNode realm) {
    this.realm = realm;
  }

  public ClassRealmNode getRealm() {
    return this.realm;
  }

  public abstract String getName();
}
