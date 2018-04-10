/*************************************************************************************
 * Copyright (c) 2011-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Fred Bricon / JBoss by Red Hat - Initial implementation.
 ************************************************************************************/

package org.eclipse.m2e.profiles.core.internal.management;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.NoSuchComponentException;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.profiles.core.internal.IProfileManager;
import org.eclipse.m2e.profiles.core.internal.MavenProfilesCoreActivator;
import org.eclipse.m2e.profiles.core.internal.ProfileData;
import org.eclipse.m2e.profiles.core.internal.ProfileState;


/**
 * Maven Profile Manager
 * 
 * @author Fred Bricon
 * @since 1.5.0
 */
public class ProfileManager implements IProfileManager {

  public void updateActiveProfiles(final IMavenProjectFacade mavenProjectFacade, final List<String> profiles,
      final boolean isOffline, final boolean isForceUpdate, IProgressMonitor monitor) throws CoreException {
    if(mavenProjectFacade == null) {
      return;
    }
    final IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();

    IProject project = mavenProjectFacade.getProject();

    final ResolverConfiguration configuration = configurationManager.getResolverConfiguration(project);

    final String profilesAsString = String.join(", ", profiles);
    if(profilesAsString.equals(configuration.getSelectedProfiles())) {
      //Nothing changed
      return;
    }

    configuration.setSelectedProfiles(profilesAsString);
    boolean isSet = configurationManager.setResolverConfiguration(project, configuration);
    if(isSet) {
      MavenUpdateRequest request = new MavenUpdateRequest(project, isOffline, isForceUpdate);
      configurationManager.updateProjectConfiguration(request, monitor);
    }

  }

  public Map<Profile, Boolean> getAvailableSettingsProfiles() throws CoreException {
    Map<Profile, Boolean> settingsProfiles = new LinkedHashMap<>();
    Settings settings = MavenPlugin.getMaven().getSettings();
    List<String> activeProfiles = settings.getActiveProfiles();

    for(org.apache.maven.settings.Profile sp : settings.getProfiles()) {
      Profile p = SettingsUtils.convertFromSettingsProfile(sp);
      boolean isAutomaticallyActivated = isActive(sp, activeProfiles);
      settingsProfiles.put(p, isAutomaticallyActivated);
    }
    return Collections.unmodifiableMap(settingsProfiles);
  }

  private boolean isActive(org.apache.maven.settings.Profile p, List<String> activeProfiles) {
    if(p.getActivation() != null && p.getActivation().isActiveByDefault()) {
      return true;
    }
    return activeProfiles.stream().anyMatch(ap -> ap.equals(p.getId()));
  }

  public List<ProfileData> getProfileDatas(IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
    if(facade == null) {
      return Collections.emptyList();
    }

    ResolverConfiguration resolverConfiguration = MavenPlugin.getProjectConfigurationManager()
        .getResolverConfiguration(facade.getProject());

    List<String> configuredProfiles = toList(resolverConfiguration.getSelectedProfiles());

    MavenProject mavenProject = facade.getMavenProject(monitor);

    List<Model> modelHierarchy = new ArrayList<>();

    getModelHierarchy(modelHierarchy, mavenProject.getModel(), monitor);

    List<Profile> availableProfiles = collectAvailableProfiles(modelHierarchy, monitor);

    final Map<Profile, Boolean> availableSettingsProfiles = getAvailableSettingsProfiles();

    availableProfiles.addAll(availableSettingsProfiles.keySet());

    List<ProfileData> statuses = new ArrayList<>();

    Map<String, List<String>> allActiveProfiles = mavenProject.getInjectedProfileIds();

    for(Profile p : availableProfiles) {
      String pId = p.getId();
      ProfileData status = new ProfileData(pId);
      boolean isDisabled = configuredProfiles.contains("!" + pId);
      if(isActive(pId, allActiveProfiles)) {
        status.setActivationState(ProfileState.Active);
      } else if(isDisabled) {
        status.setActivationState(ProfileState.Disabled);
      }
      boolean isUserSelected = isDisabled || configuredProfiles.contains(pId);

      status.setUserSelected(isUserSelected);

      Boolean isAutoActiveSettingProfile = availableSettingsProfiles.get(p);
      boolean isAutoActive = (isAutoActiveSettingProfile != null && isAutoActiveSettingProfile)
          || (status.getActivationState().isActive() && !isUserSelected);
      status.setAutoActive(isAutoActive);
      status.setSource(findSource(p, modelHierarchy));
      statuses.add(status);
    }

    return Collections.unmodifiableList(statuses);
  }

