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

import java.util.Date;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.m2e.core.search.ISearchResultGA;
import org.eclipse.m2e.core.search.ISearchResultGAVEC;

/**
 * @author Matthew Piggott
 * @since 1.17.0
 */
public class SearchResultFile implements IAdaptable, ISearchResultGAVEC {

  private String version;

  private SearchResult searchResult;

  private long size;

  private Date date;

  private String filename;

  private boolean hasSources;

  private String classifier;

  private String extension;

  public SearchResultFile(String version, String classifier, String extension, SearchResult searchResult, long size,
      Date date, String filename, boolean hasSources) {
    this.version = version;
    this.searchResult = searchResult;
    this.date = date;
    this.size = size;
    this.filename = filename;
    this.hasSources = hasSources;
    this.classifier = classifier;
    this.extension = extension;
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public String getGroupId() {
    return searchResult.getGroupId();
  }

  @Override
  public String getArtifactId() {
    return searchResult.getArtifactId();
  }

  @Override
  public String getClassname() {
    return searchResult.getClassname();
  }

  @Override
  public String getPackageName() {
    return searchResult.getPackageName();
  }

  @Override
  public ISearchResultGA getSearchResult() {
    return searchResult;
  }

  @Override
  public String getClassifier() {
    return this.classifier;
  }

  @Override
  public String getExtension() {
    return this.extension;
  }

  @Override
  public long getSize() {
    return size;
  }

  @Override
  public Date getDate() {
    return date;
  }

  @Override
  public String getFilename() {
    return filename;
  }

  @Override
  public boolean hasSources() {
    return hasSources;
  }
}
