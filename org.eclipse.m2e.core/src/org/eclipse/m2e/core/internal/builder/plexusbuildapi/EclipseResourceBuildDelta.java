/*******************************************************************************
 * Copyright (c) 2008-2023 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Christoph Läubrich - extracted and implement wrapper API
 *******************************************************************************/

package org.eclipse.m2e.core.internal.builder.plexusbuildapi;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework;


public final class EclipseResourceBuildDelta implements IIncrementalBuildFramework.BuildDelta, IAdaptable {
  private final IResource resource;

  private final IResourceDelta delta;

  /**
   * @param resource
   * @param delta
   */
  public EclipseResourceBuildDelta(IResourceDelta delta) {
    this.resource = delta != null ? delta.getResource() : null;
    this.delta = delta;
  }

  @Override
  public boolean hasDelta(File file) {
    return hasDelta(getRelativePath(file));
  }

  private boolean hasDelta(IPath path) {
    return path == null || this.delta.findMember(path) != null;
  }

  /**
   * Returns path relative to delta resource location.
   */
  private IPath getRelativePath(File file) {
    IPath basepath = this.resource.getLocation();
    IPath path = IPath.fromOSString(file.getAbsolutePath());

    if(!basepath.isPrefixOf(path)) {
      return null;
    }

    return path.removeFirstSegments(basepath.segmentCount());
  }

  @Override
  public <T> T getAdapter(Class<T> adapter) {
    if(adapter.isInstance(delta)) {
      return adapter.cast(delta);
    }
    if(adapter.isInstance(resource)) {
      return adapter.cast(resource);
    }
    return null;
  }

}
