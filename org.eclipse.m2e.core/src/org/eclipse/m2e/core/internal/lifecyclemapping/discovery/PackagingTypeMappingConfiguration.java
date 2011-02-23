/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecyclemapping.discovery;

import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;


/**
 * PackagingTypeMappingConfiguration
 * 
 * @author igor
 */
public class PackagingTypeMappingConfiguration {

  public static class Key implements ILifecycleMappingElementKey {
    private final String packaging;

    public Key(String packaging) {
      this.packaging = packaging;
    }

    public int hashCode() {
      return packaging.hashCode();
    }

    public boolean equals(Object obj) {
      if(obj == this) {
        return true;
      }
      if(!(obj instanceof Key)) {
        return false;
      }
      return packaging.equals(((Key) obj).packaging);
    }

    public String getPackaging() {
      return packaging;
    }

  }

  private final String packaging;

  private final String lifecycleMappingId;

  private final ILifecycleMapping lifecycleMapping;

  public PackagingTypeMappingConfiguration(String packaging, String lifecycleMappingId,
      ILifecycleMapping lifecycleMapping) {
    this.packaging = packaging;
    this.lifecycleMappingId = lifecycleMappingId;
    this.lifecycleMapping = lifecycleMapping;
  }

  public String getPackaging() {
    return packaging;
  }

  public String getLifecycleMappingId() {
    return lifecycleMappingId;
  }

  public ILifecycleMapping getLifecycleMapping() {
    return lifecycleMapping;
  }

  public boolean isOK() {
    return lifecycleMappingId != null && lifecycleMapping != null;
  }

  public ILifecycleMappingElementKey getLifecycleMappingElementKey() {
    return new Key(packaging);
  }

}
