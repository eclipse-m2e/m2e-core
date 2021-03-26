/*******************************************************************************
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Takari, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal;

import java.net.URLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Workaround for JDK bug http://bugs.java.com/view_bug.do?bug_id=6207022
 *
 * @since 1.6
 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=442524
 * @see http://bugs.java.com/view_bug.do?bug_id=6207022
 */
public final class URLConnectionCaches {

  private static final Logger log = LoggerFactory.getLogger(URLConnectionCaches.class);

  private static final URLConnection conn = new URLConnection(null) {
    public void connect() {
    }
  };

  private URLConnectionCaches() {
  }

  /**
   * Disables caching of URLConnections by default.
   * <p>
   * According to JDK bug 6207022, JVM caches opened jar files, which results in JVM crashes or other unpredictable
   * results if the jar are closed, overwritten and reopened.
   * <p>
   * This workaround disables URLConnection caching for all protocols, not just jar files, which may result in
   * performance problems for other URLConnection clients. There is also no way to guarantee that other code does not
   * reenable caching of URLConnections.
   * <p>
   * Longer term solution is likely two fold. First, always create unique copies of jar files passed to Maven class
   * realms. Second, use {@link URLConnection#setUseCaches(boolean)} to false for all jar files opened by Maven and
   * Sisu. Both require significant effort, however.
   */
  public static void disable() {
    conn.setDefaultUseCaches(false);
  }

  public static void assertDisabled() {
    if(conn.getDefaultUseCaches()) {
      log.error("Unexpected URLConnection defaultUseCaches enabled");
    }
  }
}
