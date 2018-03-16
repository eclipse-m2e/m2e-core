/*******************************************************************************
 * Copyright (c) 2010, 2018 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.index.filter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.index.IIndex;
import org.eclipse.m2e.core.internal.index.IndexedArtifact;
import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.index.SearchExpression;


/**
 * FilteredIndex
 * 
 * @author igor
 */
public class FilteredIndex implements IIndex {

  private final IIndex index;

  private final IProject project;

  public FilteredIndex(IProject project, IIndex index) {
    this.project = project;
    this.index = index;
  }

  public IndexedArtifactFile getIndexedArtifactFile(ArtifactKey artifact) throws CoreException {
    return index.getIndexedArtifactFile(artifact);
  }

  public IndexedArtifactFile identify(File file) throws CoreException {
    return index.identify(file);
  }

  public Collection<IndexedArtifact> find(SearchExpression groupId, SearchExpression artifactId,
      SearchExpression version, SearchExpression packaging) throws CoreException {
    return filter(index.find(groupId, artifactId, version, packaging));
  }

  public Collection<IndexedArtifact> find(Collection<SearchExpression> groupId,
      Collection<SearchExpression> artifactId, Collection<SearchExpression> version,
      Collection<SearchExpression> packaging) throws CoreException {
    return filter(index.find(groupId, artifactId, version, packaging));
  }

  public Map<String, IndexedArtifact> search(SearchExpression expression, String searchType) throws CoreException {
    return filter(index.search(expression, searchType));
  }

  public Map<String, IndexedArtifact> search(SearchExpression expression, String searchType, int classifier)
      throws CoreException {
    return filter(index.search(expression, searchType, classifier));
  }

  // filter methods

  protected Collection<IndexedArtifact> filter(Collection<IndexedArtifact> indexedArtifacts) {
    ArrayList<IndexedArtifact> result = new ArrayList<IndexedArtifact>();
    for(IndexedArtifact indexedArtifact : indexedArtifacts) {
      indexedArtifact = filter(indexedArtifact);
      if(indexedArtifact != null && !indexedArtifact.getFiles().isEmpty()) {
        result.add(indexedArtifact);
      }
    }
    return result;
  }

  protected IndexedArtifact filter(IndexedArtifact original) {
    ArtifactFilterManager arifactFilterManager = MavenPluginActivator.getDefault().getArifactFilterManager();
    IndexedArtifact result = new IndexedArtifact(original.getGroupId(), original.getArtifactId(),
        original.getPackageName(), original.getClassname(), original.getPackaging());
    for(IndexedArtifactFile file : original.getFiles()) {
      if(arifactFilterManager.filter(project, file.getAdapter(ArtifactKey.class)).isOK()) {
        result.addFile(file);
      }
    }
    return result;
  }

  private Map<String, IndexedArtifact> filter(Map<String, IndexedArtifact> original) {
    LinkedHashMap<String, IndexedArtifact> result = new LinkedHashMap<String, IndexedArtifact>();
    for(Map.Entry<String, IndexedArtifact> entry : original.entrySet()) {
      IndexedArtifact filtered = filter(entry.getValue());
      if(filtered != null && !filtered.getFiles().isEmpty()) {
        result.put(entry.getKey(), filtered);
      }
    }
    return result;
  }

}
