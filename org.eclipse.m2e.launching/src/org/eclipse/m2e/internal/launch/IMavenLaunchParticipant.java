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

package org.eclipse.m2e.internal.launch;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;


/**
 * Participates in Maven luanch command line construction.
 * 
 * @noimplement This is an experimental interface and can be changed or removed without notice.
 */
public interface IMavenLaunchParticipant {

  /**
   * Returns additional program arguments or <code>null</code>.
   */
  public String getProgramArguments(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor);

  /**
   * Returns additional vm arguments or <code>null</code>
   */
  public String getVMArguments(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor);
}
