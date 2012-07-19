/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Andrew Eisenberg - Work on Bug 350414
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecyclemapping;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IMarker;
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
import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionFilter;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.io.xpp3.LifecycleMappingMetadataSourceXpp3Reader;
import org.eclipse.m2e.core.internal.markers.MavenProblemInfo;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.internal.markers.SourceLocationHelper;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.ILifecycleMappingConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


/**
 * @author igor
 */
public class LifecycleMappingFactory {

  private static final Logger log = LoggerFactory.getLogger(LifecycleMappingFactory.class);

  public static final String LIFECYCLE_MAPPING_PLUGIN_GROUPID = "org.eclipse.m2e"; //$NON-NLS-1$

  public static final String LIFECYCLE_MAPPING_PLUGIN_ARTIFACTID = "lifecycle-mapping"; //$NON-NLS-1$

  public static final String LIFECYCLE_MAPPING_PLUGIN_VERSION = "1.0.0"; //$NON-NLS-1$

  private static final String LIFECYCLE_MAPPING_PLUGIN_KEY = LIFECYCLE_MAPPING_PLUGIN_GROUPID
      + ":" + LIFECYCLE_MAPPING_PLUGIN_ARTIFACTID; //$NON-NLS-1$

  private static final String DEFAULT_LIFECYCLE_METADATA_BUNDLE = "org.eclipse.m2e.lifecyclemapping.defaults";

  public static final String LIFECYCLE_MAPPING_METADATA_SOURCE_NAME = "lifecycle-mapping-metadata.xml"; //$NON-NLS-1$

  private static final String LIFECYCLE_MAPPING_METADATA_SOURCE_PATH = "/" + LIFECYCLE_MAPPING_METADATA_SOURCE_NAME; //$NON-NLS-1$

  private static final String LIFECYCLE_MAPPING_METADATA_EMBEDDED_SOURCE_PATH = "META-INF/m2e/" + LIFECYCLE_MAPPING_METADATA_SOURCE_NAME; //$NON-NLS-1$

  public static final String EXTENSION_LIFECYCLE_MAPPINGS = IMavenConstants.PLUGIN_ID + ".lifecycleMappings"; //$NON-NLS-1$

  public static final String EXTENSION_PROJECT_CONFIGURATORS = IMavenConstants.PLUGIN_ID + ".projectConfigurators"; //$NON-NLS-1$

  public static final String EXTENSION_LIFECYCLE_MAPPING_METADATA_SOURCE = IMavenConstants.PLUGIN_ID
      + ".lifecycleMappingMetadataSource"; //$NON-NLS-1$

  private static final String ELEMENT_LIFECYCLE_MAPPING_METADATA = "lifecycleMappingMetadata"; //$NON-NLS-1$

  private static final String ELEMENT_LIFECYCLE_MAPPING = "lifecycleMapping"; //$NON-NLS-1$

  private static final String ELEMENT_SOURCES = "sources"; //$NON-NLS-1$

  private static final String ELEMENT_SOURCE = "source"; //$NON-NLS-1$

  private static final String ATTR_CLASS = "class"; //$NON-NLS-1$

  private static final String ATTR_ID = "id"; //$NON-NLS-1$

  private static final String ATTR_NAME = "name"; //$NON-NLS-1$

  private static final String ELEMENT_CONFIGURATOR = "configurator"; //$NON-NLS-1$

  private static final String ELEMENT_MESSAGE = "message"; //$NON-NLS-1$

  private static final String ELEMENT_RUN_ON_INCREMENTAL = "runOnIncremental";

  private static final String ELEMENT_RUN_ON_CONFIGURATION = "runOnConfiguration";

  private static final String ATTR_GROUPID = "groupId";

  private static final String ATTR_ARTIFACTID = "artifactId";

  private static final String ATTR_VERSION = "version";

  private static final String ATTR_SECONDARY_TO = "secondaryTo";

  private static final String LIFECYCLE_MAPPING_METADATA_CLASSIFIER = "lifecycle-mapping-metadata";

  private static List<LifecycleMappingMetadataSource> bundleMetadataSources = null;

  public static LifecycleMappingResult calculateLifecycleMapping(MavenExecutionRequest templateRequest,
      MavenProjectFacade projectFacade, IProgressMonitor monitor) {
    long start = System.currentTimeMillis();
    log.debug("Loading lifecycle mapping for {}.", projectFacade.toString()); //$NON-NLS-1$

    LifecycleMappingResult result = new LifecycleMappingResult();

    try {
      MavenProject mavenProject = projectFacade.getMavenProject(monitor);

      calculateEffectiveLifecycleMappingMetadata(result, templateRequest, mavenProject,
          projectFacade.getMojoExecutions(), monitor);

      instantiateLifecycleMapping(result, mavenProject, result.getLifecycleMappingId());

      if(result.getLifecycleMapping() != null) {
        instantiateProjectConfigurators(mavenProject, result, result.getMojoExecutionMapping());
      }
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
      result.addProblem(new MavenProblemInfo(1, ex)); // XXX that looses most of useful info
    } finally {
      log.info("Using {} lifecycle mapping for {}.", result.getLifecycleMappingId(), projectFacade.toString()); //$NON-NLS-1$
      log.debug(
          "Loaded lifecycle mapping in {} ms for {}.", System.currentTimeMillis() - start, projectFacade.toString()); //$NON-NLS-1$
    }
    return result;
  }

