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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.m2e.profiles.core.internal.ProfileState;
import org.eclipse.m2e.profiles.core.internal.ProfileData;
import org.eclipse.osgi.util.NLS;

/**
 * Maven Profile Manager
 * 
 * @author Fred Bricon
 * @since 1.5.0
 */
public class ProfileManager implements IProfileManager {

	public void updateActiveProfiles(final IMavenProjectFacade mavenProjectFacade, 
									 final List<String> profiles, 
									 final boolean isOffline, 
									 final boolean isForceUpdate, 
									 IProgressMonitor monitor) throws CoreException {
		if (mavenProjectFacade == null) {
			return;
		}
		final IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();

		IProject project = mavenProjectFacade.getProject();
		
		final ResolverConfiguration configuration =configurationManager.getResolverConfiguration(project);

		final String profilesAsString = getAsString(profiles);
		if (profilesAsString.equals(configuration.getSelectedProfiles())) {
			//Nothing changed
			return;
		}
		
		configuration.setSelectedProfiles(profilesAsString);
		boolean isSet = configurationManager.setResolverConfiguration(project, configuration);
		if (isSet) {
			MavenUpdateRequest request = new MavenUpdateRequest(project, isOffline, isForceUpdate);
			configurationManager.updateProjectConfiguration(request, monitor);
		}

	}
	
	private String getAsString(List<String> profiles) {
		StringBuilder sb = new StringBuilder();
		boolean addComma = false;
		if (profiles != null){
			for (String p : profiles) {
				if (addComma) {
					sb.append(", "); //$NON-NLS-1$
				}
				sb.append(p);
				addComma = true;
			}
		}
		return sb.toString();
	}
	
	public Map<Profile, Boolean> getAvailableSettingsProfiles() throws CoreException {
		Map<Profile, Boolean> settingsProfiles = new LinkedHashMap<Profile, Boolean>();
		Settings settings = MavenPlugin.getMaven().getSettings();
		List<String> activeProfiles = settings.getActiveProfiles();
		
		for (org.apache.maven.settings.Profile sp : settings.getProfiles()) {
			Profile p = SettingsUtils.convertFromSettingsProfile(sp);
			boolean isAutomaticallyActivated = isActive(sp, activeProfiles);
			settingsProfiles.put(p, isAutomaticallyActivated);
		}
		return Collections.unmodifiableMap(settingsProfiles);
	}

