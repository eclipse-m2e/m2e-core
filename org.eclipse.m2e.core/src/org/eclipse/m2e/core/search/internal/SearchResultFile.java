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

import java.util.Date;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.m2e.core.search.ISearchResultGAVEC;

/**
 * SearchResultFile
 *
 * @author Matthew Piggott
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

  public String getVersion() {
    return version;
  }

  public String getGroupId() {
    return searchResult.getGroupId();
  }

  public String getArtifactId() {
    return searchResult.getArtifactId();
  }

  public String getClassname() {
    return searchResult.getClassname();
  }

  public String getPackageName() {
    return searchResult.getPackageName();
  }

  public SearchResult getSearchResult() {
    return searchResult;
  }

  public String getClassifier() {
    return this.classifier;
  }

  public String getExtension() {
    return this.extension;
  }

  public long getSize() {
    return size;
  }

  public Date getDate() {
    return date;
  }

  public String getFilename() {
    return filename;
  }

  public boolean hasSources() {
    return hasSources;
  }
}
