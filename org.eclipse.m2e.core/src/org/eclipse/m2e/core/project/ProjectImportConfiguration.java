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

package org.eclipse.m2e.core.project;

import java.util.regex.Matcher;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.model.Model;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;


/**
 * Project import configuration bean.
 */
public class ProjectImportConfiguration {

  private static final String GROUP_ID = "\\[groupId\\]"; //$NON-NLS-1$

  private static final String ARTIFACT_ID = "\\[artifactId\\]"; //$NON-NLS-1$

  private static final String VERSION = "\\[version\\]"; //$NON-NLS-1$

  private static final String NAME = "\\[name\\]"; //$NON-NLS-1$

  /** resolver configuration bean */
  private ResolverConfiguration resolverConfiguration;

  /** the project name template */
  private String projectNameTemplate = ""; //$NON-NLS-1$

  /** Creates a new configuration. */
  public ProjectImportConfiguration(ResolverConfiguration resolverConfiguration) {
    this.resolverConfiguration = resolverConfiguration;
  }

  /** Creates a new configuration. */
  public ProjectImportConfiguration() {
    this(new ResolverConfiguration());
  }

  /** Returns the resolver configuration bean. */
  public ResolverConfiguration getResolverConfiguration() {
    return resolverConfiguration;
  }

  /** Sets the project name template. */
  public void setProjectNameTemplate(String projectNameTemplate) {
    this.projectNameTemplate = projectNameTemplate;
  }

  /** Returns the project name template. */
  public String getProjectNameTemplate() {
    return projectNameTemplate;
  }

  /**
   * Calculates the project name for the given model.
   * 
   * @deprecated This method does not take into account MavenProjectInfo.basedirRename
   */
  @Deprecated
  public String getProjectName(Model model) {
    // XXX should use resolved MavenProject or Model
    if(projectNameTemplate.length() == 0) {
      return cleanProjectNameComponent(model.getArtifactId(), false);
    }

    String artifactId = model.getArtifactId();
    String groupId = model.getGroupId();
    if(groupId == null && model.getParent() != null) {
      groupId = model.getParent().getGroupId();
    }
    String version = model.getVersion();
    if(version == null && model.getParent() != null) {
      version = model.getParent().getVersion();
    }
    String name = model.getName();
    if(name == null || name.trim().isEmpty()) {
      name = artifactId;
    }

    // XXX needs MavenProjectManager update to resolve groupId and version
    return projectNameTemplate.replaceAll(GROUP_ID, cleanProjectNameComponent(groupId, true))
        .replaceAll(ARTIFACT_ID, cleanProjectNameComponent(artifactId, true))
        .replaceAll(NAME, cleanProjectNameComponent(name, true))
        .replaceAll(VERSION, version == null ? "" : cleanProjectNameComponent(version, true)); //$NON-NLS-1$
  }

  private static final String cleanProjectNameComponent(String value, boolean quote) {
    // remove property placeholders
    value = value.replaceAll("\\$\\{[^\\}]++\\}", ""); //$NON-NLS-1$ $NON-NLS-2$
    if(quote) {
      value = Matcher.quoteReplacement(value);
    }
    return value;
  }

  /**
   * @deprecated This method does not take into account MavenProjectInfo.basedirRename. Use
   *             IMavenProjectImportResult#getProject instead
   */
  @Deprecated
  public IProject getProject(IWorkspaceRoot root, Model model) {
    return root.getProject(getProjectName(model));
  }

  /**
   * @deprecated business logic does not belong to a value object
   */
  @Deprecated
  public IStatus validateProjectName(Model model) {
    String projectName = getProjectName(model);
    IWorkspace workspace = ResourcesPlugin.getWorkspace();

    // check if the project name is valid
    IStatus nameStatus = workspace.validateName(projectName, IResource.PROJECT);
    if(!nameStatus.isOK()) {
      return nameStatus;
    }

    // check if project already exists
    if(workspace.getRoot().getProject(projectName).exists()) {
      return new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, 0,
          NLS.bind(Messages.importProjectExists, projectName), null); //$NON-NLS-1$
    }

    return Status.OK_STATUS;
  }
}
