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

package org.eclipse.m2e.core.internal.index;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.m2e.core.embedder.ArtifactKey;


/**
 * @author igor
 */
public interface IIndex {

  // search keys

  String SEARCH_GROUP = "groupId"; //$NON-NLS-1$

  String SEARCH_ARTIFACT = "artifact"; //$NON-NLS-1$

  String SEARCH_PLUGIN = "plugin"; //$NON-NLS-1$

  String SEARCH_ARCHETYPE = "archetype"; //$NON-NLS-1$

  String SEARCH_PACKAGING = "packaging"; //$NON-NLS-1$

  String SEARCH_SHA1 = "sha1"; //$NON-NLS-1$

  /**
   * like SEARCH_ARTIFACT but will only return artifacts with packaging == pom
   */
  String SEARCH_PARENTS = "parents"; //$NON-NLS-1$

  // search classifiers

//  public enum SearchClassifiers {
//    JARS,
//
//    JAVADOCS,
//
//    SOURCES,
//
//    TESTS
//  }
//
//  public Set<SearchClassifiers> ALL_CLASSIFIERS = new HashSet<IIndex.SearchClassifiers>(Arrays.asList(SearchClassifiers
//      .values()));

  //

  int SEARCH_JARS = 1 << 0;

  int SEARCH_JAVADOCS = 1 << 1;

  int SEARCH_SOURCES = 1 << 2;

  int SEARCH_TESTS = 1 << 3;

  int SEARCH_ALL = 15;

  // availability flags

  int PRESENT = 1;

  int NOT_PRESENT = 0;

  int NOT_AVAILABLE = 2;

  // index queries

  IndexedArtifactFile getIndexedArtifactFile(ArtifactKey artifact) throws CoreException;

  IndexedArtifactFile identify(File file) throws CoreException;

  /**
   * Performs a search for artifacts with given parameters.
   *
   * @param groupId
   * @param artifactId
   * @param version
   * @param packaging
   * @return
   * @throws CoreException
   */
  Collection<IndexedArtifact> find(SearchExpression groupId, SearchExpression artifactId,
      SearchExpression version, SearchExpression packaging) throws CoreException;

  /**
   * Performs a search for artifacts with given parameters. Similar to
   * {@link IIndex#find(SearchExpression, SearchExpression, SearchExpression, SearchExpression)}, but here you are able
   * to pass in multiple values for all searches. All elements of collections will form an "OR" of one query.
   *
   * @param groupId
   * @param artifactId
   * @param version
   * @param packaging
   * @return
   * @throws CoreException
   */
  Collection<IndexedArtifact> find(Collection<SearchExpression> groupId,
      Collection<SearchExpression> artifactId, Collection<SearchExpression> version,
      Collection<SearchExpression> packaging) throws CoreException;

  /**
   * Convenience method to search in all indexes enabled for repositories defined in settings.xml. This method always
   * performs "scored" search.
   */
  Map<String, IndexedArtifact> search(SearchExpression expression, String searchType) throws CoreException;

  /**
   * Convenience method to search in all indexes enabled for repositories defined in settings.xml. This method always
   * performs "scored" search.
   *
   * @param term - search term
   * @param searchType - query type. Should be one of the SEARCH_* values.
   * @param classifier - the type of classifiers to search for, SEARCH_ALL, SEARCH_JAVADOCS, SEARCH_SOURCES,
   *          SEARCH_TESTS
   */
  Map<String, IndexedArtifact> search(SearchExpression expression, String searchType, int classifier)
      throws CoreException;
}
