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

package org.eclipse.m2e.logback.appender;

import org.osgi.framework.Bundle;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;


public class EclipseLogAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
  private static final String BUNDLE_ID = "org.eclipse.m2e.logback.appender"; //$NON-NLS-1$

  @Override
  protected void append(ILoggingEvent logEvent) {
    int severity = 0;
    switch(logEvent.getLevel().levelInt) {
      case Level.ERROR_INT:
        severity = IStatus.ERROR;
        break;
      case Level.WARN_INT:
        severity = IStatus.WARNING;
        break;
      case Level.INFO_INT:
        severity = IStatus.INFO;
        break;
      default:
        return;
    }

    IStatus status = new Status(severity, BUNDLE_ID, logEvent.getFormattedMessage(), getThrowable(logEvent));
    ILog eclipseLog = Platform.getLog(getSelfBundle());
    eclipseLog.log(status);
  }

  private Bundle self;

  private Bundle getSelfBundle() {
    if(self == null) {
      self = Platform.getBundle(BUNDLE_ID);
    }
    return self;
  }

  private Throwable getThrowable(ILoggingEvent logEvent) {
    if(logEvent.getThrowableProxy() instanceof ThrowableProxy) {
      return ((ThrowableProxy) logEvent.getThrowableProxy()).getThrowable();
    }

    Object[] args = logEvent.getArgumentArray();
    if(args == null || args.length == 0) {
      return null;
    }

    Object lastObject = args[args.length - 1];
    if(lastObject instanceof Throwable) {
      return (Throwable) lastObject;
    }

    return null;
  }
}
