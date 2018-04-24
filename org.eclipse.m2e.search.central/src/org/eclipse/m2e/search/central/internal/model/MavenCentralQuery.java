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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Constructs a query for maven central search. Does not determine whether the
 * resulting query is valid.
 * 
 * @author Matthew Piggott
 * @since 1.17.0
 */
public class MavenCentralQuery {

  private static final String KEYWORD = ""; //$NON-NLS-1$

  private static final String ARTIFACT_ID = "a"; //$NON-NLS-1$

  private static final String CLASS = "fc"; //$NON-NLS-1$

  private static final String GROUP_ID = "g"; //$NON-NLS-1$

  private static final String PACKAGING = "p"; //$NON-NLS-1$

  private Map<String, String> terms = new HashMap<>();

  public void setArtifactId(String artifactId) {
    terms.put(ARTIFACT_ID, artifactId);
  }

  public void setClass(String clazz) {
    terms.put(CLASS, clazz);
  }

  public void setGroupId(String groupId) {
    terms.put(GROUP_ID, groupId);
  }

  public void setKeyword(String keyword) {
    terms.put(KEYWORD, keyword);
  }

  public void setPackaging(String packaging) {
    terms.put(PACKAGING, packaging);
  }

  @Override
  public String toString() {
    StringBuilder query = new StringBuilder();
    for (Entry<String, String> entry : terms.entrySet()) {
      if (query.length() > 0) {
        query.append(" AND "); //$NON-NLS-1$
      }
      if (!KEYWORD.equals(entry.getKey())) {
        query.append(entry.getKey()).append(':');
      }
      query.append(entry.getValue());
    }
    return query.toString();
  }
}
