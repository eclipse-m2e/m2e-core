/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc.and others
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

package org.eclipse.m2e.core.internal.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.eclipse.m2e.core.internal.IMavenConstants;


/**
 * Maven preferences initializer.
 *
 * @author Eugene Kuleshov
 */
public class MavenPreferenceInitializer extends AbstractPreferenceInitializer {

  @Override
  public void initializeDefaultPreferences() {
    IEclipsePreferences store = DefaultScope.INSTANCE.getNode(IMavenConstants.PLUGIN_ID);

    store.putBoolean(MavenPreferenceConstants.P_DEBUG_OUTPUT, false);

    store.putBoolean(MavenPreferenceConstants.P_OFFLINE, false);
    store.putBoolean(MavenPreferenceConstants.P_GLOBAL_UPDATE_NEVER, true);

    store.putBoolean(MavenPreferenceConstants.P_DOWNLOAD_SOURCES, false);
    store.putBoolean(MavenPreferenceConstants.P_DOWNLOAD_JAVADOC, false);

    store.put(MavenPreferenceConstants.P_OUTPUT_FOLDER, "target-eclipse"); //$NON-NLS-1$

    store.put(MavenPreferenceConstants.P_RUNTIMES, ""); //$NON-NLS-1$
    store.put(MavenPreferenceConstants.P_DEFAULT_RUNTIME, ""); //$NON-NLS-1$

    store.putBoolean(MavenPreferenceConstants.P_UPDATE_INDEXES, false);
    store.putBoolean(MavenPreferenceConstants.P_UPDATE_PROJECTS, false);

    store.putBoolean(MavenPreferenceConstants.P_HIDE_FOLDERS_OF_NESTED_PROJECTS, false);
    store.putBoolean(MavenPreferenceConstants.P_QUERY_CENTRAL_TO_IDENTIFY_ARTIFACT, false);
    store.putBoolean(MavenPreferenceConstants.P_DEFAULT_POM_EDITOR_PAGE, true);

    store.putBoolean(MavenPreferenceConstants.P_SHOW_CONSOLE_ON_ERR, true);
    store.putBoolean(MavenPreferenceConstants.P_SHOW_CONSOLE_ON_OUTPUT, false);

    store.put(MavenPreferenceConstants.P_DUP_OF_PARENT_GROUPID_PB, ProblemSeverity.warning.toString());
    store.put(MavenPreferenceConstants.P_DUP_OF_PARENT_VERSION_PB, ProblemSeverity.warning.toString());
    store.put(MavenPreferenceConstants.P_OVERRIDING_MANAGED_VERSION_PB, ProblemSeverity.warning.toString());
    store.put(MavenPreferenceConstants.P_OUT_OF_DATE_PROJECT_CONFIG_PB, ProblemSeverity.error.toString());
    store.put(MavenPreferenceConstants.P_NOT_COVERED_MOJO_EXECUTION_PB, ProblemSeverity.warning.toString());

    // set to null since the plugin state location is not available by the time execution reaches here
    store.remove(MavenPreferenceConstants.P_WORKSPACE_MAPPINGS_LOCATION);

    store.putBoolean(MavenPreferenceConstants.P_AUTO_UPDATE_CONFIGURATION, true);

    store.putBoolean(MavenPreferenceConstants.P_ENABLE_SNAPSHOT_ARCHETYPES, false);
  }
}
