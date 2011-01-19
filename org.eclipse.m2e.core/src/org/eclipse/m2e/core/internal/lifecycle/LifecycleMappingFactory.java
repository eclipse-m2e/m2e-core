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
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.osgi.util.NLS;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.eclipse.m2e.core.internal.lifecycle.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecycle.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionAction;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.internal.lifecycle.model.io.xpp3.LifecycleMappingMetadataSourceXpp3Reader;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.LifecycleMappingConfigurationException;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.NoopLifecycleMapping;


/**
 * @author igor
 */
public class LifecycleMappingFactory {
  private static Logger log = LoggerFactory.getLogger(LifecycleMappingFactory.class);

  public static final String LIFECYCLE_MAPPING_PLUGIN_GROUPID = "org.eclipse.m2e"; //$NON-NLS-1$

  public static final String LIFECYCLE_MAPPING_PLUGIN_ARTIFACTID = "lifecycle-mapping"; //$NON-NLS-1$

  public static final String LIFECYCLE_MAPPING_PLUGIN_VERSION = "1.0.0"; //$NON-NLS-1$

  private static final String LIFECYCLE_MAPPING_PLUGIN_KEY = LIFECYCLE_MAPPING_PLUGIN_GROUPID
      + ":" + LIFECYCLE_MAPPING_PLUGIN_ARTIFACTID; //$NON-NLS-1$

  private static final String DEFAULT_LIFECYCLE_METADATA_BUNDLE = "org.eclipse.m2e.lifecyclemapping.defaults";

  private static final String LIFECYCLE_MAPPING_METADATA_SOURCE_PATH = "/lifecycle-mapping-metadata.xml";

  private static final String EXTENSION_LIFECYCLE_MAPPINGS = IMavenConstants.PLUGIN_ID + ".lifecycleMappings"; //$NON-NLS-1$

  private static final String EXTENSION_PROJECT_CONFIGURATORS = IMavenConstants.PLUGIN_ID + ".projectConfigurators"; //$NON-NLS-1$

  private static final String EXTENSION_LIFECYCLE_MAPPING_METADATA_SOURCE = IMavenConstants.PLUGIN_ID
      + ".lifecycleMappingMetadataSource"; //$NON-NLS-1$

  private static final String ELEMENT_LIFECYCLE_MAPPING_METADATA = "lifecycleMappingMetadata"; //$NON-NLS-1$

  private static final String ELEMENT_LIFECYCLE_MAPPING = "lifecycleMapping"; //$NON-NLS-1$

  private static final String ELEMENT_SOURCES = "sources"; //$NON-NLS-1$

  private static final String ELEMENT_SOURCE = "source"; //$NON-NLS-1$

  private static final String ATTR_CLASS = "class"; //$NON-NLS-1$

  private static final String ATTR_ID = "id"; //$NON-NLS-1$

  private static final String ATTR_NAME = "name"; //$NON-NLS-1$

  private static final String ELEMENT_CONFIGURATOR = "configurator"; //$NON-NLS-1$

  private static final String ELEMENT_RUN_ON_INCREMENTAL = "runOnIncremental";

  private static final String ATTR_GROUPID = "groupId";

  private static final String ATTR_ARTIFACTID = "artifactId";

  private static final String ATTR_VERSION = "version";

  private static final String ATTR_SECONDARY_TO = "secondaryTo";

  private static final String LIFECYCLE_MAPPING_METADATA_CLASSIFIER = "lifecycle-mapping-metadata";

  public static ILifecycleMapping getLifecycleMapping(MavenExecutionRequest templateRequest,
      IMavenProjectFacade projectFacade, IProgressMonitor monitor) {
    long start = System.currentTimeMillis();
    log.debug("Loading lifecycle mapping for {}.", projectFacade.toString()); //$NON-NLS-1$

    String packagingType = projectFacade.getPackaging();
    if("pom".equals(packagingType)) { //$NON-NLS-1$
      log.debug("Using NoopLifecycleMapping lifecycle mapping for {}.", projectFacade.toString()); //$NON-NLS-1$
      return new NoopLifecycleMapping();
    }

    try {
      return getLifecycleMappingImpl(templateRequest, projectFacade, monitor);
    } catch(CoreException ex) {
      MavenLogger.log(ex);
      InvalidLifecycleMapping lifecycleMapping = new InvalidLifecycleMapping();
      lifecycleMapping.addProblem(0, ex.getMessage()); // XXX that looses most of useful info
      return lifecycleMapping;
    } finally {
      log.debug(
          "Loaded lifecycle mapping in {} ms for {}.", System.currentTimeMillis() - start, projectFacade.toString()); //$NON-NLS-1$
    }
  }

