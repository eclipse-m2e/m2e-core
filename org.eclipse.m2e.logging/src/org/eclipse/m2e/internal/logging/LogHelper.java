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

package org.eclipse.m2e.internal.logging;

import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;


public class LogHelper {
  public static void logJavaProperties(Logger log) {
    Properties javaProperties = System.getProperties();
    SortedMap<String, String> sortedProperties = new TreeMap<String, String>();
    for(Object key : javaProperties.keySet()) {
      sortedProperties.put((String) key, (String) javaProperties.get(key));
    }
    log.info("Java properties (ordered by property name):");
    for(String key : sortedProperties.keySet()) {
      log.info("   {}={}", key, sortedProperties.get(key));
    }
  }
}
