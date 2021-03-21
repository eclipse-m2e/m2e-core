/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
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

package org.eclipse.m2e.internal.launch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;


public class MavenLaunchParticipantInfo {

  private final IConfigurationElement extension;

  private MavenLaunchParticipantInfo(IConfigurationElement extension) {
    this.extension = extension;
  }

  public String getId() {
    return extension.getAttribute("id");
  }

  public String getName() {
    return extension.getAttribute("name");
  }

  public IMavenLaunchParticipant createParticipant() throws CoreException {
    return (IMavenLaunchParticipant) extension.createExecutableExtension("class");
  }

  public List<String> getModes() {
    String modes = extension.getAttribute("modes");
    if(modes == null) {
      return Collections.emptyList();
    }
    List<String> result = new ArrayList<>();
    StringTokenizer st = new StringTokenizer(modes, ",");
    while(st.hasMoreTokens()) {
      result.add(st.nextToken().trim());
    }
    return result;
  }

  public static List<MavenLaunchParticipantInfo> readParticipantsInfo() {
    List<MavenLaunchParticipantInfo> result = new ArrayList<>();

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint extensionPoint = registry.getExtensionPoint("org.eclipse.m2e.launching.mavenLaunchParticipants");
    if(extensionPoint != null) {
      for(IExtension extension : extensionPoint.getExtensions()) {
        for(IConfigurationElement element : extension.getConfigurationElements()) {
          result.add(new MavenLaunchParticipantInfo(element));
        }
      }
    }

    return result;
  }
}
