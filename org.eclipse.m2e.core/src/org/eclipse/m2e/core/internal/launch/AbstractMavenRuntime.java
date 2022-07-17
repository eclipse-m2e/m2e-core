/*******************************************************************************
 * Copyright (c) 2014, 2022 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Takari, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.launch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;


/**
 * @since 1.5
 */
public abstract class AbstractMavenRuntime {

  /**
   * Receive notification of content of plexus configuration.
   *
   * @author Igor Fedorenko
   * @see AbstractMavenRuntime#createLauncherConfiguration
   */
  public interface IMavenLauncherConfiguration {

    /**
     * Special realm name used for launcher classpath entries.
     */
    String LAUNCHER_REALM = "]launcher"; //$NON-NLS-1$

    void setMainType(String type, String realm);

    void addRealm(String realm);

    void addProjectEntry(IMavenProjectFacade facade);

    void addArchiveEntry(String entry) throws CoreException;
  }

  private final String name;

  private List<ClasspathEntry> extensions;

  protected AbstractMavenRuntime(String name) {
    this.name = name;
  }

  public String getName() {
    return name != null ? name : getLocation();
  }

  public List<ClasspathEntry> getExtensions() {
    return extensions;
  }

  public void setExtensions(List<ClasspathEntry> extensions) {
    this.extensions = extensions != null && !extensions.isEmpty() ? new ArrayList<>(extensions) : null;
  }

  public boolean isLegacy() {
    return name == null;
  }

  protected void collectExtensions(IMavenLauncherConfiguration collector, IProgressMonitor monitor)
      throws CoreException {
    if(extensions != null) {
      IMavenProjectRegistry registry = MavenPlugin.getMavenProjectRegistry();
      for(ClasspathEntry entry : extensions) {
        if(entry instanceof ProjectClasspathEntry projectClasspathEntry) {
          collectProject(collector, projectClasspathEntry, registry, monitor);
        }
      }
    }
  }

  private void collectProject(IMavenLauncherConfiguration collector, ProjectClasspathEntry entry,
      IMavenProjectRegistry registry, IProgressMonitor monitor) throws CoreException {
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(entry.getProject());
    IMavenProjectFacade facade = registry.create(project, monitor);
    if(facade == null) {
      throw new CoreException(Status.error(NLS.bind(Messages.AbstractMavenRuntime_unknownProject, entry.getProject())));
    }
    collector.addProjectEntry(facade);
    MavenProject mavenProject = facade.getMavenProject(monitor);
    for(Artifact dependency : mavenProject.getArtifacts()) {
      if(Artifact.SCOPE_COMPILE.equals(dependency.getScope()) || Artifact.SCOPE_RUNTIME.equals(dependency.getScope())) {
        collector.addArchiveEntry(dependency.getFile().getAbsolutePath());
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    return o != null && getClass().equals(o.getClass()) && getName().equals(((AbstractMavenRuntime) o).getName());
  }

  @Override
  public int hashCode() {
    return getName().hashCode();
  }

  protected boolean isSupportedVersion() {
    DefaultArtifactVersion version = new DefaultArtifactVersion(getVersion());
    return version.getMajorVersion() >= 3;
  }

  public String getSettings() {
    String settings = MavenPlugin.getMavenConfiguration().getGlobalSettingsFile();
    if(settings != null && !settings.isBlank()) {
      try {
        settings = new File(settings).getCanonicalPath();
      } catch(IOException ex) {
      }
    }
    return settings;
  }

  public abstract boolean isEditable();

  /**
   * Reads m2.conf file and notifies configuration collector of the logical content of plexus configuration. Collector
   * callback methods are invoked in the order corresponding configuration elements are present in m2.conf file.
   */
  public abstract void createLauncherConfiguration(IMavenLauncherConfiguration collector, IProgressMonitor monitor)
      throws CoreException;

  public abstract String getLocation();

  public abstract boolean isAvailable();

  public abstract String getVersion();
}
