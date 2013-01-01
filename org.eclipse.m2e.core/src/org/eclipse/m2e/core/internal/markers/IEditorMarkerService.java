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

package org.eclipse.m2e.core.internal.markers;

import org.eclipse.core.resources.IFile;

import org.apache.maven.project.MavenProject;


/**
 * IEditorMarkerService
 * 
 * @author mkleint
 */
public interface IEditorMarkerService {
  /**
   * adds m2e's own editor markers to the pom file in question
   * 
   * @param markerManager
   * @param pom
   * @param mavenProject
   * @param type
   */
  void addEditorHintMarkers(IMavenMarkerManager markerManager, IFile pom, MavenProject mavenProject, String type);
}
