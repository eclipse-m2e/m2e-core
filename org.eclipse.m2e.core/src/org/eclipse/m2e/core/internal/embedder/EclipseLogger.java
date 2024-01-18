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

package org.eclipse.m2e.core.internal.embedder;

import org.slf4j.LoggerFactory;

import org.codehaus.plexus.logging.Logger;

import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.internal.Messages;


class EclipseLogger implements Logger {
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(EclipseLogger.class);

  private final IMavenConfiguration mavenConfiguration;

  public EclipseLogger(IMavenConfiguration mavenConfiguration) {
    this.mavenConfiguration = mavenConfiguration;
  }

  @Override
  public void debug(String msg) {
    if(isDebugEnabled()) {
      log.debug(msg);
    }
  }

  @Override
  public void debug(String msg, Throwable t) {
    if(isDebugEnabled()) {
      log.debug(toMessageString(msg, t), t);
    }
  }

  @Override
  public void info(String msg) {
    if(isInfoEnabled()) {
      log.info(msg);
    }
  }

  @Override
  public void info(String msg, Throwable t) {
    if(isInfoEnabled()) {
      log.info(toMessageString(msg, t), t);
    }
  }

  @Override
  public void warn(String msg) {
    if(isWarnEnabled()) {
      log.warn(msg);
    }
  }

  @Override
  public void warn(String msg, Throwable t) {
    if(isWarnEnabled()) {
      log.warn(toMessageString(msg, t), t);
    }
  }

  @Override
  public void fatalError(String msg) {
    if(isFatalErrorEnabled()) {
      log.error(msg);
    }
  }

  @Override
  public void fatalError(String msg, Throwable t) {
    if(isFatalErrorEnabled()) {
      log.error(toMessageString(msg, t), t);
    }
  }

  @Override
  public void error(String msg) {
    if(isErrorEnabled()) {
      log.error(msg);
    }
  }

  @Override
  public void error(String msg, Throwable t) {
    if(isErrorEnabled()) {
      log.error(toMessageString(msg, t), t);
    }
  }

  @Override
  public boolean isDebugEnabled() {
    return mavenConfiguration.isDebugOutput();
  }

  @Override
  public boolean isInfoEnabled() {
    return true;
  }

  @Override
  public boolean isWarnEnabled() {
    return true;
  }

  @Override
  public boolean isErrorEnabled() {
    return true;
  }

  @Override
  public boolean isFatalErrorEnabled() {
    return true;
  }

  @Override
  public void setThreshold(int treshold) {
  }

  @Override
  public int getThreshold() {
    return LEVEL_DEBUG;
  }

  @Override
  public Logger getChildLogger(String name) {
    return this;
  }

  @Override
  public String getName() {
    return Messages.EclipseLogger_name;
  }

  private String toMessageString(String msg, Throwable t) {
    if(t == null || t.getMessage() == null) {
      return msg;
    }
    if(msg == null) {
      return t.getMessage();
    }
    return msg + " " + t.getMessage();
  }
}
