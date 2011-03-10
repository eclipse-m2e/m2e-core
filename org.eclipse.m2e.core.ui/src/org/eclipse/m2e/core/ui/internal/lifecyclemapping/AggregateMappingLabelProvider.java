/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.lifecyclemapping;

import java.util.List;

import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration.MojoExecutionMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration.ProjectConfiguratorMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.PackagingTypeMappingConfiguration.LifecycleStrategyMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.PackagingTypeMappingConfiguration.PackagingTypeMappingRequirement;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.osgi.util.NLS;

/**
 * AggregateMappingLabelProvider
 *
 * @author mkleint
 */
@SuppressWarnings("restriction")
public class AggregateMappingLabelProvider implements ILifecycleMappingLabelProvider {

  private final List<ILifecycleMappingLabelProvider> content;
  private final ILifecycleMappingRequirement element;

  public AggregateMappingLabelProvider(ILifecycleMappingRequirement element, List<ILifecycleMappingLabelProvider> content) {
    this.content = content;
    this.element = element;
  }

  public String getMavenText() {
    if(element instanceof LifecycleStrategyMappingRequirement) {
      return NLS.bind("Connector {0}",
          ((LifecycleStrategyMappingRequirement) element).getLifecycleMappingId());
    } else if(element instanceof MojoExecutionMappingRequirement) {
      MojoExecutionKey exec = ((MojoExecutionMappingRequirement) element).getExecution();
      return exec.getArtifactId() + ":" + exec.getVersion() + ":" + exec.getGoal(); //TODO
    } else if(element instanceof PackagingTypeMappingRequirement) {
      return NLS.bind("Packaging {0}", ((PackagingTypeMappingRequirement) element).getPackaging());
    } else if(element instanceof ProjectConfiguratorMappingRequirement) {
      return NLS.bind("Connector {0}",
          ((ProjectConfiguratorMappingRequirement) element).getProjectConfiguratorId());
    }
    throw new IllegalStateException();
  }

  public String getEclipseMappingText(LifecycleMappingConfiguration mappingConfiguration) {
    String match = null;
    for (ILifecycleMappingLabelProvider pr : content) {
      if (match == null) {
        match = pr.getEclipseMappingText(mappingConfiguration);
      } else {
        if (!match.equals(pr.getEclipseMappingText(mappingConfiguration))) {
          return "Multiple values";
        }
      }
    }
    return match;
  }

  public boolean isError(LifecycleMappingConfiguration mappingConfiguration) {
    for (ILifecycleMappingLabelProvider pr : content) {
      if (pr.isError(mappingConfiguration)) {
        return true;
      }
    }
    return false;
  }

  public ILifecycleMappingLabelProvider[] getChildren() {
    return content.toArray(new ILifecycleMappingLabelProvider[0]);
  }

  public ILifecycleMappingRequirement getKey() {
    return element;
  }

}
