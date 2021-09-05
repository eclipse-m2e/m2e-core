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

package org.eclipse.m2e.editor.composites;

import java.util.Comparator;

import org.eclipse.m2e.editor.composites.DependenciesComposite.Dependency;


public class DependenciesComparator<T> implements Comparator<T> {

  private boolean sortByGroups = true;

  @Override
  public int compare(T o1, T o2) {
    String[] gav1;
    String[] gav2;

    gav1 = toGAV(o1);

    gav2 = toGAV(o2);

    return compareGAVs(gav1, gav2);
  }

  protected String[] toGAV(Object obj) {
    if(obj instanceof Dependency) {
      return toGAV((Dependency) obj);
    }
    return toGAV((org.apache.maven.model.Dependency) obj);
  }

  protected String[] toGAV(Dependency dep) {
    String[] gav = new String[3];
    gav[0] = dep.groupId;
    gav[1] = dep.artifactId;
    gav[2] = dep.version;
    return gav;
  }

  protected String[] toGAV(org.apache.maven.model.Dependency dep) {
    String[] gav = new String[3];
    gav[0] = dep.getGroupId();
    gav[1] = dep.getArtifactId();
    gav[2] = dep.getVersion();
    return gav;
  }

  protected int compareGAVs(String[] gav1, String[] gav2) {

    String g1 = gav1[0] == null ? "" : gav1[0]; //$NON-NLS-1$
    String g2 = gav2[0] == null ? "" : gav2[0]; //$NON-NLS-1$

    String a1 = gav1[1] == null ? "" : gav1[1]; //$NON-NLS-1$
    String a2 = gav2[1] == null ? "" : gav2[1]; //$NON-NLS-1$

    String v1 = gav1[2] == null ? "" : gav1[2]; //$NON-NLS-1$
    String v2 = gav2[2] == null ? "" : gav2[2]; //$NON-NLS-1$

    return compareDependencies(g1, a1, v1, g2, a2, v2);
  }

  protected int compareDependencies(String group1, String artifact1, String version1, String group2, String artifact2,
      String version2) {
    int comp = 0;
    if(sortByGroups && (comp = group1.compareTo(group2)) != 0) {
      return comp;
    }
    if((comp = artifact1.compareTo(artifact2)) != 0) {
      return comp;
    }

    return version1.compareTo(version2);
  }

  /**
   * Set this to false to ignore groupIDs while sorting
   *
   * @param sortByGroups
   */
  public void setSortByGroups(boolean sortByGroups) {
    this.sortByGroups = sortByGroups;
  }
}
