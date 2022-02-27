/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.jdt;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;


/**
 * Maven preferences initializer.
 */
public class MavenPreferenceInitializer extends AbstractPreferenceInitializer {

  @Override
  public void initializeDefaultPreferences() {
    IEclipsePreferences store = DefaultScope.INSTANCE.getNode(MavenJdtPlugin.PLUGIN_ID);

    store.put(MavenJdtPlugin.PREFERENCES_JRE_SYSTEM_LIBRARY_VERSION,
        JreSystemVersion.EXECUTION_ENVIRONMENT_FROM_PLUGIN_CONFIG.name());

  }
}
