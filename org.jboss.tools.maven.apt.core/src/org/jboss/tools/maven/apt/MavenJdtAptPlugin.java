/*******************************************************************************
 * Copyright (c) 2011 Knowledge Computing Corp.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Karl M. Davis (Knowledge Computing Corp.) - initial API and implementation
 *******************************************************************************/

package org.jboss.tools.maven.apt;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;


/**
 * This is the {@link BundleActivator} for the Eclipse plugin providing the {@link AptProjectConfigurator}
 * {@link AbstractProjectConfigurator} implementation.
 */
public class MavenJdtAptPlugin implements BundleActivator {
  public static final String PLUGIN_ID = "org.jboss.tools.maven.apt"; //$NON-NLS-1$

  /**
   * Status IDs for system log entries. Must be unique per plugin.
   */
  public static final int STATUS_EXCEPTION = 1;

  private static BundleContext context;

  static BundleContext getContext() {
    return context;
  }

  /**
   * Constructor.
   */
  public MavenJdtAptPlugin() {
  }

  /**
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext bundleContext) throws Exception {
    MavenJdtAptPlugin.context = bundleContext;
  }

  /**
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext bundleContext) throws Exception {
    MavenJdtAptPlugin.context = null;
  }

  /**
   * Convenience wrapper for rethrowing exceptions as {@link CoreException}s, with severity of {@link IStatus#ERROR}.
   */
  public static Status createErrorStatus(Throwable e, String message) {
    return new Status(IStatus.ERROR, PLUGIN_ID, STATUS_EXCEPTION, message, e);
  }

  /**
   * Convenience wrapper for rethrowing exceptions as {@link CoreException}s, with severity of {@link IStatus#WARNING}.
   */
  public static Status createWarningStatus(Throwable e, String message) {
    return new Status(IStatus.WARNING, PLUGIN_ID, STATUS_EXCEPTION, message, e);
  }

  /**
   * Convenience wrapper for rethrowing exceptions as {@link CoreException}s, with severity of {@link IStatus#INFO}.
   */
  public static Status createInfoStatus(Throwable e, String message) {
    return new Status(IStatus.INFO, PLUGIN_ID, STATUS_EXCEPTION, message, e);
  }
}
