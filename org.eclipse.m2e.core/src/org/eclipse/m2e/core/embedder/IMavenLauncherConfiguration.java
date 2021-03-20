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

package org.eclipse.m2e.core.embedder;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * Receive notification of content of plexus configuration.
 * 
 * @author Igor Fedorenko
 * @see MavenRuntime#createLauncherConfiguration
 * @deprecated as of version 1.5, m2e does not provide API to access or configure Maven Installations
 */
@Deprecated
public interface IMavenLauncherConfiguration {

  /**
   * Special realm name used for launcher classpath entries.
   */
  public static final String LAUNCHER_REALM = "]launcher"; //$NON-NLS-1$

  public void setMainType(String type, String realm);

  public void addRealm(String realm);

  public void addProjectEntry(IMavenProjectFacade facade);

  public void addArchiveEntry(String entry) throws CoreException;
}
