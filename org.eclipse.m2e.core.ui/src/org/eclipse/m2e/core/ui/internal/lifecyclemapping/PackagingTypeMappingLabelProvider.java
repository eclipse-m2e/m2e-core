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
import java.util.Collections;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingDiscoveryRequest;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.PackagingTypeMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ProjectLifecycleMappingConfiguration;


/**
 * PackagingTypeMappingLabelProvider
 *
 * @author igor
 */
@SuppressWarnings("restriction")
public class PackagingTypeMappingLabelProvider implements ILifecycleMappingLabelProvider {

  private final PackagingTypeMappingConfiguration element;

  private final ProjectLifecycleMappingConfiguration prjconf;

  public PackagingTypeMappingLabelProvider(ProjectLifecycleMappingConfiguration prjconf,
      PackagingTypeMappingConfiguration element) {
    this.element = element;
    this.prjconf = prjconf;
  }

  @Override
  public String getMavenText() {
    return prjconf.getRelpath();
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider#isError()
   */
  @Override
  public boolean isError(LifecycleMappingDiscoveryRequest mappingConfiguration) {
    return !mappingConfiguration.isRequirementSatisfied(getKey());
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider#getKey()
   */
  @Override
  public ILifecycleMappingRequirement getKey() {
    return element.getLifecycleMappingRequirement();
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider#getProjects()
   */
  @Override
  public Collection<MavenProject> getProjects() {
    return Collections.singleton(prjconf.getMavenProject());
  }
}
