/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.tests.common;

import java.io.File;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;


public class MavenHelpers {

  public static String setUserSettings(String settingsFile) throws CoreException {
    if(settingsFile != null && settingsFile.length() > 0) {
      settingsFile = new File(settingsFile).getAbsolutePath();
    }
    IMavenConfiguration mavenConfiguration = MavenPlugin.getMavenConfiguration();
    String oldUserSettingsFile = mavenConfiguration.getUserSettingsFile();
    mavenConfiguration.setUserSettingsFile(settingsFile);
    return oldUserSettingsFile;
  }

  public static String getUserSettings() {
    IMavenConfiguration mavenConfiguration = MavenPlugin.getMavenConfiguration();
    return mavenConfiguration.getUserSettingsFile();
  }

}