	private boolean isActive(Profile p, List<Profile> activeProfiles) {
		for (Profile activeProfile : activeProfiles) {
			if (activeProfile.getId().equals(p.getId())) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isActive(org.apache.maven.settings.Profile p, List<String> activeProfiles) {
		if (p.getActivation() != null && p.getActivation().isActiveByDefault()){
			return true;
		}
		for (String activeProfile : activeProfiles) {
			if (activeProfile.equals(p.getId())) {
				return true;
			}
		}
		return false;
	}

	public List<ProfileData> getProfileDatas(
			IMavenProjectFacade facade,
			IProgressMonitor monitor
			) throws CoreException {
		if (facade == null) {
			return Collections.emptyList();
		}
		
		ResolverConfiguration resolverConfiguration = MavenPlugin.getProjectConfigurationManager()
														.getResolverConfiguration(facade.getProject());

		List<String> configuredProfiles = toList(resolverConfiguration.getSelectedProfiles());
		
		MavenProject mavenProject = facade.getMavenProject(monitor);
		
		List<Model> modelHierarchy = new ArrayList<Model>();

		getModelHierarchy(modelHierarchy, mavenProject.getModel(), monitor);

		List<Profile> availableProfiles = collectAvailableProfiles(modelHierarchy, monitor);

		final Map<Profile, Boolean> availableSettingsProfiles = getAvailableSettingsProfiles();
		
		Set<Profile> settingsProfiles = new HashSet<Profile>(availableSettingsProfiles.keySet());
		
		List<ProfileData> statuses = new ArrayList<ProfileData>();
		
		//First we put user configured profiles
		for (String pId : configuredProfiles) {
			if (StringUtils.isEmpty(pId)) continue;
			boolean isDisabled = pId.startsWith("!");
			String id = (isDisabled)?pId.substring(1):pId;
			ProfileData status = new ProfileData(id);
			status.setUserSelected(true);
			ProfileState state = isDisabled?ProfileState.Disabled:ProfileState.Active;
			status.setActivationState(state);
			
			Profile p = get(id, availableProfiles);
			
			if (p == null){
				p = get(id, settingsProfiles);
				if(p != null){
					status.setAutoActive(availableSettingsProfiles.get(p));
				}
			} 

			status.setSource(findSource(p, modelHierarchy));
			statuses.add(status);
		}
		
		final List<Profile> activeProfiles = mavenProject.getActiveProfiles();
		//Iterate on the remaining project profiles
		addStatuses(statuses, availableProfiles, modelHierarchy, new ActivationPredicate() {
			@Override
			boolean isActive(Profile p) {
				return ProfileManager.this.isActive(p, activeProfiles);
			}
		});

		//Iterate on the remaining settings profiles
		addStatuses(statuses, settingsProfiles, modelHierarchy, new ActivationPredicate() {
			@Override
			boolean isActive(Profile p) {
				return availableSettingsProfiles.get(p);
			}
		});
		return Collections.unmodifiableList(statuses);
	}

	private List<String> toList(String profilesAsText) {
		List<String> profiles; 
	    if (profilesAsText != null && profilesAsText.trim().length() > 0) {
	      String[] profilesArray = profilesAsText.split("[,\\s\\|]"); 
	      profiles = new ArrayList<String>(profilesArray.length);
	      for (String profile : profilesArray) {
	         profiles.add(profile);
	      }
	    } else {
	      profiles = new ArrayList<String>(0);
	    }
	    return profiles;
	}

	private String findSource(Profile profile, List<Model> modelHierarchy) {
		if (profile != null) {
			if ("settings.xml".equals(profile.getSource())) { //$NON-NLS-1$
				return profile.getSource();
			}
			for (Model m : modelHierarchy) {
				for (Profile p : m.getProfiles()) {
					if(p.equals(profile)) {
						return  m.getArtifactId();
					}
				}
			}
		} 
		return "undefined"; //$NON-NLS-1$
	}

	protected List<Profile> collectAvailableProfiles(List<Model> models, IProgressMonitor monitor) throws CoreException {
		List<Profile> profiles = new ArrayList<Profile>();
		for (Model m : models) {
			profiles.addAll(m.getProfiles());
		}
		return profiles;
	}

	protected List<Model> getModelHierarchy(List<Model> models, Model projectModel, IProgressMonitor monitor) throws CoreException {
		if (projectModel == null) {
			return null;
		}
		models.add(projectModel);
		Parent p  = projectModel.getParent();
		if (p != null) {
			
			IMaven maven = MavenPlugin.getMaven(); 
			
			List<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
			repositories.addAll(getProjectRepositories(projectModel));
			repositories.addAll(maven.getArtifactRepositories());
			
			Model parentModel = resolvePomModel(p.getGroupId(), p.getArtifactId(), p.getVersion(), repositories, monitor);
			if (parentModel != null) {
				getModelHierarchy(models, parentModel, monitor);
			}
		}
		return models;
	}
	
	 private List<ArtifactRepository> getProjectRepositories(Model projectModel) {
		 List<ArtifactRepository> repos = new ArrayList<ArtifactRepository>();
		 List<Repository> modelRepos = projectModel.getRepositories();
		 if (modelRepos != null && !modelRepos.isEmpty()) {
			 RepositorySystem repositorySystem = getRepositorySystem();
			 for (Repository modelRepo : modelRepos) {
				ArtifactRepository ar;
				try {
					ar = repositorySystem.buildArtifactRepository(modelRepo);
					if (ar != null) {
						repos.add(ar);
					}
				} catch (InvalidRepositoryException e) {
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
		} catch (ComponentLookupException e) {
			throw new NoSuchComponentException(e);
		}
	}

	private Model resolvePomModel(String groupId, String artifactId, String version, List<ArtifactRepository> repositories, IProgressMonitor monitor)
		      throws CoreException {
	    monitor.subTask(NLS.bind("Resolving {0}:{1}:{2}", new Object[] { groupId, artifactId, version}));

	    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getMavenProject(groupId, artifactId, version);
	    IMaven maven = MavenPlugin.getMaven(); 
	    
	    if (facade != null) {
	    	return facade.getMavenProject(monitor).getModel();
	    }
	    
	    Artifact artifact = maven.resolve(groupId, artifactId, version, "pom", null, repositories, monitor); //$NON-NLS-1$
	    File file = artifact.getFile();
	    if(file == null) {
	      return null;
	    }
	    
	    return maven.readModel(file);
	 }

	private void addStatuses(List<ProfileData> statuses, Collection<Profile> profiles, List<Model> modelHierarchy, ActivationPredicate predicate) {
		for (Profile p : profiles) {
			ProfileData status = new ProfileData(p.getId());
			status.setSource(findSource(p, modelHierarchy));
			boolean isActive = predicate.isActive(p);
			ProfileState activationState = (isActive)?ProfileState.Active:ProfileState.Inactive;
			status.setAutoActive(isActive);
			status.setActivationState(activationState);
			statuses.add(status);
		}
	}

	private Profile get(String id, Collection<Profile> profiles) {
		Iterator<Profile> ite = profiles.iterator();
		Profile found = null;
		while(ite.hasNext()) {
			Profile p = ite.next(); 
			if(id.equals(p.getId())) {
				found = p;
				ite.remove();
				break;
			}
		}
		return found;
	}

	private abstract class ActivationPredicate {
		abstract boolean isActive(Profile p);
	}
}
