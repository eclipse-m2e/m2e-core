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

package org.eclipse.m2e.jdt;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;

import org.apache.maven.artifact.Artifact;

import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * Instances of this class can be used to incrementally define IClasspathEntry[] arrays used by JDT to decribe Java
 * Project classpath and contents of Java classpath containers.
 * 
 * @author igor
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IClasspathDescriptor {

  public static interface EntryFilter {
    public boolean accept(IClasspathEntryDescriptor descriptor);
  }

  /**
   * @return true if classpath contains entry with specified path, false otherwise.
   */
  public boolean containsPath(IPath path);

  /**
   * Convenience method, equivalent to
   * <code>addSourceEntry(sourcePath, outputLocation, new IPath[0], new IPath[0], generated)</code>
   */
  public IClasspathEntryDescriptor addSourceEntry(IPath sourcePath, IPath outputLocation, boolean generated);

  /**
   * Adds project source folder to the classpath. The source folder must exist in the workspace unless generated is
   * true. In the latter case, the source classpath entry will be marked as optional. 
   */
  public IClasspathEntryDescriptor addSourceEntry(IPath sourcePath, IPath outputLocation, IPath[] inclusion,
      IPath[] exclusion, boolean generated);

  /**
   * Adds fully populated IClasspathEntry instance to the classpath.
   */
  public IClasspathEntryDescriptor addEntry(IClasspathEntry entry);

  /**
   * Adds and returns new project classpath entry.
   */
  public IClasspathEntryDescriptor addProjectEntry(IPath entryPath);

  /**
   * Adds and returns new library entry to the classpath
   */
  public IClasspathEntryDescriptor addLibraryEntry(IPath entryPath);

  /**
   * Removes entry with specified path from the classpath. That this can remove multiple entries because classpath does
   * not enforce uniqueness of entry paths
   * 
   * @TODO should really be removeEntries (i.e. plural)
   */
  public List<IClasspathEntryDescriptor> removeEntry(IPath path);

  /**
   * Removed entries that match EntryFilter (i.e. EntryFilter#accept(entry) returns true) from the classpath
   * 
   * @TODO should really be removeEntries (i.e. plural)
   */
  public List<IClasspathEntryDescriptor> removeEntry(EntryFilter filter);

  /**
   * Renders classpath as IClasspathEntry[] array
   * 
   * @TODO should really be toEntriesArray
   */
  public IClasspathEntry[] getEntries();

  /**
   * Returns underlying "live" list of IClasspathEntryDescriptor instances. Changes to the list will be immediately
   * reflected in the classpath.
   */
  public List<IClasspathEntryDescriptor> getEntryDescriptors();

  // deprecated, to be removed before 1.0

  /**
   * Adds Maven artifact with corresponding sources and javadoc paths to the classpath.
   * 
   * @deprecated this method exposes Maven core classes, which are not part of m2eclipse-jdt API
   */
  public IClasspathEntryDescriptor addLibraryEntry(Artifact artifact, IPath srcPath, IPath srcRoot, String javaDocUrl);

  /**
   * Adds worksapce Maven project dependency to the classpath
   * 
   * @deprecated this method exposes Maven core classes, which are not part of m2eclipse-jdt API
   */
  public IClasspathEntryDescriptor addProjectEntry(Artifact artifact, IMavenProjectFacade projectFacade);
}
