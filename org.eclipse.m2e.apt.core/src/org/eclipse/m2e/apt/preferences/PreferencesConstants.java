/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