  public static void calculateEffectiveLifecycleMappingMetadata(LifecycleMappingResult result,
      MavenExecutionRequest templateRequest, MavenProject mavenProject, List<MojoExecution> mojoExecutions,
      IProgressMonitor monitor) throws CoreException {

    String packagingType = mavenProject.getPackaging();
    if("pom".equals(packagingType)) { //$NON-NLS-1$
      log.debug("Using NoopLifecycleMapping lifecycle mapping for {}.", mavenProject.toString()); //$NON-NLS-1$

      LifecycleMappingMetadata lifecycleMappingMetadata = new LifecycleMappingMetadata();
      lifecycleMappingMetadata.setLifecycleMappingId("NULL"); // TODO proper constant

      result.setLifecycleMappingMetadata(lifecycleMappingMetadata);

      Map<MojoExecutionKey, List<IPluginExecutionMetadata>> executionMapping = new LinkedHashMap<MojoExecutionKey, List<IPluginExecutionMetadata>>();
      result.setMojoExecutionMapping(executionMapping);

      return;
    }

    List<MappingMetadataSource> metadataSources;
    try {
      metadataSources = getProjectMetadataSources(templateRequest, mavenProject, getBundleMetadataSources(),
          mojoExecutions, true, monitor);
    } catch(LifecycleMappingConfigurationException e) {
      // could not read/parse/interpret mapping metadata configured in the pom or inherited from parent pom.
      // record the problem and return
      result.addProblem(new MavenProblemInfo(mavenProject, e));
      return;
    }

    calculateEffectiveLifecycleMappingMetadata(result, templateRequest, metadataSources, mavenProject, mojoExecutions,
        true);
  }

  public static List<MappingMetadataSource> getProjectMetadataSources(MavenExecutionRequest templateRequest,
      MavenProject mavenProject, List<LifecycleMappingMetadataSource> bundleMetadataSources,
      List<MojoExecution> mojoExecutions, boolean includeDefault, IProgressMonitor monitor) throws CoreException,
      LifecycleMappingConfigurationException {
    List<MappingMetadataSource> metadataSources = new ArrayList<MappingMetadataSource>();

    // List order
    // 1. this pom embedded, this pom referenced, parent embedded, parent referenced, grand parent embedded...
    // 2. preferences in project  (*** not implemented yet)
    // 3. preferebces in ancestor project  (*** not implemented yet)
    // 4. preferences in workspace 
    // 5. sources contributed by eclipse extensions
    // 6. maven-plugin embedded metadata
    // 7. default source, if present
    // TODO validate metadata and replace invalid entries with error mapping
    for(LifecycleMappingMetadataSource source : getPomMappingMetadataSources(mavenProject, templateRequest, monitor)) {
      metadataSources.add(new SimpleMappingMetadataSource(source));
    }
    metadataSources.add(new SimpleMappingMetadataSource(getWorkspacePreferencesMetadataSources()));
    
    // TODO filter out invalid metadata from sources contributed by eclipse extensions and the default source 
    metadataSources.add(new SimpleMappingMetadataSource(bundleMetadataSources));
    for(LifecycleMappingMetadataSource source : getMavenPluginEmbeddedMetadataSources(mojoExecutions,
        mavenProject.getPluginArtifactRepositories(), monitor)) {
      metadataSources.add(new SimpleMappingMetadataSource(source));
    }
    if(includeDefault) {
      LifecycleMappingMetadataSource defaultSource = getDefaultLifecycleMappingMetadataSource();
      if(defaultSource != null) {
        metadataSources.add(new SimpleMappingMetadataSource(defaultSource));
      }
    }

    return metadataSources;
  }

  private static List<LifecycleMappingMetadataSource> getMavenPluginEmbeddedMetadataSources(
      List<MojoExecution> mojoExecutions, List<ArtifactRepository> remoteRepositories, IProgressMonitor monitor) {
    Map<File, LifecycleMappingMetadataSource> result = new LinkedHashMap<File, LifecycleMappingMetadataSource>();

    if(mojoExecutions == null || mojoExecutions.isEmpty()) {
      // TODO need to understand under what conditions execution plan is null here
      return Collections.emptyList();
    }

    MavenImpl maven = (MavenImpl) MavenPlugin.getMaven();

    for(MojoExecution execution : mojoExecutions) {
      Artifact artifact;
      try {
        artifact = maven.resolvePluginArtifact(execution.getPlugin(), remoteRepositories, monitor);
      } catch(CoreException e) {
        // skip this plugin, it won't run anyways
        continue;
      }

      File file = artifact.getFile();
      if(file == null || result.containsKey(file) || !file.canRead()) {
        continue;
      }
      LifecycleMappingMetadataSource metadata = readMavenPluginEmbeddedMetadata(artifact);
      if(metadata != null) {
        // enforce embedded metadata only contains mappings for this plugin and nothing else
        for(LifecycleMappingMetadata lifecycleMetadta : metadata.getLifecycleMappings()) {
          enforcePluginMapping(artifact, lifecycleMetadta.getPluginExecutions());
        }
        enforcePluginMapping(artifact, metadata.getPluginExecutions());

        result.put(file, metadata);
      }
    }

    return new ArrayList<LifecycleMappingMetadataSource>(result.values());
  }

