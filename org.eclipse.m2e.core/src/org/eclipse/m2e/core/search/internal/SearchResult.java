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
package org.eclipse.m2e.core.search.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.m2e.core.search.ISearchProvider;
import org.eclipse.m2e.core.search.ISearchResultGA;
import org.eclipse.m2e.core.search.ISearchResultGAVEC;

/**
 * @author Matthew Piggott
 * @since 1.17.0
 */
public class SearchResult implements ISearchResultGA {

  private String groupId;

  private String artifactId;

  private List<ISearchResultGAVEC> components = new ArrayList<>();

  private String classname;

  private String packageName;

  private ISearchProvider searchProvider;

  public SearchResult(ISearchProvider searchProvider, String groupId, String artifactId) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.searchProvider = searchProvider;
  }

  public SearchResult(ISearchProvider searchProvider, String groupId, String artifactId, String classname,
      String packageName) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.classname = classname;
    this.packageName = packageName;
    this.searchProvider = searchProvider;
  }

  public String getGroupId() {
    return this.groupId;
  }

  public String getArtifactId() {
    return this.artifactId;
  }

  public String getClassname() {
    return classname;
  }

  public String getPackageName() {
    return packageName;
  }

  public List<ISearchResultGAVEC> getComponents() {
    return Collections.unmodifiableList(components);
  }

  public void addVersion(String version, String classifier, String extension, long size, Date date, String filename,
      boolean hasSources) {
    components.add(new SearchResultFile(version, classifier, extension, this, size, date, filename, hasSources));
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.ISearchResult#getProvider()
   */
  public ISearchProvider getProvider() {
    return searchProvider;
  }

  public void setComponents(List<ISearchResultGAVEC> components) {
    this.components = components;
  }
}
