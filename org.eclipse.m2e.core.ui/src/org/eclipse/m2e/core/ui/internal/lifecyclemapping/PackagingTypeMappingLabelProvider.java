/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.lifecyclemapping;

import org.eclipse.m2e.core.internal.lifecycle.discovery.PackagingTypeMappingConfiguration;


/**
 * PackagingTypeMappingLabelProvider
 * 
 * @author igor
 */
@SuppressWarnings("restriction")
public class PackagingTypeMappingLabelProvider implements ILifecycleMappingLabelProvider {

  private PackagingTypeMappingConfiguration element;

  public PackagingTypeMappingLabelProvider(PackagingTypeMappingConfiguration element) {
    this.element = element;
  }

  public String getMavenText() {
    StringBuilder sb = new StringBuilder();
    if(element.getLifecycleMappingId() == null) {
      sb.append("ERROR no lifecycle mapping strategy");
    } else if(element.getLifecycleMapping() == null) {
      sb.append("ERROR no lifecycle mapping strategy implementation with id=").append(element.getLifecycleMappingId());
    } else {
      sb.append("OK lifecycleMappingId=").append(element.getLifecycleMappingId());
    }
    return sb.toString();
  }

  public String getEclipseMappingText() {
    return "(project)";
  }

}
