/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecyclemapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.markers.MavenProblemInfo;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.configurator.AbstractLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


public class LifecycleMappingResult {
  private LifecycleMappingMetadata lifecycleMappingMetadata;

  private Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mojoExecutionMapping;

  private List<MavenProblemInfo> problems = new ArrayList<MavenProblemInfo>();

  private AbstractLifecycleMapping lifecycleMapping;

  private Map<String, AbstractProjectConfigurator> configurators;

  public String getLifecycleMappingId() {
    return lifecycleMappingMetadata != null ? lifecycleMappingMetadata.getLifecycleMappingId() : null;
  }

  public LifecycleMappingMetadata getLifecycleMappingMetadata() {
    return this.lifecycleMappingMetadata;
  }

  public Map<MojoExecutionKey, List<IPluginExecutionMetadata>> getMojoExecutionMapping() {
    return mojoExecutionMapping;
  }

  public boolean hasProblems() {
    return !problems.isEmpty();
  }

  public List<MavenProblemInfo> getProblems() {
    return problems;
  }

  public void setLifecycleMappingMetadata(LifecycleMappingMetadata lifecycleMappingMetadata) {
    this.lifecycleMappingMetadata = lifecycleMappingMetadata;
  }

  public void setMojoExecutionMapping(Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mojoExecutionMapping) {
    this.mojoExecutionMapping = mojoExecutionMapping;
  }

  public void addProblem(MavenProblemInfo problem) {
    problems.add(problem);
  }

  public void setLifecycleMapping(AbstractLifecycleMapping lifecycleMapping) {
    this.lifecycleMapping = lifecycleMapping;
  }

  public AbstractLifecycleMapping getLifecycleMapping() {
    return lifecycleMapping;
  }

  public Map<String, AbstractProjectConfigurator> getProjectConfigurators() {
    return configurators;
  }

  public void setProjectConfigurators(Map<String, AbstractProjectConfigurator> configurators) {
    this.configurators = configurators;
  }

}
