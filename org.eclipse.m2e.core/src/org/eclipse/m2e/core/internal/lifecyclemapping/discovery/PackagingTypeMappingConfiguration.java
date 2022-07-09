/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.internal.lifecyclemapping.discovery;

/**
 * Represents project packaging type and corresponding lifecycle mapping metadata.
 */
public class PackagingTypeMappingConfiguration {
  private PackagingTypeMappingConfiguration() {
  } // static use only

  public static record PackagingTypeMappingRequirement(String packaging) implements ILifecycleMappingRequirement {

  }

  public static class LifecycleStrategyMappingRequirement implements ILifecycleMappingRequirement {
    private final String packaging;

    private final String lifecycleMappingId;

    public LifecycleStrategyMappingRequirement(String packaging, String lifecycleMappingId) {
      this.packaging = packaging;
      this.lifecycleMappingId = lifecycleMappingId;
    }

    @Override
    public int hashCode() {
      return lifecycleMappingId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if(this == obj) {
        return true;
      }

      return obj instanceof LifecycleStrategyMappingRequirement other
          && lifecycleMappingId.equals(other.lifecycleMappingId);
    }

    public String getLifecycleMappingId() {
      return lifecycleMappingId;
    }

    public String getPackaging() {
      return packaging;
    }
  }
}
