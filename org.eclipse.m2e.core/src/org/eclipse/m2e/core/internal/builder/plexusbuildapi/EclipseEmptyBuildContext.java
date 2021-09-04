/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

import org.codehaus.plexus.util.Scanner;

import org.sonatype.plexus.build.incremental.EmptyScanner;

import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework;


public class EclipseEmptyBuildContext extends AbstractEclipseBuildContext {

  private final Scanner emptyScanner;

  private final IProject project;

  public EclipseEmptyBuildContext(IProject project, Map<String, Object> context,
      IIncrementalBuildFramework.BuildResultCollector results) {
    super(context, results);
    this.project = project;
    this.emptyScanner = new EmptyScanner(project.getLocation().toFile());
  }

  @Override
  public boolean hasDelta(String relpath) {
    return false;
  }

  @Override
  public boolean hasDelta(File file) {
    return false;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public boolean hasDelta(List relpaths) {
    return false;
  }

  @Override
  public Scanner newScanner(File basedir) {
    return emptyScanner;
  }

  @Override
  public Scanner newDeleteScanner(File basedir) {
    return emptyScanner;
  }

  @Override
  public Scanner newScanner(File basedir, boolean ignoreDelta) {
    // @TODO should ignoreDelta be considered?
    return emptyScanner;
  }

  @Override
  public boolean isIncremental() {
    return true;
  }

  @Override
  protected IResource getBaseResource() {
    return project;
  }

}
