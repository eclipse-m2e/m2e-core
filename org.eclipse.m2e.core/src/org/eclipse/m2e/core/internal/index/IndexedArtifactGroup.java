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

import java.util.LinkedHashMap;

import org.eclipse.m2e.core.index.IndexedArtifact;
import org.eclipse.m2e.core.repository.IRepository;


public class IndexedArtifactGroup implements Comparable<IndexedArtifactGroup>{
  private final IRepository repository;
  private final String prefix;
  private final LinkedHashMap<String, IndexedArtifactGroup> nodes = new LinkedHashMap<String, IndexedArtifactGroup>();
  private final LinkedHashMap<String, IndexedArtifact> files = new LinkedHashMap<String, IndexedArtifact>();

  public IndexedArtifactGroup(IRepository repository, String prefix) {
    this.repository = repository;
    this.prefix = prefix;
  }

  public LinkedHashMap<String, IndexedArtifactGroup> getNodes() {
    return nodes;
  }

  public LinkedHashMap<String, IndexedArtifact> getFiles() {
    return files;
  }

  public String getPrefix() {
    return prefix;
  }
  
  public IRepository getRepository() {
    return this.repository;
  }

  /*
   * Compare the groups by prefix
   */
  public int compareTo(IndexedArtifactGroup o) {
    if(o == null){
      return -1;
    }
    return getPrefix().compareToIgnoreCase(o.getPrefix());
  }

}