  private static ILifecycleMapping getLifecycleMappingImpl(MavenExecutionRequest templateRequest,
      IMavenProjectFacade projectFacade, IProgressMonitor monitor) throws CoreException {

    MavenProject mavenProject = projectFacade.getMavenProject(monitor);

    InvalidLifecycleMapping invalidLifecycleMapping = new InvalidLifecycleMapping();

    // List order
    // 1. this pom embedded, this pom referenced, parent embedded, parent referenced, grand parent embeded...
    // 2. sources contributed by eclipse extensions
    // 3. default source, if present
    List<MappingMetadataSource> metadataSources = new ArrayList<MappingMetadataSource>();
    try {
      for(LifecycleMappingMetadataSource source : getPomMappingMetadataSources(mavenProject, templateRequest, monitor)) {
        metadataSources.add(new SimpleMappingMetadataSource(source));
      }
    } catch(CoreException e) {
      invalidLifecycleMapping.addProblem(1, e.getMessage()); // XXX that looses most of useful info
    } catch(LifecycleMappingConfigurationException e) {
      invalidLifecycleMapping.addProblem(1, e.getMessage());
    }
    metadataSources.add(new SimpleMappingMetadataSource(getBundleMetadataSources()));
    LifecycleMappingMetadataSource defaultSource = getDefaultLifecycleMappingMetadataSource();
    if(defaultSource != null) {
      metadataSources.add(new SimpleMappingMetadataSource(defaultSource));
    }

    if(invalidLifecycleMapping.hasProblems()) {
      // all bets are off if we can't load metadata sources
      return invalidLifecycleMapping;
    }

    //
    // PHASE 1. Look for lifecycle mapping for packaging type
    //

    LifecycleMappingMetadata lifecycleMappingMetadata = null;
    MappingMetadataSource originalMetadataSource = null;

    for(int i = 0; i < metadataSources.size(); i++ ) {
      MappingMetadataSource source = metadataSources.get(i);
      try {
        lifecycleMappingMetadata = source.getLifecycleMappingMetadata(mavenProject.getPackaging());
        if(lifecycleMappingMetadata != null) {
          originalMetadataSource = new SimpleMappingMetadataSource(lifecycleMappingMetadata);
          metadataSources.add(i, originalMetadataSource);
          break;
        }
      } catch(DuplicateMappingException e) {
        invalidLifecycleMapping.addProblem(1, NLS.bind(Messages.LifecycleDuplicate, mavenProject.getPackaging()));
        break;
      }
    }

    if(lifecycleMappingMetadata == null) {
      log.debug("Could not find lifecycle mapping metadata for {}.", mavenProject.toString());

      invalidLifecycleMapping.addMissingLifecyclePackaging(1,
          NLS.bind(Messages.LifecycleMissing, mavenProject.getPackaging()), mavenProject.getPackaging());
      return invalidLifecycleMapping;
    }

    AbstractLifecycleMapping lifecycleMapping = getLifecycleMapping(lifecycleMappingMetadata.getLifecycleMappingId());
    if(lifecycleMapping == null) {
      invalidLifecycleMapping.addMissingLifecycleExtensionPoint(1,
          NLS.bind(Messages.LifecycleMappingNotAvailable, lifecycleMappingMetadata.getLifecycleMappingId()),
          lifecycleMappingMetadata.getLifecycleMappingId());
      return invalidLifecycleMapping;
    }

    //
    // PHASE 2. Bind project configurators to mojo executions.
    //

    IMaven maven = MavenPlugin.getDefault().getMaven();
    MavenSession session = maven.createSession(DefaultMavenExecutionRequest.copy(templateRequest), mavenProject);
    List<String> goals = Arrays.asList("deploy"); //$NON-NLS-1$
    MavenExecutionPlan executionPlan = maven.calculateExecutionPlan(session, mavenProject, goals, false, monitor);

    Map<MojoExecutionKey, List<PluginExecutionMetadata>> executionMapping = new LinkedHashMap<MojoExecutionKey, List<PluginExecutionMetadata>>();
    List<MojoExecution> mojoExecutions = new ArrayList<MojoExecution>();

    for(MojoExecution execution : executionPlan.getMojoExecutions()) {
      MojoExecutionKey executionKey = new MojoExecutionKey(execution);

      PluginExecutionMetadata primaryMetadata = null;

      // find primary mapping first
      try {
        for(MappingMetadataSource source : metadataSources) {
          for(PluginExecutionMetadata executionMetadata : source.getPluginExecutionMetadata(execution)) {
            if(LifecycleMappingFactory.isPrimaryMapping(executionMetadata)) {
              if(primaryMetadata != null) {
                primaryMetadata = null;
                throw new DuplicateMappingException();
              }
              primaryMetadata = executionMetadata;
            }
          }
          if(primaryMetadata != null) {
            break;
          }
        }
      } catch(DuplicateMappingException e) {
        lifecycleMapping.addProblem(1, NLS.bind(Messages.PluginExecutionMappingDuplicate, executionKey.toString()));
      }

      List<PluginExecutionMetadata> executionMetadatas = new ArrayList<PluginExecutionMetadata>();
      if(primaryMetadata != null) {
        executionMetadatas.add(primaryMetadata);

        // attach any secondary mapping
        for(MappingMetadataSource source : metadataSources) {
          List<PluginExecutionMetadata> metadatas = source.getPluginExecutionMetadata(execution);
          if(metadatas != null) {
            for(PluginExecutionMetadata metadata : metadatas) {
              if(LifecycleMappingFactory.isSecondaryMapping(metadata, primaryMetadata)) {
                executionMetadatas.add(metadata);
              }
            }
          }
        }

        executionMapping.put(executionKey, executionMetadatas);
      }
      if(!executionMetadatas.isEmpty() && executionMetadatas.get(0).getAction() != PluginExecutionAction.ignore) {
        mojoExecutions.add(maven.setupMojoExecution(session, mavenProject, execution));
      } else {
        // do not setup non-interesting/ignore mojo executions
        mojoExecutions.add(execution);
      }
    }

    lifecycleMapping.setMavenProjectFacade(projectFacade);
    lifecycleMapping.initializeMapping(mojoExecutions, executionMapping);

    return lifecycleMapping;
  }

