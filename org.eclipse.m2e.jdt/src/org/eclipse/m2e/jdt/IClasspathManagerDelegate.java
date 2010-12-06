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

package org.eclipse.m2e.jdt;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.m2e.core.project.IMavenProjectFacade;

/**
 * IBuildPathManagerDelegate
 * 
 * @author igor
 */
public interface IClasspathManagerDelegate {

  public void populateClasspath(IClasspathDescriptor classpath, IMavenProjectFacade projectFacade, int kind,
      IProgressMonitor monitor) throws CoreException;

}
