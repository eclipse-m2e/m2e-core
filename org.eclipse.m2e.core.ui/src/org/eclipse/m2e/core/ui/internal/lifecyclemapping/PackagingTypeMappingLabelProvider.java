/*******************************************************************************
 * Copyright (c) 2008-2013 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

  private PackagingTypeMappingConfiguration element;

  private ProjectLifecycleMappingConfiguration prjconf;

  public PackagingTypeMappingLabelProvider(ProjectLifecycleMappingConfiguration prjconf,
      PackagingTypeMappingConfiguration element) {
    this.element = element;
    this.prjconf = prjconf;
  }

  public String getMavenText() {
    return prjconf.getRelpath();
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider#isError()
   */
  public boolean isError(LifecycleMappingDiscoveryRequest mappingConfiguration) {
    return !mappingConfiguration.isRequirementSatisfied(getKey());
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider#getKey()
   */
  public ILifecycleMappingRequirement getKey() {
    return element.getLifecycleMappingRequirement();
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider#getProjects()
   */
  public Collection<MavenProject> getProjects() {
    return Collections.singleton(prjconf.getMavenProject());
  }
}
