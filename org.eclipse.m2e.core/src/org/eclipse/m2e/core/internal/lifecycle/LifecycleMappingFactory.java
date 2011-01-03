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
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;
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
import org.eclipse.m2e.core.internal.lifecycle.model.LifecycleMappingData;
import org.eclipse.m2e.core.internal.lifecycle.model.LifecycleMappingMetadataData;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionData;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionFilter;
import org.eclipse.m2e.core.internal.lifecycle.model.io.xpp3.LifecycleMappingMetadataDataXpp3Reader;
import org.eclipse.m2e.core.internal.project.IgnoreMojoProjectConfiguration;
import org.eclipse.m2e.core.internal.project.MojoExecutionProjectConfigurator;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.CustomizableLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.LifecycleMappingConfigurationException;
import org.eclipse.m2e.core.project.configurator.LifecycleMappingMetadata;
import org.eclipse.m2e.core.project.configurator.PluginExecutionAction;
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

  private static final String ATTR_CLASS = "class"; //$NON-NLS-1$

  private static final String ATTR_PACKAGING_TYPE = "packaging-type"; //$NON-NLS-1$

  private static final String ATTR_ID = "id"; //$NON-NLS-1$

  private static final String ATTR_NAME = "name"; //$NON-NLS-1$

  private static final String ELEMENT_CONFIGURATOR = "configurator"; //$NON-NLS-1$

  private static final String ELEMENT_PLUGIN_EXECUTION = "pluginExecution"; //$NON-NLS-1$

  private static final String ELEMENT_PLUGIN_EXECUTION_FILTER = "pluginExecutionFilter"; //$NON-NLS-1$

  private static final String ELEMENT_RUN_ON_INCREMENTAL = "runOnIncremental";

  private static final String ATTR_GROUPID = "groupId";

  private static final String ATTR_ARTIFACTID = "artifactId";

  private static final String ATTR_VERSION = "version";

  private static final String LIFECYCLE_MAPPING_METADATA_CLASSIFIER = "lifecycle-mapping-metadata";

  /**
   * Returns default lifecycle mapping for specified packaging type or null if no such lifecycle mapping
   */
  public static ILifecycleMapping getLifecycleMapping(IMavenProjectFacade mavenProjectFacade, String packagingType) {
    for(LifecycleMappingMetadata lifecycleMappingMetadata : mavenProjectFacade.getLifecycleMappingMetadataSources()) {
      String lifecycleMappingId = lifecycleMappingMetadata.getLifecycleMappingId(packagingType);
      if (lifecycleMappingId != null) {
        return getLifecycleMapping(lifecycleMappingId);
      }
    }
    return getLifecycleMappingFor(packagingType);
  }

  /**
   * Returns default lifecycle mapping for specified packaging type or null if no such lifecycle mapping
   */
  private static ILifecycleMapping getLifecycleMappingFor(String packagingType) {
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

  private static AbstractProjectConfigurator createProjectConfigurator(PluginExecutionMetadata pluginExecutionMetadata) {
    PluginExecutionAction pluginExecutionAction = pluginExecutionMetadata.getAction();
    if(pluginExecutionAction == PluginExecutionAction.IGNORE) {
      return new IgnoreMojoProjectConfiguration(pluginExecutionMetadata.getFilter());
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
      AbstractProjectConfigurator result = createProjectConfigurator(configuratorId, true/*bare*/);
      result.addPluginExecutionFilter(pluginExecutionMetadata.getFilter());
      return result;
    }
    throw new IllegalStateException("An action must be specified.");
  }

  protected static ILifecycleMapping createLifecycleMapping(IConfigurationElement element) {
    String mappingId = null;
    try {
      ILifecycleMapping mapping = (ILifecycleMapping) element.createExecutableExtension(ATTR_CLASS);
      mappingId = element.getAttribute(ATTR_ID);
      if(mapping instanceof AbstractLifecycleMapping) {
        AbstractLifecycleMapping abstractLifecycleMapping = (AbstractLifecycleMapping) mapping;
        abstractLifecycleMapping.setId(mappingId);
        abstractLifecycleMapping.setName(element.getAttribute(ATTR_NAME));
      }
      if(mapping instanceof CustomizableLifecycleMapping) {
        CustomizableLifecycleMapping customizable = (CustomizableLifecycleMapping) mapping;
        for(IConfigurationElement pluginExecution : element.getChildren(ELEMENT_PLUGIN_EXECUTION)) {
          String pluginExecutionXml = toXml(pluginExecution);
          PluginExecutionData pluginExecutionData = new LifecycleMappingMetadataDataXpp3Reader()
              .readPluginExecutionData(new StringReader(pluginExecutionXml));
          PluginExecutionMetadata pluginExecutionMetadata = createPluginExecutionMetadata(pluginExecutionData);
          AbstractProjectConfigurator configurator = createProjectConfigurator(pluginExecutionMetadata);
          configurator.addPluginExecutionFilter(pluginExecutionMetadata.getFilter());
          customizable.addConfigurator(configurator);
        }
      }
      return mapping;
    } catch(CoreException ex) {
      MavenLogger.log(ex);
    } catch(IOException e) {
      throw new LifecycleMappingConfigurationException("Cannot read lifecycle mapping metadata for " + mappingId, e);
    } catch(XmlPullParserException e) {
      throw new LifecycleMappingConfigurationException("Cannot parse lifecycle mapping metadata for " + mappingId, e);
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

  private static PluginExecutionFilter createPluginExecutionFilter(IConfigurationElement configurationElement) {
    String configurationElementXml = toXml(configurationElement);
    try {
      return new LifecycleMappingMetadataDataXpp3Reader().readPluginExecutionFilter(new StringReader(
          configurationElementXml));
    } catch(IOException e) {
      throw new LifecycleMappingConfigurationException("Cannot read plugin execution filter", e);
    } catch(XmlPullParserException e) {
      throw new LifecycleMappingConfigurationException("Cannot parse plugin execution filter", e);
    }
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
                  for(IConfigurationElement mojo : element.getChildren(ELEMENT_PLUGIN_EXECUTION_FILTER)) {
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
          return createProjectConfigurator(pluginExecutionMetadata);
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

                for(IConfigurationElement mojo : element.getChildren(ELEMENT_PLUGIN_EXECUTION_FILTER)) {
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
    for(IConfigurationElement mojo : configuration.getChildren(ELEMENT_PLUGIN_EXECUTION_FILTER)) {
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
      try {
        return createLifecycleMappingMetadata(groupId, artifactId, version, file);
      } catch(IOException e) {
        throw new LifecycleMappingConfigurationException("Cannot read lifecycle mapping metadata for " + artifact, e);
      } catch(XmlPullParserException e) {
        throw new LifecycleMappingConfigurationException("Cannot parse lifecycle mapping metadata for " + artifact,
            e);
      } catch(RuntimeException e) {
        throw new LifecycleMappingConfigurationException("Cannot load lifecycle mapping metadata for " + artifact, e);
      }
    } catch(CoreException ex) {
      throw new LifecycleMappingConfigurationException(ex);
    }
  }

  private static PluginExecutionMetadata createPluginExecutionMetadata(PluginExecutionData pluginExecutionData) {
    Xpp3Dom actionDom = ((Xpp3Dom) pluginExecutionData.getAction()).getChild(0);
    PluginExecutionAction action = PluginExecutionAction.valueOf(actionDom.getName().toUpperCase());
    return new PluginExecutionMetadata(pluginExecutionData.getFilter(), action, actionDom);
  }

  private static LifecycleMappingMetadata createLifecycleMappingMetadata(String groupId, String artifactId,
      String version, File configuration) throws IOException, XmlPullParserException {
    InputStream in = new FileInputStream(configuration);
    try {
      LifecycleMappingMetadataData lifecycleMappingMetadataData = new LifecycleMappingMetadataDataXpp3Reader().read(in);
      LifecycleMappingMetadata metadata = new LifecycleMappingMetadata(groupId, artifactId, version);

      for(LifecycleMappingData lifecycleMappingData : lifecycleMappingMetadataData.getLifecycleMappings()) {
        metadata.addLifecycleMapping(lifecycleMappingData.getPackagingType(),
            lifecycleMappingData.getLifecycleMappingId());
      }

      for(PluginExecutionData pluginExecutionData : lifecycleMappingMetadataData.getPluginExecutions()) {
        PluginExecutionMetadata pluginExecutionMetadata = createPluginExecutionMetadata(pluginExecutionData);
        metadata.addPluginExecution(pluginExecutionMetadata);
      }

      return metadata;
    } finally {
      IOUtil.close(in);
    }
  }

  private static void toXml(IConfigurationElement configurationElement, StringBuilder output) {
    output.append('<').append(configurationElement.getName());
    for(String attrName : configurationElement.getAttributeNames()) {
      String attrValue = configurationElement.getAttribute(attrName);
      if(attrValue != null) {
        output.append(' ').append(attrName).append("=\"").append(attrValue).append('"');
      }
    }
    output.append('>');
    String configurationElementValue = configurationElement.getValue();
    if(configurationElementValue != null) {
      output.append(configurationElementValue);
    }
    for(IConfigurationElement childElement : configurationElement.getChildren()) {
      toXml(childElement, output);
    }
    output.append("</").append(configurationElement.getName()).append('>');
  }

  private static String toXml(IConfigurationElement configurationElement) {
    if (configurationElement == null) {
      return null;
    }
    
    StringBuilder output = new StringBuilder();
    toXml(configurationElement, output);
    return output.toString();
  }
}
