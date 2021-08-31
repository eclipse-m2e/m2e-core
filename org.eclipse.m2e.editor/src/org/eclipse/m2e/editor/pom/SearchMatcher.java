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
 *      Björn Michael <b.michael@gmx.de> - Bug 549161, case-insensitive filter
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

  @Override
  public boolean isMatchingArtifact(String groupId, String artifactId) {
    String text = searchControl.getSearchText().getText().toLowerCase();
    return (artifactId != null && artifactId.toLowerCase().contains(text)) //
        || (groupId != null && groupId.toLowerCase().contains(text));
  }

  @Override
  public boolean isEmpty() {
    return searchControl.getSearchText().getText() == null //
        || searchControl.getSearchText().getText().trim().isEmpty();
  }

}
