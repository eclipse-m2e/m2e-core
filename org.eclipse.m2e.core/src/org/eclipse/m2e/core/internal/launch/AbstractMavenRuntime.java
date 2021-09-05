/*******************************************************************************
 * Copyright (c) 2014 Takari, Inc.
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

import org.eclipse.aether.util.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenLauncherConfiguration;
import org.eclipse.m2e.core.embedder.MavenRuntime;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;


/**
 * @since 1.5
 */
@SuppressWarnings("deprecation")
public abstract class AbstractMavenRuntime implements MavenRuntime {

  private static final VersionRange SUPPORTED_VERSION;

  static {
    VersionRange supportedVersion;
    try {
      supportedVersion = VersionRange.createFromVersionSpec("[3.0,)");
    } catch(InvalidVersionSpecificationException ex) {
      supportedVersion = null;
    }
    SUPPORTED_VERSION = supportedVersion;
  }

  private static final IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();

  private static final IMavenProjectRegistry registry = MavenPlugin.getMavenProjectRegistry();

  private final String name;

  private List<ClasspathEntry> extensions;

  @Deprecated
  protected AbstractMavenRuntime() {
    this.name = null;
  }

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
      for(ClasspathEntry entry : extensions) {
        if(entry instanceof ProjectClasspathEntry) {
          collectProject(collector, (ProjectClasspathEntry) entry, monitor);
        }
      }
    }
  }

  private void collectProject(IMavenLauncherConfiguration collector, ProjectClasspathEntry entry,
      IProgressMonitor monitor) throws CoreException {
    IProject project = workspace.getProject(entry.getProject());
    IMavenProjectFacade facade = registry.create(project, monitor);
    if(facade == null) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, NLS.bind(
          Messages.AbstractMavenRuntime_unknownProject, entry.getProject())));
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
    if(o != null && getClass().equals(o.getClass())) {
      return getName().equals(((AbstractMavenRuntime) o).getName());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getName().hashCode();
  }

  protected boolean isSupportedVersion() {
    return SUPPORTED_VERSION != null && SUPPORTED_VERSION.containsVersion(new DefaultArtifactVersion(getVersion()));
  }

  @Override
  public String getSettings() {
    String settings = MavenPlugin.getMavenConfiguration().getGlobalSettingsFile();
    if(!StringUtils.isEmpty(settings)) {
      try {
        settings = new File(settings).getCanonicalPath();
      } catch(IOException ex) {
      }
    }
    return settings;
  }

}
