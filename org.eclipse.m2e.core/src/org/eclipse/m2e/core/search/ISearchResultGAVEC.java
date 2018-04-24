/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.core.search;

import java.util.Date;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.m2e.core.embedder.ArtifactKey;

/**
 * ISearchResultGAVEC represents a specific maven artifact.
 *
 * @author Matthew Piggott
 */
public interface ISearchResultGAVEC extends IAdaptable {
  /**
   * Returns the groupId of the component.
   */
  String getGroupId();

  /**
   * Returns the artifactId of this component.
   */
  String getArtifactId();

  /**
   * Returns the version of the component.
   */
  String getVersion();

  /**
   * Returns the extension of the component.
   * 
   * @return
   */
  String getExtension();

  /**
   * Returns the classifier of this component.
   * 
   * @return
   */
  String getClassifier();

  /**
   * Returns the catalog date of the component.
   */
  Date getDate();

  /**
   * Returns the component's filename.
   */
  String getFilename();

  /**
   * Returns the size of the component in bytes or -1 when unknown.
   */
  long getSize();

  /**
   * Returns the classname used to find the component.
   */
  String getClassname();

  /**
   * The package name of the class.
   */
  String getPackageName();

  /**
   * Return the ISearchResultGA
   */
  ISearchResultGA getSearchResult();

  /**
   * Returns whether a source bundle is available for the component.
   */
  boolean hasSources();

  @Override
  default <T> T getAdapter(Class<T> adapter) {
    if(adapter == ArtifactKey.class) {
      return adapter.cast(new ArtifactKey(getGroupId(), getArtifactId(), getVersion(), getClassifier()));
    }
    return null;
  }
}
