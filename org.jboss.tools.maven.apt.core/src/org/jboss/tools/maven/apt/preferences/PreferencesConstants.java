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

package org.jboss.tools.maven.apt.preferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.tools.maven.apt.MavenJdtAptPlugin;


/**
 * PreferencesConstants
 * 
 * @author Fred Bricon
 */
public class PreferencesConstants {

  public static Map<String, String> DEFAULT_OPTIONS;

  public static String USE_PROJECT_SPECIFIC_SETTINGS = "useProjectSpecificSettings"; //$NON-NLS-1$
  
  public static String ANNOTATION_PROCESS_DURING_RECONCILE = MavenJdtAptPlugin.PLUGIN_ID + ".aptProcessDuringReconcile"; //$NON-NLS-1$

  public static String MODE = MavenJdtAptPlugin.PLUGIN_ID + ".mode"; //$NON-NLS-1$

  static {
    Map<String, String> options = new HashMap<String, String>(1);
    options.put(MODE, AnnotationProcessingMode.disabled.toString());
    options.put(ANNOTATION_PROCESS_DURING_RECONCILE, "true");
    DEFAULT_OPTIONS = Collections.unmodifiableMap(options);
  }

  private PreferencesConstants() {
    // prevent instantiation
  }

}
