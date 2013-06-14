/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.util;

import java.io.File;

import org.eclipse.core.resources.IFile;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * Represents single MavenProject in project parent hierarchy.
 * 
 * @since 1.5
 */
public class ParentHierarchyEntry {

  private final MavenProject project;

  private final IMavenProjectFacade facade;

  ParentHierarchyEntry(MavenProject project, IMavenProjectFacade facade) {
    this.project = project;
    this.facade = facade;
  }

  /**
   * Returns MavenProject of this parent hierarchy entry. Never {@code null}.
   */
  public MavenProject getProject() {
    return project;
  }

  /**
   * Returns pom.xml file this parent hierarchy entry was read from. Shortcut for {@code getProject().getFile()}. Never
   * {@code null}.
   */
  public File getFile() {
    return project.getFile();
  }

  /**
   * Returns IMavenProjectFacade that corresponds to this parent hierarchy entry. Returns {@code null} if the entry was
   * resolved from a Maven repository and not from Eclipse workspace project.
   */
  public IMavenProjectFacade getFacade() {
    return facade;
  }

  /**
   * Returns IFile this parent hierarchy entry was read from. Returns {@code null} if the entry was resolved from a
   * Maven repository and not from Eclipse workspace project.
   */
  public IFile getResource() {
    return facade != null ? facade.getPom() : null;
  }
}
