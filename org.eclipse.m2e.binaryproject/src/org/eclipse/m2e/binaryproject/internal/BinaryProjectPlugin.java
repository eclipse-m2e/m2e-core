/*******************************************************************************
 * Copyright (c) 2012 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.binaryproject.internal;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

public class BinaryProjectPlugin implements BundleActivator {

  public static final String PLUGIN_ID = "org.eclipse.m2e.binaryproject";

  public static final String LIFECYCLE_MAPPING_ID = "org.eclipse.m2e.binaryproject";

  public static final String P_GROUPID = "groupId";

  public static final String P_ARTIFACTID = "artifactId";

  public static final String P_VERSION = "version";

  public static final String P_TYPE = "type";

  public static final String P_CLASSIFIER = "classifier";

  /**
   * Name of IProject persistent property that identifies absolute filesystem path of the target jar artifact of the
   * workspace binary project.
   */
  public static final QualifiedName QNAME_JAR = new QualifiedName(PLUGIN_ID, "jar");

  private static BinaryProjectPlugin SELF;

  public static BinaryProjectPlugin getInstance() {
    return SELF;
  }

  private Bundle bundle;

  public IProject create(String groupId, String artifactId, String version, List<ArtifactRepository> repositories,
      IProgressMonitor monitor) throws CoreException {
    IMaven maven = MavenPlugin.getMaven();

    Artifact pomArtifact =
        maven.resolve(groupId, artifactId, version, "pom" /* type */, null /* classifier */, repositories, monitor);

    ResolverConfiguration resolverConfig = new ResolverConfiguration();
    resolverConfig.setLifecycleMappingId(LIFECYCLE_MAPPING_ID);

    String projectName = groupId + "_" + artifactId + "_" + version;

    IPath stateLocation = Platform.getStateLocation(bundle);

    IPath projectLocation = stateLocation.append(projectName);
    projectLocation.toFile().mkdirs();

    File pomFile = new File(projectLocation.toFile(), "pom.xml");

    try {
      FileUtils.copyFile(pomArtifact.getFile(), pomFile);
    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, "Could not create binary project", e));
    }

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = workspace.getRoot();

    IProject project = root.getProject(projectName);

    IProjectDescription description = workspace.newProjectDescription(projectName);
    description.setLocation(projectLocation);
    project.create(description, monitor);
    project.open(monitor);

    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(BinaryProjectPlugin.PLUGIN_ID);

    projectNode.put(P_GROUPID, groupId);
    projectNode.put(P_ARTIFACTID, artifactId);
    projectNode.put(P_VERSION, version);
    // String type = projectNode.get( P_TYPE, "jar" );
    // String classifier = projectNode.get( P_CLASSIFIER, (String) null );

    try {
      projectNode.flush();
    } catch (BackingStoreException e) {
      throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, "Could not create binary project", e));
    }

    IProjectConfigurationManager configManager = MavenPlugin.getProjectConfigurationManager();

    configManager.enableMavenNature(project, resolverConfig, monitor);

    return project;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    this.bundle = context.getBundle();
    BinaryProjectPlugin.SELF = this;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    BinaryProjectPlugin.SELF = null;
    this.bundle = null;
  }
}
