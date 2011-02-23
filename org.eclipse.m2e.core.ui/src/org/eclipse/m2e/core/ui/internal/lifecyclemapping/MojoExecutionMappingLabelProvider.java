/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.lifecyclemapping;

import org.eclipse.m2e.core.internal.lifecycle.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecycle.discovery.MojoExecutionMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecycle.discovery.MojoExecutionMappingConfiguration.MojoExecutionMappingInfo;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


/**
 * MojoExecutionMappingLabelProvider
 * 
 * @author igor
 */
@SuppressWarnings("restriction")
public class MojoExecutionMappingLabelProvider implements ILifecycleMappingLabelProvider {

  private MojoExecutionMappingConfiguration element;

  public MojoExecutionMappingLabelProvider(MojoExecutionMappingConfiguration element) {
    this.element = element;
  }

  public String getMavenText() {
    StringBuilder sb = new StringBuilder();

    if(element.getMappings().isEmpty()) {
      if(LifecycleMappingFactory.isInterestingPhase(element.getExecution().getLifecyclePhase())) {
        sb.append("ERROR not covered plugin execution");
      } else {
        sb.append("OK not interesting");
      }
    } else {
      for(MojoExecutionMappingInfo mapping : element.getMappings()) {
        switch(mapping.getMapping().getAction()) {
          case configurator:
            if(mapping.getConfigurator() == null) {
              sb.append("ERROR no project configurator with id=").append(
                  LifecycleMappingFactory.getProjectConfiguratorId(mapping.getMapping()));
            } else {
              sb.append("OK configurator"); // TODO more detail
            }
            break;
          case execute:
            sb.append("OK execute"); // TODO add details
            break;
          case error:
            sb.append("ERROR ").append(LifecycleMappingFactory.getActionMessage(mapping.getMapping()));
            break;
          case ignore:
            sb.append("OK (ignore)");
            break;
        }
      }
    }

    return sb.toString();
  }

  public String getEclipseMappingText() {
    MojoExecutionKey execution = element.getExecution();
    return execution.getArtifactId() + " (goal " + execution.getGoal() + ")";
  }

}
