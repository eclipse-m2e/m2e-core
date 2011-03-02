/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.lifecyclemapping;

import java.util.List;

import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingElementKey;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.PackagingTypeMappingConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.osgi.util.NLS;

/**
 * AggregateMappingLabelProvider
 *
 * @author mkleint
 */
public class AggregateMappingLabelProvider implements ILifecycleMappingLabelProvider {

  private final List<ILifecycleMappingLabelProvider> content;
  private final ILifecycleMappingElementKey key;

  public AggregateMappingLabelProvider(ILifecycleMappingElementKey key, List<ILifecycleMappingLabelProvider> content) {
    this.content = content;
    this.key = key;
  }
  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider#getMavenText()
   */
  public String getMavenText() {
    if(key instanceof PackagingTypeMappingConfiguration.Key) {
      return NLS.bind("Packaging {0}", ((PackagingTypeMappingConfiguration.Key) key).getPackaging());
    }
    if(key instanceof MojoExecutionMappingConfiguration.Key) {
      MojoExecutionKey exec = ((MojoExecutionMappingConfiguration.Key) key).getExecution();
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
  public boolean isError() {
    for (ILifecycleMappingLabelProvider pr : content) {
      if (pr.isError()) {
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
  public ILifecycleMappingElementKey getKey() {
    return key;
  }

}
