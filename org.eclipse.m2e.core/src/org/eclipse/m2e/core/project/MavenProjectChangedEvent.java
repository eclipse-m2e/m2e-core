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

package org.eclipse.m2e.core.project;

import org.eclipse.core.resources.IFile;

public class MavenProjectChangedEvent {

  private final IFile source;
  
  private final int kind;

  private final int flags;

  public static final int KIND_ADDED = 1;

  public static final int KIND_REMOVED = 2;

  public static final int KIND_CHANGED = 3;

  public static final int FLAG_NONE = 0;

  public static final int FLAG_DEPENDENCIES = 1;

  public static final int FLAG_DEPENDENCY_SOURCES = 2;

  public static final int FLAG_ENTRY_SOURCES = 3;

  private final IMavenProjectFacade oldMavenProject;

  private final IMavenProjectFacade mavenProject;

  public MavenProjectChangedEvent(IFile source, int kind, int flags, IMavenProjectFacade oldMavenProject, IMavenProjectFacade mavenProject) {
    this.source = source;
    this.kind = kind;
    this.flags = flags;
    this.oldMavenProject = oldMavenProject;
    this.mavenProject = mavenProject;
  }

  public int getKind() {
    return kind;
  }

  public int getFlags() {
    return flags;
  }

  public IMavenProjectFacade getMavenProject() {
    return mavenProject;
  }

  public IMavenProjectFacade getOldMavenProject() {
    return oldMavenProject;
  }

  public IFile getSource() {
    return source;
  }
}
