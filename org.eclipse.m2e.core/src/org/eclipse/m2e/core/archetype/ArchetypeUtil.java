/*******************************************************************************
 * Copyright (c) 2013, 2019 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.archetype;

import java.util.Objects;

import org.apache.maven.archetype.catalog.Archetype;


/**
 * Helper class to handle {@link Archetype}s.
 * 
 * @author Fred Bricon
 * @provisional This class is provisional and can be changed, moved or removed without notice
 * @since 1.3
 */
public class ArchetypeUtil {

  /**
   * Checks {@link Archetype} equality by testing <code>groupId</code>, <code>artifactId</code> and <code>version</code>
   */
  public static boolean areEqual(Archetype one, Archetype another) {
    if(one == another) {
      return true;
    }

    if(another == null) {
      return false;
    }

    return Objects.equals(one.getGroupId(), another.getGroupId())
        && Objects.equals(one.getArtifactId(), another.getArtifactId())
        && Objects.equals(one.getVersion(), another.getVersion());
  }

  /**
   * Computes an {@link Archetype} hashcode from the original {@link Archetype#hashCode()} result plus the
   * {@link Archetype#getVersion()} hashcode. Returns -1 if the archetype is null.
   */
  public static int getHashCode(Archetype archetype) {
    if(archetype == null) {
      return -1;
    }
    int hashCode = archetype.hashCode();
    String version = archetype.getVersion();
    if(version != null) {
      hashCode += 31 * version.hashCode();
    }
    return hashCode;
  }
}
