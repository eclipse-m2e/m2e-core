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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;

import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.Scanner;

import org.sonatype.plexus.build.incremental.EmptyScanner;

import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework;


/**
 * EclipseBuildContext
 *
 * @author igor
 */
public class EclipseBuildContext extends AbstractEclipseBuildContext {

  protected final IProject project;

  public EclipseBuildContext(IProject project, Map<String, Object> context,
      IIncrementalBuildFramework.BuildResultCollector results) {
    super(context, results);
    this.project = project;
  }

  @Override
  public boolean hasDelta(String relpath) {
    return true;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public boolean hasDelta(List relpath) {
    return true;
  }

  @Override
  public boolean hasDelta(File file) {
    return true;
  }

  @Override
  public Scanner newDeleteScanner(File basedir) {
    return new EmptyScanner(basedir);
  }

  @Override
  public Scanner newScanner(File basedir) {
    IPath relpath = getRelativePath(basedir);
    if(relpath != null) {
      IResource resource = project.findMember(relpath);
      return resource != null ? new ResourceScanner(resource) : new EmptyScanner(basedir);
    }
    File projectBasedir = getBaseResource().getFullPath().toFile();
    addMessage(projectBasedir, -1, -1, NLS.bind(Messages.buildConextFileAccessOutsideOfProjectBasedir, basedir),
        SEVERITY_WARNING, null);
    DirectoryScanner ds = new DirectoryScanner();
    ds.setBasedir(basedir);
    return ds;
  }

  @Override
  public Scanner newScanner(File basedir, boolean ignoreDelta) {
    return newScanner(basedir);
  }

  @Override
  protected IProject getBaseResource() {
    return project;
  }

  @Override
  public boolean isIncremental() {
    return false;
  }

}
