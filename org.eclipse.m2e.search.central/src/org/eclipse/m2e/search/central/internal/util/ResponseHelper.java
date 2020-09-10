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
package org.eclipse.m2e.search.central.internal.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.m2e.core.search.ISearchProvider.SearchType;
import org.eclipse.m2e.core.search.ISearchResultGA;
import org.eclipse.m2e.core.search.ISearchResultGAVEC;
import org.eclipse.m2e.search.central.internal.CentralSearchProvider;
import org.eclipse.m2e.search.central.internal.model.CentralSearchResponse;
import org.eclipse.m2e.search.central.internal.model.Doc;
import org.eclipse.m2e.search.central.internal.model.SearchResult;

/**
 * @author Matthew Piggott
 * @since 1.17.0
 */
public class ResponseHelper {

  public static List<ISearchResultGAVEC> getArtifacts(CentralSearchProvider provider, SearchType searchType,
      CentralSearchResponse searchResponse) {
    List<ISearchResultGA> results = convert(provider, searchType, searchResponse);
    List<ISearchResultGAVEC> files = new ArrayList<>();
    for (ISearchResultGA result : results) {
      files.addAll(result.getComponents());
    }
    return files;
  }

  public static List<ISearchResultGA> convert(CentralSearchProvider provider, SearchType searchType,
      CentralSearchResponse searchResponse) {
    List<ISearchResultGA> results = new ArrayList<>();
    for (Doc doc : searchResponse.getResponse().getDocs()) {
      SearchResult result = new SearchResult(provider, doc.getG(), doc.getA());

      boolean hasSources = hasSources(doc.getEc());
      for (String[] ec : toClassifierExtension(doc.getEc())) {
        StringBuilder filename = new StringBuilder(doc.getA());
        filename.append('-').append(doc.getLatestVersion());
        if (ec[0] != null) {
          filename.append('-').append(ec[0]);
        }
        if (ec[1] != null) {
          filename.append('.').append(ec[1]);
        }
        String version = doc.getV() == null ? doc.getLatestVersion() : doc.getV();
        result.addArtifact(version, ec[0], ec[1], -1, null, filename.toString(), hasSources);
      }

      result.setArtifacts(searchType.filter(result.getArtifacts()));

      if (!result.getComponents().isEmpty()) {
        results.add(result);
      }
    }
    return results;
  }

  private static String[][] toClassifierExtension(String[] ec) {
    List<String[]> classifierExtensions = new ArrayList<>();

    for (int i = 0; i < ec.length; i++) {
      classifierExtensions.add(new String[] { getClassifier(ec[i]), getExtension(ec[i]) });
    }
    return classifierExtensions.toArray(new String[][] {});
  }

  private static String getClassifier(String ec) {
    if (!ec.startsWith("-")) { //$NON-NLS-1$
      return null;
    }
    int nextPeriod = ec.indexOf('.');
    if (nextPeriod == -1) {
      return ec.substring(1);
    }
    return ec.substring(1, nextPeriod);
  }

  private static String getExtension(String ec) {
    int nextPeriod = ec.indexOf('.');
    if (nextPeriod == -1) {
      return null;
    }
    return ec.substring(nextPeriod + 1);
  }

  private static boolean hasSources(String[] ec) {
    for (String candidate : ec) {
      if ("-sources.jar".equals(candidate)) { //$NON-NLS-1$
        return true;
      }
    }
    return false;
  }
}
