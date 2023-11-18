/*******************************************************************************
 * Copyright (c) 2008-2022 Sonatype, Inc.
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
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.ThreadBuildContext;

import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework;


/**
 * @author igor
 */
public abstract class AbstractEclipseBuildContext implements BuildContext, IIncrementalBuildFramework.BuildContext {

  protected final Map<String, Object> context;

  private final IIncrementalBuildFramework.BuildResultCollector results;

  protected AbstractEclipseBuildContext(Map<String, Object> context,
      IIncrementalBuildFramework.BuildResultCollector results) {
    this.context = context;
    this.results = results;
  }

  @Override
  public void refresh(File file) {
    results.refresh(file);
  }

  @Override
  public OutputStream newFileOutputStream(File file) throws IOException {
    return new ChangedFileOutputStream(file, this);
  }

  /**
   * Returns path relative to delta resource location.
   */
  protected IPath getRelativePath(File file) {
    IPath basepath = getBaseResource().getLocation();
    IPath path = IPath.fromOSString(file.getAbsolutePath());

    if(!basepath.isPrefixOf(path)) {
      return null;
    }

    return path.removeFirstSegments(basepath.segmentCount());
  }

  protected IResource getResource(File file) {
    IPath relpath = getRelativePath(file);
    if(relpath == null) {
      return null;
    }
    IResource baseResource = getBaseResource();
    if(baseResource instanceof IContainer container) {
      return container.findMember(relpath);
    }
    return null;
  }

  protected abstract IResource getBaseResource();

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
    IResource targetResource = getResource(target);
    IResource sourceResource = getResource(source);
    return targetResource != null && targetResource.isAccessible() && !hasDelta(target) && sourceResource != null
        && sourceResource.isAccessible() && !hasDelta(source)
        && targetResource.getLocalTimeStamp() >= sourceResource.getLocalTimeStamp();
  }

  @Override
  public void release() {
    ThreadBuildContext.setThreadBuildContext(null);
  }
}
