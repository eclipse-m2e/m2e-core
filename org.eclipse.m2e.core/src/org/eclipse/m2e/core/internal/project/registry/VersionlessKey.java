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

package org.eclipse.m2e.core.internal.project.registry;

import java.io.Serializable;


/**
 * VersionlessKey
 *
 * @author igor
 */
public class VersionlessKey implements Serializable {
  private static final long serialVersionUID = 2125977578206347429L;

  private final String namespace;

  private final String id;

  public VersionlessKey(String namespace, String id) {
    if(namespace == null || id == null) {
      throw new NullPointerException();
    }
    this.namespace = namespace;
    this.id = id;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getId() {
    return id;
  }

  @Override
  public int hashCode() {
    int hash = namespace.hashCode();
    hash = hash * 17 + id.hashCode();
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == this) {
      return true;
    }
    if(!(obj instanceof VersionlessKey)) {
      return false;
    }
    VersionlessKey other = (VersionlessKey) obj;
    return namespace.equals(other.namespace) && id.equals(other.id);
  }

  @Override
  public String toString() {
    return namespace + "/" + id; //$NON-NLS-1$
  }
}
