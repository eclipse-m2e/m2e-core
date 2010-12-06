/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

import org.codehaus.plexus.util.AbstractScanner;

public class ResourceDeltaScanner extends AbstractScanner {

  protected final IResourceDelta delta;

  protected final List<String> includedDirectories = new ArrayList<String>();

  protected final List<String> includedFiles = new ArrayList<String>();

  protected final boolean deleted;
  
  public ResourceDeltaScanner(IResourceDelta delta, boolean deleted) {
    this.delta = delta;
    this.deleted = deleted;
  }

  public String[] getIncludedDirectories() {
    return includedDirectories.toArray(new String[includedDirectories.size()]);
  }

  public String[] getIncludedFiles() {
    return includedFiles.toArray(new String[includedFiles.size()]);
  }

  public void scan() {
    try {
      setupDefaultFilters();
      scanDelta();
    } catch(CoreException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void scanDelta() throws CoreException {
    delta.accept(new IResourceDeltaVisitor() {

      @SuppressWarnings("synthetic-access")
      public boolean visit(IResourceDelta delta) {
        String relpath = getRelativePath(delta);
        if (isInteresting(delta) && isIncluded(relpath) && !isExcluded(relpath)) {
          IResource resource = delta.getResource();
          if (resource instanceof IContainer) {
            includedDirectories.add(relpath);
          } else {
            includedFiles.add(relpath);
          }
          return true;
        } else if (delta.getResource() instanceof IFolder) {
          return couldHoldIncluded(relpath);
        }

        return false;
      }

    });
  }

  protected boolean isInteresting(IResourceDelta delta) {
    return deleted? EclipseIncrementalBuildContext.isRemove(delta): EclipseIncrementalBuildContext.isContentChange(delta);
  }

  protected String getRelativePath(IResourceDelta delta) {
    return delta.getFullPath().removeFirstSegments(this.delta.getFullPath().segmentCount()).toOSString();
  }

  public File getBasedir() {
    return delta.getResource().getLocation().toFile();
  }

}
