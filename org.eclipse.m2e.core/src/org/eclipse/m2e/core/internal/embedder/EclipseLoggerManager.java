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

package org.eclipse.m2e.core.internal.embedder;

import org.codehaus.plexus.logging.AbstractLoggerManager;
import org.codehaus.plexus.logging.Logger;

import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;


/**
 * EclipseLoggerManager
 * 
 * @author igor
 */
public class EclipseLoggerManager extends AbstractLoggerManager {

  private EclipseLogger logger;

  public EclipseLoggerManager(MavenConsole console, IMavenConfiguration mavenConfiguration) {
    this.logger = new EclipseLogger(console, mavenConfiguration);
  }

  public int getActiveLoggerCount() {
    return 1;
  }

  public Logger getLoggerForComponent(String arg0, String arg1) {
    return logger;
  }

  public int getThreshold() {
    return Logger.LEVEL_DEBUG;
  }

  public int getThreshold(String arg0, String arg1) {
    return Logger.LEVEL_DEBUG;
  }

  public void returnComponentLogger(String arg0, String arg1) {
  }

  public void setThreshold(int arg0) {
  }

  public void setThreshold(String arg0, String arg1, int arg2) {
  }

  public void setThresholds(int arg0) {
  }

}
