/*******************************************************************************
 * Copyright (c) 2014, 2022 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.ManifestElement;


/**
 * @since 1.5
 */
public class Bundles {
  private Bundles() {
  }

  private static final Logger log = LoggerFactory.getLogger(Bundles.class);

  public static List<String> getClasspathEntries(Bundle bundle) {
    log.info("getClasspathEntries(Bundle={})", bundle);
    Set<String> cp = new LinkedHashSet<>();
    if(inDevelopmentMode()) {
      Collections.addAll(cp, getDevClassPath(bundle.getSymbolicName()));
    }
    cp.addAll(parseBundleClasspath(bundle));
    List<String> entries = new ArrayList<>();
    for(String cpe : cp) {
      String entry = getClasspathEntryPath(bundle, cpe);
      if(entry != null) {
        log.info("\tEntry: {}", entry);
        entries.add(entry);
      }
    }
    return entries;
  }

  private static List<String> parseBundleClasspath(Bundle bundle) {
    String header = bundle.getHeaders().get(Constants.BUNDLE_CLASSPATH);
    try {
      ManifestElement[] cpEntries = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, header);
      if(cpEntries != null) {
        return Arrays.stream(cpEntries).map(ManifestElement::getValue).toList();
      }
    } catch(BundleException ex) {
      log.warn("Could not parse bundle classpath of {}", bundle, ex);
    }
    return List.of(".");
  }

  public static String getClasspathEntryPath(Bundle bundle, String cp) {
    // try embedded entries first
    try {
      if(".".equals(cp)) {
        return FileLocator.getBundleFileLocation(bundle).orElseThrow().getCanonicalPath();
      }
      URL url = bundle.getEntry(cp);
      if(url != null) {
        String path = FileLocator.toFileURL(url).getFile();
        return IPath.fromOSString(path).toOSString();
      }
    } catch(IOException | NoSuchElementException ex) {
      log.warn("Could not get entry {} for bundle {}", cp, bundle, ex);
    }

    // in development mode entries can be absolute paths outside of bundle basedir
    if(inDevelopmentMode()) {
      File file = new File(cp);
      if(file.exists() && file.isAbsolute()) {
        return file.getAbsolutePath();
      }
    }

    log.debug("Bundle {} does not have entry {}", bundle, cp);
    return null;
  }

  @SuppressWarnings("restriction")
  private static String[] getDevClassPath(String bundleSymbolicName) {
    return org.eclipse.core.internal.runtime.DevClassPathHelper.getDevClassPath(bundleSymbolicName);
  }

  @SuppressWarnings("restriction")
  private static boolean inDevelopmentMode() {
    return org.eclipse.core.internal.runtime.DevClassPathHelper.inDevelopmentMode();
  }
}
