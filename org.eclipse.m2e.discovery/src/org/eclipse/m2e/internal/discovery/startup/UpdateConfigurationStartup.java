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
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.jobs.UpdateConfigurationJob;
import org.eclipse.m2e.internal.discovery.DiscoveryActivator;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.internal.IPreferenceConstants;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.internal.util.PrefUtil;


@SuppressWarnings("restriction")
public class UpdateConfigurationStartup implements IStartup {

  private static final String PROJECT_PREF = DiscoveryActivator.PLUGIN_ID + ".pref.projects"; //$NON-NLS-1$

  public void earlyStartup() {
    final MavenPlugin plugin = MavenPlugin.getDefault();
    new UpdateConfigurationJob(plugin, getMarkedProjects()).schedule();
    disableStartup();
  }

  /*
   * Enables the early startup for this bundle
   */
  public static void enableStartup() {
    updateMarkedProjects();
    String[] disabledEarlyActivation = Workbench.getInstance().getDisabledEarlyActivatedPlugins();

    if(!isDisabled(disabledEarlyActivation)) {
      return;
    }

    StringBuffer preference = new StringBuffer();
    for(String item : disabledEarlyActivation) {
      if(!DiscoveryActivator.PLUGIN_ID.equals(item)) {
        preference.append(item).append(IPreferenceConstants.SEPARATOR);
      }
    }
    setPreference(preference.toString());
  }

  /*
   * Disables the early startup for this bundle 
   */
  public static void disableStartup() {
    clearMarkedProjects();
    String[] disabledEarlyActivation = Workbench.getInstance().getDisabledEarlyActivatedPlugins();

    // Determine if we're already disabled
    if(isDisabled(disabledEarlyActivation)) {
      return;
    }

    // Add ourself to disabled
    StringBuffer preference = new StringBuffer();
    for(String item : disabledEarlyActivation) {
      preference.append(item).append(IPreferenceConstants.SEPARATOR);
    }
    preference.append(DiscoveryActivator.PLUGIN_ID).append(IPreferenceConstants.SEPARATOR);
    setPreference(preference.toString());
  }

  private static boolean isDisabled(String[] disabledEarlyActivation) {
    for(String item : disabledEarlyActivation) {
      if(DiscoveryActivator.PLUGIN_ID.equals(item)) {
        return true;
      }
    }
    return false;
  }

  private static void setPreference(String pref) {
    IPreferenceStore store = PrefUtil.getInternalPreferenceStore();
    store.putValue(IPreferenceConstants.PLUGINS_NOT_ACTIVATED_ON_STARTUP, pref);
    PrefUtil.savePrefs();
  }

  public static IProject[] getMarkedProjects() {
    String[] projectNames = DiscoveryActivator.getDefault().getPreferenceStore().getString(PROJECT_PREF)
        .split(String.valueOf(IPreferenceConstants.SEPARATOR));
    List<IProject> projects = new ArrayList<IProject>(projectNames.length);
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    for(String projectName : projectNames) {
      if(!projectName.isEmpty()) {
        IProject project = root.getProject(projectName);
        if(project != null) {
          projects.add(project);
        }
      }
    }
    return projects.toArray(new IProject[projects.size()]);
  }

  public static void updateMarkedProjects() {
    StringBuilder sb = new StringBuilder();
    for(IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      try {
        if(project.findMarkers(IMavenConstants.MARKER_CONFIGURATION_ID, true, IResource.DEPTH_ONE).length > 0) {
          sb.append(project.getName()).append(IPreferenceConstants.SEPARATOR);
        }
      } catch(CoreException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    DiscoveryActivator.getDefault().getPreferenceStore().putValue(PROJECT_PREF, sb.toString());
  }

  public static void clearMarkedProjects() {
    DiscoveryActivator.getDefault().getPreferenceStore().putValue(PROJECT_PREF, ""); //$NON-NLS-1$
  }
}