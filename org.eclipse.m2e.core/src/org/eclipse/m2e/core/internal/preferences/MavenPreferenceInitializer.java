/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.apache.maven.cli.MavenCli;

import org.eclipse.m2e.core.internal.IMavenConstants;


/**
 * Maven preferences initializer.
 * 
 * @author Eugene Kuleshov
 */
public class MavenPreferenceInitializer extends AbstractPreferenceInitializer {

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
   */
  public void initializeDefaultPreferences() {
    IEclipsePreferences store = DefaultScope.INSTANCE.getNode(IMavenConstants.PLUGIN_ID);

    store.put(MavenPreferenceConstants.P_USER_SETTINGS_FILE, //
        MavenCli.DEFAULT_USER_SETTINGS_FILE.getAbsolutePath());
    
    store.put(MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE, ""); //$NON-NLS-1$

    store.putBoolean(MavenPreferenceConstants.P_DEBUG_OUTPUT, false);

    store.putBoolean(MavenPreferenceConstants.P_OFFLINE, false);
    store.putBoolean(MavenPreferenceConstants.P_GLOBAL_UPDATE_NEVER, true);

    store.putBoolean(MavenPreferenceConstants.P_DOWNLOAD_SOURCES, false);
    store.putBoolean(MavenPreferenceConstants.P_DOWNLOAD_JAVADOC, false);

    // store.setDefault( MavenPreferenceConstants.P_GLOBAL_CHECKSUM_POLICY, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);
    // store.setDefault( MavenPreferenceConstants.P_UPDATE_SNAPSHOTS, false);
    // store.setDefault( MavenPreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION, false);

    store.put(MavenPreferenceConstants.P_OUTPUT_FOLDER, "target-eclipse"); //$NON-NLS-1$

    store.put(MavenPreferenceConstants.P_RUNTIMES, ""); //$NON-NLS-1$
    store.put(MavenPreferenceConstants.P_DEFAULT_RUNTIME, ""); //$NON-NLS-1$

    store.putBoolean(MavenPreferenceConstants.P_UPDATE_INDEXES, true);
    store.putBoolean(MavenPreferenceConstants.P_UPDATE_PROJECTS, false);
    
    store.putBoolean(MavenPreferenceConstants.P_HIDE_FOLDERS_OF_NESTED_PROJECTS, false);
    
    store.putBoolean(MavenPreferenceConstants.P_SHOW_CONSOLE_ON_ERR, true);
    store.putBoolean(MavenPreferenceConstants.P_SHOW_CONSOLE_ON_OUTPUT, false);
    
    // set to null since the plugin state location is not available by the time execution reaches here
    store.put(MavenPreferenceConstants.P_WORKSPACE_MAPPINGS_LOCATION, null);
  }
}
