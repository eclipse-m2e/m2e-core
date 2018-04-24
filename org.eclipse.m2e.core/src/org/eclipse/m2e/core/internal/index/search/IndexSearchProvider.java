/*******************************************************************************
 * Copyright (c) 2018 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.core.internal.index.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.index.IIndex;
import org.eclipse.m2e.core.internal.index.IndexManager;
import org.eclipse.m2e.core.internal.index.IndexedArtifact;
import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.index.UserInputSearchExpression;
import org.eclipse.m2e.core.search.ISearchProvider;
import org.eclipse.m2e.core.search.ISearchResultGA;
import org.eclipse.m2e.core.search.ISearchResultGAVEC;
import org.eclipse.m2e.core.search.internal.SearchResult;

/**
 * @author Matthew Piggott
 */
public class IndexSearchProvider implements ISearchProvider {

  private IndexManager indexManager = MavenPlugin.getIndexManager();

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.search.ISearchProvider#find(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public List<ISearchResultGA> find(IProgressMonitor monitor, SearchType searchType, String query) throws CoreException {
    int classifier = showClassifiers(searchType) ? getClassifier() : IIndex.SEARCH_ALL;

    Map<String, IndexedArtifact> res = indexManager.getAllIndexes().search(new UserInputSearchExpression(query),
        getSearchType(searchType), classifier);

    if(res.isEmpty()) {
      return Collections.emptyList();
    }

    List<ISearchResultGA> results = new ArrayList<>();
    for(IndexedArtifact art : res.values()) {
      SearchResult result = new SearchResult(this, art.getGroupId(), art.getArtifactId(), art.getClassname(),
          art.getPackageName());
      for(IndexedArtifactFile file : art.getFiles()) {
        result.addVersion(file.version, file.getDependency().getClassifier(), file.getDependency().getType(), file.size,
            file.date, file.fname, file.sourcesExists == IIndex.PRESENT);
      }
      result.setComponents(searchType.filter(result.getComponents()));
      if(!result.getComponents().isEmpty()) {
        results.add(result);
      }
    }
    return results;
  }

  @Override
  public List<ISearchResultGAVEC> getArtifacts(IProgressMonitor monitor, ISearchResultGA result) {
    return result.getComponents();
  }

  @Override
  public IStatus getStatus() {
    if(!MavenPlugin.getMavenConfiguration().isUpdateIndexesOnStartup()) {
      return new Status(IStatus.WARNING, IMavenConstants.PLUGIN_ID,
          Messages.IndexSearchProvider_unavailableRemoteRepositoriesIndexes);
    }

    return Status.OK_STATUS;
  }

  private int getClassifier() {
    // mkleint: no more allowing people to opt in/out displaying javadoc and sources..
    // allow tests and every other classifier..
    return IIndex.SEARCH_JARS + IIndex.SEARCH_TESTS;
  }

  private static String getSearchType(SearchType searchType) {
    switch(searchType) {
      case plugin:
        return IIndex.SEARCH_PLUGIN;
      case artifact:
        return IIndex.SEARCH_ARTIFACT;
      case parent:
        return IIndex.SEARCH_PARENTS;
      default:
        return null;
    }
  }

  private boolean showClassifiers(SearchType type) {
    return type != null && SearchType.artifact.equals(type);
  }
}
