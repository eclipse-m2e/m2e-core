/*******************************************************************************
 * Copyright (c) 2018 Sonatype Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.search.central.internal.model;

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

  private List<ISearchResultGAVEC> artifacts = new ArrayList<>();

  private String classname;

  private String packageName;

  private ISearchProvider searchProvider;

  public SearchResult(ISearchProvider searchProvider, String groupId, String artifactId) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.searchProvider = searchProvider;
  }

  @Override
  public String getGroupId() {
    return this.groupId;
  }

  @Override
  public String getArtifactId() {
    return this.artifactId;
  }

  @Override
  public String getClassname() {
    return classname;
  }

  @Override
  public String getPackageName() {
    return packageName;
  }

  @Override
  public List<ISearchResultGAVEC> getComponents() {
    return Collections.unmodifiableList(artifacts);
  }

  public void addArtifact(String version, String classifier, String extension, long size, Date date, String filename,
      boolean hasSources) {
    artifacts.add(new SearchResultFile(version, classifier, extension, this, size, date, filename, hasSources));
  }

  public List<ISearchResultGAVEC> getArtifacts() {
    return artifacts;
  }

  public void setArtifacts(List<ISearchResultGAVEC> artifacts) {
    this.artifacts = artifacts;
  }

  @Override
  public ISearchProvider getProvider() {
    return searchProvider;
  }
}
