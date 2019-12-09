/*******************************************************************************
 * Copyright (c) 2008-2013 Sonatype, Inc. and others.
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

package org.eclipse.m2e.core.ui.internal.lifecyclemapping;

import java.util.Collection;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingDiscoveryRequest;


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

  public boolean isError(LifecycleMappingDiscoveryRequest mappingConfiguration);

  public ILifecycleMappingRequirement getKey();

  public Collection<MavenProject> getProjects();
}
