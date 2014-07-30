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

package org.eclipse.m2e.logback.configuration;

import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;


public class LogHelper {
  public static void logJavaProperties(Logger log) {
    Properties javaProperties = System.getProperties();
    SortedMap<String, String> sortedProperties = new TreeMap<String, String>();
    for(String key : javaProperties.stringPropertyNames()) {
      sortedProperties.put(key, javaProperties.getProperty(key));
    }
    log.debug("Java properties (ordered by property name):"); //$NON-NLS-1$
    for(String key : sortedProperties.keySet()) {
      log.debug("   {}={}", key, sortedProperties.get(key));
    }
  }
}
