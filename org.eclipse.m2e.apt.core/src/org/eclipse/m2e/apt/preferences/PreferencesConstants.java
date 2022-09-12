/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.apt.preferences;

import java.util.Map;

import org.eclipse.m2e.apt.MavenJdtAptPlugin;


/**
 * PreferencesConstants
 *
 * @author Fred Bricon
 */
public class PreferencesConstants {

  public static final String USE_PROJECT_SPECIFIC_SETTINGS = "useProjectSpecificSettings"; //$NON-NLS-1$

  public static final String ANNOTATION_PROCESS_DURING_RECONCILE = MavenJdtAptPlugin.PLUGIN_ID
      + ".aptProcessDuringReconcile"; //$NON-NLS-1$

  public static final String MODE = MavenJdtAptPlugin.PLUGIN_ID + ".mode"; //$NON-NLS-1$

  public static final Map<String, String> DEFAULT_OPTIONS = Map.of(MODE, AnnotationProcessingMode.disabled.toString(),
      ANNOTATION_PROCESS_DURING_RECONCILE, "true");

  private PreferencesConstants() { // prevent instantiation
  }

}
