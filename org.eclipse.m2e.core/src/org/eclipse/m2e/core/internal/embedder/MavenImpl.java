/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.internal.embedder;

import static org.eclipse.m2e.core.internal.M2EUtils.copyProperties;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import org.eclipse.aether.ConfigurationProperties;
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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.converters.ConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.converters.lookup.DefaultConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.IOUtil;
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
import org.apache.maven.extension.internal.CoreExports;
import org.apache.maven.extension.internal.CoreExtensionEntry;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.internal.DependencyContext;
import org.apache.maven.lifecycle.internal.LifecycleExecutionPlanCalculator;
import org.apache.maven.lifecycle.internal.MojoExecutor;
import org.apache.maven.model.ConfigurationContainer;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.interpolation.ModelInterpolator;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginContainerException;
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
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.NoSuchComponentException;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;


public class MavenImpl implements IMaven, IMavenConfigurationChangeListener {
  private static final Logger log = LoggerFactory.getLogger(MavenImpl.class);

  /**
   * Id of maven core class realm
   */
  public static final String MAVEN_CORE_REALM_ID = "plexus.core"; //$NON-NLS-1$

  private DefaultPlexusContainer plexus;

  private final IMavenConfiguration mavenConfiguration;

  private final ConverterLookup converterLookup = new DefaultConverterLookup();

  private final ArrayList<ISettingsChangeListener> settingsListeners = new ArrayList<ISettingsChangeListener>();

  private final ArrayList<ILocalRepositoryListener> localRepositoryListeners = new ArrayList<ILocalRepositoryListener>();

  /**
   * Cached parsed settings.xml instance
   */
  private Settings settings;

  /** File length of cached user settings */
  private long settings_length;

  /** Last modified timestamp of cached user settings */
  private long settings_timestamp;

  public MavenImpl(IMavenConfiguration mavenConfiguration) {
    this.mavenConfiguration = mavenConfiguration;
    mavenConfiguration.addConfigurationChangeListener(this);
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  public MavenExecutionRequest createExecutionRequest(IProgressMonitor monitor) throws CoreException {
    MavenExecutionRequest request = createExecutionRequest();

    // logging
    request.setTransferListener(createArtifactTransferListener(monitor));

    return request;
  }

  /*package*/MavenExecutionRequest createExecutionRequest() throws CoreException {
    MavenExecutionRequest request = new DefaultMavenExecutionRequest();

    // this causes problems with unexpected "stale project configuration" error markers
    // need to think how to manage ${maven.build.timestamp} properly inside workspace
    //request.setStartTime( new Date() );

    if(mavenConfiguration.getGlobalSettingsFile() != null) {
      request.setGlobalSettingsFile(new File(mavenConfiguration.getGlobalSettingsFile()));
    }
    File userSettingsFile = SettingsXmlConfigurationProcessor.DEFAULT_USER_SETTINGS_FILE;
    if(mavenConfiguration.getUserSettingsFile() != null) {
      userSettingsFile = new File(mavenConfiguration.getUserSettingsFile());
    }
    request.setUserSettingsFile(userSettingsFile);

    try {
      lookup(MavenExecutionRequestPopulator.class).populateFromSettings(request, getSettings());
    } catch(MavenExecutionRequestPopulationException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          Messages.MavenImpl_error_no_exec_req, ex));
    }

    ArtifactRepository localRepository = getLocalRepository();
    request.setLocalRepository(localRepository);
    request.setLocalRepositoryPath(localRepository.getBasedir());
    request.setOffline(mavenConfiguration.isOffline());
    
    request.getUserProperties().put("m2e.version", MavenPluginActivator.getVersion()); //$NON-NLS-1$
    request.getUserProperties().put(ConfigurationProperties.USER_AGENT, MavenPluginActivator.getUserAgent());

    MavenExecutionContext.populateSystemProperties(request);

    request.setCacheNotFound(true);
    request.setCacheTransferError(true);

