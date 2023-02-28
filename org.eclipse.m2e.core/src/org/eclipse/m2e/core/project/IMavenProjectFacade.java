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

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.ArtifactRef;
import org.eclipse.m2e.core.embedder.ArtifactRepositoryRef;
import org.eclipse.m2e.core.embedder.IComponentLookup;
import org.eclipse.m2e.core.embedder.IMavenExecutableLocation;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


/**
 * IMavenProjectFacade
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @author Igor Fedorenko
 */
public interface IMavenProjectFacade extends IMavenExecutableLocation {

  /**
   * Returns project relative paths of resource directories
   */
  List<IPath> getResourceLocations();

  /**
   * Returns project relative paths of test resource directories
   */
  List<IPath> getTestResourceLocations();

  List<IPath> getCompileSourceLocations();

  List<IPath> getTestCompileSourceLocations();

  /**
   * Returns project resource for given file system location or null the location is outside of project.
   *
   * @param resourceLocation absolute file system location
   * @return IPath the full, absolute workspace path resourceLocation
   */
  IPath getProjectRelativePath(String resourceLocation);

  /**
   * @return the full, absolute path of this project where build results are placed relative to the workspace or
   *         <code>null</code> if the directory cannot be determined or is outside of the workspace.
   */
  IPath getBuildOutputLocation();

  /**
   * Returns the full, absolute path of this project maven build output directory relative to the workspace or null if
   * maven build output directory cannot be determined or outside of the workspace.
   */
  IPath getOutputLocation();

  /**
   * Returns the full, absolute path of this project maven build test output directory relative to the workspace or null
   * if maven build output directory cannot be determined or outside of the workspace.
   */
  IPath getTestOutputLocation();

  String getFinalName();

  IPath getFullPath();

  /**
   * Lazy load and cache MavenProject instance
   */
  MavenProject getMavenProject(IProgressMonitor monitor) throws CoreException;

  /**
   * Returns cached MavenProject instance associated with this facade or <code>null</code>, if the cache has not been
   * populated yet, for example right after workspace restart. Clients must use
   * {@link #getMavenProject(IProgressMonitor)} unless they are prepared to deal with <code>null</code> return value.
   */
  MavenProject getMavenProject();

  String getPackaging();

  IProject getProject();

  IFile getPom();

  /**
   * Returns the full, absolute path of the given file relative to the workspace. Returns null if the file does not
   * exist or is not a member of this project.
   */
  IPath getFullPath(File file);

  List<String> getMavenProjectModules();

  Set<ArtifactRef> getMavenProjectArtifacts();

  /**
   * @return the configuration associated with this facade
   */
  IProjectConfiguration getConfiguration();

  /**
   * @deprecated use {@link #getConfiguration()} instead
   * @return
   */
  @Deprecated(forRemoval = true)
  ResolverConfiguration getResolverConfiguration();

  /**
   * @return true if maven project needs to be re-read from disk
   */
  boolean isStale();

  ArtifactKey getArtifactKey();

  /**
   * Associates the value with the key in session (i.e. transient) context. Intended as a mechanism to cache state
   * derived from MavenProject. Session properties are cleared when MavenProject is re-read from disk.
   *
   * @see #getSessionProperty(String)
   */
  void setSessionProperty(String key, Object value);

  /**
   * @return the value associated with the key in session context or null if the key is not associated with any value.
   * @see #setSessionProperty(String, Object)
   */
  Object getSessionProperty(String key);

  Set<ArtifactRepositoryRef> getArtifactRepositoryRefs();

  Set<ArtifactRepositoryRef> getPluginArtifactRepositoryRefs();

  /**
   * Returns fully setup MojoExecution instance bound to project build lifecycle that matches provided mojoExecutionKey.
   * Returns null if no such mojo execution.
   */
  MojoExecution getMojoExecution(MojoExecutionKey mojoExecutionKey, IProgressMonitor monitor)
      throws CoreException;

  MavenExecutionPlan calculateExecutionPlan(Collection<String> tasks, IProgressMonitor monitor);

  MavenExecutionPlan setupExecutionPlan(Collection<String> tasks, IProgressMonitor monitor);

  /**
   * Returns list of fully setup MojoExecution instances bound to project build lifecycle that matche provided groupId,
   * artifactId and (vararg) goals. Returns empty list if no such mojo executions.
   */
  List<MojoExecution> getMojoExecutions(String groupId, String artifactId, IProgressMonitor monitor,
      String... goals) throws CoreException;

  // lifecycle mapping

  String getLifecycleMappingId();

  Map<MojoExecutionKey, List<IPluginExecutionMetadata>> getMojoExecutionMapping();

  /**
   * @return a project-specific Maven execution context.
   * @since 2.0
   */
  IMavenExecutionContext createExecutionContext();

  /**
   * Returns the component lookup for this projects context. This will include potentially defined
   * <b>core</b>-extensions, but not <b>project</b>-scoped extensions! If you need project-scoped extensions as well use
   * {@link #createExecutionContext()};
   */
  IComponentLookup getComponentLookup();

}
