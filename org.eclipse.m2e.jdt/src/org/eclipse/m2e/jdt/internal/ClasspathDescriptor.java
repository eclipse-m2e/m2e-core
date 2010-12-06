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

package org.eclipse.m2e.jdt.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

import org.apache.maven.artifact.Artifact;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;


/**
 * This class is an attempt to encapsulate list of IClasspathEntry's and operations on the list such as "removeEntry",
 * "addSourceEntry" and "addEntryAttribute". The idea is to provide JavaProjectConfigurator's classpath whiteboard they
 * can use to incrementally define classpath in a consistent manner.
 * 
 * @author igor
 */
public class ClasspathDescriptor implements IClasspathDescriptor {

  private final ArrayList<IClasspathEntryDescriptor> entries = new ArrayList<IClasspathEntryDescriptor>();

  public ClasspathDescriptor(IJavaProject project) {
  }

  /**
   * @return true if classpath contains entry with specified path, false otherwise.
   */
  public boolean containsPath(IPath path) {
    for(IClasspathEntryDescriptor descriptor : entries) {
      if(path.equals(descriptor.getPath())) {
        return true;
      }
    }
    return false;
  }

  public ClasspathEntryDescriptor addSourceEntry(IPath sourcePath, IPath outputLocation, boolean generated) {
    return addSourceEntry(sourcePath, //
        outputLocation, //
        new IPath[0] /* inclusion */, //
        new IPath[0] /* exclusion */, //
        generated);
  }

  public List<IClasspathEntryDescriptor> removeEntry(final IPath path) {
    return removeEntry(new EntryFilter() {
      public boolean accept(IClasspathEntryDescriptor descriptor) {
        return path.equals(descriptor.getPath());
      }
    });
  }

  public List<IClasspathEntryDescriptor> removeEntry(EntryFilter filter) {
    ArrayList<IClasspathEntryDescriptor> result = new ArrayList<IClasspathEntryDescriptor>();

    Iterator<IClasspathEntryDescriptor> iter = entries.iterator();
    while(iter.hasNext()) {
      IClasspathEntryDescriptor descriptor = iter.next();
      if(filter.accept(descriptor)) {
        result.add(descriptor);
        iter.remove();
      }
    }

    return result;
  }

  public ClasspathEntryDescriptor addSourceEntry(IPath sourcePath, IPath outputLocation, IPath[] inclusion,
      IPath[] exclusion, boolean generated) {
//    IWorkspaceRoot workspaceRoot = project.getProject().getWorkspace().getRoot();
//
//    Util.createFolder(workspaceRoot.getFolder(sourcePath), generated);

    ClasspathEntryDescriptor descriptor = new ClasspathEntryDescriptor(IClasspathEntry.CPE_SOURCE, sourcePath);
    descriptor.setOutputLocation(outputLocation);
    descriptor.setInclusionPatterns(inclusion);
    descriptor.setExclusionPatterns(exclusion);
    if(generated) {
      descriptor.setClasspathAttribute(IClasspathAttribute.OPTIONAL, "true"); //$NON-NLS-1$
    }

    entries.add(descriptor);

    return descriptor;
  }

  public IClasspathEntry[] getEntries() {
    IClasspathEntry[] result = new IClasspathEntry[entries.size()];

    for(int i = 0; i < entries.size(); i++ ) {
      result[i] = entries.get(i).toClasspathEntry();
    }

    return result;
  }

  public List<IClasspathEntryDescriptor> getEntryDescriptors() {
    return entries;
  }

  public ClasspathEntryDescriptor addEntry(IClasspathEntry cpe) {
    ClasspathEntryDescriptor entry = new ClasspathEntryDescriptor(cpe);
    entries.add(entry);
    return entry;
  }

  @SuppressWarnings("deprecation")
  public ClasspathEntryDescriptor addProjectEntry(Artifact a, IMavenProjectFacade projectFacade) {
    ClasspathEntryDescriptor entry = addProjectEntry(projectFacade.getFullPath());
    entry.setArtifactKey(new ArtifactKey(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getClassifier()));
    entry.setScope(a.getScope());
    entry.setOptionalDependency(a.isOptional());
    return entry;
  }

  public ClasspathEntryDescriptor addProjectEntry(IPath entryPath) {
    ClasspathEntryDescriptor entry = new ClasspathEntryDescriptor(IClasspathEntry.CPE_PROJECT, entryPath);

    entries.add(entry);

    return entry;
  }

  @SuppressWarnings("deprecation")
  public ClasspathEntryDescriptor addLibraryEntry(Artifact artifact, IPath srcPath, IPath srcRoot, String javaDocUrl) {
    ArtifactKey artifactKey = new ArtifactKey(artifact);
    IPath entryPath = new Path(artifact.getFile().getAbsolutePath());

    ClasspathEntryDescriptor entry = addLibraryEntry(entryPath);
    entry.setArtifactKey(artifactKey);

    if(javaDocUrl != null) {
      entry.setClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, javaDocUrl);
    }

    return entry;
  }

  public ClasspathEntryDescriptor addLibraryEntry(IPath entryPath) {
    ClasspathEntryDescriptor entry = new ClasspathEntryDescriptor(IClasspathEntry.CPE_LIBRARY, entryPath);

    entries.add(entry);

    return entry;
  }
}
