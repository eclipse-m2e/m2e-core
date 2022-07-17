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
 * RequiredCapability
 *
 * @author igor
 */
public abstract class RequiredCapability implements Serializable {

  private static final long serialVersionUID = -5445156687502685383L;

  private final VersionlessKey versionlessKey;

  public RequiredCapability(String namepsace, String id) {
    if(namepsace == null || id == null) {
      throw new NullPointerException();
    }
    this.versionlessKey = new VersionlessKey(namepsace, id);
  }

  public VersionlessKey getVersionlessKey() {
    return versionlessKey;
  }

  /**
   * Returns true if provided capability *potentially* satisfies this requirement. Capability/requirement match will be
   * used to check if workspace project changes (new/changed/remove projects and metadata changes) affect other
   * projects. isPotentialMatch Implementations should be good enough to avoid obviously pointless project dependency
   * refreshes, but does not have to be perfectly precise.<br/>
   *
   * @param matchResolved is a hint that defines whether requirements can be narrowed down to a certain version of
   *          capability, e.g. resolved dependency
   */
  public abstract boolean isPotentialMatch(Capability capability, boolean versionMatch);

}