  /**
   * Returns lifecycle mapping metadata sources embedded or referenced by pom.xml in the following order
   * <ol>
   * <li>this pom.xml embedded</li>
   * <li>this pom.xml referenced</li>
   * <li>parent pom.xml embedded</li>
   * <li>parent pom.xml referenced</li>
   * <li>grand parent embedded</li>
   * <li>and so on</li>
   * </ol>
   * Returns empty list if no metadata sources are embedded/referenced by pom.xml
   * 
   * @throws CoreException if metadata sources cannot be resolved or read
   */
  public static List<LifecycleMappingMetadataSource> getPomMappingMetadataSources(MavenProject mavenProject,
      MavenExecutionRequest templateRequest, IProgressMonitor monitor) throws CoreException {
    IMaven maven = MavenPlugin.getDefault().getMaven();

    ArrayList<LifecycleMappingMetadataSource> sources = new ArrayList<LifecycleMappingMetadataSource>();

    HashSet<String> referenced = new LinkedHashSet<String>();

    MavenProject project = mavenProject;
    do {
      if(monitor.isCanceled()) {
        break;
      }

      LifecycleMappingMetadataSource embeddedSource = getEmbeddedMetadataSource(project);
      if(embeddedSource != null) {
        sources.add(embeddedSource);
      }

      for(LifecycleMappingMetadataSource referencedSource : getReferencedMetadataSources(referenced, project, monitor)) {
        sources.add(referencedSource);
      }

      MavenExecutionRequest request = DefaultMavenExecutionRequest.copy(templateRequest); // TODO ain't nice
      project = maven.resolveParentProject(request, project, monitor);
    } while(project != null);

    return sources;
  }

