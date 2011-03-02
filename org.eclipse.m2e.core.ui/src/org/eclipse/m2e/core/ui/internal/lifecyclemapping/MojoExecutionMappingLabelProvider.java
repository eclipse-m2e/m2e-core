/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.lifecyclemapping;

import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingElementKey;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscoveryProposal;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration.MojoExecutionMappingInfo;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ProjectLifecycleMappingConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.osgi.util.NLS;


/**
 * MojoExecutionMappingLabelProvider
 * 
 * @author igor
 */
@SuppressWarnings("restriction")
public class MojoExecutionMappingLabelProvider implements ILifecycleMappingLabelProvider {

  private final MojoExecutionMappingConfiguration element;
  private final ProjectLifecycleMappingConfiguration prjconf;

  public MojoExecutionMappingLabelProvider(ProjectLifecycleMappingConfiguration prjconf, MojoExecutionMappingConfiguration element) {
    this.element = element;
    this.prjconf =  prjconf;
  }

  public String getMavenText() {
    MojoExecutionKey execution = element.getExecution();
    if ("default".equals(execution.getExecutionId())) {
      return NLS.bind("{0}", prjconf.getRelpath());
    }
    //TODO is execution id actually important or just takes up space
    return NLS.bind("Execution {0}, in {1}", execution.getExecutionId(), prjconf.getRelpath());
  }

  public String getEclipseMappingText(LifecycleMappingConfiguration mappingConfiguration) {
    StringBuilder sb = new StringBuilder();
    if(element.getMappings().isEmpty()) {
      if(LifecycleMappingFactory.isInterestingPhase(element.getExecution().getLifecyclePhase())) {
        sb.append("Not covered");
      }
    } else {
      for(MojoExecutionMappingInfo mapping : element.getMappings()) {
        switch(mapping.getMapping().getAction()) {
          case configurator:
            if(mapping.getConfigurator() == null) {
              sb.append("Missing Connector '").append(
                  LifecycleMappingFactory.getProjectConfiguratorId(mapping.getMapping()) + "'");
            }
            break;
          case execute:
            sb.append("Executing Maven goal");
            break;
          case error:
            sb.append("Not supported - ").append(LifecycleMappingFactory.getActionMessage(mapping.getMapping()));
            break;
          case ignore:
            sb.append("Ignoring");
            break;
        }
      }
    }

    return sb.toString();
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider#isError()
   */
  public boolean isError() {
    if(element.getMappings().isEmpty()) {
      if(LifecycleMappingFactory.isInterestingPhase(element.getExecution().getLifecyclePhase())) {
        return true;
      } else {
        return false;
      }
    } else {
      for(MojoExecutionMappingInfo mapping : element.getMappings()) {
        switch(mapping.getMapping().getAction()) {
          case configurator:
            if(mapping.getConfigurator() == null) {
              return true;
            } else {
              return false;
            }
          case execute:
            return false;
          case error:
            return true;
          case ignore:
            return false;
        }
      }
    }
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider#getKey()
   */
  public ILifecycleMappingElementKey getKey() {
    return element.getLifecycleMappingElementKey();
  }
}
