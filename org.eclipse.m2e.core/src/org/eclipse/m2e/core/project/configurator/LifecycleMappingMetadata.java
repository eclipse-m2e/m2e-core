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
package org.eclipse.m2e.core.project.configurator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class LifecycleMappingMetadata {
  public static final String PLUGIN_GROUPID = "org.eclipse.m2e"; //$NON-NLS-1$

  public static final String PLUGIN_ARTIFACTID = "lifecycle-mapping-metadata-sources"; //$NON-NLS-1$

  public static final String PLUGIN_KEY = PLUGIN_GROUPID + ":" + PLUGIN_ARTIFACTID; //$NON-NLS-1$

  public static final String ELEMENT_SOURCES = "sources"; //$NON-NLS-1$

  public static final String ELEMENT_SOURCE = "source"; //$NON-NLS-1$

  private final String groupId;

  private final String artifactId;

  private final String version;

  private Map<String, String> lifecycleMappingsByPackagingType = new LinkedHashMap<String, String>();

  private List<PluginExecutionMetadata> pluginExecutions = new ArrayList<PluginExecutionMetadata>();

  public LifecycleMappingMetadata(String groupId, String artifactId, String version) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
  }

  public void addLifecycleMapping(String packagingType, String lifecycleMappingId) {
    lifecycleMappingsByPackagingType.put(packagingType, lifecycleMappingId);
  }

  public String getLifecycleMappingId(String packagingType) {
    return lifecycleMappingsByPackagingType.get(packagingType);
  }

  public void addPluginExecution(PluginExecutionMetadata pluginExecutionMetadata) {
    pluginExecutions.add(pluginExecutionMetadata);
  }

  public List<PluginExecutionMetadata> getPluginExecutions() {
    // TODO: return a clone?
    return pluginExecutions;
  }

  public String getGroupId() {
    return this.groupId;
  }

  public String getArtifactId() {
    return this.artifactId;
  }

  public String getVersion() {
    return this.version;
  }
}