  private static void enforcePluginMapping(Artifact artifact, List<PluginExecutionMetadata> executions) {
    if(executions == null) {
      return;
    }
    ListIterator<PluginExecutionMetadata> iter = executions.listIterator();
    while(iter.hasNext()) {
      PluginExecutionMetadata execution = iter.next();
      PluginExecutionFilter filter = execution.getFilter();
      if(!isNullOrEqual(artifact.getGroupId(), filter.getGroupId())
          || !isNullOrEqual(artifact.getArtifactId(), filter.getArtifactId())
          || !isNullOrEqual(artifact.getBaseVersion(), filter.getVersionRange())) {
        String mappingGAV = filter.getGroupId() + ":" + filter.getArtifactId() + ":" + filter.getVersionRange();
        String pluginGAV = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getBaseVersion();
        log.warn(
            "Ignoring plugin execution mapping {} defined in maven plugin {} because it matches other plugins and/or plugin versions",
            mappingGAV, pluginGAV);
        iter.remove();
      } else {
        // filter may have empty GAV elements
        filter.setGroupId(artifact.getGroupId());
        filter.setArtifactId(artifact.getArtifactId());
        filter.setVersionRange(artifact.getBaseVersion());
      }
    }
  }

  private static boolean isNullOrEqual(String expected, String actual) {
    return actual == null || actual.equals(expected);
  }

  private static LifecycleMappingMetadataSource readMavenPluginEmbeddedMetadata(Artifact artifact) {
    File file = artifact.getFile();
    try {
      if(file.isFile()) {
        JarFile jar = new JarFile(file);
        try {
          ZipEntry entry = jar.getEntry(LIFECYCLE_MAPPING_METADATA_EMBEDDED_SOURCE_PATH);
          if(entry == null) {
            return null;
          }
          InputStream is = jar.getInputStream(entry);
          return readMavenPluginEmbeddedMetadata(artifact, is);
        } finally {
          try {
            jar.close();
          } catch(IOException e) {
            // too bad
          }
        }
      } else if(file.isDirectory()) {
        try {
          InputStream is = new BufferedInputStream(new FileInputStream(new File(file,
              LIFECYCLE_MAPPING_METADATA_EMBEDDED_SOURCE_PATH)));
          try {
            return readMavenPluginEmbeddedMetadata(artifact, is);
          } finally {
            IOUtil.close(is);
          }
        } catch(FileNotFoundException e) {
          // expected and tolerated
        }
      }
    } catch(XmlPullParserException e) {
      throw new LifecycleMappingConfigurationException("Cannot read lifecycle mapping metadata for artifact "
          + artifact, e);
    } catch(IOException e) {
      throw new LifecycleMappingConfigurationException("Cannot read lifecycle mapping metadata for artifact "
          + artifact, e);
    }
    return null;
  }

  private static LifecycleMappingMetadataSource readMavenPluginEmbeddedMetadata(Artifact artifact, InputStream is)
      throws IOException, XmlPullParserException {
    return new LifecycleMappingMetadataSourceXpp3Reader().read(is);
  }

  public static LifecycleMappingMetadataSource getWorkspacePreferencesMetadataSources() {
    LifecycleMappingMetadataSource source = null;
    String mappings = MavenPluginActivator.getDefault().getMavenConfiguration().getWorkspaceMappings();
    if (mappings != null) {
      LifecycleMappingMetadataSourceXpp3Reader reader = new LifecycleMappingMetadataSourceXpp3Reader();
      try {
        source = reader.read(new StringReader(mappings));
      } catch(IOException ex) {
        log.error(ex.getMessage(), ex);
      } catch(XmlPullParserException ex) {
        log.error(ex.getMessage(), ex);
      }
    }
    if (source == null) {
      source = new LifecycleMappingMetadataSource();
    }
    return source;
  }

