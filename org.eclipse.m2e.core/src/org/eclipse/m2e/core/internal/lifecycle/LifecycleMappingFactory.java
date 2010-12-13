/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecycle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.project.IgnoreMojoProjectConfiguration;
import org.eclipse.m2e.core.internal.project.MojoExecutionProjectConfigurator;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.CustomizableLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.LifecycleMappingConfigurationException;
import org.eclipse.m2e.core.project.configurator.LifecycleMappingMetadata;
import org.eclipse.m2e.core.project.configurator.PluginExecutionAction;
import org.eclipse.m2e.core.project.configurator.PluginExecutionFilter;
import org.eclipse.m2e.core.project.configurator.PluginExecutionMetadata;


/**
 * LifecycleMappingFactory
 * 
 * @author igor
 */
public class LifecycleMappingFactory {

  public static final String EXTENSION_LIFECYCLE_MAPPINGS = IMavenConstants.PLUGIN_ID + ".lifecycleMappings"; //$NON-NLS-1$

  public static final String EXTENSION_PROJECT_CONFIGURATORS = IMavenConstants.PLUGIN_ID + ".projectConfigurators"; //$NON-NLS-1$

  private static final String ELEMENT_LIFECYCLE_MAPPING = "lifecycleMapping"; //$NON-NLS-1$

  private static final String ELEMENT_LIFECYCLE_MAPPING_METADATA = "lifecycleMappingMetadata"; //$NON-NLS-1$

  private static final String ATTR_CLASS = "class"; //$NON-NLS-1$

  private static final String ATTR_PACKAGING_TYPE = "packaging-type"; //$NON-NLS-1$

  private static final String ATTR_ID = "id"; //$NON-NLS-1$

  private static final String ELEMENT_CONFIGURATOR = "configurator"; //$NON-NLS-1$

  private static final String ELEMENT_MOJOS = "mojos"; //$NON-NLS-1$

  private static final String ELEMENT_MOJO = "mojo"; //$NON-NLS-1$

  private static final String ELEMENT_ACTION = "action"; //$NON-NLS-1$

  private static final String ELEMENT_IGNORE = "ignore"; //$NON-NLS-1$

  private static final String ELEMENT_EXECUTE = "execute";

  private static final String ELEMENT_RUN_ON_INCREMENTAL = "runOnIncremental";

  private static final String ATTR_GROUPID = "groupId";

  private static final String ATTR_ARTIFACTID = "artifactId";

  private static final String ATTR_VERSION = "version";

  private static final String ATTR_VERSIONRANGE = "versionRange";

  private static final String ATTR_GOALS = "goals";

  private static final String ATTR_GOAL = "goal";

  private static final String LIFECYCLE_MAPPING_METADATA_CLASSIFIER = "lifecycle-mapping-metadata";

