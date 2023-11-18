/*******************************************************************************
 * Copyright (c) 2008-2021 Sonatype, Inc.
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
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import org.apache.maven.model.Resource;


/**
 * Collection of helper methods to map between MavenProject and IResource.
 *
 * @author igor
 */
public class MavenProjectUtils {

  private MavenProjectUtils() {
  }

  /**
   * Returns project resource path for given file-system location or null if the location is outside of project.
   *
   * @param resourceLocation absolute file-system location
   * @return IPath the full, absolute workspace path of resourceLocation
   */
  public static IPath getProjectRelativePath(IProject project, String resourceLocation) {
    if(project == null || resourceLocation == null) {
      return null;
    }
    IPath projectLocation = project.getLocation();
    IPath directory = IPath.fromOSString(resourceLocation); // this is an absolute path!
    if(projectLocation == null || !projectLocation.isPrefixOf(directory)) {
      return null;
    }
    return directory.removeFirstSegments(projectLocation.segmentCount()).makeRelative().setDevice(null);
  }

  public static List<IPath> getResourceLocations(IProject project, List<Resource> resources) {
    LinkedHashSet<IPath> locations = new LinkedHashSet<>();
    for(Resource resource : resources) {
      locations.add(getProjectRelativePath(project, resource.getDirectory()));
    }
    locations.remove(null);
    return List.copyOf(locations);
  }

  public static List<IPath> getSourceLocations(IProject project, List<String> roots) {
    LinkedHashSet<IPath> locations = new LinkedHashSet<>();
    for(String root : roots) {
      IPath path = getProjectRelativePath(project, root);
      if(path != null) {
        locations.add(path);
      }
    }
    return List.copyOf(locations);
  }

  /**
   * Returns the {@link IResource} of the given project that has the same absolute path in the local file system like
   * the given file or null if the file does not point into the project or no such resource <b>exists in the
   * workspace</b>.
   */
  public static IResource getProjectResource(IProject project, File file) {
    String resourceLocation = file != null ? file.getAbsolutePath() : null;
    IPath relativePath = getProjectRelativePath(project, resourceLocation);
    return relativePath != null ? project.findMember(relativePath) : null;
  }

  /**
   * Returns the full, absolute path of the given file relative to the workspace. Returns null if the file does not
   * exist or is not a member of this project.
   */
  public static IPath getFullPath(IProject project, File file) {
    IResource resource = getProjectResource(project, file);
    return resource != null ? resource.getFullPath() : null;
  }
}

