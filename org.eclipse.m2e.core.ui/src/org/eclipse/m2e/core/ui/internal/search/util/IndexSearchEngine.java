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
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.search.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;

import org.apache.maven.artifact.versioning.ComparableVersion;

import org.eclipse.m2e.core.internal.index.IIndex;
import org.eclipse.m2e.core.internal.index.IndexManager;
import org.eclipse.m2e.core.internal.index.IndexedArtifact;
import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.index.MatchTyped.MatchType;
import org.eclipse.m2e.core.internal.index.MatchTypedStringSearchExpression;
import org.eclipse.m2e.core.internal.index.SearchExpression;


/**
 * Search engine integrating {@link IndexManager} with POM XML editor.
 *
 * @author Lukas Krecan
 * @author Eugene Kuleshov
 */
public class IndexSearchEngine implements SearchEngine {

  private final IIndex index;

  public IndexSearchEngine(IIndex index) {
    this.index = index;
  }

  protected boolean isBlank(String str) {
    return str == null || str.trim().length() == 0;
  }

  @Override
  public Collection<String> findArtifactIds(String groupId, String searchExpression, Packaging packaging,
      ArtifactInfo containingArtifact) {
    // TODO add support for implicit groupIds in plugin dependencies "org.apache.maven.plugins", ...
    // Someone, give me here access to settings.xml, to be able to pick up "real" predefined groupIds added by user
    // Currently, I am just simulating the "factory defaults" of maven, but user changes to settings.xml
    // will not be picked up this way!
    ArrayList<SearchExpression> groupIdSearchExpressions = new ArrayList<>();
    if(isBlank(groupId)) {
      // values from effective settings
      // we are wiring in the defaults only, but user changes are lost!
      // org.apache.maven.plugins
      // org.codehaus.mojo
      groupIdSearchExpressions.add(new MatchTypedStringSearchExpression("org.apache.maven.plugins", MatchType.EXACT));
      groupIdSearchExpressions.add(new MatchTypedStringSearchExpression("org.codehaus.mojo", MatchType.EXACT));
    } else {
      groupIdSearchExpressions.add(new MatchTypedStringSearchExpression(groupId, MatchType.EXACT));
    }

    try {
      TreeSet<String> ids = new TreeSet<>();
      for(IndexedArtifact artifact : index.find(groupIdSearchExpressions, null, null,
          packaging.toSearchExpression() == null ? null : Collections.singleton(packaging.toSearchExpression()))) {
        ids.add(artifact.getArtifactId());
      }
      return subSet(ids, searchExpression);
    } catch(CoreException ex) {
      throw new SearchException(ex.getMessage(), ex.getStatus().getException());
    }
  }

  @Override
  public Collection<String> findClassifiers(String groupId, String artifactId, String version, String prefix,
      Packaging packaging) {
    try {
      Collection<IndexedArtifact> values = index.find(new MatchTypedStringSearchExpression(groupId, MatchType.EXACT),
          new MatchTypedStringSearchExpression(artifactId, MatchType.EXACT), null, packaging.toSearchExpression());
      if(values.isEmpty()) {
        return Collections.emptySet();
      }

      TreeSet<String> ids = new TreeSet<>();
      Set<IndexedArtifactFile> files = values.iterator().next().getFiles();
      for(IndexedArtifactFile artifactFile : files) {
        if(artifactFile.classifier != null) {
          ids.add(artifactFile.classifier);
        }
      }
      return subSet(ids, prefix);
    } catch(CoreException ex) {
      throw new SearchException(ex.getMessage(), ex.getStatus().getException());
    }
  }

  @Override
  public Collection<String> findGroupIds(String searchExpression, Packaging packaging, ArtifactInfo containingArtifact) {
    try {
      TreeSet<String> ids = new TreeSet<>();

      SearchExpression groupSearchExpression = isBlank(searchExpression) ? null : new MatchTypedStringSearchExpression(
          searchExpression, MatchType.PARTIAL);

      for(IndexedArtifact artifact : index.find(groupSearchExpression, null, null, packaging.toSearchExpression())) {
        ids.add(artifact.getGroupId());
      }
      return subSet(ids, searchExpression);
    } catch(CoreException ex) {
      throw new SearchException(ex.getMessage(), ex.getStatus().getException());
    }
  }

  @Override
  public Collection<String> findTypes(String groupId, String artifactId, String version, String prefix,
      Packaging packaging) {
    try {
      Collection<IndexedArtifact> values = index.find(new MatchTypedStringSearchExpression(groupId, MatchType.EXACT),
          new MatchTypedStringSearchExpression(artifactId, MatchType.EXACT), null, packaging.toSearchExpression());
      if(values.isEmpty()) {
        return Collections.emptySet();
      }

      TreeSet<String> ids = new TreeSet<>();
      Set<IndexedArtifactFile> files = values.iterator().next().getFiles();
      for(IndexedArtifactFile artifactFile : files) {
        if(artifactFile.type != null) {
          ids.add(artifactFile.type);
        }
      }
      return subSet(ids, prefix);
    } catch(CoreException ex) {
      throw new SearchException(ex.getMessage(), ex.getStatus().getException());
    }
  }

  @Override
  public Collection<String> findVersions(String groupId, String artifactId, String searchExpression, Packaging packaging) {
    try {
      Collection<IndexedArtifact> values = index.find(new MatchTypedStringSearchExpression(groupId, MatchType.EXACT),
          new MatchTypedStringSearchExpression(artifactId, MatchType.EXACT), null, packaging.toSearchExpression());
      if(values.isEmpty()) {
        return Collections.emptySet();
      }

      TreeSet<String> ids = new TreeSet<>();
      Set<IndexedArtifactFile> files = values.iterator().next().getFiles();
      for(IndexedArtifactFile artifactFile : files) {
        ids.add(artifactFile.version);
      }
      Collection<String> result = subSet(ids, searchExpression);

      // sort results according to o.a.m.artifact.versioning.ComparableVersion
      SortedSet<ComparableVersion> versions = new TreeSet<>();
      for(String version : result) {
        versions.add(new ComparableVersion(version));
      }
      result = null; // not used any more
      List<String> sorted = new ArrayList<>(versions.size());
      for(ComparableVersion version : versions) {
        sorted.add(version.toString());
      }
      versions = null; // not used any more
      Collections.reverse(sorted);
      return sorted;
    } catch(CoreException ex) {
      throw new SearchException(ex.getMessage(), ex.getStatus().getException());
    }
  }

  private Collection<String> subSet(TreeSet<String> ids, String searchExpression) {
    if(searchExpression == null || searchExpression.length() == 0) {
      return ids;
    }
    int n = searchExpression.length();
    return ids.subSet(searchExpression, //
        searchExpression.substring(0, n - 1) + ((char) (searchExpression.charAt(n - 1) + 1)));
  }

}
