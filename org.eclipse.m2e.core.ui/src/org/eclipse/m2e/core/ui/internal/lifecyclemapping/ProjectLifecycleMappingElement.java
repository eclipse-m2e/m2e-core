/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.lifecyclemapping;

import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingElement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ProjectLifecycleMappingConfiguration;


/**
 * Represents instance of ILifecycleMappingElement within context of specific ProjectLifecycleMappingConfiguration
 */
@SuppressWarnings("restriction")
public class ProjectLifecycleMappingElement {
  private final ProjectLifecycleMappingConfiguration project;

  private final ILifecycleMappingElement element;
  
  public ProjectLifecycleMappingElement(ProjectLifecycleMappingConfiguration project, ILifecycleMappingElement element) {
    this.project = project;
    this.element = element;
  }

  public ProjectLifecycleMappingConfiguration getProject() {
    return project;
  }

  public ILifecycleMappingElement getElement() {
    return element;
  }
}
