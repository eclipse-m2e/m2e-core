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

package org.eclipse.m2e.core.internal.index;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.versioning.ArtifactVersion;


public class IndexedArtifact implements Comparable<IndexedArtifact> {

  public static final Comparator<IndexedArtifactFile> FILE_INFO_COMPARATOR = new Comparator<IndexedArtifactFile>() {

    public int compare(IndexedArtifactFile f1, IndexedArtifactFile f2) {
      ArtifactVersion v1 = f1.getArtifactVersion();
      ArtifactVersion v2 = f2.getArtifactVersion();
      int r = -v1.compareTo(v2);
      if(r != 0) {
        return r;
      }

      String c1 = f1.classifier;
      String c2 = f2.classifier;
      if(c1 == null) {
        return c2 == null ? 0 : -1;
      }
      if(c2 == null) {
        return 1;
      }
      return c1.compareTo(c2);
    }

  };

  private final String group;

  private final String artifact;

  private final String packageName;

  private final String className;

  private final String packaging;

  //a non-zero odd-prime hash seed
  private static final int SEED = 17;

  /**
   * Set<IndexedArtifactFile>
   */
  private final Set<IndexedArtifactFile> files = new TreeSet<IndexedArtifactFile>(FILE_INFO_COMPARATOR);

  public IndexedArtifact(String group, String artifact, String packageName, String className, String packaging) {
    this.group = group;
    this.artifact = artifact;
    this.packageName = packageName;
    this.className = className;
    this.packaging = packaging;
  }

  public void addFile(IndexedArtifactFile indexedArtifactFile) {
    getFiles().add(indexedArtifactFile);
  }

  public String getPackageName() {
    if(packageName != null && packageName.startsWith(".") && packageName.length() > 1) { //$NON-NLS-1$
      return packageName.substring(1);
    }
    return packageName;
  }

  public String toString() {
	  StringBuilder sb = new StringBuilder(
        "\n" + getClassname() + "  " + packageName + "  " + getGroupId() + " : " + getArtifactId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    return sb.toString();
  }

  public String getGroupId() {
    return group;
  }

  public String getArtifactId() {
    return artifact;
  }

  public String getPackaging() {
    return packaging;
  }

  public String getClassname() {
    return className;
  }

  public Set<IndexedArtifactFile> getFiles() {
    return files;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    int result = SEED;
    result *= fieldHash(getGroupId());
    result *= fieldHash(getArtifactId());
    result *= fieldHash(getPackaging());
    result *= fieldHash(getClassname());
    result *= fieldHash(getPackageName());
    return result;
  }

  private int fieldHash(Object field) {
    if(field == null) {
      return SEED;
    }
    return field.hashCode();
  }

  /**
   * Assumes all the fields are important for equals.
   */
  public boolean equals(Object artifact) {
    if(this == artifact) {
      return true;
    } else if(!(artifact instanceof IndexedArtifact)) {
      return false;
    } else {
      IndexedArtifact other = (IndexedArtifact) artifact;
      return fieldsEqual(this.getGroupId(), other.getGroupId())
          && fieldsEqual(this.getArtifactId(), other.getArtifactId())
          && fieldsEqual(this.getPackageName(), other.getPackageName())
          && fieldsEqual(this.getPackaging(), other.getPackaging())
          && fieldsEqual(this.getClassname(), other.getClassname());
    }
  }

  private boolean fieldsEqual(Object field1, Object field2) {
    return field1 == null ? field2 == null : field1.equals(field2);
  }

  public int compareTo(IndexedArtifact o) {
    if(this.equals(o))
      return 0;
    int comparison = 0;
    if(group != null && (comparison = group.compareTo(o.getGroupId())) != 0)
      return comparison;
    if(artifact != null && (comparison = artifact.compareTo(o.getArtifactId())) != 0)
      return comparison;
    if(packageName != null && (comparison = packageName.compareTo(o.getPackageName())) != 0)
      return comparison;
    if(className != null && (comparison = className.compareTo(o.getClassname())) != 0)
      return comparison;
    return 0;
  }
}
