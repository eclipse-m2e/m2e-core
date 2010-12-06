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

package org.eclipse.m2e.core.internal.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;

import org.codehaus.plexus.util.AbstractScanner;

/**
 * WorkspaceScanner
 *
 * @author igor
 */
public class ResourceScanner extends AbstractScanner {
  
  protected final IResource resource;

  protected final List<String> includedDirectories = new ArrayList<String>();

  protected final List<String> includedFiles = new ArrayList<String>();
  
  public ResourceScanner(IResource resource) {
    this.resource = resource;
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
      scanResource();
    } catch(CoreException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void scanResource() throws CoreException {
    resource.accept(new IResourceVisitor() {

      public boolean visit(IResource resource) {
        String relpath = getRelativePath(resource);
        if (isIncluded(relpath) && !isExcluded(relpath)) {
          if (resource instanceof IContainer) {
            includedDirectories.add(relpath);
          } else {
            includedFiles.add(relpath);
          }
          return true;
        } else if (resource instanceof IFolder) {
          return couldHoldIncluded(relpath);
        }

        return false;
      }

    });
  }

  protected String getRelativePath(IResource resource) {
    return resource.getFullPath().removeFirstSegments(this.resource.getFullPath().segmentCount()).toOSString();
  }

  public File getBasedir() {
    return resource.getLocation().toFile();
  }

}
