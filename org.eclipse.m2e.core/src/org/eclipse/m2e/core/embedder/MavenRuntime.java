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
import org.eclipse.core.runtime.IProgressMonitor;


/**
 * Maven runtime
 * 
 * @author Eugene Kuleshov
 * @author Igor Fedorenko
 * @noimplement This interface is not intended to be implemented by clients.
 * @deprecated as of version 1.5, m2e does not provide API to access or configure Maven Installations
 */
@Deprecated
@SuppressWarnings("deprecation")
public interface MavenRuntime {

  public abstract boolean isEditable();

  /**
   * Reads m2.conf file and notifies configuration collector of the logical content of plexus configuration. Collector
   * callback methods are invoked in the order corresponding configuration elements are present in m2.conf file.
   */
  public abstract void createLauncherConfiguration(IMavenLauncherConfiguration collector, IProgressMonitor monitor)
      throws CoreException;

  public abstract String getLocation();

  public abstract String getSettings();

  public abstract boolean isAvailable();

  public String getVersion();
}
