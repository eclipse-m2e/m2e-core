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

package org.eclipse.m2e.core.internal.project.registry;

import java.io.Serializable;


/**
 * Capability
 * 
 * @author igor
 */
public abstract class Capability implements Serializable {

  private static final long serialVersionUID = 6057170997402911788L;

  private final VersionlessKey versionlessKey;

  public Capability(String namespace, String id) {
    if(namespace == null || id == null) {
      throw new NullPointerException();
    }
    this.versionlessKey = new VersionlessKey(namespace, id);
  }

  public VersionlessKey getVersionlessKey() {
    return versionlessKey;
  }

}
