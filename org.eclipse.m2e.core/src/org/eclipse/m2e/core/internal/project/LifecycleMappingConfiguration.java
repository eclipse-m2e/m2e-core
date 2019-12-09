/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.internal.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.ILifecycleMappingConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


public class LifecycleMappingConfiguration implements ILifecycleMappingConfiguration, Serializable {

  private static final long serialVersionUID = -4584011302289465969L;

  private static Logger log = LoggerFactory.getLogger(LifecycleMappingConfiguration.class);

  private final String lifecycleMappingId;

  private final Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mojoExecutionMapping;

  private final Map<MojoExecutionKey, Xpp3Dom> mojoExecutionConfiguration;

  public LifecycleMappingConfiguration(String lifecycleMappingId,
      Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mojoExecutionMapping,
      Map<MojoExecutionKey, Xpp3Dom> mojoExecutionConfiguration) {
    this.lifecycleMappingId = lifecycleMappingId;
    this.mojoExecutionMapping = mojoExecutionMapping;
    this.mojoExecutionConfiguration = mojoExecutionConfiguration;
  }

  public String getLifecycleMappingId() {
    return lifecycleMappingId;
  }

  public Map<MojoExecutionKey, List<IPluginExecutionMetadata>> getMojoExecutionMapping() {
    return mojoExecutionMapping;
  }

  public Xpp3Dom getMojoExecutionConfiguration(MojoExecutionKey key) {
    return mojoExecutionConfiguration.get(key);
  }

  public static void persist(IMavenProjectFacade facade, IProgressMonitor monitor) {
    LifecycleMappingConfiguration configuration;
    try {
      configuration = newLifecycleMappingConfiguration(facade, monitor);

      if(configuration == null) {
        return;
      }
    } catch(CoreException ex) {
      log.warn("Could not persist build lifecycle mapping configuration for {}.", facade.toString(), ex);
      return;
    }

    persist(facade.getProject(), configuration);
  }

  private static void persist(IProject project, LifecycleMappingConfiguration configuration) {
    try {
      File configFile = getConfigurationFile(project);
      ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(configFile));
      try {
        oos.writeObject(configuration);
        oos.flush();
      } finally {
        IOUtil.close(oos);
      }
    } catch(IOException ex) {
      log.warn("Could not persist build lifecycle mapping configuration for {}.", project.toString(), ex);
    }
  }

  public static LifecycleMappingConfiguration newLifecycleMappingConfiguration(IMavenProjectFacade facade,
      IProgressMonitor monitor) throws CoreException {
    String lifecycleMappingId = facade.getLifecycleMappingId();
    Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mojoExecutionMapping = facade.getMojoExecutionMapping();

    if(lifecycleMappingId == null || mojoExecutionMapping == null) {
      return null;
    }

    Map<MojoExecutionKey, Xpp3Dom> mojoExecutionConfiguration = new LinkedHashMap<MojoExecutionKey, Xpp3Dom>();

    for(Map.Entry<MojoExecutionKey, List<IPluginExecutionMetadata>> entry : mojoExecutionMapping.entrySet()) {
      List<IPluginExecutionMetadata> metadatas = entry.getValue();
      if(metadatas != null) {
        for(IPluginExecutionMetadata metadata : metadatas) {
          if(metadata.getAction() == PluginExecutionAction.configurator) {
            MojoExecution execution = facade.getMojoExecution(entry.getKey(), monitor);
            mojoExecutionConfiguration.put(entry.getKey(), execution.getConfiguration());
            break;
          }
        }
      }
    }

    LifecycleMappingConfiguration configuration = new LifecycleMappingConfiguration(lifecycleMappingId,
        mojoExecutionMapping, mojoExecutionConfiguration);
    return configuration;
  }

  private static File getConfigurationFile(IProject project) {
    File stateLocationDir = MavenPluginActivator.getDefault().getStateLocation().toFile();
    File configFile = new File(stateLocationDir, project.getName() + ".lifecyclemapping");
    return configFile;
  }

  public static LifecycleMappingConfiguration restore(IMavenProjectFacade facade, IProgressMonitor monitor) {
    File configFile = getConfigurationFile(facade.getProject());
    try {
      ObjectInputStream ois = new ObjectInputStream(new FileInputStream(configFile));
      try {
        Object obj = ois.readObject();
        if(obj instanceof LifecycleMappingConfiguration) {
          return (LifecycleMappingConfiguration) obj;
        }
      } finally {
        IOUtil.close(ois);
      }
    } catch(ClassNotFoundException ex) {
      log.warn("Could not read persistent build lifecycle mapping configuration for {}.", facade.toString(), ex);
    } catch(IOException ex) {
      //log.warn("Could not read persistent build lifecycle mapping configuration for {}.", facade.toString(), ex);
      remove(facade.getProject());
    }
    return null;
  }

  public static void remove(IProject project) {
    getConfigurationFile(project).delete();
  }

  public static void persistEmpty(IProject project) {
    persist(project, new LifecycleMappingConfiguration(null, null, null));
  }
}