  public static void calculateEffectiveLifecycleMappingMetadata(LifecycleMappingResult result,
      MavenExecutionRequest templateRequest, List<MappingMetadataSource> metadataSources, MavenProject mavenProject,
      List<MojoExecution> mojoExecutions, boolean applyDefaultStrategy) {

    IMaven maven = MavenPlugin.getMaven();
    MavenSession session = maven.createSession(newMavenExecutionRequest(templateRequest), mavenProject);

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
        log.error("Duplicate lifecycle mapping metadata for {}.", mavenProject.toString());
        result.addProblem(new MavenProblemInfo(1, NLS.bind(Messages.LifecycleDuplicate, mavenProject.getPackaging())));
        return; // fatal error
      }
    }

    if(lifecycleMappingMetadata == null && applyDefaultStrategy) {
      lifecycleMappingMetadata = new LifecycleMappingMetadata();
      lifecycleMappingMetadata.setLifecycleMappingId("DEFAULT"); // TODO proper constant
      lifecycleMappingMetadata.setPackagingType(mavenProject.getPackaging());
    }

    // TODO if lifecycleMappingMetadata.lifecycleMappingId==null, convert to error lifecycle mapping metadata

    result.setLifecycleMappingMetadata(lifecycleMappingMetadata);

    //
    // PHASE 2. Bind project configurators to mojo executions.
    //

    Map<MojoExecutionKey, List<IPluginExecutionMetadata>> executionMapping = new LinkedHashMap<MojoExecutionKey, List<IPluginExecutionMetadata>>();

    if(mojoExecutions != null) {
      for(MojoExecution execution : mojoExecutions) {
        MojoExecutionKey executionKey = new MojoExecutionKey(execution);

        PluginExecutionMetadata primaryMetadata = null;

        // find primary mapping first
        try {
          for(MappingMetadataSource source : metadataSources) {
            try {
              List<PluginExecutionMetadata> metadatas = applyParametersFilter(session,
                  source.getPluginExecutionMetadata(executionKey), mavenProject, execution);
              for(PluginExecutionMetadata executionMetadata : metadatas) {
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
            } catch(CoreException e) {
              SourceLocation location = SourceLocationHelper.findLocation(mavenProject, executionKey);
              result.addProblem(new MavenProblemInfo(location, e));
            }
          }
        } catch(DuplicateMappingException e) {
          log.debug("Duplicate plugin execution mapping metadata for {}.", executionKey.toString());
          result.addProblem(new MavenProblemInfo(1, NLS.bind(Messages.PluginExecutionMappingDuplicate,
              executionKey.toString())));
        }

        if(primaryMetadata != null && !isValidPluginExecutionMetadata(primaryMetadata)) {
          log.debug("Invalid plugin execution mapping metadata for {}.", executionKey.toString());
          result.addProblem(new MavenProblemInfo(1, NLS.bind(Messages.PluginExecutionMappingInvalid,
              executionKey.toString())));
          primaryMetadata = null;
        }

        List<IPluginExecutionMetadata> executionMetadatas = new ArrayList<IPluginExecutionMetadata>();
        if(primaryMetadata != null) {
          executionMetadatas.add(primaryMetadata);

          if(primaryMetadata.getAction() == PluginExecutionAction.configurator) {
            // attach any secondary mapping
            for(MappingMetadataSource source : metadataSources) {
              try {
                List<PluginExecutionMetadata> metadatas = source.getPluginExecutionMetadata(executionKey);
                metadatas = applyParametersFilter(session, metadatas, mavenProject, execution);
                for(PluginExecutionMetadata metadata : metadatas) {
                  if(isValidPluginExecutionMetadata(metadata)) {
                    if(metadata.getAction() == PluginExecutionAction.configurator
                        && isSecondaryMapping(metadata, primaryMetadata)) {
                      executionMetadatas.add(metadata);
                    }
                  } else {
                    log.debug("Invalid secondary lifecycle mapping metadata for {}.", executionKey.toString());
                  }
                }
              } catch(CoreException e) {
                SourceLocation location = SourceLocationHelper.findLocation(mavenProject, executionKey);
                result.addProblem(new MavenProblemInfo(location, e));
              }
            }
          }
        }

        // TODO valid executionMetadatas and convert to error mapping invalid enties.

        executionMapping.put(executionKey, executionMetadatas);
      }
    } else {
      log.debug("Execution plan is null, could not calculate mojo execution mapping for {}.", mavenProject.toString());
    }

    result.setMojoExecutionMapping(executionMapping);
  }

  private static List<PluginExecutionMetadata> applyParametersFilter(MavenSession session,
      List<PluginExecutionMetadata> metadatas, MavenProject mavenProject, MojoExecution execution) throws CoreException {
    IMaven maven = MavenPlugin.getMaven();

    List<PluginExecutionMetadata> result = new ArrayList<PluginExecutionMetadata>();
    all_metadatas: for(PluginExecutionMetadata metadata : metadatas) {
      @SuppressWarnings("unchecked")
      Map<String, String> parameters = metadata.getFilter().getParameters();
      if(!parameters.isEmpty()) {
        for(String name : parameters.keySet()) {
          String value = parameters.get(name);
          MojoExecution setupExecution = maven.setupMojoExecution(session, mavenProject, execution);
          if(!eq(value, maven.getMojoParameterValue(session, setupExecution, name, String.class))) {
            continue all_metadatas;
          }
        }
      }
      result.add(metadata);
    }
    return result;
  }

  private static boolean isValidPluginExecutionMetadata(PluginExecutionMetadata metadata) {
    switch(metadata.getAction()) {
      case error:
      case execute:
      case ignore:
        return true;
      case configurator:
        try {
          getProjectConfiguratorId(metadata);
          return true;
        } catch(LifecycleMappingConfigurationException e) {
          // fall through
        }
        return false;
    }
    return false;
  }

  public static void instantiateLifecycleMapping(LifecycleMappingResult result, MavenProject mavenProject,
      String lifecycleMappingId) {
    // validate lifecycle mapping id and bail if it's invalid
    AbstractLifecycleMapping lifecycleMapping = null;
    if(lifecycleMappingId != null) {
      lifecycleMapping = getLifecycleMapping(lifecycleMappingId);
      if(lifecycleMapping == null) {
        SourceLocation markerLocation = SourceLocationHelper.findPackagingLocation(mavenProject);
        result.addProblem(new MissingLifecycleExtensionPoint(lifecycleMappingId, markerLocation));
      }
    }
    result.setLifecycleMapping(lifecycleMapping);
  }

  public static void instantiateProjectConfigurators(MavenProject mavenProject, LifecycleMappingResult result,
      Map<MojoExecutionKey, List<IPluginExecutionMetadata>> map) {
    if(map == null) {
      Map<String, AbstractProjectConfigurator> configurators = Collections.emptyMap();
      result.setProjectConfigurators(configurators);
      return;
    }

    Map<String, AbstractProjectConfigurator> configurators = new LinkedHashMap<String, AbstractProjectConfigurator>();
    for(Map.Entry<MojoExecutionKey, List<IPluginExecutionMetadata>> entry : map.entrySet()) {
      MojoExecutionKey executionKey = entry.getKey();
      List<IPluginExecutionMetadata> executionMetadatas = entry.getValue();

      if(executionMetadatas == null || executionMetadatas.isEmpty()) {
        if(isInterestingPhase(executionKey.getLifecyclePhase())) {
          SourceLocation markerLocation = SourceLocationHelper.findLocation(mavenProject, executionKey);
          result.addProblem(new NotCoveredMojoExecution(executionKey, markerLocation));
        }
        continue;
      }

      for(IPluginExecutionMetadata metadata : executionMetadatas) {
        String message = LifecycleMappingFactory.getActionMessage(metadata);
        switch(metadata.getAction()) {
          case error: {
            if(message == null) {
              message = NLS.bind(Messages.LifecycleConfigurationPluginExecutionErrorMessage, executionKey.toString());
            }
            SourceLocation markerLocation = SourceLocationHelper.findLocation(mavenProject, executionKey);
            result.addProblem(new ActionMessageProblemInfo(message, IMarker.SEVERITY_ERROR, executionKey,
                markerLocation, isPomMapping(metadata)));
            break;
          }
          case execute:
            if(message != null) {
              SourceLocation markerLocation = SourceLocationHelper.findLocation(mavenProject, executionKey);
              result.addProblem(new ActionMessageProblemInfo(message, IMarker.SEVERITY_WARNING, executionKey,
                  markerLocation, isPomMapping(metadata)));
            }
            break;
          case configurator:
            String configuratorId = LifecycleMappingFactory.getProjectConfiguratorId(metadata);
            try {
              if(!configurators.containsKey(configuratorId)) {
                configurators.put(configuratorId, LifecycleMappingFactory.createProjectConfigurator(metadata));
              }
            } catch(LifecycleMappingConfigurationException e) {
              log.debug("Could not instantiate project configurator {}.", configuratorId, e);
              SourceLocation markerLocation = SourceLocationHelper.findLocation(mavenProject, executionKey);
              result.addProblem(new MissingConfiguratorProblemInfo(configuratorId, markerLocation));
              result.addProblem(new NotCoveredMojoExecution(executionKey, markerLocation));
            }
            break;
          case ignore:
            if(message != null) {
              SourceLocation markerLocation = SourceLocationHelper.findLocation(mavenProject, executionKey);
              result.addProblem(new ActionMessageProblemInfo(message, IMarker.SEVERITY_WARNING, executionKey,
                  markerLocation, isPomMapping(metadata)));
            }
            break;
          default:
            // TODO invalid metadata
        }
      }
    }

    result.setProjectConfigurators(configurators);
  }

  private static boolean isPomMapping(IPluginExecutionMetadata metadata) {
    LifecycleMappingMetadataSource source = ((PluginExecutionMetadata) metadata).getSource();
    return source != null && source.getSource() instanceof MavenProject;
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
    IMaven maven = MavenPlugin.getMaven();

    ArrayList<LifecycleMappingMetadataSource> sources = new ArrayList<LifecycleMappingMetadataSource>();

    HashSet<String> referenced = new LinkedHashSet<String>();

    MavenProject project = mavenProject;
    do {
      if(monitor.isCanceled()) {
        break;
      }

      LifecycleMappingMetadataSource embeddedSource = getEmbeddedMetadataSource(project);
      if(embeddedSource != null) {
        maven.detachFromSession(project); // don't cache maven session 
        embeddedSource.setSource(project);
        sources.add(embeddedSource);
      }

      for(LifecycleMappingMetadataSource referencedSource : getReferencedMetadataSources(referenced, project, monitor)) {
        sources.add(referencedSource);
      }

      // TODO ideally, we need to reuse the same parent MavenProject instance in all child modules
      //      each instance takes ~1M, so we can easily safe 100M+ of heap for larger workspaces
      MavenExecutionRequest request = newMavenExecutionRequest(templateRequest);
      project = maven.resolveParentProject(request, project, monitor);
    } while(project != null);

    return sources;
  }

  private static MavenExecutionRequest newMavenExecutionRequest(MavenExecutionRequest templateRequest) {
    // TODO ain't nice
    MavenExecutionRequest copy = DefaultMavenExecutionRequest.copy(templateRequest);
    copy.setStartTime(templateRequest.getStartTime());
    return copy;
  }

  public static AbstractProjectConfigurator createProjectConfigurator(IPluginExecutionMetadata metadata) {
    PluginExecutionAction pluginExecutionAction = metadata.getAction();
    if(pluginExecutionAction != PluginExecutionAction.configurator) {
      throw new IllegalArgumentException();
    }
    String configuratorId = getProjectConfiguratorId(metadata);
    AbstractProjectConfigurator projectConfigurator = createProjectConfigurator(configuratorId);
    if(projectConfigurator == null) {
      String message = NLS.bind(Messages.ProjectConfiguratorNotAvailable, configuratorId);
      throw new LifecycleMappingConfigurationException(message);
    }
    return projectConfigurator;
  }

  public static String getProjectConfiguratorId(IPluginExecutionMetadata metadata) {
    Xpp3Dom child = ((PluginExecutionMetadata) metadata).getConfiguration().getChild(ATTR_ID);
    if(child == null || child.getValue().trim().length() == 0) {
      throw new LifecycleMappingConfigurationException("A configurator id must be specified");
    }
    return child.getValue();
  }

  public static String getActionMessage(IPluginExecutionMetadata metadata) {
    Xpp3Dom child = ((PluginExecutionMetadata) metadata).getConfiguration().getChild(ELEMENT_MESSAGE);
    if(child == null || child.getValue().trim().length() == 0) {
      return null;
    }
    return child.getValue();
  }

  public static LifecycleMappingMetadataSource createLifecycleMappingMetadataSource(InputStream is) throws IOException,
      XmlPullParserException {
    LifecycleMappingMetadataSource metadataSource = new LifecycleMappingMetadataSourceXpp3Reader().read(is);

    postCreateLifecycleMappingMetadataSource(metadataSource);

    return metadataSource;
  }

  private static void postCreateLifecycleMappingMetadataSource(LifecycleMappingMetadataSource metadataSource) {
    for(LifecycleMappingMetadata lifecycleMappingMetadata : metadataSource.getLifecycleMappings()) {
      lifecycleMappingMetadata.setSource(metadataSource);
      for(PluginExecutionMetadata executionMetadata : lifecycleMappingMetadata.getPluginExecutions()) {
        executionMetadata.setSource(metadataSource);
      }
    }

    for(PluginExecutionMetadata executionMetadata : metadataSource.getPluginExecutions()) {
      executionMetadata.setSource(metadataSource);
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
      log.error(ex.getMessage(), ex);
    }
    return null;
  }

  public static MojoExecutionBuildParticipant createMojoExecutionBuildParicipant(IMavenProjectFacade projectFacade,
      MojoExecution mojoExecution, IPluginExecutionMetadata executionMetadata) {
    boolean runOnIncremental = true;
    boolean runOnConfiguration = false;
    Xpp3Dom child = ((PluginExecutionMetadata) executionMetadata).getConfiguration().getChild(
        ELEMENT_RUN_ON_INCREMENTAL);
    if(child != null) {
      runOnIncremental = Boolean.parseBoolean(child.getValue());
    }
    child = ((PluginExecutionMetadata) executionMetadata).getConfiguration().getChild(ELEMENT_RUN_ON_CONFIGURATION);
    if(child != null) {
      runOnConfiguration = Boolean.parseBoolean(child.getValue());
    }
    return new MojoExecutionBuildParticipant(mojoExecution, runOnIncremental, runOnConfiguration);
  }

  public static Map<String, IConfigurationElement> getLifecycleMappingExtensions() {
    Map<String, IConfigurationElement> mappings = new HashMap<String, IConfigurationElement>(); // not ordered

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_LIFECYCLE_MAPPINGS);
    if(configuratorsExtensionPoint != null) {
      IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
      for(IExtension extension : configuratorExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_LIFECYCLE_MAPPING)) {
            mappings.put(element.getAttribute(ATTR_ID), element);
          }
        }
      }
    }

    return mappings;
  }

  private static AbstractLifecycleMapping getLifecycleMapping(String mappingId) {
    IConfigurationElement element = getLifecycleMappingExtensions().get(mappingId);
    if(element != null && element.getName().equals(ELEMENT_LIFECYCLE_MAPPING)) {
      if(mappingId.equals(element.getAttribute(ATTR_ID))) {
        return createLifecycleMapping(element);
      }
    }
    return null;
  }

  public static AbstractProjectConfigurator createProjectConfigurator(String configuratorId) {
    IConfigurationElement element = getProjectConfiguratorExtension(configuratorId);
    if(element != null) {
      try {
        AbstractProjectConfigurator configurator = (AbstractProjectConfigurator) element
            .createExecutableExtension(AbstractProjectConfigurator.ATTR_CLASS);

        configurator.setProjectManager(MavenPlugin.getMavenProjectRegistry());
        configurator.setMavenConfiguration(MavenPlugin.getMavenConfiguration());
        configurator.setMarkerManager(MavenPluginActivator.getDefault().getMavenMarkerManager());

        return configurator;
      } catch(CoreException ex) {
        log.error(ex.getMessage(), ex);
      }
    }
    return null;
  }

  public static Map<String, IConfigurationElement> getProjectConfiguratorExtensions() {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    return getProjectConfiguratorExtensions(registry);
  }

  public static Map<String, IConfigurationElement> getProjectConfiguratorExtensions(IExtensionRegistry registry) {
    Map<String, IConfigurationElement> extensions = new HashMap<String, IConfigurationElement>();
    IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_PROJECT_CONFIGURATORS);
    if(configuratorsExtensionPoint != null) {
      IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
      for(IExtension extension : configuratorExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_CONFIGURATOR)) {
            extensions.put(element.getAttribute(AbstractProjectConfigurator.ATTR_ID), element);
          }
        }
      }
    }
    return extensions;
  }

  private static IConfigurationElement getProjectConfiguratorExtension(String configuratorId) {
    IConfigurationElement element = getProjectConfiguratorExtensions().get(configuratorId);
    if(element != null && element.getName().equals(ELEMENT_CONFIGURATOR)) {
      if(configuratorId.equals(element.getAttribute(AbstractProjectConfigurator.ATTR_ID))) {
        return element;
      }
    }
    return null;
  }

  private static void checkCompatibleVersion(Plugin metadataPlugin) {
    ComparableVersion version = new ComparableVersion(metadataPlugin.getVersion());
    if(!version.equals(new ComparableVersion(LIFECYCLE_MAPPING_PLUGIN_VERSION))) {
      SourceLocation location = SourceLocationHelper.findLocation(metadataPlugin, SourceLocationHelper.VERSION);
      throw new LifecycleMappingConfigurationException(NLS.bind(Messages.LifecycleMappingPluginVersionIncompatible,
          metadataPlugin.getVersion()), location);
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
            LifecycleMappingMetadataSource metadataSource = new LifecycleMappingMetadataSourceXpp3Reader()
                .read(new StringReader(lifecycleMappingDom.toString()));
            postCreateLifecycleMappingMetadataSource(metadataSource);
            String packagingType = mavenProject.getPackaging();
            if(!"pom".equals(packagingType)) { //$NON-NLS-1$
              for(LifecycleMappingMetadata lifecycleMappingMetadata : metadataSource.getLifecycleMappings()) {
                if(!packagingType.equals(lifecycleMappingMetadata.getPackagingType())) {
                  SourceLocation location = SourceLocationHelper.findLocation(metadataPlugin,
                      SourceLocationHelper.CONFIGURATION);
                  throw new LifecycleMappingConfigurationException(NLS.bind(Messages.LifecycleMappingPackagingMismatch,
                      lifecycleMappingMetadata.getPackagingType(), packagingType), location);
                }
              }
            }
            return metadataSource;
          } catch(IOException e) {
            throw new LifecycleMappingConfigurationException(
                "Cannot read lifecycle mapping metadata for maven project " + mavenProject, e);
          } catch(XmlPullParserException e) {
            throw new LifecycleMappingConfigurationException(
                "Cannot parse lifecycle mapping metadata for maven project " + mavenProject, e);
          } catch(LifecycleMappingConfigurationException e) {
            throw e;
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
              try {
                LifecycleMappingMetadataSource lifecycleMappingMetadataSource = getLifecycleMappingMetadataSource(
                    groupId, artifactId, version, mavenProject.getRemoteArtifactRepositories(), monitor);
                metadataSources.add(lifecycleMappingMetadataSource);
              } catch(LifecycleMappingConfigurationException e) {
                SourceLocation location = SourceLocationHelper.findLocation(plugin, SourceLocationHelper.CONFIGURATION);
                e.setLocation(location);
                throw e;
              }
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

    MavenImpl maven = (MavenImpl) MavenPlugin.getMaven();
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
    IMaven maven = MavenPlugin.getMaven();
    try {
      // TODO this does not resolve workspace artifacts
      Artifact artifact = maven.resolve(groupId, artifactId, version, "xml", LIFECYCLE_MAPPING_METADATA_CLASSIFIER,
          repositories, monitor);

      File file = artifact.getFile();
      if(file == null || !file.exists() || !file.canRead()) {
        throw new LifecycleMappingConfigurationException("Cannot find file for artifact " + artifact);
      }
      try {
        LifecycleMappingMetadataSource metadataSource = createLifecycleMappingMetadataSource(groupId, artifactId,
            version, file);
        metadataSource.setSource(artifact);
        return metadataSource;
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
      LifecycleMappingMetadataSource lifecycleMappingMetadataSource = createLifecycleMappingMetadataSource(in);
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
  public synchronized static List<LifecycleMappingMetadataSource> getBundleMetadataSources() {
    if(bundleMetadataSources == null) {
      bundleMetadataSources = new ArrayList<LifecycleMappingMetadataSource>();

      IExtensionRegistry registry = Platform.getExtensionRegistry();
      IExtensionPoint configuratorsExtensionPoint = registry
          .getExtensionPoint(EXTENSION_LIFECYCLE_MAPPING_METADATA_SOURCE);
      if(configuratorsExtensionPoint != null) {
        IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
        for(IExtension extension : configuratorExtensions) {
          RegistryContributor contributor = (RegistryContributor) extension.getContributor();
          Bundle bundle = Platform.getBundle(contributor.getActualName());
          LifecycleMappingMetadataSource source = getMetadataSource(bundle);
          if(source != null) {
            bundleMetadataSources.add(source);
          }
        }
      }
    }
    return bundleMetadataSources;
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
          return createLifecycleMappingMetadataSource(in);
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

  private static boolean isPrimaryMapping(PluginExecutionMetadata executionMetadata) {
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

  private static boolean isSecondaryMapping(PluginExecutionMetadata metadata, PluginExecutionMetadata primaryMetadata) {
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

  public static ILifecycleMapping getLifecycleMapping(IMavenProjectFacade facade) {
    ILifecycleMapping lifecycleMapping = (ILifecycleMapping) facade
        .getSessionProperty(MavenProjectFacade.PROP_LIFECYCLE_MAPPING);
    if(lifecycleMapping == null) {
      String lifecycleMappingId = facade.getLifecycleMappingId();
      if(lifecycleMappingId != null) {
        lifecycleMapping = getLifecycleMapping(lifecycleMappingId);
      }
      if(lifecycleMapping == null) {
        lifecycleMapping = new InvalidLifecycleMapping();
      }
      facade.setSessionProperty(MavenProjectFacade.PROP_LIFECYCLE_MAPPING, lifecycleMapping);
    }
    return lifecycleMapping;
  }

  public static Map<String, AbstractProjectConfigurator> getProjectConfigurators(IMavenProjectFacade facade) {
    @SuppressWarnings("unchecked")
    Map<String, AbstractProjectConfigurator> configurators = (Map<String, AbstractProjectConfigurator>) facade
        .getSessionProperty(MavenProjectFacade.PROP_CONFIGURATORS);
    if(configurators == null) {
      // Project configurators are stored as a facade session property, so they are "lost" on eclipse restart.
      LifecycleMappingResult result = new LifecycleMappingResult();
      instantiateProjectConfigurators(facade.getMavenProject(), result, facade.getMojoExecutionMapping());
      configurators = result.getProjectConfigurators();
      // TODO deal with configurators that have been removed since facade was first created
      facade.setSessionProperty(MavenProjectFacade.PROP_CONFIGURATORS, configurators);
    }
    return configurators;
  }

  public static boolean isLifecycleMappingChanged(IMavenProjectFacade newFacade,
      ILifecycleMappingConfiguration oldConfiguration, IProgressMonitor monitor) {
    if(oldConfiguration == null || newFacade == null) {
      return false; // we have bigger problems to worry
    }

    String lifecycleMappingId = newFacade.getLifecycleMappingId();
    if(lifecycleMappingId == null || newFacade.getMojoExecutionMapping() == null) {
      return false; // we have bigger problems to worry
    }

    if(!eq(lifecycleMappingId, oldConfiguration.getLifecycleMappingId())) {
      return true;
    }

    // at this point we know lifecycleMappingId is not null and has not changed
    AbstractLifecycleMapping lifecycleMapping = getLifecycleMapping(lifecycleMappingId);
    if(lifecycleMapping == null) {
      return false; // we have bigger problems to worry about
    }

    return lifecycleMapping.hasLifecycleMappingChanged(newFacade, oldConfiguration, monitor);
  }

  private static <T> boolean eq(T a, T b) {
    return a != null ? a.equals(b) : b == null;
  }

  private static final String[] INTERESTING_PHASES = {"validate", //
      "initialize", //
      "generate-sources", //
      "process-sources", //
      "generate-resources", //
      "process-resources", //
      "compile", //
      "process-classes", //
      "generate-test-sources", //
      "process-test-sources", //
      "generate-test-resources", //
      "process-test-resources", //
      "test-compile", //
      "process-test-classes", //
  // "test", //
  // "prepare-package", //
  // "package", //
  //"pre-integration-test", //
  // "integration-test", //
  // "post-integration-test", //
  // "verify", //
  // "install", //
  // "deploy", //
  };

  public static boolean isInterestingPhase(String phase) {
    for(String interestingPhase : INTERESTING_PHASES) {
      if(interestingPhase.equals(phase)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param bundleMetadataSources The bundleMetadataSources to set.
   */
  public synchronized static void setBundleMetadataSources(List<LifecycleMappingMetadataSource> bundleMetadataSources) {
    LifecycleMappingFactory.bundleMetadataSources = bundleMetadataSources;
  }
}
