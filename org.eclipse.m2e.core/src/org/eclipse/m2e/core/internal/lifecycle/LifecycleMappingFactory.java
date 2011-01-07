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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;

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
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.lifecycle.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecycle.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionAction;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionFilter;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.internal.lifecycle.model.io.xpp3.LifecycleMappingMetadataSourceXpp3Reader;
import org.eclipse.m2e.core.internal.project.IgnoreMojoProjectConfigurator;
import org.eclipse.m2e.core.internal.project.MojoExecutionProjectConfigurator;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractCustomizableLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.AbstractLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.LifecycleMappingConfigurationException;
import org.eclipse.m2e.core.project.configurator.NoopLifecycleMapping;


/**
 * LifecycleMappingFactory
 * 
 * @author igor
 */
public class LifecycleMappingFactory {
  private static Logger log = LoggerFactory.getLogger(LifecycleMappingFactory.class);

  private static final String DEFAULT_LIFECYCLE_METADATA_SOURCE_PATH = "/resources/default-lifecycle-mapping-metadata.xml";

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

  private static ILifecycleMapping getLifecycleMapping(LifecycleMappingMetadataSource lifecycleMappingMetadataSource,
      String packagingType) {
    for(LifecycleMappingMetadata lifecycleMappingMetadata : lifecycleMappingMetadataSource.getLifecycleMappings()) {
      if(packagingType.equals(lifecycleMappingMetadata.getPackagingType())) {
        // Found it
        ILifecycleMapping lifecycleMapping = getLifecycleMapping(lifecycleMappingMetadata.getLifecycleMappingId());
        if(lifecycleMapping == null) {
          String message = NLS.bind(Messages.LifecycleMappingNotAvailable,
              lifecycleMappingMetadata.getLifecycleMappingId());
          throw new LifecycleMappingConfigurationException(message);
        }
        if(lifecycleMapping instanceof AbstractCustomizableLifecycleMapping) {
          AbstractCustomizableLifecycleMapping customizable = (AbstractCustomizableLifecycleMapping) lifecycleMapping;
          for(PluginExecutionMetadata pluginExecutionMetadata : lifecycleMappingMetadata.getPluginExecutions()) {
            customizable.addCustomPluginExecutionMetadata(pluginExecutionMetadata);
          }
        }

        return lifecycleMapping;
      }
    }
    return null;
  }

  public static ILifecycleMapping getLifecycleMapping(IMavenProjectFacade mavenProjectFacade) {
    log.debug("Loading lifecycle mapping for {}.", mavenProjectFacade.toString()); //$NON-NLS-1$

    MavenProject mavenProject = mavenProjectFacade.getMavenProject();
    String packagingType = mavenProjectFacade.getPackaging();
    if("pom".equals(packagingType)) { //$NON-NLS-1$
      log.debug("Using NoopLifecycleMapping lifecycle mapping for {}.", mavenProject.toString()); //$NON-NLS-1$
      return new NoopLifecycleMapping();
    }

    ILifecycleMapping lifecycleMapping = null;
    try {
      // Try to find a lifecycle mapping for this packaging type in metadata sources embedded or referenced in/from pom
      for(LifecycleMappingMetadataSource lifecycleMappingMetadataSource : mavenProjectFacade
          .getLifecycleMappingMetadataSources()) {
        lifecycleMapping = getLifecycleMapping(lifecycleMappingMetadataSource, packagingType);
        if(lifecycleMapping != null) {
          break;
        }
      }

      if(lifecycleMapping == null) {
        // Try to find an eclipse extension that declares a lifecycle mapping for this packaging type
        lifecycleMapping = getLifecycleMappingForPackagingType(packagingType);
      }

      if(lifecycleMapping == null && useDefaultLifecycleMappingMetadataSource) {
        // Try to find a lifecycle mapping for this packaging type in default metadata
        LifecycleMappingMetadataSource lifecycleMappingMetadataSource = getDefaultLifecycleMappingMetadataSource();
        if(lifecycleMappingMetadataSource != null) {
          lifecycleMapping = getLifecycleMapping(lifecycleMappingMetadataSource, packagingType);
        }
      }
    } catch(LifecycleMappingConfigurationException e) {
      MavenPlugin
          .getDefault()
          .getMavenMarkerManager()
          .addMarker(mavenProjectFacade.getPom(), IMavenConstants.MARKER_CONFIGURATION_ID, e.getMessage(),
              1 /*lineNumber*/,
              IMarker.SEVERITY_ERROR);
    }

    if(lifecycleMapping == null) {
      log.debug("Could not load lifecycle mapping for {}.", mavenProject.toString());
    } else {
      log.debug("Using {} lifecycle mapping for {}.", lifecycleMapping.getId(), mavenProject.toString());
    }
    return lifecycleMapping;
  }

