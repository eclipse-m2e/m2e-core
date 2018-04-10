/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.discovery.startup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.internal.IPreferenceConstants;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.internal.util.PrefUtil;
import org.eclipse.ui.statushandlers.StatusManager;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.UpdateMavenProjectJob;
import org.eclipse.m2e.internal.discovery.DiscoveryActivator;
import org.eclipse.m2e.internal.discovery.Messages;


@SuppressWarnings("restriction")
public class UpdateConfigurationStartup implements IStartup {

  private static final String PROJECT_PREF = DiscoveryActivator.PLUGIN_ID + ".pref.projects"; //$NON-NLS-1$

  public void earlyStartup() {
    IProject[] projects = getSavedProjects();
    if(projects != null && projects.length > 0) {
      updateConfiguration(projects);
    }
    disableStartup();
  }

  /*
   * Enables the early startup for this bundle
   */
  public static void enableStartup() {
    saveMarkedProjects();
    addEarlyStartup();
  }

  /*
   * Enable early startup for this bundle, also add the list of projects to those that should be configured
   */
  public static void enableStartup(Collection<String> knownProjects) {
    if(knownProjects != null) {
      Set<String> projects = new HashSet<String>(knownProjects);
      for(IProject project : getMarkedProjects()) {
        projects.add(project.getName());
      }
      saveProjects(projects);
    } else {
      saveMarkedProjects();
    }
    addEarlyStartup();
  }

  /*
   * Disables the early startup for this bundle 
   */
  public static void disableStartup() {
    clearSavedProjects();
    removeEarlyStartup();
  }

  /*
   * Configure projects
   */
  public static void updateConfiguration() {
    Collection<IProject> projects = getMarkedProjects();
    new UpdateMavenProjectJob(projects.toArray(new IProject[projects.size()])).schedule();
  }

  private static void updateConfiguration(IProject[] projects) {
    new UpdateMavenProjectJob(projects).schedule();
  }

  private static void addEarlyStartup() {
    String[] disabledEarlyActivation = Workbench.getInstance().getDisabledEarlyActivatedPlugins();

    // If we aren't disabled, nothing to do
    if(!isDisabled(disabledEarlyActivation)) {
      return;
    }

    String[] disabledPlugins = new String[disabledEarlyActivation.length - 1];
    int index = 0;
    for(String plugin : disabledEarlyActivation) {
      if(!DiscoveryActivator.PLUGIN_ID.equals(plugin)) {
        disabledPlugins[index] = plugin;
      }
    }
    setEarlyActivationPreference(disabledPlugins);
  }

  private static void removeEarlyStartup() {
    String[] disabledEarlyActivation = Workbench.getInstance().getDisabledEarlyActivatedPlugins();

    // Determine if we're already disabled
    if(isDisabled(disabledEarlyActivation)) {
      return;
    }

    String[] disabledPlugins = new String[disabledEarlyActivation.length + 1];
    System.arraycopy(disabledEarlyActivation, 0, disabledPlugins, 0, disabledEarlyActivation.length);
    disabledPlugins[disabledPlugins.length - 1] = DiscoveryActivator.PLUGIN_ID;

    setEarlyActivationPreference(disabledPlugins);
  }

  private static boolean isDisabled(String[] disabledEarlyActivation) {
    for(String item : disabledEarlyActivation) {
      if(DiscoveryActivator.PLUGIN_ID.equals(item)) {
        return true;
      }
    }
    return false;
  }

  private static void setEarlyActivationPreference(String[] disabledPlugins) {// Add ourself to disabled
	StringBuilder preference = new StringBuilder();
    for(String item : disabledPlugins) {
      preference.append(item).append(IPreferenceConstants.SEPARATOR);
    }

    IPreferenceStore store = PrefUtil.getInternalPreferenceStore();
    store.putValue(IPreferenceConstants.PLUGINS_NOT_ACTIVATED_ON_STARTUP, preference.toString());
    PrefUtil.savePrefs();
  }

  /*
   * Get projects we saved previously  
   */
  public static IProject[] getSavedProjects() {
    String[] projectNames = DiscoveryActivator.getDefault().getPreferenceStore().getString(PROJECT_PREF)
        .split(String.valueOf(IPreferenceConstants.SEPARATOR));
    List<IProject> projects = new ArrayList<IProject>(projectNames.length);
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    for(String projectName : projectNames) {
      if(projectName.length() > 0) {
        IProject project = root.getProject(projectName);
        if(project != null) {
          projects.add(project);
        }
      }
    }
    return projects.toArray(new IProject[projects.size()]);
  }

  /*
   * Save a list of projects which have configuration markers
   */
  public static void saveMarkedProjects() {
    StringBuilder sb = new StringBuilder();
    for(IProject project : getMarkedProjects()) {
      sb.append(project.getName()).append(IPreferenceConstants.SEPARATOR);
    }
    DiscoveryActivator.getDefault().getPreferenceStore().putValue(PROJECT_PREF, sb.toString());
  }

  /*
   * Save a list of projects which have configuration markers
   */
  public static void saveProjects(Collection<String> projects) {
    StringBuilder sb = new StringBuilder();
    for(String project : projects) {
      sb.append(project).append(IPreferenceConstants.SEPARATOR);
    }
    DiscoveryActivator.getDefault().getPreferenceStore().putValue(PROJECT_PREF, sb.toString());
  }

  private static Collection<IProject> getMarkedProjects() {
    List<IProject> projects = new ArrayList<IProject>();
    MultiStatus status = new MultiStatus(DiscoveryActivator.PLUGIN_ID, 0,
        Messages.UpdateConfigurationStartup_MarkerError, null);
    for(IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      try {
        if(project.findMarkers(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID, true, IResource.DEPTH_ONE).length > 0) {
          projects.add(project);
        }
      } catch(CoreException e) {
        status.add(e.getStatus());
      }
    }
    if(status.getChildren().length > 0) {
      StatusManager.getManager().handle(status);
    }
    return projects;
  }

  /*
   * Empty the list of saved projects
   */
  public static void clearSavedProjects() {
    DiscoveryActivator.getDefault().getPreferenceStore().putValue(PROJECT_PREF, ""); //$NON-NLS-1$
  }
}
