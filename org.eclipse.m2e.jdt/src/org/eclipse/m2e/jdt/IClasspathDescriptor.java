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

package org.eclipse.m2e.jdt;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;


/**
 * Instances of this class can be used to incrementally define IClasspathEntry[] arrays used by JDT to describe Java
 * Project classpath and contents of Java classpath containers.
 *
 * @author igor
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IClasspathDescriptor {

  public interface EntryFilter {
    boolean accept(IClasspathEntryDescriptor descriptor);
  }

  /**
   * @return true if classpath contains entry with specified path, false otherwise.
   */
  boolean containsPath(IPath path);

  /**
   * Convenience method, equivalent to
   * <code>addSourceEntry(sourcePath, outputLocation, new IPath[0], new IPath[0], generated)</code>
   */
  IClasspathEntryDescriptor addSourceEntry(IPath sourcePath, IPath outputLocation, boolean generated);

  /**
   * Adds project source folder to the classpath. The source folder must exist in the workspace unless generated is
   * true. In the latter case, the source classpath entry will be marked as optional.
   */
  IClasspathEntryDescriptor addSourceEntry(IPath sourcePath, IPath outputLocation, IPath[] inclusion, IPath[] exclusion,
      boolean generated);

  /**
   * Adds fully populated IClasspathEntry instance to the classpath.
   */
  IClasspathEntryDescriptor addEntry(IClasspathEntry entry);

  /**
   * Removes entry from stale entries list.
   *
   * @since 1.7
   */
  void touchEntry(IPath entryPath);

  /**
   * Replaces a single ClasspathEntry instance matched by filter. Returns null if none were replaced.
   *
   * @since 1.6
   */
  IClasspathEntryDescriptor replaceEntry(EntryFilter filter, IClasspathEntry entry);

  /**
   * Adds and returns new project classpath entry.
   */
  IClasspathEntryDescriptor addProjectEntry(IPath entryPath);

  /**
   * Adds and returns new library entry to the classpath
   */
  IClasspathEntryDescriptor addLibraryEntry(IPath entryPath);

  /**
   * Removes entry with specified path from the classpath. That this can remove multiple entries because classpath does
   * not enforce uniqueness of entry paths
   *
   * @TODO should really be removeEntries (i.e. plural)
   */
  List<IClasspathEntryDescriptor> removeEntry(IPath path);

  /**
   * Removes entries that match EntryFilter (i.e. EntryFilter#accept(entry) returns true) from the classpath
   *
   * @TODO should really be removeEntries (i.e. plural)
   */
  List<IClasspathEntryDescriptor> removeEntry(EntryFilter filter);

  /**
   * Renders classpath as IClasspathEntry[] array
   *
   * @TODO should really be toEntriesArray
   */
  IClasspathEntry[] getEntries();

  /**
   * Returns underlying "live" list of IClasspathEntryDescriptor instances. Changes to the list will be immediately
   * reflected in the classpath.
   */
  List<IClasspathEntryDescriptor> getEntryDescriptors();

}