  /**
   * Returns default lifecycle mapping for specified packaging type or null if no such lifecycle mapping
   */
  private static ILifecycleMapping getLifecycleMappingForPackagingType(String packagingType) {
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

  public static AbstractProjectConfigurator createProjectConfigurator(PluginExecutionMetadata pluginExecutionMetadata) {
    PluginExecutionAction pluginExecutionAction = pluginExecutionMetadata.getAction();
    if(pluginExecutionAction == PluginExecutionAction.CONFIGURATOR) {
      Xpp3Dom child = pluginExecutionMetadata.getConfiguration().getChild(ATTR_ID);
      if(child == null || child.getValue().trim().length() == 0) {
        throw new LifecycleMappingConfigurationException("A configurator id must be specified");
      }
      String configuratorId = child.getValue();
      AbstractProjectConfigurator projectConfigurator = createProjectConfigurator(configuratorId, true/*bare*/);
      if(projectConfigurator == null) {
        String message = NLS.bind(Messages.ProjectConfiguratorNotAvailable, configuratorId);
        throw new LifecycleMappingConfigurationException(message);
      }
      projectConfigurator.addPluginExecutionFilter(pluginExecutionMetadata.getFilter());
      return projectConfigurator;
    }

    AbstractProjectConfigurator configurator = null;
    if(pluginExecutionAction == PluginExecutionAction.IGNORE) {
      configurator = new IgnoreMojoProjectConfigurator(pluginExecutionMetadata.getFilter());
    } else if(pluginExecutionAction == PluginExecutionAction.EXECUTE) {
      configurator = createMojoExecution(pluginExecutionMetadata);
    } else {
      throw new IllegalStateException("An action must be specified.");
    }

    MavenPlugin plugin = MavenPlugin.getDefault();
    configurator.setProjectManager(plugin.getMavenProjectManager());
    configurator.setMavenConfiguration(plugin.getMavenConfiguration());
    configurator.setMarkerManager(plugin.getMavenMarkerManager());
    configurator.setConsole(plugin.getConsole());
    return configurator;
  }

  private static ILifecycleMapping createLifecycleMapping(IConfigurationElement element) {
    String mappingId = null;
    try {
      ILifecycleMapping mapping = (ILifecycleMapping) element.createExecutableExtension(ATTR_CLASS);
      mappingId = element.getAttribute(ATTR_ID);
      if(mapping instanceof AbstractLifecycleMapping) {
        AbstractLifecycleMapping abstractLifecycleMapping = (AbstractLifecycleMapping) mapping;
        abstractLifecycleMapping.setId(mappingId);
        abstractLifecycleMapping.setName(element.getAttribute(ATTR_NAME));
      }
      if(mapping instanceof AbstractCustomizableLifecycleMapping) {
        AbstractCustomizableLifecycleMapping customizable = (AbstractCustomizableLifecycleMapping) mapping;
        for(IConfigurationElement pluginExecution : element.getChildren(ELEMENT_PLUGIN_EXECUTION)) {
          String pluginExecutionXml = toXml(pluginExecution);
          PluginExecutionMetadata pluginExecutionMetadata = new LifecycleMappingMetadataSourceXpp3Reader()
              .readPluginExecutionMetadata(new StringReader(pluginExecutionXml));
          customizable.addEclipseExtensionPluginExecutionMetadata(pluginExecutionMetadata);
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

  private static AbstractProjectConfigurator createProjectConfigurator(String pluginExecutionXml) throws IOException,
      XmlPullParserException {
    PluginExecutionMetadata pluginExecutionMetadata = new LifecycleMappingMetadataSourceXpp3Reader()
        .readPluginExecutionMetadata(new StringReader(pluginExecutionXml));
    AbstractProjectConfigurator configurator = createProjectConfigurator(pluginExecutionMetadata);
    configurator.addPluginExecutionFilter(pluginExecutionMetadata.getFilter());
    return configurator;
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
      return new LifecycleMappingMetadataSourceXpp3Reader().readPluginExecutionFilter(new StringReader(
          configurationElementXml));
    } catch(IOException e) {
      throw new LifecycleMappingConfigurationException("Cannot read plugin execution filter", e);
    } catch(XmlPullParserException e) {
      throw new LifecycleMappingConfigurationException("Cannot parse plugin execution filter", e);
    }
  }

  private static ILifecycleMapping getLifecycleMapping(String mappingId) {
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

  private static AbstractProjectConfigurator createProjectConfigurator(String configuratorId, boolean bare) {
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
                  for(IConfigurationElement pluginExecutionFilter : element
                      .getChildren(ELEMENT_PLUGIN_EXECUTION_FILTER)) {
                    configurator.addPluginExecutionFilter(createPluginExecutionFilter(pluginExecutionFilter));
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

  public static AbstractProjectConfigurator createProjectConfiguratorFromMetadataSources(
      IMavenProjectFacade mavenProjectFacade, MojoExecution mojoExecution) {
    try {
      // First look in lifecycle metadata sources embedded or referenced in/from pom
      for(LifecycleMappingMetadataSource lifecycleMappingMetadataSource : mavenProjectFacade
          .getLifecycleMappingMetadataSources()) {
        for(PluginExecutionMetadata pluginExecutionMetadata : lifecycleMappingMetadataSource.getPluginExecutions()) {
          if(pluginExecutionMetadata.getFilter().match(mojoExecution)) {
            AbstractProjectConfigurator projectConfigurator = createProjectConfigurator(pluginExecutionMetadata);
            if(projectConfigurator != null) {
              return projectConfigurator;
            }
          }
        }
      }
    } catch(LifecycleMappingConfigurationException e) {
      MavenPlugin
          .getDefault()
          .getMavenMarkerManager()
          .addMarker(mavenProjectFacade.getPom(), IMavenConstants.MARKER_CONFIGURATION_ID, e.getMessage(),
              1 /*lineNumber*/, IMarker.SEVERITY_ERROR);
    }

    return null;
  }

  public static AbstractProjectConfigurator createProjectConfiguratorFor(IMavenProjectFacade mavenProjectFacade,
      MojoExecution mojoExecution) {
    try {
      IExtensionRegistry registry = Platform.getExtensionRegistry();
      IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_PROJECT_CONFIGURATORS);
      if(configuratorsExtensionPoint != null) {
        IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
        for(IExtension extension : configuratorExtensions) {
          IConfigurationElement[] elements = extension.getConfigurationElements();
          for(IConfigurationElement element : elements) {
            if(element.getName().equals(ELEMENT_CONFIGURATOR)) {
              if(isConfiguratorEnabledFor(element, mojoExecution)) {
                try {
                  AbstractProjectConfigurator configurator = (AbstractProjectConfigurator) element
                      .createExecutableExtension(AbstractProjectConfigurator.ATTR_CLASS);

                  MavenPlugin plugin = MavenPlugin.getDefault();
                  configurator.setProjectManager(plugin.getMavenProjectManager());
                  configurator.setMavenConfiguration(plugin.getMavenConfiguration());
                  configurator.setMarkerManager(plugin.getMavenMarkerManager());
                  configurator.setConsole(plugin.getConsole());

                  for(IConfigurationElement pluginExecutionFilter : element
                      .getChildren(ELEMENT_PLUGIN_EXECUTION_FILTER)) {
                    configurator.addPluginExecutionFilter(createPluginExecutionFilter(pluginExecutionFilter));
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

      // Look in default lifecycle metadata
      if(useDefaultLifecycleMappingMetadataSource) {
        LifecycleMappingMetadataSource lifecycleMappingMetadataSource = getDefaultLifecycleMappingMetadataSource();
        if(lifecycleMappingMetadataSource != null) {
          for(PluginExecutionMetadata pluginExecutionMetadata : lifecycleMappingMetadataSource.getPluginExecutions()) {
            if(pluginExecutionMetadata.getFilter().match(mojoExecution)) {
              AbstractProjectConfigurator defaultProjectConfigurator = createProjectConfigurator(pluginExecutionMetadata);
              if(defaultProjectConfigurator != null) {
                return defaultProjectConfigurator;
              }
            }
          }
        }
      }
    } catch(LifecycleMappingConfigurationException e) {
      MavenPlugin
          .getDefault()
          .getMavenMarkerManager()
          .addMarker(mavenProjectFacade.getPom(), IMavenConstants.MARKER_CONFIGURATION_ID, e.getMessage(),
              1 /*lineNumber*/, IMarker.SEVERITY_ERROR);
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

  public static List<LifecycleMappingMetadataSource> getLifecycleMappingMetadataSources(MavenProject mavenProject) {
    List<LifecycleMappingMetadataSource> lifecycleMappingMetadataSources = new ArrayList<LifecycleMappingMetadataSource>();

    PluginManagement pluginManagement = mavenProject.getPluginManagement();
    if(pluginManagement != null) {
      // First look for any lifecycle mapping metadata sources referenced from pom
      Plugin metadataSourcesPlugin = pluginManagement.getPluginsAsMap().get(LifecycleMappingMetadataSource.PLUGIN_KEY);
      if(metadataSourcesPlugin != null) {
        Xpp3Dom configuration = (Xpp3Dom) metadataSourcesPlugin.getConfiguration();
        if(configuration != null) {
          Xpp3Dom sources = configuration.getChild(LifecycleMappingMetadataSource.ELEMENT_SOURCES);
          if(sources != null) {
            for(Xpp3Dom source : sources.getChildren(LifecycleMappingMetadataSource.ELEMENT_SOURCE)) {
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
              LifecycleMappingMetadataSource lifecycleMappingMetadataSource = LifecycleMappingFactory
                  .getLifecycleMappingMetadataSource(groupId, artifactId, version,
                      mavenProject.getRemoteArtifactRepositories());

              // Does this metadata override any other metadata?
              Iterator<LifecycleMappingMetadataSource> iter = lifecycleMappingMetadataSources.iterator();
              while(iter.hasNext()) {
                LifecycleMappingMetadataSource otherLifecycleMappingMetadata = iter.next();
                if(otherLifecycleMappingMetadata.getGroupId().equals(lifecycleMappingMetadataSource.getGroupId())
                    && otherLifecycleMappingMetadata.getArtifactId().equals(
                        lifecycleMappingMetadataSource.getArtifactId())) {
                  iter.remove();
                  break;
                }
              }

              lifecycleMappingMetadataSources.add(0, lifecycleMappingMetadataSource);
            }
          }
        }
      }

      // Look for lifecycle mapping metadata explicitly configured (i.e. embedded) in pom
      Plugin explicitMetadataPlugin = pluginManagement.getPluginsAsMap().get("org.eclipse.m2e:lifecycle-mapping"); //$NON-NLS-1$
      if(explicitMetadataPlugin != null) {
        Xpp3Dom configurationDom = (Xpp3Dom) explicitMetadataPlugin.getConfiguration();
        if(configurationDom != null) {
          Xpp3Dom lifecycleMappingDom = configurationDom.getChild(0);
          if(lifecycleMappingDom != null) {
            try {
              LifecycleMappingMetadataSource lifecycleMappingMetadataSource = new LifecycleMappingMetadataSourceXpp3Reader()
                  .read(new StringReader(lifecycleMappingDom.toString()));
              if(lifecycleMappingMetadataSource != null) {
                lifecycleMappingMetadataSources.add(0, lifecycleMappingMetadataSource);
              }
            } catch(IOException e) {
              throw new LifecycleMappingConfigurationException(
                  "Cannot read lifecycle mapping metadata for maven project " + mavenProject, e);
            } catch(XmlPullParserException e) {
              throw new LifecycleMappingConfigurationException(
                  "Cannot parse lifecycle mapping metadata for maven project " + mavenProject, e);
            } catch(RuntimeException e) {
              throw new LifecycleMappingConfigurationException(
                  "Cannot load lifecycle mapping metadata for maven project " + mavenProject, e);
            }
          }
        }
      }
    }

    return lifecycleMappingMetadataSources;
  }

  private static LifecycleMappingMetadataSource defaultLifecycleMappingMetadataSource;

  public static LifecycleMappingMetadataSource getDefaultLifecycleMappingMetadataSource() {
    if(!useDefaultLifecycleMappingMetadataSource) {
      return null;
    }
    if(defaultLifecycleMappingMetadataSource == null) {
      InputStream is = LifecycleMappingFactory.class.getResourceAsStream(DEFAULT_LIFECYCLE_METADATA_SOURCE_PATH);
      try {
        defaultLifecycleMappingMetadataSource = new LifecycleMappingMetadataSourceXpp3Reader().read(is);
      } catch(IOException e) {
        throw new LifecycleMappingConfigurationException("Cannot read default lifecycle mapping metadata", e);
      } catch(XmlPullParserException e) {
        throw new LifecycleMappingConfigurationException("Cannot parse default lifecycle mapping metadata", e);
      } catch(RuntimeException e) {
        throw new LifecycleMappingConfigurationException("Cannot load default lifecycle mapping metadata", e);
      } finally {
        IOUtil.close(is);
      }
    }
    return defaultLifecycleMappingMetadataSource;
  }

  /** For unit tests only */
  public static void setDefaultLifecycleMappingMetadataSource(
      LifecycleMappingMetadataSource defaultLifecycleMappingMetadataSource) {
    LifecycleMappingFactory.defaultLifecycleMappingMetadataSource = defaultLifecycleMappingMetadataSource;
    useDefaultLifecycleMappingMetadataSource = true;
  }

  private static boolean useDefaultLifecycleMappingMetadataSource = true;

  /** For unit tests only */
  public static void setUseDefaultLifecycleMappingMetadataSource(boolean use) {
    useDefaultLifecycleMappingMetadataSource = use;
    if(!use) {
      defaultLifecycleMappingMetadataSource = null;
    }
  }

  // TODO: cache LifecycleMappingMetadataSource instances
  private static LifecycleMappingMetadataSource getLifecycleMappingMetadataSource(String groupId, String artifactId,
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
        return createLifecycleMappingMetadataSource(groupId, artifactId, version, file);
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

  private static LifecycleMappingMetadataSource createLifecycleMappingMetadataSource(String groupId, String artifactId,
      String version, File configuration) throws IOException, XmlPullParserException {
    InputStream in = new FileInputStream(configuration);
    try {
      LifecycleMappingMetadataSource lifecycleMappingMetadataSource = new LifecycleMappingMetadataSourceXpp3Reader()
          .read(in);
      lifecycleMappingMetadataSource.setGroupId(groupId);
      lifecycleMappingMetadataSource.setArtifactId(artifactId);
      lifecycleMappingMetadataSource.setVersion(version);
      return lifecycleMappingMetadataSource;
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
