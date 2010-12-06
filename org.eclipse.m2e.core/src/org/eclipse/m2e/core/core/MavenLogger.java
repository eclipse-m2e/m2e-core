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

package org.eclipse.m2e.core.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;


/**
 * Maven Logger
 * 
 * @author Eugene Kuleshov
 */
public class MavenLogger {

  private static ILog LOG;

  public static void setLog(ILog log) {
    LOG = log;
  }

  public static void log(IStatus status) {
    LOG.log(status);
  }

  public static void log(CoreException ex) {
    IStatus s = ex.getStatus();
    if(s.getException() == null) {
      int n = s.getSeverity();
      log(new Status(n == IStatus.CANCEL || n == IStatus.ERROR || n == IStatus.INFO //
          || n == IStatus.WARNING || n == IStatus.OK ? n : IStatus.ERROR, //
          s.getPlugin() == null ? IMavenConstants.PLUGIN_ID : s.getPlugin(), //
          s.getCode(), //
          s.getMessage() == null ? s.toString() : s.getMessage(), //
          ex));
    } else {
      log(s);
    }
  }

  public static void log(String msg, Throwable t) {
    log(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, msg, t));
  }

  public static void log(String msg) {
    log(new Status(IStatus.OK, IMavenConstants.PLUGIN_ID, msg));
  }
}
