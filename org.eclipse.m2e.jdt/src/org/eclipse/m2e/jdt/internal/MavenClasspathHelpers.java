/*******************************************************************************
 * Copyright (c) 2010-2018 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.jdt.internal;

import java.util.stream.Stream;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.m2e.jdt.IClasspathManager;


public class MavenClasspathHelpers {

  public static boolean isMaven2ClasspathContainer(IPath containerPath) {
    return containerPath != null && containerPath.segmentCount() > 0
        && IClasspathManager.CONTAINER_ID.equals(containerPath.segment(0));
  }

  public static IClasspathEntry getDefaultContainerEntry() {
    return JavaCore.newContainerEntry(new Path(IClasspathManager.CONTAINER_ID));
  }

  public static IClasspathEntry getDefaultContainerEntry(boolean isExported) {
    return JavaCore.newContainerEntry(new Path(IClasspathManager.CONTAINER_ID), isExported);
  }

  public static boolean isTestSource(IClasspathEntry entry) {
    return "true".equals(getAttribute(entry, IClasspathManager.TEST_ATTRIBUTE));
  }

  public static String getAttribute(IClasspathEntry entry, String key) {
    if(entry == null || entry.getExtraAttributes().length == 0 || key == null) {
      return null;
    }
    return Stream.of(entry.getExtraAttributes()).filter(a -> key.equals(a.getName())).findFirst()
        .map(IClasspathAttribute::getValue).orElse(null);
  }
}