  /**
   * Returns default lifecycle mapping for specified packaging type or null if no such lifecycle mapping
   */
  public static ILifecycleMapping getLifecycleMappingFor(String packagingType) {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_LIFECYCLE_MAPPINGS);
    if(configuratorsExtensionPoint != null) {
      IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
      for(IExtension extension : configuratorExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_LIFECYCLE_MAPPING)) {
            if(packagingType.equals(element.getAttribute(ATTR_PACKAGING_TYPE))) {
              return createLifecycleMapping(element);
            }
          }
        }
      }
    }
    return null;
  }

  private static AbstractProjectConfigurator getProjectConfigurator(PluginExecutionMetadata pluginExecutionMetadata) {
    PluginExecutionAction pluginExecutionAction = pluginExecutionMetadata.getAction();
    if(pluginExecutionAction == PluginExecutionAction.IGNORE) {
      return new IgnoreMojoProjectConfiguration();
    }
    if(pluginExecutionAction == PluginExecutionAction.EXECUTE) {
      return createMojoExecution(pluginExecutionMetadata);
    }
    if(pluginExecutionAction == PluginExecutionAction.CONFIGURATOR) {
      Xpp3Dom child = pluginExecutionMetadata.getConfiguration().getChild(ATTR_ID);
      if(child == null || child.getValue().trim().length() == 0) {
        throw new LifecycleMappingConfigurationException("A configurator id must be specified");
      }
      String configuratorId = child.getValue();
      return createProjectConfigurator(configuratorId, true/*bare*/);
    }
    throw new IllegalStateException("An action must be specified.");
  }

  protected static ILifecycleMapping createLifecycleMapping(IConfigurationElement element) {
    try {
      ILifecycleMapping mapping = (ILifecycleMapping) element.createExecutableExtension(ATTR_CLASS);
      if(mapping instanceof CustomizableLifecycleMapping) {
        CustomizableLifecycleMapping customizable = (CustomizableLifecycleMapping) mapping;
        for(IConfigurationElement mojo : element.getChildren(ELEMENT_MOJO)) {
          AbstractProjectConfigurator configurator = null;
          if(mojo.getChildren(ELEMENT_IGNORE).length > 0) {
            configurator = new IgnoreMojoProjectConfiguration();
          } else if(mojo.getChildren(ELEMENT_EXECUTE).length > 0) {
            configurator = createMojoExecution(mojo.getChildren(ELEMENT_EXECUTE)[0]);
          } else if(mojo.getChildren(ELEMENT_CONFIGURATOR).length > 0) {
            String configuratorId = mojo.getChildren(ELEMENT_CONFIGURATOR)[0].getAttribute(ATTR_ID);
            configurator = createProjectConfigurator(configuratorId, true/*bare*/);
          } else {
            MavenLogger.log("Invalid lifecycle mapping configuration element: " + mojo.toString());
          }
          if(configurator != null) {
            configurator.addPluginExecutionFilter(createPluginExecutionFilter(mojo));
            customizable.addConfigurator(configurator);
          }
        }
      }
      return mapping;
    } catch(CoreException ex) {
      MavenLogger.log(ex);
    }
    return null;
  }

  private static AbstractProjectConfigurator createMojoExecution(PluginExecutionMetadata pluginExecutionMetadata) {
    boolean runOnIncremental = true;
    Xpp3Dom child = pluginExecutionMetadata.getConfiguration().getChild(ELEMENT_RUN_ON_INCREMENTAL);
    if(child != null) {
      runOnIncremental = Boolean.parseBoolean(child.getValue());
    }
    return new MojoExecutionProjectConfigurator(pluginExecutionMetadata.getFilter(), runOnIncremental);
  }

  private static AbstractProjectConfigurator createMojoExecution(IConfigurationElement configuration) {
    boolean runOnIncremental = true; // TODO
    return new MojoExecutionProjectConfigurator(runOnIncremental);
  }

  private static PluginExecutionFilter createPluginExecutionFilter(IConfigurationElement configuration) {
    String groupId = configuration.getAttribute(ATTR_GROUPID);
    String artifactId = configuration.getAttribute(ATTR_ARTIFACTID);
    String versionRange = configuration.getAttribute(ATTR_VERSIONRANGE);
    String goals = configuration.getAttribute(ATTR_GOALS);
    return new PluginExecutionFilter(groupId, artifactId, versionRange, goals);
  }

  public static ILifecycleMapping getLifecycleMapping(String mappingId) {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_LIFECYCLE_MAPPINGS);
    if(configuratorsExtensionPoint != null) {
      IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
      for(IExtension extension : configuratorExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_LIFECYCLE_MAPPING)) {
            if(mappingId.equals(element.getAttribute(ATTR_ID)))
              return createLifecycleMapping(element);
          }
        }
      }
    }
    return null;
  }

  public static AbstractProjectConfigurator getProjectConfigurator(String configuratorId) {
    return createProjectConfigurator(configuratorId, false/*bare*/);
  }

  protected static AbstractProjectConfigurator createProjectConfigurator(String configuratorId, boolean bare) {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_PROJECT_CONFIGURATORS);
    if(configuratorsExtensionPoint != null) {
      IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
      for(IExtension extension : configuratorExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_CONFIGURATOR)) {
            if(configuratorId.equals(element.getAttribute(AbstractProjectConfigurator.ATTR_ID))) {
              try {
                AbstractProjectConfigurator configurator = (AbstractProjectConfigurator) element
                    .createExecutableExtension(AbstractProjectConfigurator.ATTR_CLASS);

                MavenPlugin plugin = MavenPlugin.getDefault();
                configurator.setProjectManager(plugin.getMavenProjectManager());
                configurator.setMavenConfiguration(plugin.getMavenConfiguration());
                configurator.setMarkerManager(plugin.getMavenMarkerManager());
                configurator.setConsole(plugin.getConsole());

                if(!bare) {
                  for(IConfigurationElement mojo : element.getChildren(ELEMENT_MOJO)) {
                    configurator.addPluginExecutionFilter(createPluginExecutionFilter(mojo));
                  }
                }

                return configurator;
              } catch(CoreException ex) {
                MavenLogger.log(ex);
              }
            }
          }
        }
      }
    }
    return null;
  }

  public static AbstractProjectConfigurator createProjectConfiguratorFor(IMavenProjectFacade mavenProjectFacade,
      MojoExecution mojoExecution) {
    for(LifecycleMappingMetadata lifecycleMappingMetadata : mavenProjectFacade.getLifecycleMappingMetadataSources()) {
      for(PluginExecutionMetadata pluginExecutionMetadata : lifecycleMappingMetadata.getPluginExecutions()) {
        if(pluginExecutionMetadata.getFilter().match(mojoExecution)) {
          return getProjectConfigurator(pluginExecutionMetadata);
        }
      }
    }
    return createProjectConfiguratorFor(mojoExecution);
  }

  private static AbstractProjectConfigurator createProjectConfiguratorFor(MojoExecution execution) {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_PROJECT_CONFIGURATORS);
    if(configuratorsExtensionPoint != null) {
      IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
      for(IExtension extension : configuratorExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_CONFIGURATOR)) {
            if(isConfiguratorEnabledFor(element, execution)) {
              try {
                AbstractProjectConfigurator configurator = (AbstractProjectConfigurator) element
                    .createExecutableExtension(AbstractProjectConfigurator.ATTR_CLASS);

                MavenPlugin plugin = MavenPlugin.getDefault();
                configurator.setProjectManager(plugin.getMavenProjectManager());
                configurator.setMavenConfiguration(plugin.getMavenConfiguration());
                configurator.setMarkerManager(plugin.getMavenMarkerManager());
                configurator.setConsole(plugin.getConsole());

                for(IConfigurationElement mojo : element.getChildren(ELEMENT_MOJO)) {
                  configurator.addPluginExecutionFilter(createPluginExecutionFilter(mojo));
                }

                return configurator;
              } catch(CoreException ex) {
                MavenLogger.log(ex);
              }
            }
          }
        }
      }
    }
    return null;
  }

  private static boolean isConfiguratorEnabledFor(IConfigurationElement configuration, MojoExecution execution) {
    for(IConfigurationElement mojo : configuration.getChildren(ELEMENT_MOJO)) {
      if(createPluginExecutionFilter(mojo).match(execution)) {
        return true;
      }
    }
    return false;
  }

  public static List<LifecycleMappingMetadata> getLifecycleMappingMetadata(MavenProject mavenProject) {
    List<LifecycleMappingMetadata> lifecycleMappingMetadataSources = new ArrayList<LifecycleMappingMetadata>();

    PluginManagement pluginManagement = mavenProject.getPluginManagement();
    if(pluginManagement == null) {
      return lifecycleMappingMetadataSources;
    }

    Plugin metadataSourcesPlugin = pluginManagement.getPluginsAsMap().get(LifecycleMappingMetadata.PLUGIN_KEY); //$NON-NLS-1$
    if(metadataSourcesPlugin == null) {
      return lifecycleMappingMetadataSources;
    }

    Xpp3Dom configuration = (Xpp3Dom) metadataSourcesPlugin.getConfiguration();
    if(configuration == null) {
      return lifecycleMappingMetadataSources;
    }
    Xpp3Dom sources = configuration.getChild(LifecycleMappingMetadata.ELEMENT_SOURCES);
    if(sources == null) {
      return lifecycleMappingMetadataSources;
    }
    for(Xpp3Dom source : sources.getChildren(LifecycleMappingMetadata.ELEMENT_SOURCE)) {
      String groupId = null;
      Xpp3Dom child = source.getChild(ATTR_GROUPID);
      if(child != null) {
        groupId = child.getValue();
      }
      String artifactId = null;
      child = source.getChild(ATTR_ARTIFACTID);
      if(child != null) {
        artifactId = child.getValue();
      }
      String version = null;
      child = source.getChild(ATTR_VERSION);
      if(child != null) {
        version = child.getValue();
      }
      LifecycleMappingMetadata lifecycleMappingMetadata = LifecycleMappingFactory
          .getLifecycleMappingMetadataFromSource(groupId, artifactId, version,
          mavenProject.getRemoteArtifactRepositories());

      // Does this metadata override any other metadata?
      Iterator<LifecycleMappingMetadata> iter = lifecycleMappingMetadataSources.iterator();
      while(iter.hasNext()) {
        LifecycleMappingMetadata otherLifecycleMappingMetadata = iter.next();
        if(otherLifecycleMappingMetadata.getGroupId().equals(lifecycleMappingMetadata.getGroupId())
            && otherLifecycleMappingMetadata.getArtifactId().equals(lifecycleMappingMetadata.getArtifactId())) {
          iter.remove();
          break;
        }
      }

      lifecycleMappingMetadataSources.add(0, lifecycleMappingMetadata);
    }

    return lifecycleMappingMetadataSources;
  }

  // TODO: cache LifecycleMappingMetadata instances
  private static LifecycleMappingMetadata getLifecycleMappingMetadataFromSource(String groupId, String artifactId,
      String version,
      List<ArtifactRepository> repositories) {
    IMaven maven = MavenPlugin.getDefault().getMaven();
    try {
      Artifact artifact = maven.resolve(groupId, artifactId, version, "xml", LIFECYCLE_MAPPING_METADATA_CLASSIFIER,
          repositories, new NullProgressMonitor());

      File file = artifact.getFile();
      if(file == null || !file.exists() || !file.canRead()) {
        throw new LifecycleMappingConfigurationException("Cannot find file for artifact " + artifact);
      }
      FileInputStream input = null;
      try {
        input = new FileInputStream(file);
        Xpp3Dom dom = Xpp3DomBuilder.build(new XmlStreamReader(input));
        return createLifecycleMappingMetadata(groupId, artifactId, version, dom);
      } catch(IOException e) {
        throw new LifecycleMappingConfigurationException("Cannot read lifecycle mapping metadata for " + artifact, e);
      } catch(XmlPullParserException e) {
        throw new LifecycleMappingConfigurationException("Cannot parse lifecycle mapping metadata for " + artifact,
            e);
      } catch(RuntimeException e) {
        throw new LifecycleMappingConfigurationException("Cannot load lifecycle mapping metadata for " + artifact, e);
      } finally {
        IOUtil.close(input);
      }
    } catch(CoreException ex) {
      throw new LifecycleMappingConfigurationException(ex);
    }
  }

  private static LifecycleMappingMetadata createLifecycleMappingMetadata(String groupId, String artifactId,
      String version, Xpp3Dom configuration) {
    if(configuration == null || !ELEMENT_LIFECYCLE_MAPPING_METADATA.equals(configuration.getName())) {
      throw new LifecycleMappingConfigurationException("Root element must be " + ELEMENT_LIFECYCLE_MAPPING_METADATA);
    }
    
    LifecycleMappingMetadata metadata = new LifecycleMappingMetadata(groupId, artifactId, version);
    Xpp3Dom mojos = configuration.getChild(ELEMENT_MOJOS);
    if(mojos != null) {
      for(Xpp3Dom mojo : mojos.getChildren(ELEMENT_MOJO)) {
        metadata.addPluginExecution(createPluginExecutionMetadata(mojo));
      }
    }
    return metadata;
  }

  private static PluginExecutionMetadata createPluginExecutionMetadata(Xpp3Dom configuration) {
    PluginExecutionFilter filter = createPluginExecutionFilter(configuration);
    Xpp3Dom actionDom = configuration.getChild(ELEMENT_ACTION);
    if(actionDom == null) {
      throw new LifecycleMappingConfigurationException("Element " + ELEMENT_ACTION + " is missing.");
    }
    if(actionDom.getChild(ELEMENT_IGNORE) != null) {
      return new PluginExecutionMetadata(filter, PluginExecutionAction.IGNORE, null /*configuration*/);
    }
    if(actionDom.getChild(ELEMENT_EXECUTE) != null) {
      return new PluginExecutionMetadata(filter, PluginExecutionAction.EXECUTE, actionDom.getChild(ELEMENT_EXECUTE));
    }
    if(actionDom.getChild(ELEMENT_CONFIGURATOR) != null) {
      return new PluginExecutionMetadata(filter, PluginExecutionAction.CONFIGURATOR,
          actionDom.getChild(ELEMENT_CONFIGURATOR));
    }

    throw new LifecycleMappingConfigurationException("An action must be specified");
  }

  private static PluginExecutionFilter createPluginExecutionFilter(Xpp3Dom configuration) {
    String groupId = null;
    Xpp3Dom child = configuration.getChild(ATTR_GROUPID);
    if(child != null) {
      groupId = child.getValue();
    }
    String artifactId = null;
    child = configuration.getChild(ATTR_ARTIFACTID);
    if(child != null) {
      artifactId = child.getValue();
    }
    String versionRange = null;
    child = configuration.getChild(ATTR_VERSIONRANGE);
    if(child != null) {
      versionRange = child.getValue();
    }
    Set<String> goals = new LinkedHashSet<String>();
    child = configuration.getChild(ATTR_GOALS);
    if(child != null) {
      for(Xpp3Dom childGoal : child.getChildren(ATTR_GOAL)) {
        goals.add(childGoal.getValue());
      }
    }
    return new PluginExecutionFilter(groupId, artifactId, versionRange, goals);
  }
}