  public static AbstractProjectConfigurator createProjectConfigurator(PluginExecutionMetadata pluginExecutionMetadata) {
    PluginExecutionAction pluginExecutionAction = pluginExecutionMetadata.getAction();
    if(pluginExecutionAction != PluginExecutionAction.configurator) {
      throw new IllegalArgumentException();
    }
    String configuratorId = getProjectConfiguratorId(pluginExecutionMetadata);
    AbstractProjectConfigurator projectConfigurator = createProjectConfigurator(configuratorId);
    if(projectConfigurator == null) {
      String message = NLS.bind(Messages.ProjectConfiguratorNotAvailable, configuratorId);
      throw new LifecycleMappingConfigurationException(message);
    }
    projectConfigurator.addPluginExecutionFilter(pluginExecutionMetadata.getFilter());
    return projectConfigurator;
  }

  public static String getProjectConfiguratorId(PluginExecutionMetadata pluginExecutionMetadata) {
    Xpp3Dom child = pluginExecutionMetadata.getConfiguration().getChild(ATTR_ID);
    if(child == null || child.getValue().trim().length() == 0) {
      throw new LifecycleMappingConfigurationException("A configurator id must be specified");
    }
    return child.getValue();
  }

  public static LifecycleMappingMetadataSource createLifecycleMappingMetadataSource(URL url) {
    try {
      return new LifecycleMappingMetadataSourceXpp3Reader().read(url.openStream());
    } catch(XmlPullParserException e) {
      throw new LifecycleMappingConfigurationException("Cannot parse lifecycle mapping metadata", e);
    } catch(IOException e) {
      throw new LifecycleMappingConfigurationException("Cannot parse lifecycle mapping metadata", e);
    }
  }

  private static AbstractLifecycleMapping createLifecycleMapping(IConfigurationElement element) {
    String mappingId = null;
    try {
      AbstractLifecycleMapping mapping = (AbstractLifecycleMapping) element.createExecutableExtension(ATTR_CLASS);
      mappingId = element.getAttribute(ATTR_ID);
      mapping.setId(mappingId);
      mapping.setName(element.getAttribute(ATTR_NAME));
      return mapping;
    } catch(CoreException ex) {
      MavenLogger.log(ex);
    }
    return null;
  }

  public static MojoExecutionBuildParticipant createMojoExecutionBuildParicipant(MojoExecution mojoExecution,
      PluginExecutionMetadata pluginExecutionMetadata) {
    boolean runOnIncremental = true;
    Xpp3Dom child = pluginExecutionMetadata.getConfiguration().getChild(ELEMENT_RUN_ON_INCREMENTAL);
    if(child != null) {
      runOnIncremental = Boolean.parseBoolean(child.getValue());
    }
    return new MojoExecutionBuildParticipant(mojoExecution, runOnIncremental);
  }

