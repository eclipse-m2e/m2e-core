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
@SuppressWarnings("restriction")
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
    if(element instanceof LifecycleStrategyMappingRequirement) {
      return NLS.bind("Connector {0}", ((LifecycleStrategyMappingRequirement) element).getLifecycleMappingId());
    } else if(element instanceof MojoExecutionMappingRequirement) {
      MojoExecutionKey exec = ((MojoExecutionMappingRequirement) element).getExecution();
      return NLS.bind("{0}:{1}:{2}",
          new String[] {exec.getArtifactId(), exec.getVersion(), exec.getGoal(), String.valueOf(content.size())});
    } else if(element instanceof PackagingTypeMappingRequirement) {
      return NLS.bind("Packaging {0}", ((PackagingTypeMappingRequirement) element).getPackaging());
    } else if(element instanceof ProjectConfiguratorMappingRequirement) {
      MojoExecutionKey exec = ((ProjectConfiguratorMappingRequirement) element).getExecution();
      return NLS.bind("{0}:{1}:{2}",
          new String[] {exec.getArtifactId(), exec.getVersion(), exec.getGoal(), String.valueOf(content.size())});
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
    if(other instanceof AggregateMappingLabelProvider) {
      return other.hashCode() == hashCode();
    }
    return false;
  }
}
