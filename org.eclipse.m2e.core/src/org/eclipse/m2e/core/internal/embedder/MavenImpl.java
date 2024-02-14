/*******************************************************************************
 * Copyright (c) 2008-2022 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Christoph Läubrich - move creation MavenExecutionRequest from MavenImpl->MavenExecutionContext
 *                         - remove IMavenExecutionContext getExecutionContext()
 *******************************************************************************/

package org.eclipse.m2e.core.internal.embedder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.converters.ConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.converters.lookup.DefaultConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.cli.configuration.SettingsXmlConfigurationProcessor;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.internal.LifecycleExecutionPlanCalculator;
import org.apache.maven.model.ConfigurationContainer;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.interpolation.ModelInterpolator;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.version.DefaultPluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.project.ProjectSorter;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.DefaultSettingsProblem;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.building.SettingsProblem.Severity;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.apache.maven.settings.io.SettingsWriter;
import org.apache.maven.wagon.proxy.ProxyInfo;

import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IComponentLookup;
import org.eclipse.m2e.core.embedder.ILocalRepositoryListener;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenConfigurationChangeListener;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.embedder.ISettingsChangeListener;
import org.eclipse.m2e.core.embedder.MavenConfigurationChangeEvent;
import org.eclipse.m2e.core.embedder.MavenSettingsLocations;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.M2EUtils;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;


@Component(service = {IMaven.class, IMavenConfigurationChangeListener.class})
public class MavenImpl implements IMaven, IMavenConfigurationChangeListener {

  private static final Logger log = LoggerFactory.getLogger(MavenImpl.class);

  @Reference
  private IMavenConfiguration mavenConfiguration;

  private final ConverterLookup converterLookup = new DefaultConverterLookup();

  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  private final List<ISettingsChangeListener> settingsListeners = new CopyOnWriteArrayList<>();

  private final List<ILocalRepositoryListener> localRepositoryListeners = new ArrayList<>();

  @Reference
  private PlexusContainerManager containerManager;

  /**
   * Cached parsed settingsCacheMap.xml instance
   */
  private Map<MavenSettingsLocations, MavenSettings> settingsCacheMap = new ConcurrentHashMap<>();

  @Override
  public String getLocalRepositoryPath() {
    String path = null;
    try {
      path = getSettings().getLocalRepository();
    } catch(CoreException ex) {
      // fall through
    }
    if(path == null) {
      path = RepositorySystem.defaultUserLocalRepository.getAbsolutePath();
    }
    return path;
  }

  /**
   * @return Returns the mavenConfiguration.
   */
  IMavenConfiguration getMavenConfiguration() {
    return this.mavenConfiguration;
  }

  @Override
  public <T> T getConfiguredMojo(MavenSession session, MojoExecution mojoExecution, Class<T> clazz)
      throws CoreException {
    try {
      MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();
      // getPluginRealm creates plugin realm and populates pluginDescriptor.classRealm field
      lookup(BuildPluginManager.class).getPluginRealm(session, mojoDescriptor.getPluginDescriptor());
      return clazz.cast(lookup(MavenPluginManager.class).getConfiguredMojo(Mojo.class, session, mojoExecution));
    } catch(PluginManagerException | PluginConfigurationException | ClassCastException | PluginResolutionException ex) {
      throw new CoreException(Status.error(NLS.bind(Messages.MavenImpl_error_mojo, mojoExecution), ex));
    }
  }

  @Override
  public void releaseMojo(Object mojo, MojoExecution mojoExecution) throws CoreException {
    lookup(MavenPluginManager.class).releaseMojo(mojo, mojoExecution);
  }

  private MavenExecutionPlan calculateExecutionPlan(MavenSession session, List<String> goals, boolean setup)
      throws CoreException {
    try {
      return lookup(LifecycleExecutor.class).calculateExecutionPlan(session, setup, goals.toArray(String[]::new));
    } catch(Exception ex) {
      throw new CoreException(Status.error(NLS.bind(Messages.MavenImpl_error_calc_build_plan, ex.getMessage()), ex));
    }
  }

  @Override
  public MavenExecutionPlan calculateExecutionPlan(MavenProject project, List<String> goals, boolean setup,
      IProgressMonitor monitor) throws CoreException {
    return getExecutionContext().execute(project,
        (context, pm) -> calculateExecutionPlan(context.getSession(), goals, setup), monitor);
  }

  private MojoExecution setupMojoExecution(MavenSession session, MavenProject project, MojoExecution execution)
      throws CoreException {
    MojoExecution clone = new MojoExecution(execution.getPlugin(), execution.getGoal(), execution.getExecutionId());
    clone.setMojoDescriptor(execution.getMojoDescriptor());
    if(execution.getConfiguration() != null) {
      clone.setConfiguration(new Xpp3Dom(execution.getConfiguration()));
    }
    clone.setLifecyclePhase(execution.getLifecyclePhase());
    LifecycleExecutionPlanCalculator executionPlanCalculator = lookup(LifecycleExecutionPlanCalculator.class);
    try {
      executionPlanCalculator.setupMojoExecution(session, project, clone);
    } catch(Exception ex) {
      throw new CoreException(Status.error(NLS.bind(Messages.MavenImpl_error_calc_build_plan, ex.getMessage()), ex));
    }
    return clone;
  }

