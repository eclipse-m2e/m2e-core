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

package org.eclipse.m2e.editor.pom;


/**
 * @author Eugene Kuleshov
 */
public class SearchMatcher extends Matcher {

  private final SearchControl searchControl;

  public SearchMatcher(SearchControl searchControl) {
    this.searchControl = searchControl;
  }

  public boolean isMatchingArtifact(String groupId, String artifactId) {
    String text = searchControl.getSearchText().getText();
    return (groupId != null && groupId.indexOf(text) > -1) //
        || (artifactId != null && artifactId.indexOf(text) > -1);
  }
  
  public boolean isEmpty() {
    return searchControl.getSearchText().getText() == null //
        || searchControl.getSearchText().getText().trim().length() == 0;
  }

}