  private boolean isActive(String profileId, Map<String, List<String>> profilesMap) {
    for(Map.Entry<String, List<String>> entry : profilesMap.entrySet()) {
      for(String pId : entry.getValue()) {
        if(pId.equals(profileId)) {
          return true;
        }
      }
    }

    return false;
  }

  private List<String> toList(String profilesAsText) {
    List<String> profiles = new ArrayList<>();
    if(profilesAsText != null && profilesAsText.trim().length() > 0) {
      profiles.addAll(Arrays.asList(profilesAsText.split("[,\\s\\|]")));
    }
    return profiles;
  }

  private String findSource(Profile profile, List<Model> modelHierarchy) {
    if(profile != null) {
      if("settings.xml".equals(profile.getSource())) { //$NON-NLS-1$
        return profile.getSource();
      }
      for(Model m : modelHierarchy) {
        for(Profile p : m.getProfiles()) {
          if(p.equals(profile)) {
            return m.getArtifactId();
          }
        }
      }
    }
    return "undefined"; //$NON-NLS-1$
  }

  protected List<Profile> collectAvailableProfiles(List<Model> models, IProgressMonitor monitor) throws CoreException {
    List<Profile> profiles = new ArrayList<Profile>();
    for(Model m : models) {
      profiles.addAll(m.getProfiles());
    }
    return profiles;
  }

  protected List<Model> getModelHierarchy(List<Model> models, Model projectModel, IProgressMonitor monitor)
      throws CoreException {
    if(projectModel == null) {
      return null;
    }
    models.add(projectModel);
    Parent p = projectModel.getParent();
    if(p != null) {

      IMaven maven = MavenPlugin.getMaven();

      List<ArtifactRepository> repositories = new ArrayList<>();
      repositories.addAll(getProjectRepositories(projectModel));
      repositories.addAll(maven.getArtifactRepositories());

      Model parentModel = resolvePomModel(p.getGroupId(), p.getArtifactId(), p.getVersion(), repositories, monitor);
      if(parentModel != null) {
        getModelHierarchy(models, parentModel, monitor);
      }
    }
    return models;
  }

  private List<ArtifactRepository> getProjectRepositories(Model projectModel) {
    List<ArtifactRepository> repos = new ArrayList<>();
    List<Repository> modelRepos = projectModel.getRepositories();
    if(modelRepos != null && !modelRepos.isEmpty()) {
      RepositorySystem repositorySystem = getRepositorySystem();
      for(Repository modelRepo : modelRepos) {
        ArtifactRepository ar;
        try {
          ar = repositorySystem.buildArtifactRepository(modelRepo);
          if(ar != null) {
            repos.add(ar);
          }
        } catch(InvalidRepositoryException e) {
          MavenProfilesCoreActivator.log(e);
        }
      }
    }
    return repos;
  }

  private RepositorySystem getRepositorySystem() {
    try {
      //TODO find an alternative way to get the Maven RepositorySystem, or use Aether directly to resolve models??
      return MavenPluginActivator.getDefault().getPlexusContainer().lookup(RepositorySystem.class);
    } catch(ComponentLookupException e) {
      throw new NoSuchComponentException(e);
    }
  }

  private Model resolvePomModel(String groupId, String artifactId, String version,
      List<ArtifactRepository> repositories, IProgressMonitor monitor) throws CoreException {
    monitor.subTask(NLS.bind("Resolving {0}:{1}:{2}", new Object[] {groupId, artifactId, version}));

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getMavenProject(groupId, artifactId, version);
    IMaven maven = MavenPlugin.getMaven();

    if(facade != null) {
      return facade.getMavenProject(monitor).getModel();
    }

    Artifact artifact = maven.resolve(groupId, artifactId, version, "pom", null, repositories, monitor); //$NON-NLS-1$
    File file = artifact.getFile();
    if(file == null) {
      return null;
    }

    return maven.readModel(file);
  }
}
