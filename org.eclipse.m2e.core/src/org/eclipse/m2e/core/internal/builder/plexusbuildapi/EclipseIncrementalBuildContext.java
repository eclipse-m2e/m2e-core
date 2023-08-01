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
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.Scanner;

import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.EmptyScanner;
import org.sonatype.plexus.build.incremental.ThreadBuildContext;

import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework;
import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework.BuildDelta;
import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework.BuildResultCollector;


public class EclipseIncrementalBuildContext implements BuildContext, IIncrementalBuildFramework.BuildContext {

  private final BuildDelta delta;

  private BuildResultCollector results;

  private Map<String, Object> context;

  private File baseDir;

  public EclipseIncrementalBuildContext(IResourceDelta delta, Map<String, Object> context,
      IIncrementalBuildFramework.BuildResultCollector results) {
    this(new EclipseResourceBuildDelta(delta), context, results,
        delta.getResource().getProject().getLocation().toFile());
  }

  public EclipseIncrementalBuildContext(BuildDelta delta, Map<String, Object> context,
      IIncrementalBuildFramework.BuildResultCollector results, File baseDir) {
    this.delta = delta;
    this.context = context;
    this.results = results;
    this.baseDir = baseDir;
  }

  @Override
  public boolean hasDelta(String relPath) {
    return hasDelta(new File(baseDir, relPath));
  }

  @Override
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
    return delta.hasDelta(file);
  }

  @Override
  public Scanner newDeleteScanner(File basedir) {
    IResourceDelta reldelta = getDelta(basedir);

    if(reldelta == null || !isRemove(reldelta)) {
      return new EmptyScanner(basedir);
    }

    return new ResourceDeltaScanner(reldelta, true);
  }

  private IResourceDelta getDelta(File file) {
    IResourceDelta adapt = Adapters.adapt(delta, IResourceDelta.class);
    if(adapt == null) {
      return null;
    }
    IPath relpath = getRelativePath(file);
    if(relpath == null) {
      return null;
    }
    return adapt.findMember(relpath);
  }

  private IPath getRelativePath(File file) {
    IResource adapt = Adapters.adapt(delta, IResource.class);
    if(adapt == null) {
      return null;
    }
    IPath basepath = adapt.getLocation();
    IPath path = Path.fromOSString(file.getAbsolutePath());
    if(!basepath.isPrefixOf(path)) {
      return null;
    }
    return path.removeFirstSegments(basepath.segmentCount());
  }

  @Override
  public Scanner newScanner(File basedir) {
    return newScanner(basedir, false);
  }

  @Override
  public Scanner newScanner(File basedir, boolean ignoreDelta) {
    DirectoryScanner ds;
    if(ignoreDelta) {
      ds = new DirectoryScanner();
    } else {
      ds = new DirectoryScanner() {
        @Override
        protected boolean isSelected(String name, File file) {
          return hasDelta(file);
        }
      };
    }
    ds.setBasedir(basedir);
    return ds;
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
  public boolean isIncremental() {
    return true;
  }

  @Override
  public void refresh(File file) {
    results.refresh(file);
  }

  @Override
  public OutputStream newFileOutputStream(File file) throws IOException {
    return new ChangedFileOutputStream(file, this);
  }

  @Override
  public void setValue(String key, Object value) {
    context.put(key, value);
  }

  @Override
  public Object getValue(String key) {
    return context.get(key);
  }

  /**
   * @deprecated Use addMessage instead
   */
  @Deprecated
  @Override
  public void addError(File file, int line, int column, String message, Throwable cause) {
    addMessage(file, line, column, message, BuildContext.SEVERITY_ERROR, cause);
  }

  /**
   * @deprecated
   * @deprecated Use addMessage instead
   */
  @Deprecated
  @Override
  public void addWarning(File file, int line, int column, String message, Throwable cause) {
    addMessage(file, line, column, message, BuildContext.SEVERITY_WARNING, cause);
  }

  @Override
  public void addMessage(File file, int line, int column, String message, int severity, Throwable cause) {
    results.addMessage(file, line, column, message, severity, cause);
  }

  @Override
  public void removeMessages(File file) {
    results.removeMessages(file);
  }

  @Override
  public boolean isUptodate(File target, File source) {
    return target != null && target.exists() && source != null && source.exists()
        && target.lastModified() > source.lastModified();
  }

  @Override
  public void release() {
    ThreadBuildContext.setThreadBuildContext(null);
  }

}
