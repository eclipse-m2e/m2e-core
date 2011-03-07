/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.lifecyclemapping;

import java.util.List;

import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingElement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.PackagingTypeMappingConfiguration;
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
  private final ILifecycleMappingElement element;

  public AggregateMappingLabelProvider(ILifecycleMappingElement element, List<ILifecycleMappingLabelProvider> content) {
    this.content = content;
    this.element = element;
  }
  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider#getMavenText()
   */
  public String getMavenText() {
    if(element instanceof PackagingTypeMappingConfiguration) {
      return NLS.bind("Packaging {0}", ((PackagingTypeMappingConfiguration) element).getPackaging());
    }
    if(element instanceof MojoExecutionMappingConfiguration) {
      MojoExecutionKey exec = ((MojoExecutionMappingConfiguration) element).getExecution();
      return exec.getArtifactId() + ":" + exec.getVersion() + ":" + exec.getGoal(); //TODO
    }
    throw new IllegalStateException();
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider#getEclipseMappingText()
   */
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

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider#isError()
   */
  public boolean isError(LifecycleMappingConfiguration mappingConfiguration) {
    for (ILifecycleMappingLabelProvider pr : content) {
      if (pr.isError(mappingConfiguration)) {
        return true;
      }
    }
    return false;
  }
  /**
   * @return
   */
  public ILifecycleMappingLabelProvider[] getChildren() {
    return content.toArray(new ILifecycleMappingLabelProvider[0]);
  }
  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider#getKey()
   */
  public ILifecycleMappingRequirement getKey() {
    return element.getLifecycleMappingRequirement();
  }

}
