/*******************************************************************************
 * Copyright (c) 2010, 2021 Sonatype, Inc.
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

package org.eclipse.m2e.logback.appender;

import org.osgi.framework.Bundle;

import org.eclipse.core.runtime.Platform;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;


public class MavenConsoleAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
  private static final String M2E_CORE_UI_BUNDLE_ID = "org.eclipse.m2e.core.ui"; //$NON-NLS-1$

  private Bundle m2eCoreUIBundle;

  @Override
  protected void append(ILoggingEvent logEvent) {
    if(!isActive()) {
      return;
    }
    MavenConsoleAppenderImpl.appendToMavenConsole(logEvent);
  }

  private boolean isActive() {
    if(m2eCoreUIBundle == null) {
      m2eCoreUIBundle = Platform.getBundle(M2E_CORE_UI_BUNDLE_ID);
      if(m2eCoreUIBundle == null) {
        System.out.println("Could not find " + M2E_CORE_UI_BUNDLE_ID + " bundle.");
        return false;
      }
    }
    return m2eCoreUIBundle.getState() == Bundle.ACTIVE;
  }

  @SuppressWarnings("restriction")
  private static class MavenConsoleAppenderImpl {
    // Bug 342232: Encapsulate references to classes of m2e.core.ui plug-in into own class to defer activation of its bundle to the time when this and other appenders are fully created.
    // In the process of activating m2e.core.ui, loggers are already used, so warnings would be emitted if the appenders were not present.

    public static void appendToMavenConsole(ILoggingEvent logEvent) {
      org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator uiPlugin;
      uiPlugin = org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator.getDefault();
      if(!uiPlugin.hasMavenConsoleImpl()) {
        return;
      }
      org.eclipse.m2e.core.ui.internal.console.MavenConsole mavenConsole = uiPlugin.getMavenConsole();
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
}
