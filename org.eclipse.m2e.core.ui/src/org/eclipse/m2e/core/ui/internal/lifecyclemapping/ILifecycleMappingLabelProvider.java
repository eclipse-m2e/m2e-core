/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.lifecyclemapping;

import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingConfiguration;

/**
 * ILifecycleMappingLabelProvider
 * 
 * @author igor
 */
public interface ILifecycleMappingLabelProvider {

  /**
   * Returns label of Maven Project element, i.e. project itself, packaging type, plugin execution, etc.
   */
  public String getMavenText();

  /**
   * Returns label of Eclipse workspace mapping of the Maven Project element, i.e. execute, ignore, configurator, error,
   * etc.
   * @param mappingConfiguration 
   */
  public String getEclipseMappingText(LifecycleMappingConfiguration mappingConfiguration);
  
  public boolean isError(LifecycleMappingConfiguration mappingConfiguration);
  
  public ILifecycleMappingRequirement getKey();
}
