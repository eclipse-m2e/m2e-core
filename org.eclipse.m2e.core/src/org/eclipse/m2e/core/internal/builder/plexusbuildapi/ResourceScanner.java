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
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.codehaus.plexus.util.AbstractScanner;


/**
 * WorkspaceScanner
 *
 * @author igor
 */
public class ResourceScanner extends AbstractScanner {

  protected final IResource resource;

  protected final List<String> includedDirectories = new ArrayList<>();

  protected final List<String> includedFiles = new ArrayList<>();

  public ResourceScanner(IResource resource) {
    this.resource = resource;
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
      scanResource();
    } catch(CoreException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void scanResource() throws CoreException {
    resource.accept(resource -> {
      String relpath = getRelativePath(resource);
      if(isIncluded(relpath) && !isExcluded(relpath)) {
        if(resource instanceof IContainer) {
          includedDirectories.add(relpath);
        } else {
          includedFiles.add(relpath);
        }
        return true;
      } else if(resource instanceof IFolder) {
        return couldHoldIncluded(relpath);
      }

      return false;
    });
  }

  protected String getRelativePath(IResource resource) {
    return resource.getFullPath().removeFirstSegments(this.resource.getFullPath().segmentCount()).toOSString();
  }

  @Override
  public File getBasedir() {
    return resource.getLocation().toFile();
  }

}
