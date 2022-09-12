/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.apt.internal.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.eclipse.m2e.apt.MavenJdtAptPlugin;
import org.eclipse.m2e.apt.preferences.AnnotationProcessingMode;
import org.eclipse.m2e.apt.preferences.PreferencesConstants;


/**
 * m2e-apt preferences initializer.
 *
 * @author Fred Bricon
 */
public class AnnotationProcessingPreferenceInitializer extends AbstractPreferenceInitializer {

  /**
   * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
   */
  @Override
  public void initializeDefaultPreferences() {
    IEclipsePreferences store = DefaultScope.INSTANCE.getNode(MavenJdtAptPlugin.PLUGIN_ID);
    store.put(PreferencesConstants.MODE, AnnotationProcessingMode.disabled.name());
  }

}
