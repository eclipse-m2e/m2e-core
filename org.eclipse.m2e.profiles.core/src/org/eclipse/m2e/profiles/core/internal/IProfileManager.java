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
package org.eclipse.m2e.profiles.core.internal;

import java.util.List;
import java.util.Map;

import org.apache.maven.model.Profile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;

/**
 * Retrieves and updates Maven profile informations for Maven Projects  
 * 
 * @author Fred Bricon
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.5.0
 */
public interface IProfileManager {
	
	/**
	 * Returns a List of {@link ProfileData} for the given mavenProjectFacade
	 *   
	 * @param mavenProjectFacade a facade of the maven project
	 * @param monitor a progress monitor
	 * @return a List of {@link ProfileData} for the given mavenProjectFacade. 
	 * @throws CoreException
	 */
	List<ProfileData> getProfileDatas(IMavenProjectFacade mavenProjectFacade, IProgressMonitor monitor) throws CoreException;

	/**
	 * Returns an unmodifiable {@link Map} of all available {@link Profile}s converted from the
	 * {@link org.apache.maven.profiles.Profile}s as defined in settings.xml.<br/>
	 * The value of each {@link Entry} indicates if the profile is active.
	 * 
	 * @return an unmodifiable {@link Map} of all the available profiles for a given project.
	 * @throws CoreException
	 */
	Map<Profile, Boolean> getAvailableSettingsProfiles() throws CoreException;

	/**
	 * Update the profiles of the resolver configuration of a {@link IMavenProjectFacade} synchronously.
	 * 
	 * @param mavenProjectFacade a facade of the maven project
	 * @param profiles the profile ids to use in the project's resolver configuration
	 * @param isOffline indicates if the maven request must be executed offline
	 * @param isForceUpdate indicates if a check for updated releases and snapshots on remote repositories must be forced.
	 * @param monitor a progress monitor
	 * @throws CoreException
	 */
	void updateActiveProfiles(IMavenProjectFacade mavenProjectFacade,
			List<String> profiles, boolean isOffline, boolean isForceUpdate, IProgressMonitor monitor)
			throws CoreException; 
}
