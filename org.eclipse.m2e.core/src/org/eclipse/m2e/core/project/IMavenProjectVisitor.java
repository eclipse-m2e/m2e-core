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

import org.eclipse.core.runtime.CoreException;

/**
 * This interface is implemented by clients that visit MavenProject tree.
 */
public interface IMavenProjectVisitor {

  public static int NONE = 0;

  public static int LOAD = 1 << 0;

  /**
   * Visit Maven project or project module
   * 
   * @param projectFacade a facade for visited Maven project
   * @return true if nested artifacts and modules should be visited
   */
  public boolean visit(IMavenProjectFacade projectFacade) throws CoreException;

  /**
   * Visit Maven project dependency/artifact
   * 
   * @param projectFacade a facade for visited Maven project
   * @param artifact an artifact for project dependency
   */
//  public void visit(IMavenProjectFacade projectFacade, Artifact artifact);

}
