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
package org.eclipse.m2e.core.search;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

/**
 * ISearchProvider used to search a source for maven components.
 *
 * @author Matthew Piggott
 */
public interface ISearchProvider {

  public static enum SearchType {
    /**
     * 
     */
    artifact,
    /**
     * 
     */
    className,

    /**
     * Find artifacts with pom packaging
     */
    parent,

    /**
     * Find artifacts with maven-plugin packaging
     */
    plugin;

    /**
     * Helper method to filter by the type
     * 
     * @param results the list of results, must be non-null
     * @return a list of components relevant for the SearchType
     */
    public List<ISearchResultGAVEC> filter(Collection<ISearchResultGAVEC> results) {
      return results.stream().filter(ga -> {
        if(this == parent) {
          return "pom".equals(ga.getExtension());
        }
        if(this == plugin) {
          return "jar".equals(ga.getExtension()) && (ga.getClassifier() == null || "".equals(ga.getClassifier()));
        }
        return !"sources".equals(ga.getClassifier()) && !"javadoc".equals(ga.getClassifier())
            && !"pom".equals(ga.getExtension());
      }).collect(Collectors.toList());
    }
  }

  /**
   * Perform a search to find components matching the query string.
   * 
   * @param monitor a progress monitor for reporting search progress
   * @param searchType the type of search
   * @param query the search term
   * @return
   * @throws CoreException
   */
  List<ISearchResultGA> find(IProgressMonitor monitor, SearchType searchType, String query) throws CoreException;

  /**
   * Retrieve the list of version, extension and classifier triples associated with a given GA.
   * 
   * @param monitor a progress monitor for reporting search progress
   * @param searchResultGA the ISearchResultGA to retrieve children for, previously provided by the find() method.
   * @return
   * @throws CoreException
   */
  List<ISearchResultGAVEC> getArtifacts(IProgressMonitor monitor, ISearchResultGA searchResultGA) throws CoreException;

  /**
   * Used to indicate a configuration error with the ISearchProvider.
   */
  IStatus getStatus();
}
