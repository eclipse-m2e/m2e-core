/*******************************************************************************
 * Copyright (c) 2008-2013 Sonatype, Inc. and others
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.osgi.util.NLS;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingDiscoveryRequest;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration.MojoExecutionMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration.ProjectConfiguratorMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.PackagingTypeMappingConfiguration.LifecycleStrategyMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.PackagingTypeMappingConfiguration.PackagingTypeMappingRequirement;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


/**
 * AggregateMappingLabelProvider
 *
 * @author mkleint
 */
public class AggregateMappingLabelProvider implements ILifecycleMappingLabelProvider {

  private final List<ILifecycleMappingLabelProvider> content;

  private final ILifecycleMappingRequirement element;

  public AggregateMappingLabelProvider(ILifecycleMappingRequirement element,
      List<ILifecycleMappingLabelProvider> content) {
    this.content = content;
    this.element = element;
  }

  @Override
  public String getMavenText() {
    if(element instanceof LifecycleStrategyMappingRequirement lifecycleStrategyMappingRequirement) {
      return NLS.bind("Connector {0}", lifecycleStrategyMappingRequirement.getLifecycleMappingId());
    } else if(element instanceof MojoExecutionMappingRequirement req) {
      MojoExecutionKey exec = req.getExecution();
      return NLS.bind("{0}:{1}:{2}",
          new String[] {exec.artifactId(), exec.version(), exec.goal(), String.valueOf(content.size())});
    } else if(element instanceof PackagingTypeMappingRequirement req) {
      return NLS.bind("Packaging {0}", req.getPackaging());
    } else if(element instanceof ProjectConfiguratorMappingRequirement req) {
      MojoExecutionKey exec = req.getExecution();
      return NLS.bind("{0}:{1}:{2}",
          new String[] {exec.artifactId(), exec.version(), exec.goal(), String.valueOf(content.size())});
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean isError(LifecycleMappingDiscoveryRequest mappingConfiguration) {
    for(ILifecycleMappingLabelProvider pr : content) {
      if(pr.isError(mappingConfiguration)) {
        return true;
      }
    }
    return false;
  }

  public ILifecycleMappingLabelProvider[] getChildren() {
    return content.toArray(new ILifecycleMappingLabelProvider[content.size()]);
  }

  @Override
  public ILifecycleMappingRequirement getKey() {
    return element;
  }

  @Override
  public Collection<MavenProject> getProjects() {
    Set<MavenProject> projects = new HashSet<>();
    for(ILifecycleMappingLabelProvider provider : content) {
      projects.addAll(provider.getProjects());
    }
    return projects;
  }

  @Override
  public int hashCode() {
    return getMavenText().hashCode();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof AggregateMappingLabelProvider && other.hashCode() == hashCode();
  }
}
