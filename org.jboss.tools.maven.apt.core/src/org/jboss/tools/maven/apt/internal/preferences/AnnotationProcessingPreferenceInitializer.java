/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.jboss.tools.maven.apt.internal.preferences;

import org.jboss.tools.maven.apt.MavenJdtAptPlugin;
import org.jboss.tools.maven.apt.preferences.AnnotationProcessingMode;
import org.jboss.tools.maven.apt.preferences.PreferencesConstants;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;


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
