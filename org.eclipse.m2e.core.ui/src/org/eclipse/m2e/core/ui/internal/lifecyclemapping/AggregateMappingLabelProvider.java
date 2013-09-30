/*******************************************************************************
 * Copyright (c) 2008-2013 Sonatype, Inc. and others
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
      return NLS.bind("Connector {0}", ((ProjectConfiguratorMappingRequirement) element).getProjectConfiguratorId());
    }
    throw new IllegalStateException();
  }

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

  public ILifecycleMappingRequirement getKey() {
    return element;
  }

  public Collection<MavenProject> getProjects() {
    Set<MavenProject> projects = new HashSet<MavenProject>();
    for(ILifecycleMappingLabelProvider provider : content) {
      projects.addAll(provider.getProjects());
    }
    return projects;
  }

  public int hashCode() {
    return getMavenText().hashCode();
  }

  public boolean equals(Object other) {
    if(other instanceof AggregateMappingLabelProvider) {
      return other.hashCode() == hashCode();
    }
    return false;
  }
}
