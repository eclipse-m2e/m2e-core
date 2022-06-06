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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.codehaus.plexus.logging.AbstractLoggerManager;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;

import org.eclipse.m2e.core.embedder.IMavenConfiguration;


/**
 * EclipseLoggerManager
 *
 * @author igor
 */
@Component(service = LoggerManager.class)
public class EclipseLoggerManager extends AbstractLoggerManager {

  @Reference
  IMavenConfiguration mavenConfiguration;

  private EclipseLogger logger;

  @Activate
  void activate() {
    logger = new EclipseLogger(mavenConfiguration);
  }

  @Override
  public int getActiveLoggerCount() {
    return 1;
  }

  @Override
  public Logger getLoggerForComponent(String arg0, String arg1) {
    return logger;
  }

  @Override
  public int getThreshold() {
    return Logger.LEVEL_DEBUG;
  }

  public int getThreshold(String arg0, String arg1) {
    return Logger.LEVEL_DEBUG;
  }

  @Override
  public void returnComponentLogger(String arg0, String arg1) {
  }

  @Override
  public void setThreshold(int arg0) {
  }

  public void setThreshold(String arg0, String arg1, int arg2) {
  }

  @Override
  public void setThresholds(int arg0) {
  }

}
