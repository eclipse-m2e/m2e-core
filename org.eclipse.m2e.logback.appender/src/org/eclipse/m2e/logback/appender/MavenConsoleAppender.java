/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

import org.eclipse.core.runtime.Platform;


public class MavenConsoleAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
  private static final String M2E_CORE_UI_BUNDLE_ID = "org.eclipse.m2e.core.ui"; //$NON-NLS-1$

  private Bundle m2eCoreUIBundle;

  @Override
  protected void append(ILoggingEvent logEvent) {
    if(!isActive()) {
      return;
    }

    new MavenConsoleAppenderImpl().append(logEvent);
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
