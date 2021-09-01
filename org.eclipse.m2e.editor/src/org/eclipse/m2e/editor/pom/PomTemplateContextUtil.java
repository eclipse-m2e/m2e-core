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

package org.eclipse.m2e.editor.pom;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;


class PomTemplateContextUtil {
  private static final Logger log = LoggerFactory.getLogger(PomTemplateContextUtil.class);

  public static final PomTemplateContextUtil INSTANCE = new PomTemplateContextUtil();

  private final Map<String, PluginDescriptor> descriptors = new HashMap<>();

  public PluginDescriptor getPluginDescriptor(String groupId, String artifactId, String version) {
    String name = groupId + ":" + artifactId + ":" + version;
    PluginDescriptor descriptor = descriptors.get(name);
    if(descriptor != null) {
      return descriptor;
    }

    try {
      IMaven embedder = MavenPlugin.getMaven();

      List<ArtifactRepository> repositories = embedder.getArtifactRepositories();

      Artifact artifact = embedder.resolve(groupId, artifactId, version, "maven-plugin", null, repositories, null); //$NON-NLS-1$

      File file = artifact.getFile();
      if(file == null) {
        String msg = "Can't resolve plugin " + name; //$NON-NLS-1$
        log.error(msg);
      } else {
        try (ZipFile zf = new ZipFile(file)) {
          ZipEntry entry = zf.getEntry("META-INF/maven/plugin.xml"); //$NON-NLS-1$
          if(entry != null) {
            InputStream is = zf.getInputStream(entry); // closed when zipFile is closed
            PluginDescriptorBuilder builder = new PluginDescriptorBuilder();
            descriptor = builder.build(new InputStreamReader(is));
            descriptors.put(name, descriptor);
            return descriptor;
          }
        } catch(Exception ex) {
          String msg = "Can't read configuration for " + name; //$NON-NLS-1$
          log.error(msg, ex);
        }
      }

    } catch(CoreException ex) {
      IStatus status = ex.getStatus();
      String msg = status.getMessage();
      if(status.getException() != null) {
        msg += "; " + status.getException().getMessage();
      }
      log.error(msg, ex);
    }
    return null;
  }
}
