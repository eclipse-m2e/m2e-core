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

package org.eclipse.m2e.core.index;

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

  public static final String SEARCH_GROUP = "groupId"; //$NON-NLS-1$

  public static final String SEARCH_ARTIFACT = "artifact"; //$NON-NLS-1$

  public static final String SEARCH_PLUGIN = "plugin"; //$NON-NLS-1$

  public static final String SEARCH_ARCHETYPE = "archetype"; //$NON-NLS-1$

  public static final String SEARCH_PACKAGING = "packaging"; //$NON-NLS-1$

  public static final String SEARCH_SHA1 = "sha1"; //$NON-NLS-1$

  /**
   * like SEARCH_ARTIFACT but will only return artifacts with packaging == pom
   */
  public static final String SEARCH_PARENTS = "parents"; //$NON-NLS-1$

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

  public static final int SEARCH_JARS = 1 << 0;

  public static final int SEARCH_JAVADOCS = 1 << 1;

  public static final int SEARCH_SOURCES = 1 << 2;

  public static final int SEARCH_TESTS = 1 << 3;

  public static final int SEARCH_ALL = 15;

  // availability flags

  public static final int PRESENT = 1;

  public static final int NOT_PRESENT = 0;

  public static final int NOT_AVAILABLE = 2;

  // index queries

  public IndexedArtifactFile getIndexedArtifactFile(ArtifactKey artifact) throws CoreException;

  public IndexedArtifactFile identify(File file) throws CoreException;

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
  public Collection<IndexedArtifact> find(SearchExpression groupId, SearchExpression artifactId,
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
  public Collection<IndexedArtifact> find(Collection<SearchExpression> groupId,
      Collection<SearchExpression> artifactId, Collection<SearchExpression> version,
      Collection<SearchExpression> packaging) throws CoreException;

  /**
   * Convenience method to search in all indexes enabled for repositories defined in settings.xml. This method always
   * performs "scored" search.
   */
  public Map<String, IndexedArtifact> search(SearchExpression expression, String searchType) throws CoreException;

  /**
   * Convenience method to search in all indexes enabled for repositories defined in settings.xml. This method always
   * performs "scored" search.
   * 
   * @param term - search term
   * @param searchType - query type. Should be one of the SEARCH_* values.
   * @param classifier - the type of classifiers to search for, SEARCH_ALL, SEARCH_JAVADOCS, SEARCH_SOURCES,
   *          SEARCH_TESTS
   */
  public Map<String, IndexedArtifact> search(SearchExpression expression, String searchType, int classifier)
      throws CoreException;
}
