/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.lifecyclemapping;

import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingElement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ProjectLifecycleMappingConfiguration;


/**
 * Represents instance of ILifecycleMappingElement within context of specific ProjectLifecycleMappingConfiguration
 */
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
