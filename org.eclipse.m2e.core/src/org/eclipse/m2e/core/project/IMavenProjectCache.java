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
package org.eclipse.m2e.core.project;

import org.apache.maven.project.MavenProject;

/**
 * interface implemented by maven pom's xml editor SourceViewer for storing and retrieving the latest valid
 * MavenProject cached for various xml editor related features. 
 * 
 * 
 * Note: Added here in order not to introduce a dependency between editor and xml.editor projects. If that is not a problem, 
 * probably move to editor.
 * @author mkleint
 */
public interface IMavenProjectCache {

  MavenProject getMavenProject();
}
