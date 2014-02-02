/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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


public class EclipseEmptyBuildContext extends AbstractEclipseBuildContext {

  private final Scanner emptyScanner;

  private final IProject project;

  public EclipseEmptyBuildContext(IProject project, Map<String, Object> context) {
    super(context);
    this.project = project;
    this.emptyScanner = new EmptyScanner(project.getLocation().toFile());
  }

  public boolean hasDelta(String relpath) {
    return false;
  }

  public boolean hasDelta(File file) {
    return false;
  }

  @SuppressWarnings("rawtypes")
  public boolean hasDelta(List relpaths) {
    return false;
  }

  public Scanner newScanner(File basedir) {
    return emptyScanner;
  }

  public Scanner newDeleteScanner(File basedir) {
    return emptyScanner;
  }

  public Scanner newScanner(File basedir, boolean ignoreDelta) {
    // @TODO should ignoreDelta be considered?
    return emptyScanner;
  }

  public boolean isIncremental() {
    return true;
  }

  protected IResource getBaseResource() {
    return project;
  }

}
