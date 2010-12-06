/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.plugins;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;

public class PluginExtensionDescriptor {
  public static final String ARTIFACT_ID = "artifactId"; //$NON-NLS-1$
  public static final String GROUP_ID = "groupId"; //$NON-NLS-1$
  public static final String NAME = "name"; //$NON-NLS-1$
  
  private String artifactId;
  private String groupId;
  private String name;
  
  private IPluginConfigurationExtension extension = null;
  
  public PluginExtensionDescriptor(IConfigurationElement element) {
    artifactId = element.getAttribute(ARTIFACT_ID);
    groupId = element.getAttribute(GROUP_ID);
    name = element.getAttribute(NAME);

    Object o;
    try {
      o = element.createExecutableExtension(AbstractProjectConfigurator.ATTR_CLASS);
      extension = (IPluginConfigurationExtension) o;
    } catch(CoreException e) {
      // TODO Auto-generated catch block
      MavenLogger.log(e);
    }
  }
  
  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }
  public String getArtifactId() {
    return artifactId;
  }
  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }
  public String getGroupId() {
    return groupId;
  }
  
  public String toString() {
    return groupId + ':' + artifactId; 
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public IPluginConfigurationExtension getExtension() {
    return extension;
  }
}
