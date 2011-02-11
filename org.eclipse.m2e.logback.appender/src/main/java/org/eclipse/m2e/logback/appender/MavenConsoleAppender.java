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
import ch.qos.logback.core.UnsynchronizedAppenderBase;

import org.eclipse.core.runtime.Platform;

import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.console.MavenConsole;


public class MavenConsoleAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
  private static final String M2E_CORE_UI_BUNDLE_ID = "org.eclipse.m2e.core.ui"; //$NON-NLS-1$

  private Bundle m2eCoreUIBundle;

  @Override
  protected void append(ILoggingEvent logEvent) {
    if(!isActive()) {
      return;
    }

    if(!M2EUIPluginActivator.getDefault().hasMavenConsoleImpl()) {
      return;
    }

    MavenConsole mavenConsole = M2EUIPluginActivator.getDefault().getMavenConsole();
    if(logEvent.getLevel().levelInt == Level.ERROR_INT) {
      mavenConsole.logError(logEvent.toString());
    } else {
      mavenConsole.logMessage(logEvent.toString());
    }
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
}
