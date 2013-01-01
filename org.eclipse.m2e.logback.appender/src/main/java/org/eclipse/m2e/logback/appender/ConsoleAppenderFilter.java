/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.logback.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;


/**
 * Filters out (disables) logging to console if -consoleLog was passed as arg to eclipse
 */
@SuppressWarnings("restriction")
public class ConsoleAppenderFilter extends Filter<ILoggingEvent> {
  private boolean consoleLogEnabled;

  public ConsoleAppenderFilter() {
    consoleLogEnabled = "true".equals(FrameworkProperties.getProperty(EclipseStarter.PROP_CONSOLE_LOG));
  }

  public FilterReply decide(ILoggingEvent loggingEvent) {
    if(consoleLogEnabled) {
      return FilterReply.NEUTRAL;
    }
    return FilterReply.DENY;
  }
}
