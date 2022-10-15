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

import static org.eclipse.m2e.core.internal.M2EUtils.copyProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
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
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.eclipse.aether.transfer.TransferListener;
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

import org.apache.maven.DefaultMaven;
import org.apache.maven.Maven;
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
import org.eclipse.m2e.core.embedder.ILocalRepositoryListener;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenConfigurationChangeListener;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.embedder.ISettingsChangeListener;
import org.eclipse.m2e.core.embedder.MavenConfigurationChangeEvent;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.embedder.ConfigurationElementImpl.ValueFactory;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


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
   * Cached parsed settings.xml instance
   */
  private Settings settings;

  /** File length of cached user settings */
  private long settingsLength;

  /** Last modified timestamp of cached user settings */
  private long settingsTimestamp;

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

  FilterRepositorySystemSession createRepositorySession(MavenExecutionRequest request) {
    try {
      DefaultRepositorySystemSession session = (DefaultRepositorySystemSession) ((DefaultMaven) lookup(Maven.class))
          .newRepositorySession(request);
      String updatePolicy = getMavenConfiguration().getGlobalUpdatePolicy();
      return new FilterRepositorySystemSession(session, request.isUpdateSnapshots() ? null : updatePolicy);
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
      throw new IllegalStateException("Could not look up Maven embedder", ex);
    }
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
    return getSettings(false);
  }

  public synchronized Settings getSettings(boolean forceReload) throws CoreException {
    // MUST NOT use createRequest!

    File userSettingsFile = SettingsXmlConfigurationProcessor.DEFAULT_USER_SETTINGS_FILE;
    if(mavenConfiguration.getUserSettingsFile() != null) {
      userSettingsFile = new File(mavenConfiguration.getUserSettingsFile());
    }

    boolean reload = forceReload || settings == null;

    if(!reload && userSettingsFile != null) {
      reload = userSettingsFile.lastModified() != settingsTimestamp || userSettingsFile.length() != settingsLength;
    }

    if(reload) {
      // TODO: Can't that delegate to buildSettings()?
      SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
      // 440696 guard against ConcurrentModificationException
      Properties systemProperties = new Properties();
      copyProperties(systemProperties, System.getProperties());
      request.setSystemProperties(systemProperties);
      if(mavenConfiguration.getGlobalSettingsFile() != null) {
        request.setGlobalSettingsFile(new File(mavenConfiguration.getGlobalSettingsFile()));
      }
      if(userSettingsFile != null) {
        request.setUserSettingsFile(userSettingsFile);
      }
      try {
        settings = lookup(SettingsBuilder.class).build(request).getEffectiveSettings();
      } catch(SettingsBuildingException ex) {
        String msg = "Could not read settings.xml, assuming default values";
        log.error(msg, ex);
        /*
         * NOTE: This method provides input for various other core functions, just bailing out would make m2e highly
         * unusuable. Instead, we fail gracefully and just ignore the broken settings, using defaults.
         */
        settings = new Settings();
      }

      if(userSettingsFile != null) {
        settingsLength = userSettingsFile.length();
        settingsTimestamp = userSettingsFile.lastModified();
      }
    }
    return settings;
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
    Settings reloadedSettings = getSettings(true);
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
    if(MavenConfigurationChangeEvent.P_USER_SETTINGS_FILE.equals(event.key())
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
        configuration.setRepositorySession(createRepositorySession(request));
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

  /**
   * Makes MavenProject instances returned by #readProject methods suitable for caching and reuse with other
   * MavenSession instances.<br/>
   * Do note that MavenProject.getParentProject() cannot be used for detached MavenProject instances. Use
   * #resolveParentProject to resolve parent project instance.
   */
  @Override
  public void detachFromSession(MavenProject project) throws CoreException {
    project.getProjectBuildingRequest().setRepositorySession(lookup(ContextRepositorySystemSession.class));
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
    try (OutputStream os = new FileOutputStream(lastUpdatedFile)) {
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
    try (InputStream is = new FileInputStream(lastUpdatedFile)) {
      lastUpdated.load(is);
    } catch(FileNotFoundException ex) {
      // that's okay
    } catch(IOException ex) {
      throw new CoreException(Status.error(Messages.MavenImpl_error_read_lastUpdated, ex));
    }
    return lastUpdated;
  }

  private File getLastUpdatedFile(ArtifactRepository localRepository, Artifact artifact) {
    return Path.of(localRepository.getBasedir(), //
        artifact.getGroupId().replace('.', '/'), artifact.getArtifactId(), artifact.getBaseVersion(),
        "m2e-lastUpdated.properties").toFile();
  }

  @Override
  public IConfigurationElement getMojoConfiguration(MavenProject project, MojoExecution execution,
      IProgressMonitor monitor) throws CoreException {
    return getExecutionContext().execute(project, (context, pm) -> {
      try {
        MavenSession session = context.getSession();
        MojoDescriptor mojo = execution.getMojoDescriptor();
        ClassRealm pluginRealm = lookup(BuildPluginManager.class).getPluginRealm(session, mojo.getPluginDescriptor());
        PluginParameterExpressionEvaluator evaluator = new PluginParameterExpressionEvaluator(session, execution);
        ValueFactory valueComputer = new ValueFactory(converterLookup, mojo, pluginRealm, evaluator);

        Xpp3Dom dom = execution.getConfiguration();
        PlexusConfiguration configuration = dom != null ? new XmlPlexusConfiguration(dom)
            : new XmlPlexusConfiguration("");
        return new ConfigurationElementImpl(configuration, "", valueComputer);
      } catch(Exception e) {
        throw new CoreException(Status.error(
            NLS.bind("Could not get the configuration for for plugin execution {0}", execution.getExecutionId()), e));
      }
    }, monitor);
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

  public WagonTransferListenerAdapter createTransferListener(IProgressMonitor monitor) {
    return new WagonTransferListenerAdapter(this, monitor);
  }

  public TransferListener createArtifactTransferListener(IProgressMonitor monitor) {
    return new ArtifactTransferListenerAdapter(this, monitor);
  }

  public PlexusContainer getPlexusContainer() throws CoreException {
    try {
      return containerManager.aquire();
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
        return containerManager.aquire(project.getBasedir()).getContainerRealm();
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
    return new MavenExecutionContext(this, (IMavenProjectFacade) null);
  }

}