  private static AbstractLifecycleMapping getLifecycleMapping(String mappingId) {
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

  private static AbstractProjectConfigurator createProjectConfigurator(String configuratorId) {
    IConfigurationElement element = getProjectConfiguratorExtension(configuratorId);
    if(element != null) {
      try {
        AbstractProjectConfigurator configurator = (AbstractProjectConfigurator) element
            .createExecutableExtension(AbstractProjectConfigurator.ATTR_CLASS);

        MavenPlugin plugin = MavenPlugin.getDefault();
        configurator.setProjectManager(plugin.getMavenProjectManager());
        configurator.setMavenConfiguration(plugin.getMavenConfiguration());
        configurator.setMarkerManager(plugin.getMavenMarkerManager());
        configurator.setConsole(plugin.getConsole());

        return configurator;
      } catch(CoreException ex) {
        MavenLogger.log(ex);
      }
    }
    return null;
  }

  private static IConfigurationElement getProjectConfiguratorExtension(String configuratorId) {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_PROJECT_CONFIGURATORS);
    if(configuratorsExtensionPoint != null) {
      IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
      for(IExtension extension : configuratorExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_CONFIGURATOR)) {
            if(configuratorId.equals(element.getAttribute(AbstractProjectConfigurator.ATTR_ID))) {
              return element;
            }
          }
        }
      }
    }
    return null;
  }

  private static void checkCompatibleVersion(Plugin metadataPlugin) {
    ComparableVersion version = new ComparableVersion(metadataPlugin.getVersion());
    if(!version.equals(new ComparableVersion(LIFECYCLE_MAPPING_PLUGIN_VERSION))) {
      throw new LifecycleMappingConfigurationException(NLS.bind(Messages.LifecycleMappingPluginVersionIncompatible,
          metadataPlugin.getVersion()));
    }
  }

  private static LifecycleMappingMetadataSource getEmbeddedMetadataSource(MavenProject mavenProject)
      throws CoreException {
    // TODO this does not merge configuration from profiles 
    PluginManagement pluginManagement = getPluginManagement(mavenProject);
    Plugin metadataPlugin = pluginManagement.getPluginsAsMap().get(LIFECYCLE_MAPPING_PLUGIN_KEY);
    if(metadataPlugin != null) {
      checkCompatibleVersion(metadataPlugin);
      Xpp3Dom configurationDom = (Xpp3Dom) metadataPlugin.getConfiguration();
      if(configurationDom != null) {
        Xpp3Dom lifecycleMappingDom = configurationDom.getChild(ELEMENT_LIFECYCLE_MAPPING_METADATA);
        if(lifecycleMappingDom != null) {
          try {
            return new LifecycleMappingMetadataSourceXpp3Reader()
                .read(new StringReader(lifecycleMappingDom.toString()));
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
    return null;
  }

  /**
   * Returns metadata sources referenced by this project in the order they are specified in pom.xml. Returns empty list
   * if no metadata sources are referenced in pom.xml.
   */
  private static List<LifecycleMappingMetadataSource> getReferencedMetadataSources(Set<String> referenced,
      MavenProject mavenProject, IProgressMonitor monitor) throws CoreException {
    List<LifecycleMappingMetadataSource> metadataSources = new ArrayList<LifecycleMappingMetadataSource>();

    PluginManagement pluginManagement = getPluginManagement(mavenProject);
    for(Plugin plugin : pluginManagement.getPlugins()) {
      if(!LIFECYCLE_MAPPING_PLUGIN_KEY.equals(plugin.getKey())) {
        continue;
      }
      Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
      if(configuration != null) {
        checkCompatibleVersion(plugin);
        Xpp3Dom sources = configuration.getChild(ELEMENT_SOURCES);
        if(sources != null) {
          for(Xpp3Dom source : sources.getChildren(ELEMENT_SOURCE)) {
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
            if(referenced.add(groupId + ":" + artifactId)) {
              LifecycleMappingMetadataSource lifecycleMappingMetadataSource = LifecycleMappingFactory
                  .getLifecycleMappingMetadataSource(groupId, artifactId, version,
                      mavenProject.getRemoteArtifactRepositories(), monitor);

              metadataSources.add(lifecycleMappingMetadataSource);
            }
          }
        }
      }
    }

    return metadataSources;
  }

  private static PluginManagement getPluginManagement(MavenProject mavenProject) throws CoreException {
    Model model = new Model();

    Build build = new Build();
    model.setBuild(build);

    PluginManagement result = new PluginManagement();
    build.setPluginManagement(result);

    if(mavenProject == null) {
      return null;
    }

    addBuild(result, mavenProject.getOriginalModel().getBuild());

    for(Profile profile : mavenProject.getActiveProfiles()) {
      addBuild(result, profile.getBuild());
    }

    MavenImpl maven = (MavenImpl) MavenPlugin.getDefault().getMaven();
    maven.interpolateModel(mavenProject, model);

    return result;
  }

  private static void addBuild(PluginManagement result, BuildBase build) {
    if(build != null) {
      PluginManagement pluginManagement = build.getPluginManagement();
      if(pluginManagement != null) {
        List<Plugin> _plugins = pluginManagement.getPlugins();
        for(Plugin plugin : _plugins) {
          result.addPlugin(plugin.clone());
        }
      }
    }
  }

  private static LifecycleMappingMetadataSource defaultLifecycleMappingMetadataSource;

  public static LifecycleMappingMetadataSource getDefaultLifecycleMappingMetadataSource() {
    if(!useDefaultLifecycleMappingMetadataSource) {
      return null;
    }
    if(defaultLifecycleMappingMetadataSource == null) {
      Bundle bundle = Platform.getBundle(DEFAULT_LIFECYCLE_METADATA_BUNDLE);
      defaultLifecycleMappingMetadataSource = getMetadataSource(bundle);
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
      String version, List<ArtifactRepository> repositories, IProgressMonitor monitor) {
    IMaven maven = MavenPlugin.getDefault().getMaven();
    try {
      // TODO this does not resolve workspace artifacts
      Artifact artifact = maven.resolve(groupId, artifactId, version, "xml", LIFECYCLE_MAPPING_METADATA_CLASSIFIER,
          repositories, monitor);

      File file = artifact.getFile();
      if(file == null || !file.exists() || !file.canRead()) {
        throw new LifecycleMappingConfigurationException("Cannot find file for artifact " + artifact);
      }
      try {
        return createLifecycleMappingMetadataSource(groupId, artifactId, version, file);
      } catch(IOException e) {
        throw new LifecycleMappingConfigurationException("Cannot read lifecycle mapping metadata for " + artifact, e);
      } catch(XmlPullParserException e) {
        throw new LifecycleMappingConfigurationException("Cannot parse lifecycle mapping metadata for " + artifact, e);
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

  /**
   * Returns lifecycle mapping metadata sources provided by all installed bundles
   */
  public static List<LifecycleMappingMetadataSource> getBundleMetadataSources() {
    // XXX cache!
    ArrayList<LifecycleMappingMetadataSource> sources = new ArrayList<LifecycleMappingMetadataSource>();

    BundleContext ctx = MavenPlugin.getDefault().getBundleContext();
    Map<String, Bundle> bundles = new HashMap<String, Bundle>();
    for(Bundle bundle : ctx.getBundles()) {
      bundles.put(bundle.getSymbolicName(), bundle);
    }

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint configuratorsExtensionPoint = registry
        .getExtensionPoint(EXTENSION_LIFECYCLE_MAPPING_METADATA_SOURCE);
    if(configuratorsExtensionPoint != null) {
      IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
      for(IExtension extension : configuratorExtensions) {
        RegistryContributor contributor = (RegistryContributor) extension.getContributor();
        Bundle bundle = bundles.get(contributor.getActualName());
        LifecycleMappingMetadataSource source = getMetadataSource(bundle);
        if(source != null) {
          sources.add(source);
        }
      }
    }

    return sources;
  }

  private static LifecycleMappingMetadataSource getMetadataSource(Bundle bundle) {
    if(bundle == null) {
      return null;
    }
    URL url = bundle.getEntry(LIFECYCLE_MAPPING_METADATA_SOURCE_PATH);
    if(url != null) {
      try {
        InputStream in = url.openStream();
        try {
          return new LifecycleMappingMetadataSourceXpp3Reader().read(in);
        } finally {
          IOUtil.close(in);
        }
      } catch(IOException e) {
        log.warn("Could not read lifecycle-mapping-metadata.xml for bundle {}", bundle.getSymbolicName(), e);
      } catch(XmlPullParserException e) {
        log.warn("Could not read lifecycle-mapping-metadata.xml for bundle {}", bundle.getSymbolicName(), e);
      }
    }
    return null;
  }

  public static boolean isPrimaryMapping(PluginExecutionMetadata executionMetadata) {
    if(executionMetadata == null) {
      return false;
    }
    if(executionMetadata.getAction() == PluginExecutionAction.configurator) {
      String configuratorId = getProjectConfiguratorId(executionMetadata);
      IConfigurationElement element = getProjectConfiguratorExtension(configuratorId);
      if(element != null) {
        return element.getAttribute(ATTR_SECONDARY_TO) == null;
      }
    }
    return true;
  }

  public static boolean isSecondaryMapping(PluginExecutionMetadata metadata, PluginExecutionMetadata primaryMetadata) {
    if(metadata == null || primaryMetadata == null) {
      return false;
    }
    if(PluginExecutionAction.configurator != metadata.getAction()
        || PluginExecutionAction.configurator != primaryMetadata.getAction()) {
      return false;
    }
    if(!isPrimaryMapping(primaryMetadata)) {
      return false;
    }
    String primaryId = getProjectConfiguratorId(primaryMetadata);
    String secondaryId = getProjectConfiguratorId(metadata);
    if(primaryId == null || secondaryId == null) {
      return false;
    }
    if(secondaryId.equals(primaryId)) {
      return false;
    }
    IConfigurationElement extension = getProjectConfiguratorExtension(secondaryId);
    if(extension == null) {
      return false;
    }
    return primaryId.equals(extension.getAttribute(ATTR_SECONDARY_TO));
  }
}
