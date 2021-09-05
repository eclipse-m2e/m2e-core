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

package org.eclipse.m2e.core.internal.builder.plexusbuildapi;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.Scanner;

import org.sonatype.plexus.build.incremental.EmptyScanner;

import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework;


public class EclipseIncrementalBuildContext extends AbstractEclipseBuildContext {

  private final IResourceDelta delta;

  public EclipseIncrementalBuildContext(IResourceDelta delta, Map<String, Object> context,
      IIncrementalBuildFramework.BuildResultCollector results) {
    super(context, results);
    this.delta = delta;
  }

  @Override
  public boolean hasDelta(String relPath) {
    IPath path = new Path(relPath);
    return hasDelta(path);
  }

  protected boolean hasDelta(IPath path) {
    return delta == null || path == null || delta.findMember(path) != null;
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public boolean hasDelta(List relPaths) {
    for(String relPath : (List<String>) relPaths) {
      if(hasDelta(relPath)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean hasDelta(File file) {
    return hasDelta(getRelativePath(file));
  }

  @Override
  public Scanner newDeleteScanner(File basedir) {
    IResourceDelta reldelta = getDelta(basedir);

    if(reldelta == null || !isRemove(reldelta)) {
      return new EmptyScanner(basedir);
    }

    return new ResourceDeltaScanner(reldelta, true);
  }

  @Override
  public Scanner newScanner(File basedir) {
    return newScanner(basedir, false);
  }

  @Override
  public Scanner newScanner(File basedir, boolean ignoreDelta) {
    if(!ignoreDelta) {
      IResourceDelta reldelta = getDelta(basedir);

      if(reldelta == null || !isContentChange(reldelta)) {
        return new EmptyScanner(basedir);
      }

      return new ResourceDeltaScanner(reldelta, false);
    }

    DirectoryScanner ds = new DirectoryScanner();
    ds.setBasedir(basedir);
    return ds;
  }

  private IResourceDelta getDelta(File file) {
    IPath relpath = getRelativePath(file);
    if(relpath == null) {
      return null;
    }
    return delta.findMember(relpath);
  }

  static boolean isContentChange(IResourceDelta delta) {
    int kind = delta.getKind();
    if(IResourceDelta.ADDED == kind) {
      return true;
    }

    if(IResourceDelta.CHANGED == kind) {
      if(delta.getResource() instanceof IContainer) {
        return true;
      }

      int flags = delta.getFlags();

      return (flags & IResourceDelta.CONTENT) != 0;
    }

    return false;
  }

  static boolean isRemove(IResourceDelta delta) {
    int kind = delta.getKind();

    if(IResourceDelta.REMOVED == kind) {
      return true;
    }

    if(IResourceDelta.CHANGED == kind && delta.getResource() instanceof IContainer) {
      return true;
    }

    return false;
  }

  @Override
  protected IResource getBaseResource() {
    return delta.getResource();
  }

  @Override
  public boolean isIncremental() {
    return true;
  }

}
