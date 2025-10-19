/*************************************************************************************
 * Copyright (c) 2011-2022 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fred Bricon / JBoss by Red Hat - Initial implementation.
 ************************************************************************************/

package org.eclipse.m2e.profiles.core.internal.management;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.IMavenToolbox;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.NoSuchComponentException;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IProjectConfiguration;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.profiles.core.internal.IProfileManager;
import org.eclipse.m2e.profiles.core.internal.ProfileData;
import org.eclipse.m2e.profiles.core.internal.ProfileState;


/**
 * Maven Profile Manager
 *
 * @author Fred Bricon
 * @since 1.5.0
 */
@Component(service = IProfileManager.class)
public class ProfileManager implements IProfileManager {

  @Reference
  private ILog log;

  @Reference
  IProjectConfigurationManager configurationManager;

  @Reference
  IMaven maven;

  public void updateActiveProfiles(final IMavenProjectFacade mavenProjectFacade, final List<String> profiles,
      final boolean isOffline, final boolean isForceUpdate, IProgressMonitor monitor) throws CoreException {
    if(mavenProjectFacade == null) {
      return;
    }
    IProject project = mavenProjectFacade.getProject();

    final ResolverConfiguration configuration = new ResolverConfiguration(
        configurationManager.getProjectConfiguration(project));

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
    Settings settings = maven.getSettings();
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

    IProjectConfiguration resolverConfiguration = MavenPlugin.getProjectConfigurationManager()
        .getProjectConfiguration(facade.getProject());

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

  protected List<Profile> collectAvailableProfiles(List<Model> models, IProgressMonitor monitor) {
    List<Profile> profiles = new ArrayList<>();
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
      Model parentModel = buildParentModel(projectModel, monitor);
      if(parentModel != null) {
        getModelHierarchy(models, parentModel, monitor);
      }
    }
    return models;
  }

  private Model buildParentModel(Model projectModel, IProgressMonitor monitor) throws CoreException {
    Model parentModel = buildParentModelViaRelativePath(projectModel);

    if(parentModel != null) {
      return parentModel;
    }

    List<ArtifactRepository> repositories = new ArrayList<>();
    repositories.addAll(getProjectRepositories(projectModel));
    repositories.addAll(maven.getArtifactRepositories());

    Parent p = projectModel.getParent();
    return resolvePomModel(p.getGroupId(), p.getArtifactId(), p.getVersion(), repositories, monitor);
  }

  /**
   * @param parent The parent to attempt to build a POM model for via it's relative path.
   * @return the POM model for the parent, or <code>null</code> if this could not be built.
   */
  private Model buildParentModelViaRelativePath(Model model) {
    if(model.getPomFile() == null) {
      return null;
    }

    String relativePath = model.getParent().getRelativePath();
    if(relativePath == null || relativePath.isEmpty()) {
      relativePath = ".." + File.separator + "pom.xml";
    }
    String relativeFileSystemPathToParentPom = ".." + File.separator + relativePath;

    String pomFileSystemPath = model.getPomFile().getPath();

    try {
      File parentPomFile = Paths.get(pomFileSystemPath, relativeFileSystemPathToParentPom).toFile().getCanonicalFile();
      if(parentPomFile.exists()) {
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        FileReader reader = new FileReader(parentPomFile);
        final Model parentModel = mavenreader.read(reader);

        // Verify that the POM found via the relative path has co-ordinates that match the parent we are searching for, if no match then return null.
        if(!modelEqualsParent(parentModel, model.getParent())) {
          return null;
        }

        parentModel.setPomFile(parentPomFile);
        return parentModel;
      }
    } catch(Exception e) {
      log.error("" + "Error building Maven model for parent POM file with relative path ["
          + relativeFileSystemPathToParentPom + "] from child POM path [" + pomFileSystemPath + "]", e);
    }

    return null; // If we have got here then we failed to locate the parent POM file
  }

  /*
   * Verifies that the co-ordinates of the model matches the parent. Note that for the
   * group ID and version we are lenient; if for whatever reason the model does not
   * have a groupId or version then we assume these properties of the co-ordinates are
   * a match. In the future this assumption could be challenged and the logic reversed
   * in order to be more strict.
   */
  private boolean modelEqualsParent(Model pomModel, Parent pomParent) {
    String pomModelGroupId = getPomModelGroupId(pomModel);
    if(pomModelGroupId != null && !pomModelGroupId.equals(pomParent.getGroupId())) {
      return false;
    }

    if(!pomModel.getArtifactId().equals(pomParent.getArtifactId())) {
      return false;
    }

    String pomModelVersion = getPomModelVersion(pomModel);
    return pomModelVersion == null || pomModelVersion.equals(pomParent.getVersion());
  }

  private String getPomModelGroupId(Model pomModel) {
    if(pomModel.getGroupId() == null && pomModel.getParent() != null) {
      return pomModel.getParent().getGroupId();
    }

    return pomModel.getGroupId();
  }

  private String getPomModelVersion(Model pomModel) {
    if(pomModel.getVersion() == null && pomModel.getParent() != null) {
      return pomModel.getParent().getVersion();
    }

    return pomModel.getVersion();
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
          log.error("can't read repository", e);
        }
      }
    }
    return repos;
  }

  private RepositorySystem getRepositorySystem() {
    try {
      //TODO find an alternative way to get the Maven RepositorySystem, or use Aether directly to resolve models??
      return maven.lookup(RepositorySystem.class);
    } catch(CoreException e) {
      if(e.getStatus().getException() instanceof ComponentLookupException) {
        throw new NoSuchComponentException((ComponentLookupException) e.getStatus().getException());
      }
      log.log(e.getStatus());
      throw new NoSuchComponentException(null);
    }
  }

  private Model resolvePomModel(String groupId, String artifactId, String version,
      List<ArtifactRepository> repositories, IProgressMonitor monitor) throws CoreException {
    monitor.subTask(NLS.bind("Resolving {0}:{1}:{2}", new Object[] {groupId, artifactId, version}));

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getMavenProject(groupId, artifactId, version);

    if(facade != null) {
      return facade.getMavenProject(monitor).getModel();
    }

    Artifact artifact = maven.resolve(groupId, artifactId, version, "pom", null, repositories, monitor); //$NON-NLS-1$
    File file = artifact.getFile();
    if(file == null) {
      return null;
    }
    return readModel(maven, file);
  }

  private Model readModel(IMaven maven, File file) throws CoreException {
    try (InputStream is = new FileInputStream(file)) {
      return IMavenToolbox.of(maven).readModel(new FileInputStream(file));
    } catch(IOException e) {
      throw new CoreException(Status.error(Messages.MavenImpl_error_read_pom, e));
    }
  }
}