  @Override
  public MojoExecution setupMojoExecution(MavenProject project, MojoExecution execution, IProgressMonitor monitor)
      throws CoreException {
    return getExecutionContext().execute(project,
        (context, pm) -> setupMojoExecution(context.getSession(), project, execution), monitor);
  }

  @Override
  public ArtifactRepository getLocalRepository() throws CoreException {
    try {
      String localRepositoryPath = getLocalRepositoryPath();
      if(localRepositoryPath != null) {
        return lookup(RepositorySystem.class).createLocalRepository(new File(localRepositoryPath));
      }
      return lookup(RepositorySystem.class).createLocalRepository(RepositorySystem.defaultUserLocalRepository);
    } catch(InvalidRepositoryException ex) {
      // can't happen
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public Settings getSettings() throws CoreException {
    return getSettings(mavenConfiguration.getSettingsLocations());
  }

  @Override
  public Settings getSettings(MavenSettingsLocations locations) throws CoreException {
    MavenSettings cache = settingsCacheMap.computeIfAbsent(locations, key -> new MavenSettings(key, MavenImpl.this));
    return cache.getSettings();
  }

  @Override
  public Settings buildSettings(String globalSettings, String userSettings) throws CoreException {
    SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
    request.setGlobalSettingsFile(globalSettings != null ? new File(globalSettings) : null);
    request.setUserSettingsFile(
        userSettings != null ? new File(userSettings) : SettingsXmlConfigurationProcessor.DEFAULT_USER_SETTINGS_FILE);
    try {
      return lookup(SettingsBuilder.class).build(request).getEffectiveSettings();
    } catch(SettingsBuildingException ex) {
      throw new CoreException(Status.error(Messages.MavenImpl_error_read_settings, ex));
    }
  }

  @Override
  public void writeSettings(Settings settings, OutputStream out) throws CoreException {
    try {
      lookup(SettingsWriter.class).write(out, null, settings);
    } catch(IOException ex) {
      throw new CoreException(Status.error(Messages.MavenImpl_error_write_settings, ex));
    }
  }

  @Override
  public List<SettingsProblem> validateSettings(String settings) {
    List<SettingsProblem> problems = new ArrayList<>();
    if(settings != null) {
      File settingsFile = new File(settings);
      if(settingsFile.canRead()) {
        SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setUserSettingsFile(settingsFile);
        try {
          lookup(SettingsBuilder.class).build(request);
        } catch(SettingsBuildingException ex) {
          problems.addAll(ex.getProblems());
        } catch(CoreException ex) {
          problems.add(new DefaultSettingsProblem(ex.getMessage(), Severity.FATAL, settings, -1, -1, ex));
        }
      } else {
        problems.add(new DefaultSettingsProblem(NLS.bind(Messages.MavenImpl_error_read_settings2, settings),
            SettingsProblem.Severity.ERROR, settings, -1, -1, null));
      }
    }

    return problems;
  }

  @Override
  public void reloadSettings() throws CoreException {
    settingsCacheMap.clear();
    Settings reloadedSettings = getSettings(mavenConfiguration.getSettingsLocations());
    for(ISettingsChangeListener listener : settingsListeners) {
      try {
        listener.settingsChanged(reloadedSettings);
      } catch(CoreException e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  @Override
  public Server decryptPassword(Server server) throws CoreException {
    SettingsDecryptionRequest request = new DefaultSettingsDecryptionRequest(server);
    SettingsDecryptionResult result = lookup(SettingsDecrypter.class).decrypt(request);
    for(SettingsProblem problem : result.getProblems()) {
      log.warn(problem.getMessage(), problem.getException());
    }
    return result.getServer();
  }

  @Override
  public void mavenConfigurationChange(MavenConfigurationChangeEvent event) throws CoreException {
    if(MavenPreferenceConstants.P_USER_SETTINGS_FILE.equals(event.key())
        || MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE.equals(event.key())) {
      reloadSettings();
    }
  }

  @Override
  public MavenProject readProject(File pomFile, IProgressMonitor monitor) throws CoreException {
    return getExecutionContext().execute((context, pm) -> {
      MavenExecutionRequest request = DefaultMavenExecutionRequest.copy(context.getExecutionRequest());
      try {
        lookup(MavenExecutionRequestPopulator.class).populateDefaults(request);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        configuration.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        configuration.setRepositorySession(
            MavenExecutionContext.createRepositorySession(request, getMavenConfiguration(), this));
        return lookup(ProjectBuilder.class).build(pomFile, configuration).getProject();
      } catch(ProjectBuildingException | MavenExecutionRequestPopulationException ex) {
        throw new CoreException(Status.error(Messages.MavenImpl_error_read_project, ex));
      }
    }, monitor);
  }

  private IMavenExecutionContext getExecutionContext() {
    return IMavenExecutionContext.getThreadContext().orElseGet(this::createExecutionContext);
  }

  @Override
  public MavenExecutionResult readMavenProject(File pomFile, ProjectBuildingRequest configuration)
      throws CoreException {
    long start = System.currentTimeMillis();

    log.debug("Reading Maven project: {}", pomFile.getAbsoluteFile()); //$NON-NLS-1$
    MavenExecutionResult result = new DefaultMavenExecutionResult();
    try {
      configuration.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
      ProjectBuildingResult projectBuildingResult = lookup(ProjectBuilder.class).build(pomFile, configuration);
      result.setProject(projectBuildingResult.getProject());
      result.setDependencyResolutionResult(projectBuildingResult.getDependencyResolutionResult());
    } catch(ProjectBuildingException ex) {
      if(ex.getResults() != null && ex.getResults().size() == 1) {
        ProjectBuildingResult projectBuildingResult = ex.getResults().get(0);
        result.setProject(projectBuildingResult.getProject());
        result.setDependencyResolutionResult(projectBuildingResult.getDependencyResolutionResult());
      }
      result.addException(ex);
    } catch(RuntimeException e) {
      result.addException(e);
    } finally {
      log.debug("Read Maven project: {} in {} ms", pomFile.getAbsoluteFile(), System.currentTimeMillis() - start); //$NON-NLS-1$
    }
    return result;
  }

  @Override
  public Map<File, MavenExecutionResult> readMavenProjects(Collection<File> pomFiles,
      ProjectBuildingRequest configuration) throws CoreException {
    long start = System.currentTimeMillis();

    log.debug("Reading {} Maven project(s): {}", pomFiles.size(), pomFiles); //$NON-NLS-1$

    List<ProjectBuildingResult> projectBuildingResults = null;
    Map<File, MavenExecutionResult> result = new LinkedHashMap<>(pomFiles.size(), 1.f);
    try {
      configuration.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
      projectBuildingResults = lookup(ProjectBuilder.class).build(new ArrayList<>(pomFiles), false, configuration);
    } catch(ProjectBuildingException ex) {
      if(ex.getResults() != null) {
        projectBuildingResults = ex.getResults();
      }
    } finally {
      log.debug("Read {} Maven project(s) in {} ms", pomFiles.size(), System.currentTimeMillis() - start); //$NON-NLS-1$
    }
    if(projectBuildingResults != null) {
      for(ProjectBuildingResult projectBuildingResult : projectBuildingResults) {
        MavenExecutionResult mavenExecutionResult = new DefaultMavenExecutionResult();
        mavenExecutionResult.setProject(projectBuildingResult.getProject());
        mavenExecutionResult.setDependencyResolutionResult(projectBuildingResult.getDependencyResolutionResult());
        if(!projectBuildingResult.getProblems().isEmpty()) {
          mavenExecutionResult
              .addException(new ProjectBuildingException(Collections.singletonList(projectBuildingResult)));
        }
        result.put(projectBuildingResult.getPomFile(), mavenExecutionResult);
      }
    }
    return result;
  }

  @Deprecated
  @Override
  public void detachFromSession(MavenProject project) {
    //noop now
  }

  private MavenProject resolveParentProject(RepositorySystemSession repositorySession, MavenProject child,
      ProjectBuildingRequest configuration) throws CoreException {
    configuration.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
    configuration.setRepositorySession(repositorySession);

    try {
      configuration.setRemoteRepositories(child.getRemoteArtifactRepositories());

      File parentFile = child.getParentFile();
      if(parentFile == null && child.getParent() != null) { // workaround MNG-6723
        parentFile = child.getParent().getFile();
      }
      if(parentFile != null) {
        return lookup(ProjectBuilder.class).build(parentFile, configuration).getProject();
      }

      Artifact parentArtifact = child.getParentArtifact();
      if(parentArtifact != null) {
        MavenProject parent = lookup(ProjectBuilder.class).build(parentArtifact, configuration).getProject();
        parentFile = parentArtifact.getFile(); // file is resolved as side-effect of the prior call
        // compensate for apparent bug in maven 3.0.4 which does not set parent.file and parent.artifact.file
        if(parent.getFile() == null) {
          parent.setFile(parentFile);
        }
        if(parent.getArtifact().getFile() == null) {
          parent.getArtifact().setFile(parentFile);
        }
        return parent;
      }
    } catch(ProjectBuildingException ex) {
      log.error("Could not read parent project", ex);
    }

    return null;
  }

  public MavenProject resolveParentProject(MavenProject child, IProgressMonitor monitor) throws CoreException {
    return getExecutionContext().execute(child, (context, pm) -> resolveParentProject(context.getRepositorySession(),
        child, context.getExecutionRequest().getProjectBuildingRequest()), monitor);
  }

  @Override
  public Artifact resolve(String groupId, String artifactId, String version, String type, String classifier,
      List<ArtifactRepository> remoteRepositories, IProgressMonitor monitor) throws CoreException {
    Artifact artifact = lookup(RepositorySystem.class).createArtifactWithClassifier(groupId, artifactId, version, type,
        classifier);

    return resolve(artifact, remoteRepositories, monitor);
  }

  public Artifact resolve(Artifact artifact, List<ArtifactRepository> remoteRepositories, IProgressMonitor monitor)
      throws CoreException {
    if(remoteRepositories == null) {
      try {
        remoteRepositories = getArtifactRepositories();
      } catch(CoreException e) {
        // we've tried
        remoteRepositories = Collections.emptyList();
      }
    }
    List<ArtifactRepository> repositories = remoteRepositories;

    return getExecutionContext().execute((context, pm) -> {
      org.eclipse.aether.RepositorySystem repoSystem = lookup(org.eclipse.aether.RepositorySystem.class);

      ArtifactRequest request = new ArtifactRequest();
      request.setArtifact(RepositoryUtils.toArtifact(artifact));
      request.setRepositories(RepositoryUtils.toRepos(repositories));

      ArtifactResult result;
      try {
        result = repoSystem.resolveArtifact(context.getRepositorySession(), request);
      } catch(ArtifactResolutionException ex) {
        result = ex.getResults().get(0);
      }

      setLastUpdated(context.getLocalRepository(), repositories, artifact);

      if(result.isResolved()) {
        artifact.selectVersion(result.getArtifact().getVersion());
        artifact.setFile(result.getArtifact().getFile());
        artifact.setResolved(true);
      } else {
        List<IStatus> members = new ArrayList<>();
        for(Exception e : result.getExceptions()) {
          if(!(e instanceof ArtifactNotFoundException)) {
            members.add(Status.error(e.getMessage(), e));
          }
        }
        if(members.isEmpty()) {
          members.add(Status.error(NLS.bind(Messages.MavenImpl_error_missing, artifact), null));
        }
        IStatus[] newMembers = members.toArray(IStatus[]::new);
        throw new CoreException(new MultiStatus(IMavenConstants.PLUGIN_ID, -1, newMembers,
            NLS.bind(Messages.MavenImpl_error_resolve, artifact), null));
      }
      return artifact;
    }, monitor);
  }

  public Artifact resolvePluginArtifact(Plugin plugin, List<ArtifactRepository> remoteRepositories,
      IProgressMonitor monitor) throws CoreException {
    Artifact artifact = lookup(RepositorySystem.class).createPluginArtifact(plugin);
    return resolve(artifact, remoteRepositories, monitor);
  }

  @Override
  public String getArtifactPath(ArtifactRepository repository, String groupId, String artifactId, String version,
      String type, String classifier) throws CoreException {
    Artifact artifact = lookup(RepositorySystem.class).createArtifactWithClassifier(groupId, artifactId, version, type,
        classifier);
    return repository.pathOf(artifact);
  }

  void setLastUpdated(ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories,
      Artifact artifact) throws CoreException {

    Properties lastUpdated = loadLastUpdated(localRepository, artifact);

    String timestamp = Long.toString(System.currentTimeMillis());

    for(ArtifactRepository repository : remoteRepositories) {
      lastUpdated.setProperty(getLastUpdatedKey(repository, artifact), timestamp);
    }

    File lastUpdatedFile = getLastUpdatedFile(localRepository, artifact);
    lastUpdatedFile.getParentFile().mkdirs();
    try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(lastUpdatedFile))) {
      lastUpdated.store(os, null);
    } catch(IOException ex) {
      throw new CoreException(Status.error(Messages.MavenImpl_error_write_lastUpdated, ex));
    }
  }

  /**
   * This is a temporary implementation that only works for artifacts resolved using #resolve.
   */
  @Override
  public boolean isUnavailable(String groupId, String artifactId, String version, String type, String classifier,
      List<ArtifactRepository> remoteRepositories) throws CoreException {
    Artifact artifact = lookup(RepositorySystem.class).createArtifactWithClassifier(groupId, artifactId, version, type,
        classifier);

    ArtifactRepository localRepository = getLocalRepository();

    File artifactFile = new File(localRepository.getBasedir(), localRepository.pathOf(artifact));

    if(artifactFile.canRead()) {
      // artifact is available locally
      return false;
    }

    if(remoteRepositories == null || remoteRepositories.isEmpty()) {
      // no remote repositories
      return true;
    }

    // now is the hard part
    Properties lastUpdated = loadLastUpdated(localRepository, artifact);

    for(ArtifactRepository repository : remoteRepositories) {
      String timestamp = lastUpdated.getProperty(getLastUpdatedKey(repository, artifact));
      if(timestamp == null) {
        // availability of the artifact from this repository has not been checked yet
        return false;
      }
    }

    // artifact is not available locally and all remote repositories have been checked in the past
    return true;
  }

  private String getLastUpdatedKey(ArtifactRepository repository, Artifact artifact) {
    StringBuilder key = new StringBuilder();

    // repository part
    key.append(repository.getId());
    if(repository.getAuthentication() != null) {
      key.append('|').append(repository.getAuthentication().getUsername());
    }
    key.append('|').append(repository.getUrl());

    // artifact part
    key.append('|').append(artifact.getClassifier());

    return key.toString();
  }

  private Properties loadLastUpdated(ArtifactRepository localRepository, Artifact artifact) throws CoreException {
    Properties lastUpdated = new Properties();
    File lastUpdatedFile = getLastUpdatedFile(localRepository, artifact);
    try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(lastUpdatedFile))) {
      lastUpdated.load(is);
    } catch(FileNotFoundException ex) {
      // that's okay
    } catch(IOException ex) {
      throw new CoreException(Status.error(Messages.MavenImpl_error_read_lastUpdated, ex));
    }
    return lastUpdated;
  }

  private File getLastUpdatedFile(ArtifactRepository localRepository, Artifact artifact) {
    return new File(localRepository.getBasedir(), basePathOf(artifact) + "/m2e-lastUpdated.properties");
  }

  private static final char PATH_SEPARATOR = '/';

  private static final char GROUP_SEPARATOR = '.';

  private String basePathOf(Artifact artifact) {
    return formatAsDirectory(artifact.getGroupId()) + PATH_SEPARATOR + artifact.getArtifactId() + PATH_SEPARATOR
        + artifact.getBaseVersion() + PATH_SEPARATOR;
  }

  private String formatAsDirectory(String directory) {
    return directory.replace(GROUP_SEPARATOR, PATH_SEPARATOR);
  }

  private <T> T getMojoParameterValue(MavenSession session, MojoExecution mojoExecution, List<String> parameterPath,
      Class<T> asType) throws CoreException {
    Xpp3Dom dom = mojoExecution.getConfiguration();
    if(dom == null) {
      return null;
    }
    PlexusConfiguration configuration = new XmlPlexusConfiguration(dom);
    for(String parameter : parameterPath) {
      configuration = configuration.getChild(parameter);
      if(configuration == null) {
        return null;
      }
    }
    return getMojoParameterValue(session, mojoExecution, configuration, asType, String.join("/", parameterPath));
  }

  private <T> T getMojoParameterValue(MavenSession session, MojoExecution mojoExecution,
      PlexusConfiguration configuration, Class<T> asType, String parameterLabel) throws CoreException {
    try {
      MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

      ClassRealm pluginRealm = lookup(BuildPluginManager.class).getPluginRealm(session,
          mojoDescriptor.getPluginDescriptor());

      ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator(session, mojoExecution);
      ConfigurationConverter typeConverter = converterLookup.lookupConverterForType(asType);

      Object value = typeConverter.fromConfiguration(converterLookup, configuration, asType,
          mojoDescriptor.getImplementationClass(), pluginRealm, expressionEvaluator, null);
      return asType.cast(value);
    } catch(Exception e) {
      throw new CoreException(Status.error(
          NLS.bind(Messages.MavenImpl_error_param_for_execution, parameterLabel, mojoExecution.getExecutionId()), e));
    }
  }

  @Override
  public <T> T getMojoParameterValue(MavenProject project, MojoExecution mojoExecution, String parameter,
      Class<T> asType, IProgressMonitor monitor) throws CoreException {
    return getExecutionContext().execute(project,
        (context, pm) -> getMojoParameterValue(context.getSession(), mojoExecution, List.of(parameter), asType),
        monitor);
  }

  /**
   * Resolves a nested configuration parameter from the given {@code mojoExecution}. It coerces from String to the given
   * type and considers expressions and default values. Deliberately no public API yet as probably refactored in the
   * near future.
   *
   * @param <T>
   * @param project the Maven project
   * @param mojoExecution the mojo execution from which to retrieve the configuration value
   * @param parameterPath the path of the parameter to look up, the first item is the name of the element directly below
   *          {@code <configuration>} and the last one is the element containing the actual value
   * @param asType the type to coerce to
   * @param monitor the progress monitor
   * @return the parameter value or {@code null} if the parameter with the given name was not found
   * @throws CoreException
   * @see IMaven#getMojoParameterValue(MavenProject, MojoExecution, String, Class, IProgressMonitor)
   */
  public <T> T getMojoParameterValue(MavenProject project, MojoExecution mojoExecution, List<String> parameterPath,
      Class<T> asType, IProgressMonitor monitor) throws CoreException {
    return getExecutionContext().execute(project,
        (context, pm) -> getMojoParameterValue(context.getSession(), mojoExecution, parameterPath, asType), monitor);
  }

  private <T> T getMojoParameterValue(String parameter, Class<T> type, MavenSession session, Plugin plugin,
      ConfigurationContainer configuration, String goal) throws CoreException {
    Xpp3Dom config = (Xpp3Dom) configuration.getConfiguration();
    config = (config != null) ? config.getChild(parameter) : null;

    PlexusConfiguration paramConfig = null;

    if(config == null) {
      MojoDescriptor mojoDescriptor;

      try {
        mojoDescriptor = lookup(BuildPluginManager.class).getMojoDescriptor(plugin, goal,
            session.getCurrentProject().getRemotePluginRepositories(), session.getRepositorySession());
      } catch(PluginNotFoundException | PluginResolutionException | PluginDescriptorParsingException
          | MojoNotFoundException | InvalidPluginDescriptorException ex) {
        throw new CoreException(Status.error(Messages.MavenImpl_error_param, ex));
      }

      PlexusConfiguration defaultConfig = mojoDescriptor.getMojoConfiguration();
      if(defaultConfig != null) {
        paramConfig = defaultConfig.getChild(parameter, false);
      }
    } else {
      paramConfig = new XmlPlexusConfiguration(config);
    }

    if(paramConfig == null) {
      return null;
    }

    try {
      MojoExecution mojoExecution = new MojoExecution(plugin, goal, "default"); //$NON-NLS-1$

      ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator(session, mojoExecution);

      ConfigurationConverter typeConverter = converterLookup.lookupConverterForType(type);

      Object value = typeConverter.fromConfiguration(converterLookup, paramConfig, type, Object.class,
          getPlexusContainer().getContainerRealm(), expressionEvaluator, null);
      return type.cast(value);
    } catch(ComponentConfigurationException | ClassCastException ex) {
      throw new CoreException(Status.error(Messages.MavenImpl_error_param, ex));
    }
  }

  @Override
  public <T> T getMojoParameterValue(MavenProject project, String parameter, Class<T> type, Plugin plugin,
      ConfigurationContainer configuration, String goal, IProgressMonitor monitor) throws CoreException {
    return getExecutionContext().execute(project,
        (context, pm) -> getMojoParameterValue(parameter, type, context.getSession(), plugin, configuration, goal),
        monitor);
  }

  @Override
  public ArtifactRepository createArtifactRepository(String id, String url) throws CoreException {
    Repository repository = new Repository();
    repository.setId(id);
    repository.setUrl(url);
    repository.setLayout("default"); //$NON-NLS-1$

    ArtifactRepository repo;
    try {
      repo = lookup(RepositorySystem.class).buildArtifactRepository(repository);
      List<ArtifactRepository> repos = new ArrayList<>(Arrays.asList(repo));
      injectSettings(repos);
    } catch(InvalidRepositoryException ex) {
      throw new CoreException(Status.error(Messages.MavenImpl_error_create_repo, ex));
    }
    return repo;
  }

  @Override
  public List<ArtifactRepository> getArtifactRepositories() throws CoreException {
    return getArtifactRepositories(true);
  }

  @Override
  public List<ArtifactRepository> getArtifactRepositories(boolean injectSettings) throws CoreException {
    List<ArtifactRepository> repositories = new ArrayList<>();
    for(Profile profile : getActiveProfiles()) {
      addArtifactRepositories(repositories, profile.getRepositories());
    }

    addDefaultRepository(repositories);

    if(injectSettings) {
      injectSettings(repositories);
    }

    return removeDuplicateRepositories(repositories);
  }

  private List<ArtifactRepository> removeDuplicateRepositories(List<ArtifactRepository> repositories) {
    List<ArtifactRepository> result = new ArrayList<>();

    Set<String> keys = new HashSet<>();
    for(ArtifactRepository repository : repositories) {
      StringBuilder key = new StringBuilder();
      if(repository.getId() != null) {
        key.append(repository.getId());
      }
      key.append(':').append(repository.getUrl()).append(':');
      if(repository.getAuthentication() != null && repository.getAuthentication().getUsername() != null) {
        key.append(repository.getAuthentication().getUsername());
      }
      if(keys.add(key.toString())) {
        result.add(repository);
      }
    }
    return result;
  }

  private void injectSettings(List<ArtifactRepository> repositories) throws CoreException {
    Settings setting = getSettings();
    RepositorySystem repositorySystem = lookup(RepositorySystem.class);
    repositorySystem.injectMirror(repositories, getMirrors());
    repositorySystem.injectProxy(repositories, setting.getProxies());
    repositorySystem.injectAuthentication(repositories, setting.getServers());
  }

  private void addDefaultRepository(List<ArtifactRepository> repositories) throws CoreException {
    if(repositories.stream().noneMatch(r -> RepositorySystem.DEFAULT_REMOTE_REPO_ID.equals(r.getId()))) {
      try {
        repositories.add(0, lookup(RepositorySystem.class).createDefaultRemoteRepository());
      } catch(InvalidRepositoryException ex) {
        log.error("Unexpected exception", ex);
      }
    }
  }

  private void addArtifactRepositories(List<ArtifactRepository> artifactRepositories, List<Repository> repositories)
      throws CoreException {
    for(Repository repository : repositories) {
      try {
        ArtifactRepository artifactRepository = lookup(RepositorySystem.class).buildArtifactRepository(repository);
        artifactRepositories.add(artifactRepository);
      } catch(InvalidRepositoryException ex) {
        throw new CoreException(Status.error(Messages.MavenImpl_error_read_settings, ex));
      }
    }
  }

  private List<Profile> getActiveProfiles() throws CoreException {
    Settings setting = getSettings();
    List<String> activeProfilesIds = setting.getActiveProfiles();
    List<Profile> activeProfiles = new ArrayList<>();
    for(org.apache.maven.settings.Profile settingsProfile : setting.getProfiles()) {
      if((settingsProfile.getActivation() != null && settingsProfile.getActivation().isActiveByDefault())
          || activeProfilesIds.contains(settingsProfile.getId())) {
        Profile profile = SettingsUtils.convertFromSettingsProfile(settingsProfile);
        activeProfiles.add(profile);
      }
    }
    return activeProfiles;
  }

  @Override
  public List<ArtifactRepository> getPluginArtifactRepositories() throws CoreException {
    return getPluginArtifactRepositories(true);
  }

  @Override
  public List<ArtifactRepository> getPluginArtifactRepositories(boolean injectSettings) throws CoreException {
    List<ArtifactRepository> repositories = new ArrayList<>();
    for(Profile profile : getActiveProfiles()) {
      addArtifactRepositories(repositories, profile.getPluginRepositories());
    }
    addDefaultRepository(repositories);

    if(injectSettings) {
      injectSettings(repositories);
    }

    return removeDuplicateRepositories(repositories);
  }

  @Override
  public Mirror getMirror(ArtifactRepository repo) throws CoreException {
    return getExecutionContext()
        .execute((c, m) -> lookup(RepositorySystem.class).getMirror(repo, c.getExecutionRequest().getMirrors()), null);
  }

  @Override
  public List<Mirror> getMirrors() throws CoreException {
    return getExecutionContext().execute((c, m) -> c.getExecutionRequest().getMirrors(), null);
  }

  @Override
  public void addLocalRepositoryListener(ILocalRepositoryListener listener) {
    localRepositoryListeners.add(listener);
  }

  @Override
  public void removeLocalRepositoryListener(ILocalRepositoryListener listener) {
    localRepositoryListeners.remove(listener);
  }

  public List<ILocalRepositoryListener> getLocalRepositoryListeners() {
    return localRepositoryListeners;
  }

  public PlexusContainer getPlexusContainer() throws CoreException {
    try {
      return containerManager.aquire().getContainer();
    } catch(Exception ex) {
      throw new CoreException(Status.error(Messages.MavenImpl_error_init_maven, ex));
    }
  }

  @Override
  public ProxyInfo getProxyInfo(String protocol) throws CoreException {
    for(Proxy proxy : getSettings().getProxies()) {
      if(proxy.isActive() && protocol.equalsIgnoreCase(proxy.getProtocol())) {
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setType(proxy.getProtocol());
        proxyInfo.setHost(proxy.getHost());
        proxyInfo.setPort(proxy.getPort());
        proxyInfo.setNonProxyHosts(proxy.getNonProxyHosts());
        proxyInfo.setUserName(proxy.getUsername());
        proxyInfo.setPassword(proxy.getPassword());
        return proxyInfo;
      }
    }

    return null;
  }

  @Override
  public List<MavenProject> getSortedProjects(List<MavenProject> projects) throws CoreException {
    try {
      ProjectSorter rm = new ProjectSorter(projects);
      return rm.getSortedProjects();
    } catch(CycleDetectedException | DuplicateProjectException ex) {
      throw new CoreException(Status.error(Messages.MavenImpl_error_sort, ex));
    }
  }

  @Override
  public String resolvePluginVersion(String groupId, String artifactId, MavenSession session) throws CoreException {
    Plugin plugin = new Plugin();
    plugin.setGroupId(groupId);
    plugin.setArtifactId(artifactId);
    PluginVersionRequest request = new DefaultPluginVersionRequest(plugin, session);
    try {
      return lookup(PluginVersionResolver.class).resolve(request).getVersion();
    } catch(PluginVersionResolutionException ex) {
      throw new CoreException(Status.error(ex.getMessage(), ex));
    }
  }

  @Override
  public <T> T lookup(Class<T> clazz) throws CoreException {
    if(clazz == PlexusContainerManager.class) {
      return clazz.cast(containerManager);
    }
    if(clazz == PlexusContainer.class) {
      return clazz.cast(getPlexusContainer());
    }
    ClassLoader ccl = Thread.currentThread().getContextClassLoader();
    try {
      PlexusContainer plexusContainer = getPlexusContainer();
      Thread.currentThread().setContextClassLoader(plexusContainer.getContainerRealm());
      return plexusContainer.lookup(clazz);
    } catch(ComponentLookupException ex) {
      throw new CoreException(Status.error(Messages.MavenImpl_error_lookup, ex));
    } finally {
      Thread.currentThread().setContextClassLoader(ccl);
    }
  }

  @Override
  public <C> Collection<C> lookupCollection(Class<C> type) throws CoreException {
    ClassLoader ccl = Thread.currentThread().getContextClassLoader();
    try {
      PlexusContainer plexusContainer = getPlexusContainer();
      Thread.currentThread().setContextClassLoader(plexusContainer.getContainerRealm());
      return plexusContainer.lookupList(type);
    } catch(ComponentLookupException ex) {
      return List.of();
    } finally {
      Thread.currentThread().setContextClassLoader(ccl);
    }
  }

  @Override
  public ClassLoader getProjectRealm(MavenProject project) {
    Objects.requireNonNull(project);
    ClassLoader classLoader = project.getClassRealm();
    if(classLoader == null) {
      try {
        return containerManager.aquire(project.getBasedir()).getContainer().getContainerRealm();
      } catch(Exception ex) {
        throw new RuntimeException(ex);
      }
    }
    return classLoader;
  }

  public void interpolateModel(MavenProject project, Model model) throws CoreException {
    ModelBuildingRequest request = new DefaultModelBuildingRequest();
    request.setUserProperties(project.getProperties());
    lookup(ModelInterpolator.class).interpolateModel(model, project.getBasedir(), request, req -> {
    });
  }

  /**
   * This is convenience method fully equivalent to
   *
   * <pre>
   * IMavenExecutionContext context = createExecutionContext();
   * context.getExecutionRequest().setOffline(offline);
   * context.getExecutionRequest().setUpdateSnapshots(forceDependencyUpdate);
   * return context.execute(callable, monitor);
   * </pre>
   *
   * @since 1.4
   */
  public static <V> V execute(IMaven maven, boolean offline, boolean forceDependencyUpdate, ICallable<V> callable,
      IProgressMonitor monitor) throws CoreException {
    IMavenExecutionContext context = maven.createExecutionContext();
    context.getExecutionRequest().setOffline(offline);
    context.getExecutionRequest().setUpdateSnapshots(forceDependencyUpdate);
    return context.execute(callable, monitor);
  }

  @Override
  public MavenExecutionContext createExecutionContext() {
    //the global context do not has a basedir nor a project supplier...
    return new MavenExecutionContext(this, null, null);
  }

  private static final class MavenSettings {
    private long userSettingsLength;

    private long userSettingsTimestamp;

    private long globalSettingsLength;

    private long globalSettingsTimestamp;

    private MavenSettingsLocations locations;

    private Settings settings;

    private IComponentLookup lookup;

    public MavenSettings(MavenSettingsLocations locations, IComponentLookup lookup) {
      this.locations = locations;
      this.lookup = lookup;
    }

    private synchronized Settings getSettings() throws CoreException {
      File global = locations.globalSettings();
      long gs;
      long gt;
      if(global != null && global.isFile()) {
        gs = global.length();
        gt = global.lastModified();
      } else {
        gs = -1;
        gt = -1;
      }
      File user = locations.userSettings();
      long us;
      long ut;
      if(user != null && user.isFile()) {
        us = user.length();
        ut = user.lastModified();
      } else {
        us = -1;
        ut = -1;
      }
      boolean reload = settings == null || gs != globalSettingsLength || gt != globalSettingsTimestamp
          || us != userSettingsLength || ut != userSettingsTimestamp;
      if(reload) {
        SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        Properties systemProperties = new Properties();
        M2EUtils.copyProperties(systemProperties, System.getProperties());
        request.setSystemProperties(systemProperties);
        if(global != null) {
          request.setGlobalSettingsFile(global);
        }
        if(user != null) {
          request.setUserSettingsFile(user);
        }
        try {
          settings = lookup.lookup(SettingsBuilder.class).build(request).getEffectiveSettings();
        } catch(SettingsBuildingException ex) {
          log.error("Could not read settingsCacheMap.xml, assuming default values", ex);
          /*
           * NOTE: This method provides input for various other core functions, just bailing out would make m2e highly
           * unusable. Instead, we fail gracefully and just ignore the broken settingsCacheMap, using defaults.
           */
          settings = new Settings();
        }
        globalSettingsLength = gs;
        globalSettingsTimestamp = gt;
        userSettingsLength = us;
        userSettingsTimestamp = ut;
      }
      return settings;
    }
  }

}
