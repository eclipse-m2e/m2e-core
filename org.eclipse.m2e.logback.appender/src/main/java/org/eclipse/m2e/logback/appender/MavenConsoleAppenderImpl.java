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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.console.MavenConsole;


@SuppressWarnings("restriction")
public class MavenConsoleAppenderImpl {
  protected void append(ILoggingEvent logEvent) {
    if(!M2EUIPluginActivator.getDefault().hasMavenConsoleImpl()) {
      return;
    }

    MavenConsole mavenConsole = M2EUIPluginActivator.getDefault().getMavenConsole();
    switch(logEvent.getLevel().levelInt) {
      case Level.DEBUG_INT:
        mavenConsole.debug(logEvent.toString());
        return;
      case Level.ERROR_INT:
        mavenConsole.error(logEvent.toString());
        return;
      case Level.WARN_INT:
      case Level.INFO_INT:
      default:
        mavenConsole.info(logEvent.toString());
        return;
    }
  }
}
