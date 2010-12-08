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

package org.eclipse.m2e.tests.common;

import java.io.File;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;


public class MavenHelpers {

  public static String setUserSettings(String settingsFile) {
    if(settingsFile != null && settingsFile.length() > 0) {
      settingsFile = new File(settingsFile).getAbsolutePath();
    }
    IMavenConfiguration mavenConfiguration = MavenPlugin.getDefault().getMavenConfiguration();
    String oldUserSettingsFile = mavenConfiguration.getUserSettingsFile();
    mavenConfiguration.setUserSettingsFile(settingsFile);
    return oldUserSettingsFile;
  }

  public static String getUserSettings() {
    IMavenConfiguration mavenConfiguration = MavenPlugin.getDefault().getMavenConfiguration();
    return mavenConfiguration.getUserSettingsFile();
  }

}
