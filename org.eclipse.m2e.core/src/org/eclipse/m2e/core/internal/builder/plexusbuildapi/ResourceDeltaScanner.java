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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

import org.codehaus.plexus.util.AbstractScanner;


public class ResourceDeltaScanner extends AbstractScanner {

  protected final IResourceDelta delta;

  protected final List<String> includedDirectories = new ArrayList<>();

  protected final List<String> includedFiles = new ArrayList<>();

  protected final boolean deleted;

  public ResourceDeltaScanner(IResourceDelta delta, boolean deleted) {
    this.delta = delta;
    this.deleted = deleted;
  }

  @Override
  public String[] getIncludedDirectories() {
    return includedDirectories.toArray(new String[includedDirectories.size()]);
  }

  @Override
  public String[] getIncludedFiles() {
    return includedFiles.toArray(new String[includedFiles.size()]);
  }

  @Override
  public void scan() {
    try {
      setupDefaultFilters();
      setupMatchPatterns();
      scanDelta();
    } catch(CoreException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void scanDelta() throws CoreException {
    delta.accept(new IResourceDeltaVisitor() {

      @Override
      @SuppressWarnings("synthetic-access")
      public boolean visit(IResourceDelta delta) {
        String relpath = getRelativePath(delta);
        if(isInteresting(delta) && isIncluded(relpath) && !isExcluded(relpath)) {
          IResource resource = delta.getResource();
          if(resource instanceof IContainer) {
            includedDirectories.add(relpath);
          } else {
            includedFiles.add(relpath);
          }
          return true;
        } else if(delta.getResource() instanceof IContainer) {
          return couldHoldIncluded(relpath);
        }

        return false;
      }

    });
  }

  protected boolean isInteresting(IResourceDelta delta) {
    return deleted ? EclipseIncrementalBuildContext.isRemove(delta) : EclipseIncrementalBuildContext
        .isContentChange(delta);
  }

  protected String getRelativePath(IResourceDelta delta) {
    return delta.getFullPath().removeFirstSegments(this.delta.getFullPath().segmentCount()).toOSString();
  }

  @Override
  public File getBasedir() {
    return delta.getResource().getLocation().toFile();
  }

}
