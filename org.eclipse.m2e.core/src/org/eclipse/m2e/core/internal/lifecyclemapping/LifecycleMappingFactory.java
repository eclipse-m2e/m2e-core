/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Andrew Eisenberg - Work on Bug 350414
 *      Fred Bricon (Red Hat) - project configurator sort (Bug #449495)
 *      Anton Tanasenko - Refactor marker resolutions and quick fixes (Bug #484359)
 *      Christoph Läubrich - #549 - Improve conflict handling of lifecycle mappings
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecyclemapping;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
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

import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
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
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingFilter;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionFilter;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.io.xpp3.LifecycleMappingMetadataSourceXpp3Reader;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.io.xpp3.LifecycleMappingMetadataSourceXpp3Writer;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.internal.markers.MavenProblemInfo;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.internal.markers.SourceLocationHelper;
import org.eclipse.m2e.core.internal.preferences.ProblemSeverity;
import org.eclipse.m2e.core.internal.project.registry.EclipseWorkspaceArtifactRepository;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractCustomizableLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.AbstractLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.ILifecycleMappingConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.NoopLifecycleMapping;


/**
 * @author igor
 */
public class LifecycleMappingFactory {

  private static final Logger log = LoggerFactory.getLogger(LifecycleMappingFactory.class);

  public static final String LIFECYCLE_MAPPING_PLUGIN_GROUPID = "org.eclipse.m2e"; //$NON-NLS-1$

  public static final String LIFECYCLE_MAPPING_PLUGIN_ARTIFACTID = "lifecycle-mapping"; //$NON-NLS-1$

  public static final String LIFECYCLE_MAPPING_PLUGIN_VERSION = "1.0.0"; //$NON-NLS-1$

  private static final String LIFECYCLE_MAPPING_PLUGIN_KEY = LIFECYCLE_MAPPING_PLUGIN_GROUPID + ":" //$NON-NLS-1$
      + LIFECYCLE_MAPPING_PLUGIN_ARTIFACTID;

  public static final String LIFECYCLE_MAPPING_METADATA_SOURCE_NAME = "lifecycle-mapping-metadata.xml"; //$NON-NLS-1$

  private static final String LIFECYCLE_MAPPING_METADATA_SOURCE_PATH = '/' + LIFECYCLE_MAPPING_METADATA_SOURCE_NAME;

  private static final String LIFECYCLE_MAPPING_METADATA_EMBEDDED_SOURCE_PATH = "META-INF/m2e/" //$NON-NLS-1$
      + LIFECYCLE_MAPPING_METADATA_SOURCE_NAME;

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

  static final String ELEMENT_RUN_ON_INCREMENTAL = "runOnIncremental";

  static final String ELEMENT_RUN_ON_CONFIGURATION = "runOnConfiguration";

  private static final String ATTR_GROUPID = "groupId";

  private static final String ATTR_ARTIFACTID = "artifactId";

  private static final String ATTR_VERSION = "version";

  private static final String LIFECYCLE_MAPPING_METADATA_CLASSIFIER = "lifecycle-mapping-metadata";

  private static List<LifecycleMappingMetadataSource> bundleMetadataSources = null;

  /**
   * Do not instantiate, use statically
   */
  private LifecycleMappingFactory() {
  }

  public static LifecycleMappingResult calculateLifecycleMapping(MavenProject mavenProject,
      List<MojoExecution> mojoExecutions, String lifecycleMappingId, IProgressMonitor monitor) {
    long start = System.currentTimeMillis();
    log.debug("Loading lifecycle mapping for {}.", mavenProject); //$NON-NLS-1$

    LifecycleMappingResult result = new LifecycleMappingResult();

    try {
      if(lifecycleMappingId != null) {
        instantiateLifecycleMapping(result, mavenProject, lifecycleMappingId);
      }

      calculateEffectiveLifecycleMappingMetadata(result, mavenProject, mojoExecutions, monitor);

      if(result.getLifecycleMapping() == null) {
        lifecycleMappingId = result.getLifecycleMappingId();
        instantiateLifecycleMapping(result, mavenProject, lifecycleMappingId);
      }

      if(result.getLifecycleMapping() instanceof AbstractCustomizableLifecycleMapping) {
        instantiateProjectConfigurators(mavenProject, result, result.getMojoExecutionMapping());
      }
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
      result.addProblem(new MavenProblemInfo(1, ex)); // XXX that looses most of useful info
    } finally {
      log.info("Using {} lifecycle mapping for {}.", result.getLifecycleMappingId(), mavenProject); //$NON-NLS-1$
      log.debug("Loaded lifecycle mapping in {} ms for {}.", System.currentTimeMillis() - start, //$NON-NLS-1$
          mavenProject);
    }
    return result;
  }

  private static void calculateEffectiveLifecycleMappingMetadata(LifecycleMappingResult result,
      MavenProject mavenProject, List<MojoExecution> mojoExecutions, IProgressMonitor monitor) throws CoreException {

    String packagingType = mavenProject.getPackaging();
    if("pom".equals(packagingType)) { //$NON-NLS-1$
      log.debug("Using NoopLifecycleMapping lifecycle mapping for {}.", mavenProject); //$NON-NLS-1$

      LifecycleMappingMetadata lifecycleMappingMetadata = new LifecycleMappingMetadata();
      lifecycleMappingMetadata.setLifecycleMappingId(NoopLifecycleMapping.LIFECYCLE_MAPPING_ID);

      result.setLifecycleMappingMetadata(lifecycleMappingMetadata);

      Map<MojoExecutionKey, List<IPluginExecutionMetadata>> executionMapping = new LinkedHashMap<>();
      result.setMojoExecutionMapping(executionMapping);

    } else if(result.getLifecycleMapping() != null
        && !(result.getLifecycleMapping() instanceof AbstractCustomizableLifecycleMapping)) {

      String lifecycleMappingId = result.getLifecycleMapping().getId();

      log.debug("Using non-customizable lifecycle mapping {} for {}.", lifecycleMappingId, mavenProject); // $NON-NLS-1$

      LifecycleMappingMetadata lifecycleMappingMetadata = new LifecycleMappingMetadata();
      lifecycleMappingMetadata.setLifecycleMappingId(lifecycleMappingId);

      result.setLifecycleMappingMetadata(lifecycleMappingMetadata);

      Map<MojoExecutionKey, List<IPluginExecutionMetadata>> executionMapping = new LinkedHashMap<>();
      result.setMojoExecutionMapping(executionMapping);
    } else {
      try {
        Map<String, List<MappingMetadataSource>> projectSources = getProjectMetadataSourcesMap(mavenProject,
            getBundleMetadataSources(), mojoExecutions, true, monitor);
        calculateEffectiveLifecycleMappingMetadata(result, asList(projectSources), mavenProject, mojoExecutions, true,
            monitor);
      } catch(LifecycleMappingConfigurationException e) {
        // could not read/parse/interpret mapping metadata configured in the pom or inherited from parent pom.
        // record the problem and return
        result.addProblem(new MavenProblemInfo(mavenProject, e));
      }
    }
  }

  public static Map<String, List<MappingMetadataSource>> getProjectMetadataSourcesMap(MavenProject mavenProject,
      List<LifecycleMappingMetadataSource> bundleMetadataSources, List<MojoExecution> mojoExecutions,
      boolean includeDefault, IProgressMonitor monitor) throws CoreException, LifecycleMappingConfigurationException {

    Map<String, List<MappingMetadataSource>> metadataSourcesMap = new HashMap<>();
    // List order
    // 1. preferences in project  (*** not implemented yet)
    // 2. preferences in ancestor project  (*** not implemented yet)
    // 3. this pom (annotated, embedded, referenced), parent (annotated, embedded, referenced), grand parent (embedded...
    // 4. preferences in workspace
    // 5. sources contributed by eclipse extensions
    // 6. maven-plugin embedded metadata
    // 7. default source, if present

    // TODO validate metadata and replace invalid entries with error mapping

    metadataSourcesMap.put("pomMappingMetadataSources", getPomMappingMetadataSources(mavenProject, monitor));

    metadataSourcesMap.put("workspaceMetadataSources",
        List.of(new SimpleMappingMetadataSource(getWorkspaceMetadata(false))));

    // TODO filter out invalid metadata from sources contributed by eclipse extensions and the default source
    if(bundleMetadataSources != null) {
      metadataSourcesMap.put("bundleMetadataSources",
          Collections.singletonList((MappingMetadataSource) new SimpleMappingMetadataSource(bundleMetadataSources)));
    }

    List<MappingMetadataSource> metadataSources = new ArrayList<>();
    for(LifecycleMappingMetadataSource source : getMavenPluginEmbeddedMetadataSources(mojoExecutions,
        mavenProject.getPluginArtifactRepositories(), monitor)) {
      metadataSources.add(new SimpleMappingMetadataSource(source));
    }
    metadataSourcesMap.put("mavenPluginEmbeddedMetadataSources", metadataSources);

    if(includeDefault) {
      LifecycleMappingMetadataSource defaultSource = getDefaultLifecycleMappingMetadataSource();
      if(defaultSource != null) {
        metadataSourcesMap.put("defaultLifecycleMappingMetadataSource",
            Collections.singletonList((MappingMetadataSource) new SimpleMappingMetadataSource(defaultSource)));
      }
    }

    return metadataSourcesMap;
  }

  public static void addLifecyclePluginExecution(LifecycleMappingMetadataSource mapping, String groupId,
      String artifactId, String version, List<String> goals, PluginExecutionAction action) {

    PluginExecutionMetadata execution = getPluginExecutionMetadata(mapping, groupId, artifactId, version, action);

    if(execution == null) {
      execution = new PluginExecutionMetadata();
      execution.setSource(mapping);
      execution.setFilter(new PluginExecutionFilter(groupId, artifactId, version, new HashSet<>()));

      Xpp3Dom actionDom = new Xpp3Dom("action");
      actionDom.addChild(new Xpp3Dom(action.toString()));
      execution.setActionDom(actionDom);

      mapping.addPluginExecution(execution);
    }

    for(String goal : goals) {
      execution.getFilter().addGoal(goal);
    }
  }

  public static void addLifecycleMappingPackagingFilter(LifecycleMappingMetadataSource mapping, String bsn,
      String version, String packaging) {
    LifecycleMappingFilter filter = getLifecycleMappingFilter(bsn, version, mapping);
    filter.getPackagingTypes().add(packaging);
  }

  public static void addLifecycleMappingExecutionFilter(LifecycleMappingMetadataSource mapping, String bsn,
      String bundleVersion, String executionGroupId, String executionArtifactId, String executionVersion, String goal) {
    LifecycleMappingFilter filter = getLifecycleMappingFilter(bsn, bundleVersion, mapping);
    PluginExecutionFilter executionFilter = getPluginExecutionFilter(executionGroupId, executionArtifactId,
        executionVersion, filter);
    executionFilter.getGoals().add(goal);
  }

  private static PluginExecutionFilter getPluginExecutionFilter(String executionGroupId, String executionArtifactId,
      String executionVersion, LifecycleMappingFilter filter) {
    for(PluginExecutionFilter executionFilter : filter.getPluginExecutions()) {
      if(Objects.equals(executionFilter.getGroupId(), executionGroupId)
          && Objects.equals(executionFilter.getArtifactId(), executionArtifactId)) {
        try {
          VersionRange filterRange = VersionRange.createFromVersionSpec(executionFilter.getVersionRange());
          if(filterRange.containsVersion(new DefaultArtifactVersion(executionVersion))) {
            return executionFilter;
          }
        } catch(InvalidVersionSpecificationException ex) {
          //can't check it then...
        }
      }
    }
    PluginExecutionFilter executionFilter = new PluginExecutionFilter(executionGroupId, executionArtifactId,
        "[" + executionVersion + ",)", new LinkedHashSet<>());
    filter.getPluginExecutions().add(executionFilter);
    return executionFilter;
  }

  private static LifecycleMappingFilter getLifecycleMappingFilter(String bsn, String version,
      LifecycleMappingMetadataSource mapping) {
    for(LifecycleMappingFilter filter : mapping.getLifecycleMappingFilters()) {
      if(Objects.equals(bsn, filter.getSymbolicName())) {
        try {
          VersionRange filterRange = VersionRange.createFromVersionSpec(filter.getVersionRange());
          if(filterRange.containsVersion(new DefaultArtifactVersion(version))) {
            return filter;
          }
        } catch(InvalidVersionSpecificationException ex) {
          //can't check it then...
        }
      }
    }
    LifecycleMappingFilter filter = new LifecycleMappingFilter();
    filter.setSymbolicName(bsn);
    filter.setVersionRange("[" + version + ",)");
    mapping.addLifecycleMappingFilter(filter);
    return filter;
  }

  private static PluginExecutionMetadata getPluginExecutionMetadata(LifecycleMappingMetadataSource mapping,
      String groupId, String artifactId, String version, PluginExecutionAction action) {
    for(PluginExecutionMetadata execution : mapping.getPluginExecutions()) {
      PluginExecutionFilter filter = execution.getFilter();
      if(Objects.equals(groupId, filter.getGroupId()) && Objects.equals(artifactId, filter.getArtifactId())
          && Objects.equals(version, filter.getVersionRange()) && action == execution.getAction()) {
        return execution;
      }
    }
    return null;
  }

  public static List<MappingMetadataSource> asList(Map<String, List<MappingMetadataSource>> map) {
    return Stream
        .of("pomMappingMetadataSources", "workspaceMetadataSources", "bundleMetadataSources",
            "mavenPluginEmbeddedMetadataSources", "defaultLifecycleMappingMetadataSource")
        .map(map::get).filter(Objects::nonNull).flatMap(List::stream).collect(Collectors.toCollection(ArrayList::new));
  }

  private static List<LifecycleMappingMetadataSource> getMavenPluginEmbeddedMetadataSources(
      List<MojoExecution> mojoExecutions, List<ArtifactRepository> remoteRepositories, IProgressMonitor monitor) {

    if(mojoExecutions == null || mojoExecutions.isEmpty()) {
      // TODO need to understand under what conditions execution plan is null here
      return Collections.emptyList();
    }
    Map<File, LifecycleMappingMetadataSource> result = new LinkedHashMap<>();

    MavenImpl maven = (MavenImpl) MavenPlugin.getMaven();

    for(MojoExecution execution : mojoExecutions) {
      Artifact artifact;
      // 422135 disable workspace resolution for plugin artifacts
      try (var d = EclipseWorkspaceArtifactRepository.setDisabled()) {
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

    return new ArrayList<>(result.values());
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
    LifecycleMappingMetadataSource metadata = null;
    try {
      if(file.isFile()) {
        try (JarFile jar = new JarFile(file)) {
          ZipEntry entry = jar.getEntry(LIFECYCLE_MAPPING_METADATA_EMBEDDED_SOURCE_PATH);
          if(entry == null) {
            return null;
          }
          InputStream is = jar.getInputStream(entry);
          metadata = createLifecycleMappingMetadataSource(is);
        }
      } else if(file.isDirectory()) {
        file = new File(file, LIFECYCLE_MAPPING_METADATA_EMBEDDED_SOURCE_PATH);
        if(file.isFile()) {
          try (InputStream is = new FileInputStream(file)) {
            metadata = createLifecycleMappingMetadataSource(is);
          }
        }
      }
    } catch(XmlPullParserException | IOException e) {
      throw new LifecycleMappingConfigurationException(
          "Cannot read lifecycle mapping metadata for artifact " + artifact, e);
    }
    if(metadata != null) {
      metadata.setSource(artifact);
    }
    return metadata;
  }

  private static File getWorkspaceMetadataFile() {
    return new File(MavenPlugin.getMavenConfiguration().getWorkspaceLifecycleMappingMetadataFile());
  }

  private static LifecycleMappingMetadataSource workspaceMetadataSource;

  public static synchronized LifecycleMappingMetadataSource getWorkspaceMetadata(boolean reload) {
    if(workspaceMetadataSource == null || reload) {
      File mappingFile = getWorkspaceMetadataFile();
      try (InputStream is = new FileInputStream(mappingFile)) {
        workspaceMetadataSource = createLifecycleMappingMetadataSource(is);
      } catch(FileNotFoundException e) {
        // this is expected, ignore
      } catch(IOException | XmlPullParserException ex) {
        log.error(ex.getMessage(), ex);
      }

      if(workspaceMetadataSource == null) {
        workspaceMetadataSource = new LifecycleMappingMetadataSource();
      }

      workspaceMetadataSource.setSource("workspace");
    }

    return workspaceMetadataSource;
  }

  public static synchronized void writeWorkspaceMetadata(LifecycleMappingMetadataSource metadata) {
    LifecycleMappingMetadataSourceXpp3Writer writer = new LifecycleMappingMetadataSourceXpp3Writer();
    File mappingFile = getWorkspaceMetadataFile();
    mappingFile.getParentFile().mkdirs();
    try (OutputStream os = new BufferedOutputStream(new FileOutputStream(mappingFile))) {
      writer.write(os, metadata);
    } catch(IOException ex) {
      log.error(ex.getMessage(), ex);
    }
    workspaceMetadataSource = metadata;
  }

  public static void calculateEffectiveLifecycleMappingMetadata(LifecycleMappingResult result,
      List<MappingMetadataSource> metadataSources, MavenProject mavenProject, List<MojoExecution> mojoExecutions,
      boolean applyDefaultStrategy, IProgressMonitor monitor) {

    //
    // PHASE 1. Look for lifecycle mapping for packaging type
    //

    LifecycleMappingMetadata lifecycleMappingMetadata = null;
    String packaging = mavenProject.getPackaging();
    PackagingTypeFilter packagingTypeFilter = new PackagingTypeFilter(metadataSources, packaging);
    for(int i = 0; i < metadataSources.size(); i++ ) {
      MappingMetadataSource source = metadataSources.get(i);
      try {
        lifecycleMappingMetadata = source.getLifecycleMappingMetadata(packaging, packagingTypeFilter);
        if(lifecycleMappingMetadata != null) {
          metadataSources.add(i, new SimpleMappingMetadataSource(lifecycleMappingMetadata));
          break;
        }
      } catch(DuplicateMappingException e) {
        SourceLocation location = SourceLocationHelper.findPackagingLocation(mavenProject);
        log.error("Duplicate lifecycle mapping metadata for {}.", mavenProject, e);
        result.addProblem(new DuplicateMappingSourceProblem(location,
            NLS.bind(Messages.LifecycleDuplicate, packaging, e.getMessage()),
            IMavenConstants.MARKER_DUPLICATEMAPPING_TYPE_PACKAGING, packaging, e));
        metadataSources.add(i, new FailedMappingMetadataSource(source, e));
        break;
      }
    }

    if(lifecycleMappingMetadata == null && applyDefaultStrategy) {
      lifecycleMappingMetadata = new LifecycleMappingMetadata();
      lifecycleMappingMetadata.setLifecycleMappingId("DEFAULT"); // TODO proper constant
      lifecycleMappingMetadata.setPackagingType(packaging);
    }

    // TODO if lifecycleMappingMetadata.lifecycleMappingId==null, convert to error lifecycle mapping metadata

    result.setLifecycleMappingMetadata(lifecycleMappingMetadata);

    //
    // PHASE 2. Bind project configurators to mojo executions.
    //
    PluginExecutionAction defaultMojoExecutionAction = MavenPlugin.getMavenConfiguration()
        .getDefaultMojoExecutionAction();

    Map<MojoExecutionKey, List<IPluginExecutionMetadata>> executionMapping = new LinkedHashMap<>();

    if(mojoExecutions != null && !mojoExecutions.isEmpty()) {
      Map<String, IConfigurationElement> elements = getProjectConfiguratorExtensions();

      for(MojoExecution execution : mojoExecutions) {
        MojoExecutionKey executionKey = new MojoExecutionKey(execution);
        MojoExecutionFilter goalFilter = new MojoExecutionFilter(metadataSources, executionKey);

        Map<String, PluginExecutionMetadata> configuratorMetadataMap = new HashMap<>();

        Map<MappingMetadataSource, List<PluginExecutionMetadata>> metadatasPerSource = new LinkedHashMap<>();

        // collect all metadatasPerSource and extract all configurator execution metadatas
        for(MappingMetadataSource source : metadataSources) {
          try {
            List<PluginExecutionMetadata> metadatas = applyParametersFilter(
                source.getPluginExecutionMetadata(executionKey), mavenProject, execution, monitor);
            metadatasPerSource.put(source, metadatas);
            for(PluginExecutionMetadata executionMetadata : metadatas) {
              if(isConfigurator(executionMetadata)) {
                String id = getProjectConfiguratorId(executionMetadata);
                configuratorMetadataMap.put(id, executionMetadata);
              }
            }
          } catch(CoreException e) {
            SourceLocation location = SourceLocationHelper.findLocation(mavenProject, executionKey);
            result.addProblem(new MavenProblemInfo(location, e));
            metadatasPerSource.put(source, Collections.<PluginExecutionMetadata> emptyList());
          }
        }

        //Sort configurator execution metadatas
        ProjectConfigurationElementSorter sorter = null;
        try {
          sorter = new ProjectConfigurationElementSorter(configuratorMetadataMap.keySet(), elements);
        } catch(CycleDetectedException ex) {
          log.error(ex.getMessage(), ex);
          result.addProblem(new MavenProblemInfo(1,
              NLS.bind("Cyclic dependency detected between project configurators for {0}", mavenProject), ex));
          return;// fatal error
        }

        //find primary mapping across different sources
        PluginExecutionMetadata primaryMetadata = null;
        try {
          for(Map.Entry<MappingMetadataSource, List<PluginExecutionMetadata>> entry : metadatasPerSource.entrySet()) {
            for(PluginExecutionMetadata executionMetadata : entry.getValue()) {
              if(goalFilter.test(executionMetadata)) {
                continue;
              }
              if(isPrimaryMapping(executionMetadata, sorter)) {
                if(primaryMetadata != null) {
                  throw new DuplicatePluginExecutionMetadataException(List.of(primaryMetadata, executionMetadata));
                }
                primaryMetadata = executionMetadata;
              }
            }
            if(primaryMetadata != null) {
              break;
            }
          }
        } catch(DuplicatePluginExecutionMetadataException e) {
          primaryMetadata = null;
          SourceLocation location = SourceLocationHelper.findLocation(mavenProject, executionKey);
          log.debug("Duplicate plugin execution mapping metadata for {}.", executionKey, e);
          result.addProblem(new DuplicateExecutionMappingSourceProblem(location,
              NLS.bind(Messages.PluginExecutionMappingDuplicate, executionKey, e.getMessage()), executionKey, e));
        }

        if(primaryMetadata != null && !isValidPluginExecutionMetadata(primaryMetadata)) {
          log.debug("Invalid plugin execution mapping metadata for {}.", executionKey);
          result.addProblem(
              new MavenProblemInfo(1, NLS.bind(Messages.PluginExecutionMappingInvalid, executionKey), null));
          primaryMetadata = null;
        }

        //add secondary configurators in order
        List<IPluginExecutionMetadata> executionMetadatas = new ArrayList<>();
        if(primaryMetadata != null) {
          executionMetadatas.add(primaryMetadata);
          if(isConfigurator(primaryMetadata)) {
            String primaryConfiguratorId = getProjectConfiguratorId(primaryMetadata);
            List<String> secondaryConfiguratorIds = sorter.getSecondaryConfigurators(primaryConfiguratorId);
            for(String id : secondaryConfiguratorIds) {
              IPluginExecutionMetadata metadata = configuratorMetadataMap.get(id);
              if(metadata == null) {
                log.debug("Invalid secondary lifecycle mapping metadata {} for {}.", id, executionKey);
              } else {
                executionMetadatas.add(metadata);
              }
            }
          }
        }
        if(defaultMojoExecutionAction != PluginExecutionAction.warn && executionMetadatas.isEmpty()
            && isInterestingPhase(execution.getLifecyclePhase())) {
          executionMetadatas.add(new DefaultPluginExecutionMetadata(execution, defaultMojoExecutionAction));
        }
        executionMapping.put(executionKey, executionMetadatas);
      }
    } else {
      log.debug("Execution plan is null, could not calculate mojo execution mapping for {}.", mavenProject);
    }

    result.setMojoExecutionMapping(executionMapping);
  }

  private static List<PluginExecutionMetadata> applyParametersFilter(List<PluginExecutionMetadata> metadatas,
      MavenProject mavenProject, MojoExecution execution, IProgressMonitor monitor) throws CoreException {
    IMaven maven = MavenPlugin.getMaven();
    List<PluginExecutionMetadata> result = new ArrayList<>();
    for(PluginExecutionMetadata metadata : metadatas) {
      if(hasMatchingParameterValue(mavenProject, execution, metadata, maven, monitor)) {
        result.add(metadata);
      }
    }
    return result;
  }

  private static boolean hasMatchingParameterValue(MavenProject mavenProject, MojoExecution execution,
      PluginExecutionMetadata metadata, IMaven maven, IProgressMonitor monitor) throws CoreException {
    Map<Object, String> parameters = metadata.getFilter().getParameters();
    for(Entry<Object, String> entry : parameters.entrySet()) {
      MojoExecution setupExecution = maven.setupMojoExecution(mavenProject, execution, monitor);
      String value = maven.getMojoParameterValue(mavenProject, setupExecution, (String) entry.getKey(), String.class,
          monitor);
      if(!Objects.equals(entry.getValue(), value)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isValidPluginExecutionMetadata(PluginExecutionMetadata metadata) {
    return switch(metadata.getAction()) {
      case error, warn, execute, ignore -> true;
      case configurator -> isConfigurator(metadata);
      default -> false;
    };
  }

  private static boolean isConfigurator(PluginExecutionMetadata metadata) {
    if(PluginExecutionAction.configurator == metadata.getAction()) {
      try {
        getProjectConfiguratorId(metadata);
        return true;
      } catch(LifecycleMappingConfigurationException e) {
        // fall through
      }
    }
    return false;
  }

  private static void instantiateLifecycleMapping(LifecycleMappingResult result, MavenProject mavenProject,
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

  private static void instantiateProjectConfigurators(MavenProject mavenProject, LifecycleMappingResult result,
      Map<MojoExecutionKey, List<IPluginExecutionMetadata>> map) {
    if(map == null) {
      Map<String, AbstractProjectConfigurator> configurators = Collections.emptyMap();
      result.setProjectConfigurators(configurators);
      return;
    }

    ProblemSeverity notCoveredMojoExecutionSeverity = ProblemSeverity
        .get(MavenPlugin.getMavenConfiguration().getNotCoveredMojoExecutionSeverity());

    boolean reportNotCoveredMojoExecutionProblems = !ProblemSeverity.ignore.equals(notCoveredMojoExecutionSeverity);

    Map<String, AbstractProjectConfigurator> configurators = new LinkedHashMap<>();
    for(Map.Entry<MojoExecutionKey, List<IPluginExecutionMetadata>> entry : map.entrySet()) {
      MojoExecutionKey executionKey = entry.getKey();
      List<IPluginExecutionMetadata> executionMetadatas = entry.getValue();

      if(executionMetadatas == null || executionMetadatas.isEmpty()) {
        if(reportNotCoveredMojoExecutionProblems && isInterestingPhase(executionKey.lifecyclePhase())) {
          SourceLocation markerLocation = SourceLocationHelper.findLocation(mavenProject, executionKey);
          result.addProblem(
              new NotCoveredMojoExecution(executionKey, notCoveredMojoExecutionSeverity.getSeverity(), markerLocation));
        }
        continue;
      }

      for(IPluginExecutionMetadata metadata : executionMetadatas) {
        String message = LifecycleMappingFactory.getActionMessage(metadata);
        switch(metadata.getAction()) {
          case error: {
            if(message == null) {
              message = NLS.bind(Messages.LifecycleConfigurationPluginExecutionErrorMessage, executionKey);
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
              if(reportNotCoveredMojoExecutionProblems) {
                SourceLocation markerLocation = SourceLocationHelper.findLocation(mavenProject, executionKey);
                result.addProblem(new MissingConfiguratorProblemInfo(configuratorId, executionKey,
                    notCoveredMojoExecutionSeverity.getSeverity(), markerLocation));
              }
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
        if(metadata instanceof DefaultPluginExecutionMetadata) {
          //we want to have a discovery hint here!
          SourceLocation markerLocation = SourceLocationHelper.findLocation(mavenProject, executionKey);
          result.addProblem(
              new NotCoveredMojoExecution(executionKey, IMarker.SEVERITY_INFO, markerLocation));
        }
      }
    }

    result.setProjectConfigurators(configurators);
  }

  private static boolean isPomMapping(IPluginExecutionMetadata metadata) {
    if(metadata instanceof PluginExecutionMetadata executionMetadata) {
      LifecycleMappingMetadataSource source = executionMetadata.getSource();
      return source != null && source.getSource() instanceof MavenProject;
    }
    return false;
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
  public static List<MappingMetadataSource> getPomMappingMetadataSources(MavenProject mavenProject,
      IProgressMonitor monitor) throws CoreException {

    List<MappingMetadataSource> sources = new ArrayList<>();

    HashSet<String> referenced = new LinkedHashSet<>();

    MavenProject project = mavenProject;
    do {
      if(monitor.isCanceled()) {
        break;
      }
      AnnotationMappingMetadataSource annSource = AnnotationMappingMetadataSource.get(project);
      if(annSource != null) {
        sources.add(annSource);
      }

      LifecycleMappingMetadataSource embeddedSource = getEmbeddedMetadataSource(project);
      if(embeddedSource != null) {
        embeddedSource.setSource(project);
        sources.add(new SimpleMappingMetadataSource(embeddedSource));
      }

      for(LifecycleMappingMetadataSource referencedSource : getReferencedMetadataSources(referenced, project,
          monitor)) {
        sources.add(new SimpleMappingMetadataSource(referencedSource));
      }
      project = project.getParent();
    } while(project != null);

    return sources;
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
    Xpp3Dom configuration = ((PluginExecutionMetadata) metadata).getConfiguration();
    Xpp3Dom child = configuration == null ? null : configuration.getChild(ATTR_ID);
    if(child == null || child.getValue().trim().length() == 0) {
      throw new LifecycleMappingConfigurationException("A configurator id must be specified");
    }
    return child.getValue();
  }

  private static String getActionMessage(IPluginExecutionMetadata metadata) {
    if(metadata instanceof PluginExecutionMetadata executionMetadata) {
      Xpp3Dom configuration = executionMetadata.getConfiguration();
      Xpp3Dom child = configuration == null ? null : configuration.getChild(ELEMENT_MESSAGE);
      if(child == null || child.getValue().isBlank()) {
        return null;
      }
      return child.getValue();
    }
    return null;
  }

  public static LifecycleMappingMetadataSource createLifecycleMappingMetadataSource(InputStream is)
      throws IOException, XmlPullParserException {
    return createLifecycleMappingMetadataSource(new InputStreamReader(is));
  }

  private static LifecycleMappingMetadataSource createLifecycleMappingMetadataSource(Reader reader)
      throws IOException, XmlPullParserException {
    LifecycleMappingMetadataSource metadataSource = new LifecycleMappingMetadataSourceXpp3Reader().read(reader);

    for(LifecycleMappingMetadata lifecycleMappingMetadata : metadataSource.getLifecycleMappings()) {
      lifecycleMappingMetadata.setSource(metadataSource);
      for(PluginExecutionMetadata executionMetadata : lifecycleMappingMetadata.getPluginExecutions()) {
        executionMetadata.setSource(metadataSource);
      }
    }
    for(PluginExecutionMetadata executionMetadata : metadataSource.getPluginExecutions()) {
      executionMetadata.setSource(metadataSource);
    }
    return metadataSource;
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

  public static MojoExecutionBuildParticipant createMojoExecutionBuildParicipant(MojoExecution mojoExecution,
      IPluginExecutionMetadata executionMetadata) {
    boolean runOnIncremental = false;
    boolean runOnConfiguration = false;
    if(executionMetadata instanceof PluginExecutionMetadata) {
      Xpp3Dom child = ((PluginExecutionMetadata) executionMetadata).getConfiguration()
          .getChild(ELEMENT_RUN_ON_INCREMENTAL);
      if(child != null) {
        runOnIncremental = Boolean.parseBoolean(child.getValue());
      }
      child = ((PluginExecutionMetadata) executionMetadata).getConfiguration().getChild(ELEMENT_RUN_ON_CONFIGURATION);
      if(child != null) {
        runOnConfiguration = Boolean.parseBoolean(child.getValue());
      }
    } else {
      runOnIncremental = true;
    }
    return new MojoExecutionBuildParticipant(mojoExecution, runOnIncremental, runOnConfiguration);
  }

  public static Map<String, IConfigurationElement> getLifecycleMappingExtensions() {
    Map<String, IConfigurationElement> mappings = new HashMap<>(); // not ordered

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_LIFECYCLE_MAPPINGS);
    if(configuratorsExtensionPoint != null) {
      for(IExtension extension : configuratorsExtensionPoint.getExtensions()) {
        for(IConfigurationElement element : extension.getConfigurationElements()) {
          if(ELEMENT_LIFECYCLE_MAPPING.equals(element.getName())) {
            mappings.put(element.getAttribute(ATTR_ID), element);
          }
        }
      }
    }
    return mappings;
  }

  private static AbstractLifecycleMapping getLifecycleMapping(String mappingId) {
    IConfigurationElement element = getLifecycleMappingExtensions().get(mappingId);
    if(element != null && ELEMENT_LIFECYCLE_MAPPING.equals(element.getName())
        && mappingId.equals(element.getAttribute(ATTR_ID))) {
      return createLifecycleMapping(element);
    }
    return null;
  }

  private static AbstractProjectConfigurator createProjectConfigurator(String configuratorId) {
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
    Map<String, IConfigurationElement> extensions = new HashMap<>();
    IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_PROJECT_CONFIGURATORS);
    if(configuratorsExtensionPoint != null) {
      IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
      for(IExtension extension : configuratorExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(ELEMENT_CONFIGURATOR.equals(element.getName())) {
            extensions.put(element.getAttribute(AbstractProjectConfigurator.ATTR_ID), element);
          }
        }
      }
    }
    return extensions;
  }

  private static IConfigurationElement getProjectConfiguratorExtension(String configuratorId,
      Map<String, IConfigurationElement> elements) {
    if(elements == null) {
      return null;
    }
    IConfigurationElement element = elements.get(configuratorId);
    if(element != null && ELEMENT_CONFIGURATOR.equals(element.getName())
        && configuratorId.equals(element.getAttribute(AbstractProjectConfigurator.ATTR_ID))) {
      return element;
    }
    return null;
  }

  private static IConfigurationElement getProjectConfiguratorExtension(String configuratorId) {
    return getProjectConfiguratorExtension(configuratorId, getProjectConfiguratorExtensions());
  }

  private static void checkCompatibleVersion(Plugin metadataPlugin) {
    String v = metadataPlugin.getVersion();
    if(v == null) {
      return; //TODO doesn't inherit version from parent, so we can't check the value
    }
    ComparableVersion version = new ComparableVersion(v);
    if(!version.equals(new ComparableVersion(LIFECYCLE_MAPPING_PLUGIN_VERSION))) {
      SourceLocation location = SourceLocationHelper.findLocation(metadataPlugin, SourceLocationHelper.VERSION);
      throw new LifecycleMappingConfigurationException(
          NLS.bind(Messages.LifecycleMappingPluginVersionIncompatible, metadataPlugin.getVersion()), location);
    }
  }

  private static LifecycleMappingMetadataSource getEmbeddedMetadataSource(MavenProject mavenProject)
      throws CoreException {
    // TODO this does not merge configuration from profiles
    PluginManagement pluginManagement = getPluginManagement(mavenProject);
    if(pluginManagement == null) {
      return null;
    }
    Plugin metadataPlugin = pluginManagement.getPluginsAsMap().get(LIFECYCLE_MAPPING_PLUGIN_KEY);
    if(metadataPlugin != null) {
      checkCompatibleVersion(metadataPlugin);

      Xpp3Dom configurationDom = (Xpp3Dom) metadataPlugin.getConfiguration();
      if(configurationDom != null) {
        Xpp3Dom lifecycleMappingDom = configurationDom.getChild(ELEMENT_LIFECYCLE_MAPPING_METADATA);
        if(lifecycleMappingDom != null) {
          try (Reader reader = new StringReader(lifecycleMappingDom.toString())) {
            LifecycleMappingMetadataSource metadataSource = createLifecycleMappingMetadataSource(reader);
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
    List<LifecycleMappingMetadataSource> metadataSources = new ArrayList<>();

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
                e.setLocation(SourceLocationHelper.findLocation(plugin, SourceLocationHelper.CONFIGURATION));
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
    ((MavenImpl) MavenPlugin.getMaven()).interpolateModel(mavenProject, model);
    return result;
  }

  private static void addBuild(PluginManagement result, BuildBase build) {
    if(build != null) {
      PluginManagement pluginManagement = build.getPluginManagement();
      if(pluginManagement != null) {
        for(Plugin plugin : pluginManagement.getPlugins()) {
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
      Bundle bundle = FrameworkUtil.getBundle(LifecycleMappingFactory.class);
      defaultLifecycleMappingMetadataSource = getMetadataSource(bundle);
      if(defaultLifecycleMappingMetadataSource == null) {
        defaultLifecycleMappingMetadataSource = new LifecycleMappingMetadataSource();
      }
      defaultLifecycleMappingMetadataSource.setSource("default");
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
    Artifact artifact;
    try {
      artifact = MavenPlugin.getMaven().resolve(groupId, artifactId, version, "xml",
          LIFECYCLE_MAPPING_METADATA_CLASSIFIER, repositories, monitor);
    } catch(CoreException ex) {
      throw new LifecycleMappingConfigurationException(ex);
    }
    File file = artifact.getFile();
    if(file == null || !file.exists() || !file.canRead()) {
      throw new LifecycleMappingConfigurationException("Cannot find file for artifact " + artifact);
    }
    try {
      LifecycleMappingMetadataSource source = createLifecycleMappingMetadataSource(groupId, artifactId, version, file);
      source.setSource(artifact);
      return source;
    } catch(IOException e) {
      throw new LifecycleMappingConfigurationException("Cannot read lifecycle mapping metadata for " + artifact, e);
    } catch(XmlPullParserException e) {
      throw new LifecycleMappingConfigurationException("Cannot parse lifecycle mapping metadata for " + artifact, e);
    } catch(RuntimeException e) {
      throw new LifecycleMappingConfigurationException("Cannot load lifecycle mapping metadata for " + artifact, e);
    }
  }

  private static LifecycleMappingMetadataSource createLifecycleMappingMetadataSource(String groupId, String artifactId,
      String version, File configuration) throws IOException, XmlPullParserException {
    try (InputStream in = new FileInputStream(configuration)) {
      LifecycleMappingMetadataSource lifecycleMappingMetadataSource = createLifecycleMappingMetadataSource(in);
      lifecycleMappingMetadataSource.setGroupId(groupId);
      lifecycleMappingMetadataSource.setArtifactId(artifactId);
      lifecycleMappingMetadataSource.setVersion(version);
      return lifecycleMappingMetadataSource;
    }
  }

  /**
   * Returns lifecycle mapping metadata sources provided by all installed bundles
   */
  public static synchronized List<LifecycleMappingMetadataSource> getBundleMetadataSources() {
    if(bundleMetadataSources == null) {
      bundleMetadataSources = new ArrayList<>();

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
      try (InputStream in = url.openStream()) {
        LifecycleMappingMetadataSource metadata = createLifecycleMappingMetadataSource(in);
        metadata.setSource(bundle);
        return metadata;
      } catch(IOException | XmlPullParserException e) {
        log.warn("Could not read lifecycle-mapping-metadata.xml for bundle {}", bundle.getSymbolicName(), e);
      }
    }
    return null;
  }

  static boolean isPrimaryMapping(PluginExecutionMetadata executionMetadata, ProjectConfigurationElementSorter sorter) {
    if(executionMetadata == null) {
      return false;
    }
    if(isConfigurator(executionMetadata)) {
      String configuratorId = getProjectConfiguratorId(executionMetadata);
      return sorter.isRootConfigurator(configuratorId);
    }
    return true;
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
      configurators = setProjectConfigurators(facade, result);
      // TODO deal with configurators that have been removed since facade was first created
      if(result.hasProblems()) {
        IMavenMarkerManager markerManager = MavenPluginActivator.getDefault().getMavenMarkerManager();
        for(MavenProblemInfo problem : result.getProblems()) {
          markerManager.addErrorMarker(facade.getPom(), IMavenConstants.MARKER_LIFECYCLEMAPPING_ID, problem);
        }
      }
    }
    return configurators;
  }

  public static Map<String, AbstractProjectConfigurator> setProjectConfigurators(IMavenProjectFacade facade,
      LifecycleMappingResult mappingResult) {

    Map<String, AbstractProjectConfigurator> unsorted = mappingResult.getProjectConfigurators();
    if(unsorted == null || unsorted.isEmpty()) {
      facade.setSessionProperty(MavenProjectFacade.PROP_CONFIGURATORS, unsorted);
      return unsorted;
    }

    Map<String, AbstractProjectConfigurator> configurators = new LinkedHashMap<>(unsorted.size());
    Map<String, IConfigurationElement> elements = getProjectConfiguratorExtensions();
    try {
      ProjectConfigurationElementSorter sorter = new ProjectConfigurationElementSorter(unsorted.keySet(), elements);
      List<String> sortedConfigurators = sorter.getSortedConfigurators();
      log.debug("{} is configured by :", facade.getProject().getName());
      for(String id : sortedConfigurators) {
        AbstractProjectConfigurator configurator = unsorted.get(id);
        if(configurator != null) {
          log.debug("\t- {}", id);
          configurators.put(id, configurator);
        }
      }
    } catch(CycleDetectedException e) {
      log.error("Cycle detecting while sorting configurators", e);
      SourceLocation location = SourceLocationHelper.findPackagingLocation(facade.getMavenProject());
      mappingResult.addProblem(new MavenProblemInfo(location, e));
    }
    facade.setSessionProperty(MavenProjectFacade.PROP_CONFIGURATORS, configurators);
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

    if(!Objects.equals(lifecycleMappingId, oldConfiguration.getLifecycleMappingId())) {
      return true;
    }

    // at this point we know lifecycleMappingId is not null and has not changed
    AbstractLifecycleMapping lifecycleMapping = getLifecycleMapping(lifecycleMappingId);
    if(lifecycleMapping == null) {
      return false; // we have bigger problems to worry about
    }

    return lifecycleMapping.hasLifecycleMappingChanged(newFacade, oldConfiguration, monitor);
  }

  private static final Set<String> INTERESTING_PHASES = Set.of( //
      "validate", //
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
      "process-test-classes" //
  // "test", //
  // "prepare-package", //
  // "package", //
  //"pre-integration-test", //
  // "integration-test", //
  // "post-integration-test", //
  // "verify", //
  // "install", //
  // "deploy", //
  );

  public static boolean isInterestingPhase(String phase) {
    return INTERESTING_PHASES.contains(phase);
  }

  /**
   * @param bundleMetadataSources The bundleMetadataSources to set.
   */
  public static synchronized void setBundleMetadataSources(List<LifecycleMappingMetadataSource> bundleMetadataSources) {
    LifecycleMappingFactory.bundleMetadataSources = bundleMetadataSources;
  }
}