    request.setGlobalChecksumPolicy(mavenConfiguration.getGlobalChecksumPolicy());
    // the right way to disable snapshot update
    // request.setUpdateSnapshots(false);
    return request;
  }

  public String getLocalRepositoryPath() {
    String path = null;
    try {
      Settings settings = getSettings();
      path = settings.getLocalRepository();
    } catch(CoreException ex) {
      // fall through
    }
    if(path == null) {
      path = RepositorySystem.defaultUserLocalRepository.getAbsolutePath();
    }
    return path;
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  public MavenExecutionResult execute(MavenExecutionRequest request, IProgressMonitor monitor) {
    // XXX is there a way to set per-request log level?

    MavenExecutionResult result;
    try {
      lookup(MavenExecutionRequestPopulator.class).populateDefaults(request);
      result = lookup(Maven.class).execute(request);
    } catch(MavenExecutionRequestPopulationException ex) {
      result = new DefaultMavenExecutionResult();
      result.addException(ex);
    } catch(Exception e) {
      result = new DefaultMavenExecutionResult();
      result.addException(e);
    }
    return result;
  }

  @SuppressWarnings("deprecation")
  public MavenSession createSession(MavenExecutionRequest request, MavenProject project) {
    RepositorySystemSession repoSession = createRepositorySession(request);
    MavenExecutionResult result = new DefaultMavenExecutionResult();
    MavenSession mavenSession = new MavenSession(plexus, repoSession, request, result);
    if(project != null) {
      mavenSession.setProjects(Collections.singletonList(project));
    }
    return mavenSession;
  }

  /*package*/FilterRepositorySystemSession createRepositorySession(MavenExecutionRequest request) {
    try {
      DefaultRepositorySystemSession session = (DefaultRepositorySystemSession) ((DefaultMaven) lookup(Maven.class))
          .newRepositorySession(request);
      final String updatePolicy = mavenConfiguration.getGlobalUpdatePolicy();
      return new FilterRepositorySystemSession(session, request.isUpdateSnapshots() ? null : updatePolicy);
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
      throw new IllegalStateException("Could not look up Maven embedder", ex);
    }
  }

  @SuppressWarnings("deprecation")
  public void execute(MavenSession session, MojoExecution execution, IProgressMonitor monitor) {
    Map<MavenProject, Set<Artifact>> artifacts = new HashMap<MavenProject, Set<Artifact>>();
    Map<MavenProject, MavenProjectMutableState> snapshots = new HashMap<MavenProject, MavenProjectMutableState>();
    for(MavenProject project : session.getProjects()) {
      artifacts.put(project, new LinkedHashSet<Artifact>(project.getArtifacts()));
      snapshots.put(project, MavenProjectMutableState.takeSnapshot(project));
    }
    try {
      MojoExecutor mojoExecutor = lookup(MojoExecutor.class);
      DependencyContext dependencyContext = mojoExecutor.newDependencyContext(session,
          Collections.singletonList(execution));
      mojoExecutor.ensureDependenciesAreResolved(execution.getMojoDescriptor(), session, dependencyContext);
      lookup(BuildPluginManager.class).executeMojo(session, execution);
    } catch(Exception ex) {
      session.getResult().addException(ex);
    } finally {
      for(MavenProject project : session.getProjects()) {
        project.setArtifactFilter(null);
        project.setResolvedArtifacts(null);
        project.setArtifacts(artifacts.get(project));
        MavenProjectMutableState snapshot = snapshots.get(project);
        if(snapshot != null) {
          snapshot.restore(project);
        }
      }
    }
  }

  public <T> T getConfiguredMojo(MavenSession session, MojoExecution mojoExecution, Class<T> clazz)
      throws CoreException {
    try {
      MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();
      // getPluginRealm creates plugin realm and populates pluginDescriptor.classRealm field 
      lookup(BuildPluginManager.class).getPluginRealm(session, mojoDescriptor.getPluginDescriptor());
      return clazz.cast(lookup(MavenPluginManager.class).getConfiguredMojo(Mojo.class, session, mojoExecution));
    } catch(PluginContainerException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, NLS.bind(
          Messages.MavenImpl_error_mojo, mojoExecution), ex));
    } catch(PluginConfigurationException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, NLS.bind(
          Messages.MavenImpl_error_mojo, mojoExecution), ex));
    } catch(ClassCastException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, NLS.bind(
          Messages.MavenImpl_error_mojo, mojoExecution), ex));
    } catch(PluginResolutionException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, NLS.bind(
          Messages.MavenImpl_error_mojo, mojoExecution), ex));
    } catch(PluginManagerException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, NLS.bind(
          Messages.MavenImpl_error_mojo, mojoExecution), ex));
    }
  }

  public void releaseMojo(Object mojo, MojoExecution mojoExecution) throws CoreException {
    lookup(MavenPluginManager.class).releaseMojo(mojo, mojoExecution);
  }

  @SuppressWarnings("deprecation")
  public MavenExecutionPlan calculateExecutionPlan(MavenSession session, MavenProject project, List<String> goals,
      boolean setup, IProgressMonitor monitor) throws CoreException {
    try {
      return lookup(LifecycleExecutor.class).calculateExecutionPlan(session, setup,
          goals.toArray(new String[goals.size()]));
    } catch(Exception ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, NLS.bind(
          Messages.MavenImpl_error_calc_build_plan, ex.getMessage()), ex));
    }
  }

  public MavenExecutionPlan calculateExecutionPlan(final MavenProject project, final List<String> goals,
      final boolean setup, final IProgressMonitor monitor) throws CoreException {
    return context().execute(project,
        (context, pm) -> calculateExecutionPlan(context.getSession(), project, goals, setup, pm), monitor);
  }

  @SuppressWarnings("deprecation")
  public MojoExecution setupMojoExecution(MavenSession session, MavenProject project, MojoExecution execution)
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
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, NLS.bind(
          Messages.MavenImpl_error_calc_build_plan, ex.getMessage()), ex));
    }
    return clone;
  }

  public MojoExecution setupMojoExecution(final MavenProject project, final MojoExecution execution,
      IProgressMonitor monitor) throws CoreException {
    return context().execute(project, (context, pm) -> setupMojoExecution(context.getSession(), project, execution),
        monitor);
  }

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

  public Settings getSettings() throws CoreException {
    return getSettings(false);
  }

  public synchronized Settings getSettings(final boolean force_reload) throws CoreException {
    // MUST NOT use createRequest!

    File userSettingsFile = SettingsXmlConfigurationProcessor.DEFAULT_USER_SETTINGS_FILE;
    if(mavenConfiguration.getUserSettingsFile() != null) {
      userSettingsFile = new File(mavenConfiguration.getUserSettingsFile());
    }

    boolean reload = force_reload || settings == null;

    if(!reload && userSettingsFile != null) {
      reload = userSettingsFile.lastModified() != settings_timestamp || userSettingsFile.length() != settings_length;
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
        settings_length = userSettingsFile.length();
        settings_timestamp = userSettingsFile.lastModified();
      }
    }
    return settings;
  }

  public Settings buildSettings(String globalSettings, String userSettings) throws CoreException {
    SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
    request.setGlobalSettingsFile(globalSettings != null ? new File(globalSettings) : null);
    request.setUserSettingsFile(userSettings != null ? new File(userSettings)
        : SettingsXmlConfigurationProcessor.DEFAULT_USER_SETTINGS_FILE);
    try {
      return lookup(SettingsBuilder.class).build(request).getEffectiveSettings();
    } catch(SettingsBuildingException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          Messages.MavenImpl_error_read_settings, ex));
    }
  }

  public void writeSettings(Settings settings, OutputStream out) throws CoreException {
    try {
      lookup(SettingsWriter.class).write(out, null, settings);
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          Messages.MavenImpl_error_write_settings, ex));
    }
  }

  public List<SettingsProblem> validateSettings(String settings) {
    List<SettingsProblem> problems = new ArrayList<SettingsProblem>();
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

  public void reloadSettings() throws CoreException {
    Settings settings = getSettings(true);
    for(ISettingsChangeListener listener : settingsListeners) {
      try {
        listener.settingsChanged(settings);
      } catch(CoreException e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  public Server decryptPassword(Server server) throws CoreException {
    SettingsDecryptionRequest request = new DefaultSettingsDecryptionRequest(server);
    SettingsDecryptionResult result = lookup(SettingsDecrypter.class).decrypt(request);
    for(SettingsProblem problem : result.getProblems()) {
      log.warn(problem.getMessage(), problem.getException());
    }
    return result.getServer();
  }

  public void mavenConfigurationChange(MavenConfigurationChangeEvent event) throws CoreException {
    if(MavenConfigurationChangeEvent.P_USER_SETTINGS_FILE.equals(event.getKey())
        || MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE.equals(event.getKey())) {
      reloadSettings();
    }
  }

  public Model readModel(InputStream in) throws CoreException {
    try {
      return lookup(ModelReader.class).read(in, null);
    } catch(IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          Messages.MavenImpl_error_read_pom, e));
    }
  }

  @SuppressWarnings("deprecation")
  public Model readModel(File pomFile) throws CoreException {
    try {
      BufferedInputStream is = new BufferedInputStream(new FileInputStream(pomFile));
      try {
        return readModel(is);
      } finally {
        IOUtil.close(is);
      }
    } catch(IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          Messages.MavenImpl_error_read_pom, e));
    }
  }

  public void writeModel(Model model, OutputStream out) throws CoreException {
    try {
      lookup(ModelWriter.class).write(out, null, model);
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          Messages.MavenImpl_error_write_pom, ex));
    }
  }

  public MavenProject readProject(final File pomFile, IProgressMonitor monitor) throws CoreException {
    return context().execute((context, pm) -> {
        MavenExecutionRequest request = DefaultMavenExecutionRequest.copy(context.getExecutionRequest());
        try {
          lookup(MavenExecutionRequestPopulator.class).populateDefaults(request);
          ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
          configuration.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
          configuration.setRepositorySession(createRepositorySession(request));
          return lookup(ProjectBuilder.class).build(pomFile, configuration).getProject();
        } catch(ProjectBuildingException ex) {
          throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
              Messages.MavenImpl_error_read_project, ex));
        } catch(MavenExecutionRequestPopulationException ex) {
          throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
              Messages.MavenImpl_error_read_project, ex));
        }
    }, monitor);
  }

  @SuppressWarnings("deprecation")
  public MavenExecutionResult readProject(MavenExecutionRequest request, IProgressMonitor monitor) throws CoreException {
    final RepositorySystemSession repositorySession = createRepositorySession(request);
    return readMavenProject(request.getPom(), repositorySession, request);
  }

  @Deprecated
  /*package*/MavenExecutionResult readMavenProject(File pomFile, RepositorySystemSession repositorySession,
      MavenExecutionRequest request) throws CoreException {
    ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
    configuration.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
    configuration.setRepositorySession(repositorySession);
    return readMavenProject(pomFile, configuration);
  }

  public MavenExecutionResult readMavenProject(File pomFile, ProjectBuildingRequest configuration) throws CoreException {
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

  public Map<File, MavenExecutionResult> readMavenProjects(Collection<File> pomFiles,
      ProjectBuildingRequest configuration)
      throws CoreException {
    long start = System.currentTimeMillis();

    log.debug("Reading {} Maven project(s): {}", pomFiles.size(), pomFiles.toString()); //$NON-NLS-1$

    List<ProjectBuildingResult> projectBuildingResults = null;
    Map<File, MavenExecutionResult> result = new LinkedHashMap<>(pomFiles.size(), 1.f);
    try {
      configuration.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
      projectBuildingResults = lookup(ProjectBuilder.class).build(new ArrayList<>(pomFiles),
          false,
          configuration);
    } catch(ProjectBuildingException ex) {
      if(ex.getResults() != null) {
        projectBuildingResults = ex.getResults();
      }
    } finally {
      log.debug("Read {} Maven project(s) in {} ms",pomFiles.size(),System.currentTimeMillis()-start); //$NON-NLS-1$
    }
    if(projectBuildingResults != null) {
      for (ProjectBuildingResult projectBuildingResult : projectBuildingResults) {
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
  public void detachFromSession(MavenProject project) throws CoreException {
    project.getProjectBuildingRequest().setRepositorySession(lookup(ContextRepositorySystemSession.class));
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  public MavenProject resolveParentProject(MavenExecutionRequest request, MavenProject child, IProgressMonitor monitor)
      throws CoreException {
    final ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
    final RepositorySystemSession repositorySession = createRepositorySession(request);
    return resolveParentProject(repositorySession, child, configuration);
  }

  /*package*/MavenProject resolveParentProject(RepositorySystemSession repositorySession, MavenProject child,
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

  public MavenProject resolveParentProject(final MavenProject child, IProgressMonitor monitor) throws CoreException {
    return context().execute(child, (context, pm) -> resolveParentProject(context.getRepositorySession(), child,
        context.getExecutionRequest().getProjectBuildingRequest()), monitor);
  }

  public Artifact resolve(String groupId, String artifactId, String version, String type, String classifier,
      List<ArtifactRepository> remoteRepositories, IProgressMonitor monitor) throws CoreException {
    Artifact artifact = lookup(RepositorySystem.class).createArtifactWithClassifier(groupId, artifactId, version, type,
        classifier);

    return resolve(artifact, remoteRepositories, monitor);
  }

  private IMavenExecutionContext context() {
    MavenExecutionContext context = MavenExecutionContext.getThreadContext();
    if(context == null) {
      context = new MavenExecutionContext(this);
    }
    return context;
  }

  public Artifact resolve(final Artifact artifact, List<ArtifactRepository> remoteRepositories, IProgressMonitor monitor)
      throws CoreException {
    if(remoteRepositories == null) {
      try {
        remoteRepositories = getArtifactRepositories();
      } catch(CoreException e) {
        // we've tried
        remoteRepositories = Collections.emptyList();
      }
    }
    final List<ArtifactRepository> _remoteRepositories = remoteRepositories;

    return context().execute((context, pm) -> {
        org.eclipse.aether.RepositorySystem repoSystem = lookup(org.eclipse.aether.RepositorySystem.class);

        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(RepositoryUtils.toArtifact(artifact));
        request.setRepositories(RepositoryUtils.toRepos(_remoteRepositories));

        ArtifactResult result;
        try {
          result = repoSystem.resolveArtifact(context.getRepositorySession(), request);
        } catch(ArtifactResolutionException ex) {
          result = ex.getResults().get(0);
        }

        setLastUpdated(context.getLocalRepository(), _remoteRepositories, artifact);

        if(result.isResolved()) {
          artifact.selectVersion(result.getArtifact().getVersion());
          artifact.setFile(result.getArtifact().getFile());
          artifact.setResolved(true);
        } else {
          ArrayList<IStatus> members = new ArrayList<IStatus>();
          for(Exception e : result.getExceptions()) {
            if(!(e instanceof ArtifactNotFoundException)) {
              members.add(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, e.getMessage(), e));
            }
          }
          if(members.isEmpty()) {
            members.add(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, NLS.bind(
                Messages.MavenImpl_error_missing, artifact), null));
          }
          IStatus[] newMembers = members.toArray(new IStatus[members.size()]);
          throw new CoreException(new MultiStatus(IMavenConstants.PLUGIN_ID, -1, newMembers, NLS.bind(
              Messages.MavenImpl_error_resolve, artifact.toString()), null));
        }

        return artifact;
      }, monitor);
  }

  public Artifact resolvePluginArtifact(Plugin plugin, List<ArtifactRepository> remoteRepositories,
      IProgressMonitor monitor) throws CoreException {
    Artifact artifact = lookup(RepositorySystem.class).createPluginArtifact(plugin);
    return resolve(artifact, remoteRepositories, monitor);
  }

  public String getArtifactPath(ArtifactRepository repository, String groupId, String artifactId, String version,
      String type, String classifier) throws CoreException {
    Artifact artifact = lookup(RepositorySystem.class).createArtifactWithClassifier(groupId, artifactId, version, type,
        classifier);
    return repository.pathOf(artifact);
  }

  /*package*/void setLastUpdated(ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories,
      Artifact artifact) throws CoreException {

    Properties lastUpdated = loadLastUpdated(localRepository, artifact);

    String timestamp = Long.toString(System.currentTimeMillis());

    for(ArtifactRepository repository : remoteRepositories) {
      lastUpdated.setProperty(getLastUpdatedKey(repository, artifact), timestamp);
    }

    File lastUpdatedFile = getLastUpdatedFile(localRepository, artifact);
    try {
      lastUpdatedFile.getParentFile().mkdirs();
      BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(lastUpdatedFile));
      try {
        lastUpdated.store(os, null);
      } finally {
        IOUtil.close(os);
      }
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          Messages.MavenImpl_error_write_lastUpdated, ex));
    }
  }

  /**
   * This is a temporary implementation that only works for artifacts resolved using #resolve.
   */
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
    try {
      BufferedInputStream is = new BufferedInputStream(new FileInputStream(lastUpdatedFile));
      try {
        lastUpdated.load(is);
      } finally {
        IOUtil.close(is);
      }
    } catch(FileNotFoundException ex) {
      // that's okay
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          Messages.MavenImpl_error_read_lastUpdated, ex));
    }
    return lastUpdated;
  }

  private File getLastUpdatedFile(ArtifactRepository localRepository, Artifact artifact) {
    return new File(localRepository.getBasedir(), basePathOf(localRepository, artifact) + "/" //$NON-NLS-1$
        + "m2e-lastUpdated.properties"); //$NON-NLS-1$
  }

  private static final char PATH_SEPARATOR = '/';

  private static final char GROUP_SEPARATOR = '.';

  private String basePathOf(ArtifactRepository repository, Artifact artifact) {
    StringBuilder path = new StringBuilder(128);

    path.append(formatAsDirectory(artifact.getGroupId())).append(PATH_SEPARATOR);
    path.append(artifact.getArtifactId()).append(PATH_SEPARATOR);
    path.append(artifact.getBaseVersion()).append(PATH_SEPARATOR);

    return path.toString();
  }

  private String formatAsDirectory(String directory) {
    return directory.replace(GROUP_SEPARATOR, PATH_SEPARATOR);
  }

  @SuppressWarnings("deprecation")
  public <T> T getMojoParameterValue(MavenSession session, MojoExecution mojoExecution, String parameter,
      Class<T> asType) throws CoreException {
    try {
      MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

      ClassRealm pluginRealm = lookup(BuildPluginManager.class).getPluginRealm(session,
          mojoDescriptor.getPluginDescriptor());

      ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator(session, mojoExecution);

      ConfigurationConverter typeConverter = converterLookup.lookupConverterForType(asType);

      Xpp3Dom dom = mojoExecution.getConfiguration();

      if(dom == null) {
        return null;
      }

      PlexusConfiguration pomConfiguration = new XmlPlexusConfiguration(dom);

      PlexusConfiguration configuration = pomConfiguration.getChild(parameter);

      if(configuration == null) {
        return null;
      }

      Object value = typeConverter.fromConfiguration(converterLookup, configuration, asType,
          mojoDescriptor.getImplementationClass(), pluginRealm, expressionEvaluator, null);
      return asType.cast(value);
    } catch(Exception e) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, NLS.bind(
          Messages.MavenImpl_error_param_for_execution, parameter, mojoExecution.getExecutionId()), e));
    }
  }

  public <T> T getMojoParameterValue(final MavenProject project, final MojoExecution mojoExecution,
      final String parameter, final Class<T> asType, final IProgressMonitor monitor) throws CoreException {
    return context().execute(project,
        (context, pm) -> getMojoParameterValue(context.getSession(), mojoExecution, parameter, asType), monitor);
  }

  @SuppressWarnings("deprecation")
  public <T> T getMojoParameterValue(String parameter, Class<T> type, MavenSession session, Plugin plugin,
      ConfigurationContainer configuration, String goal) throws CoreException {
    Xpp3Dom config = (Xpp3Dom) configuration.getConfiguration();
    config = (config != null) ? config.getChild(parameter) : null;

    PlexusConfiguration paramConfig = null;

    if(config == null) {
      MojoDescriptor mojoDescriptor;

      try {
        mojoDescriptor = lookup(BuildPluginManager.class).getMojoDescriptor(plugin, goal,
            session.getCurrentProject().getRemotePluginRepositories(), session.getRepositorySession());
      } catch(PluginNotFoundException ex) {
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
            Messages.MavenImpl_error_param, ex));
      } catch(PluginResolutionException ex) {
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
            Messages.MavenImpl_error_param, ex));
      } catch(PluginDescriptorParsingException ex) {
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
            Messages.MavenImpl_error_param, ex));
      } catch(MojoNotFoundException ex) {
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
            Messages.MavenImpl_error_param, ex));
      } catch(InvalidPluginDescriptorException ex) {
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
            Messages.MavenImpl_error_param, ex));
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
          plexus.getContainerRealm(), expressionEvaluator, null);
      return type.cast(value);
    } catch(ComponentConfigurationException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, Messages.MavenImpl_error_param,
          ex));
    } catch(ClassCastException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, Messages.MavenImpl_error_param,
          ex));
    }
  }

  public <T> T getMojoParameterValue(final MavenProject project, final String parameter, final Class<T> type,
      final Plugin plugin, final ConfigurationContainer configuration, final String goal, IProgressMonitor monitor)
      throws CoreException {
    return context().execute(project,
        (context, pm) -> getMojoParameterValue(parameter, type, context.getSession(), plugin, configuration, goal),
        monitor);
  }

  public ArtifactRepository createArtifactRepository(String id, String url) throws CoreException {
    Repository repository = new Repository();
    repository.setId(id);
    repository.setUrl(url);
    repository.setLayout("default"); //$NON-NLS-1$

    ArtifactRepository repo;
    try {
      repo = lookup(RepositorySystem.class).buildArtifactRepository(repository);
      ArrayList<ArtifactRepository> repos = new ArrayList<ArtifactRepository>(Arrays.asList(repo));
      injectSettings(repos);
    } catch(InvalidRepositoryException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          Messages.MavenImpl_error_create_repo, ex));
    }
    return repo;
  }

  public List<ArtifactRepository> getArtifactRepositories() throws CoreException {
    return getArtifactRepositories(true);
  }

  public List<ArtifactRepository> getArtifactRepositories(boolean injectSettings) throws CoreException {
    ArrayList<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
    for(Profile profile : getActiveProfiles()) {
      addArtifactRepositories(repositories, profile.getRepositories());
    }

    addDefaultRepository(repositories);

    if(injectSettings) {
      injectSettings(repositories);
    }

    return removeDuplicateRepositories(repositories);
  }

  private List<ArtifactRepository> removeDuplicateRepositories(ArrayList<ArtifactRepository> repositories) {
    ArrayList<ArtifactRepository> result = new ArrayList<ArtifactRepository>();

    HashSet<String> keys = new HashSet<String>();
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

  private void injectSettings(ArrayList<ArtifactRepository> repositories) throws CoreException {
    Settings settings = getSettings();
    RepositorySystem repositorySystem = lookup(RepositorySystem.class);
    repositorySystem.injectMirror(repositories, getMirrors());
    repositorySystem.injectProxy(repositories, settings.getProxies());
    repositorySystem.injectAuthentication(repositories, settings.getServers());
  }

  private void addDefaultRepository(ArrayList<ArtifactRepository> repositories) throws CoreException {
    for(ArtifactRepository repository : repositories) {
      if(RepositorySystem.DEFAULT_REMOTE_REPO_ID.equals(repository.getId())) {
        return;
      }
    }
    try {
      repositories.add(0, lookup(RepositorySystem.class).createDefaultRemoteRepository());
    } catch(InvalidRepositoryException ex) {
      log.error("Unexpected exception", ex);
    }
  }

  private void addArtifactRepositories(ArrayList<ArtifactRepository> artifactRepositories, List<Repository> repositories)
      throws CoreException {
    for(Repository repository : repositories) {
      try {
        ArtifactRepository artifactRepository = lookup(RepositorySystem.class).buildArtifactRepository(repository);
        artifactRepositories.add(artifactRepository);
      } catch(InvalidRepositoryException ex) {
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
            Messages.MavenImpl_error_read_settings, ex));
      }
    }
  }

  private List<Profile> getActiveProfiles() throws CoreException {
    Settings settings = getSettings();
    List<String> activeProfilesIds = settings.getActiveProfiles();
    ArrayList<Profile> activeProfiles = new ArrayList<Profile>();
    for(org.apache.maven.settings.Profile settingsProfile : settings.getProfiles()) {
      if((settingsProfile.getActivation() != null && settingsProfile.getActivation().isActiveByDefault())
          || activeProfilesIds.contains(settingsProfile.getId())) {
        Profile profile = SettingsUtils.convertFromSettingsProfile(settingsProfile);
        activeProfiles.add(profile);
      }
    }
    return activeProfiles;
  }

  public List<ArtifactRepository> getPluginArtifactRepositories() throws CoreException {
    return getPluginArtifactRepositories(true);
  }

  public List<ArtifactRepository> getPluginArtifactRepositories(boolean injectSettings) throws CoreException {
    ArrayList<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
    for(Profile profile : getActiveProfiles()) {
      addArtifactRepositories(repositories, profile.getPluginRepositories());
    }
    addDefaultRepository(repositories);

    if(injectSettings) {
      injectSettings(repositories);
    }

    return removeDuplicateRepositories(repositories);
  }

  public Mirror getMirror(ArtifactRepository repo) throws CoreException {
    MavenExecutionRequest request = createExecutionRequest(new NullProgressMonitor());
    populateDefaults(request);
    return lookup(RepositorySystem.class).getMirror(repo, request.getMirrors());
  };

  public void populateDefaults(MavenExecutionRequest request) throws CoreException {
    try {
      lookup(MavenExecutionRequestPopulator.class).populateDefaults(request);
    } catch(MavenExecutionRequestPopulationException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          Messages.MavenImpl_error_read_config, ex));
    }
  }

  public List<Mirror> getMirrors() throws CoreException {
    MavenExecutionRequest request = createExecutionRequest(null);
    populateDefaults(request);
    return request.getMirrors();
  }

  public void addSettingsChangeListener(ISettingsChangeListener listener) {
    settingsListeners.add(listener);
  }

  public void removeSettingsChangeListener(ISettingsChangeListener listener) {
    settingsListeners.remove(listener);
  }

  public void addLocalRepositoryListener(ILocalRepositoryListener listener) {
    localRepositoryListeners.add(listener);
  }

  public void removeLocalRepositoryListener(ILocalRepositoryListener listener) {
    localRepositoryListeners.remove(listener);
  }

  public List<ILocalRepositoryListener> getLocalRepositoryListeners() {
    return localRepositoryListeners;
  }

  @SuppressWarnings("deprecation")
  public WagonTransferListenerAdapter createTransferListener(IProgressMonitor monitor) {
    return new WagonTransferListenerAdapter(this, monitor);
  }

  public TransferListener createArtifactTransferListener(IProgressMonitor monitor) {
    return new ArtifactTransferListenerAdapter(this, monitor);
  }

  public PlexusContainer getPlexusContainer() throws CoreException {
    try {
      return getPlexusContainer0();
    } catch(PlexusContainerException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          Messages.MavenImpl_error_init_maven, ex));
    }
  }

  private synchronized PlexusContainer getPlexusContainer0() throws PlexusContainerException {
    if(plexus == null) {
      plexus = newPlexusContainer();
      plexus.setLoggerManager(new EclipseLoggerManager(mavenConfiguration));
    }
    return plexus;
  }

  public ProxyInfo getProxyInfo(String protocol) throws CoreException {
    Settings settings = getSettings();

    for(Proxy proxy : settings.getProxies()) {
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

  public List<MavenProject> getSortedProjects(List<MavenProject> projects) throws CoreException {
    try {
      ProjectSorter rm = new ProjectSorter(projects);
      return rm.getSortedProjects();
    } catch(CycleDetectedException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, Messages.MavenImpl_error_sort, ex));
    } catch(DuplicateProjectException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, Messages.MavenImpl_error_sort, ex));
    }
  }

  public String resolvePluginVersion(String groupId, String artifactId, MavenSession session) throws CoreException {
    Plugin plugin = new Plugin();
    plugin.setGroupId(groupId);
    plugin.setArtifactId(artifactId);
    PluginVersionRequest request = new DefaultPluginVersionRequest(plugin, session);
    try {
      return lookup(PluginVersionResolver.class).resolve(request).getVersion();
    } catch(PluginVersionResolutionException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, ex.getMessage(), ex));
    }
  }

  public <T> T lookup(Class<T> clazz) throws CoreException {
    try {
      return getPlexusContainer().lookup(clazz);
    } catch(ComponentLookupException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, Messages.MavenImpl_error_lookup,
          ex));
    }
  }

  /**
   * @since 1.5
   */
  public <T> T lookupComponent(Class<T> clazz) {
    try {
      return getPlexusContainer0().lookup(clazz);
    } catch(ComponentLookupException ex) {
      throw new NoSuchComponentException(ex);
    } catch(PlexusContainerException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static DefaultPlexusContainer newPlexusContainer() throws PlexusContainerException {
    final ClassWorld classWorld = new ClassWorld(MAVEN_CORE_REALM_ID, ClassWorld.class.getClassLoader());
    final ClassRealm realm;
    try {
      realm = classWorld.getRealm(MAVEN_CORE_REALM_ID);
    } catch(NoSuchRealmException e) {
      throw new PlexusContainerException("Could not lookup required class realm", e);
    }
    final ContainerConfiguration mavenCoreCC = new DefaultContainerConfiguration() //
        .setClassWorld(classWorld) //
        .setRealm(realm) //
        .setClassPathScanning(PlexusConstants.SCANNING_INDEX) //
        .setAutoWiring(true) //
        .setName("mavenCore"); //$NON-NLS-1$

    final Module logginModule = new AbstractModule() {
      protected void configure() {
        bind(ILoggerFactory.class).toInstance(LoggerFactory.getILoggerFactory());
      }
    };
    final Module coreExportsModule = new AbstractModule() {
      protected void configure() {
        ClassRealm realm = mavenCoreCC.getRealm();
        CoreExtensionEntry entry = CoreExtensionEntry.discoverFrom(realm);
        CoreExports exports = new CoreExports(entry);
        bind(CoreExports.class).toInstance(exports);
      }
    };
    return new DefaultPlexusContainer(mavenCoreCC, logginModule, new ExtensionModule(), coreExportsModule);
  }

  public synchronized void disposeContainer() {
    if(plexus != null) {
      plexus.dispose();
    }
  }

  public ClassLoader getProjectRealm(MavenProject project) {
    ClassLoader classLoader = project.getClassRealm();
    if(classLoader == null) {
      classLoader = plexus.getContainerRealm();
    }
    return classLoader;
  }

  public void interpolateModel(MavenProject project, Model model) throws CoreException {
    ModelBuildingRequest request = new DefaultModelBuildingRequest();
    request.setUserProperties(project.getProperties());
    ModelProblemCollector problems = req -> {
    };
    lookup(ModelInterpolator.class).interpolateModel(model, project.getBasedir(), request, problems);
  }

  public <V> V execute(boolean offline, boolean forceDependencyUpdate, ICallable<V> callable, IProgressMonitor monitor)
      throws CoreException {
    IMavenExecutionContext context = createExecutionContext();
    context.getExecutionRequest().setOffline(offline);
    context.getExecutionRequest().setUpdateSnapshots(forceDependencyUpdate);
    return context.execute(callable, monitor);
  }

  public <V> V execute(ICallable<V> callable, IProgressMonitor monitor) throws CoreException {
    return context().execute(callable, monitor);
  }

  public void execute(final MavenProject project, final MojoExecution execution, final IProgressMonitor monitor)
      throws CoreException {
    context().execute(project, (context, pm) -> {
        execute(context.getSession(), execution, pm);
        return null;
    }, monitor);
  }

  public MavenExecutionContext createExecutionContext() {
    return new MavenExecutionContext(this);
  }

  public MavenExecutionContext getExecutionContext() {
    return MavenExecutionContext.getThreadContext();
  }

}
