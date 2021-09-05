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

package org.eclipse.m2e.core.internal.index.nexus;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;

import org.apache.maven.index.Field;
import org.apache.maven.index.MAVEN;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.index.IMutableIndex;
import org.eclipse.m2e.core.internal.index.IndexedArtifact;
import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.index.SearchExpression;
import org.eclipse.m2e.core.repository.IRepository;


/**
 * NexusIndex
 *
 * @author igor
 */
public class NexusIndex implements IMutableIndex {

  /**
   * Repository index is disabled.
   */
  public static final String DETAILS_DISABLED = "off"; //$NON-NLS-1$

  /**
   * Only artifact index information is used. Classname index is disabled.
   */
  public static final String DETAILS_MIN = "min"; //$NON-NLS-1$

  /**
   * Both artifact and classname indexes are used.
   */
  public static final String DETAILS_FULL = "full"; //$NON-NLS-1$

  private final NexusIndexManager indexManager;

  private final IRepository repository;

  private final String indexDetails;

  NexusIndex(NexusIndexManager indexManager, IRepository repository, String indexDetails) {
    this.indexManager = indexManager;
    this.repository = repository;
    this.indexDetails = indexDetails;
  }

  public String getRepositoryUrl() {
    return this.repository.getUrl();
  }

  public String getIndexDetails() {
    return this.indexDetails;
  }

  @Override
  public void addArtifact(File pomFile, ArtifactKey artifactKey) {
    indexManager.addDocument(repository, pomFile, artifactKey);
  }

  @Override
  public void removeArtifact(File pomFile, ArtifactKey artifactKey) {
    indexManager.removeDocument(repository, pomFile, artifactKey, null);
  }

  @Override
  public Collection<IndexedArtifact> find(SearchExpression groupId, SearchExpression artifactId,
      SearchExpression version, SearchExpression packaging) throws CoreException {
    return find(wrapIfNotNull(groupId), wrapIfNotNull(artifactId), wrapIfNotNull(version), wrapIfNotNull(packaging));
  }

  /**
   * Method wrapping one SearchExpression into a collection, if it is not null.
   *
   * @param sex
   * @return
   */
  private Collection<SearchExpression> wrapIfNotNull(SearchExpression se) {
    if(se == null) {
      return null;
    }
    return Collections.singleton(se);
  }

  @Override
  public Collection<IndexedArtifact> find(Collection<SearchExpression> groupId,
      Collection<SearchExpression> artifactId, Collection<SearchExpression> version,
      Collection<SearchExpression> packaging) throws CoreException {
    BooleanQuery.Builder query = new BooleanQuery.Builder();

    addQueryFromSearchExpressionCollection(query, MAVEN.PACKAGING, packaging);

    addQueryFromSearchExpressionCollection(query, MAVEN.GROUP_ID, groupId);

    addQueryFromSearchExpressionCollection(query, MAVEN.ARTIFACT_ID, artifactId);

    addQueryFromSearchExpressionCollection(query, MAVEN.VERSION, version);

    return indexManager.search(repository, query.build()).values();
  }

  private void addQueryFromSearchExpressionCollection(final BooleanQuery.Builder query, final Field field,
      final Collection<SearchExpression> sec) {
    if(sec != null && !sec.isEmpty()) {
      if(sec.size() > 1) {
        BooleanQuery.Builder q = new BooleanQuery.Builder();
        for(SearchExpression se : sec) {
          q.add(indexManager.constructQuery(field, se), Occur.SHOULD);
        }
        query.add(q.build(), Occur.MUST);
      } else {
        query.add(indexManager.constructQuery(field, sec.iterator().next()), Occur.MUST);
      }
    }
  }

  @Override
  public IndexedArtifactFile getIndexedArtifactFile(ArtifactKey artifact) throws CoreException {
    return indexManager.getIndexedArtifactFile(repository, artifact);
  }

  @Override
  public IndexedArtifactFile identify(File file) throws CoreException {
    return indexManager.identify(repository, file);
  }

  @Override
  public void updateIndex(boolean force, IProgressMonitor monitor) throws CoreException {
    indexManager.updateIndex(repository, force, monitor);
  }

  public void scheduleIndexUpdate(boolean force) {
    indexManager.scheduleIndexUpdate(repository, force);
  }

  public IndexedArtifactGroup[] getRootIndexedArtifactGroups() throws CoreException {
    return indexManager.getRootIndexedArtifactGroups(repository);
  }

  public boolean isUpdating() {
    return indexManager.isUpdatingIndex(repository);
  }

  public IRepository getRepository() {
    return repository;
  }

  public boolean isEnabled() {
    return DETAILS_MIN.equals(indexDetails) || DETAILS_FULL.equals(indexDetails);
  }

  public void setIndexDetails(String details) throws CoreException {
    indexManager.setIndexDetails(repository, details, null/*async*/);
  }

  @Override
  public Map<String, IndexedArtifact> search(SearchExpression term, String searchType) throws CoreException {
    return indexManager.search(getRepository(), term, searchType);
  }

  @Override
  public Map<String, IndexedArtifact> search(SearchExpression term, String searchType, int classifier)
      throws CoreException {
    return indexManager.search(getRepository(), term, searchType, classifier);
  }
}
