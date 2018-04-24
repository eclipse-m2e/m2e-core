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
package org.eclipse.m2e.search.central.internal;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.search.ISearchProvider;
import org.eclipse.m2e.core.search.ISearchResultGA;
import org.eclipse.m2e.core.search.ISearchResultGAVEC;
import org.eclipse.m2e.search.central.internal.model.MavenCentralQuery;
import org.eclipse.m2e.search.central.internal.util.MavenCentral;
import org.eclipse.m2e.search.central.internal.util.ResponseHelper;

/**
 * @author Matthew Piggott
 * @since 1.17.0
 */
public class CentralSearchProvider implements ISearchProvider {

  private MavenCentral mavenCentral;

  public CentralSearchProvider() {
    mavenCentral = new MavenCentral();
  }

  @Override
  public List<ISearchResultGA> find(IProgressMonitor monitor, SearchType searchType, String queryTerm)
      throws CoreException {
    return ResponseHelper.convert(this, searchType, mavenCentral.query(createQuery(searchType, queryTerm)));
  }

  @Override
  public List<ISearchResultGAVEC> getArtifacts(IProgressMonitor monitor, ISearchResultGA source)
      throws CoreException {
    MavenCentralQuery query = new MavenCentralQuery();
    query.setGroupId(source.getGroupId());
    query.setArtifactId(source.getArtifactId());
    return ResponseHelper.getArtifacts(this, SearchType.artifact, mavenCentral.getVersionsByGA(query));
  }

  private MavenCentralQuery createQuery(SearchType searchType, String keyword) {
    MavenCentralQuery query = new MavenCentralQuery();
    switch (searchType) {
    case plugin:
      query.setKeyword(keyword);
      query.setPackaging("maven-plugin"); //$NON-NLS-1$
      break;
    case className:
      query.setClass(keyword);
      break;
    case parent:
      query.setKeyword(keyword);
      query.setPackaging("pom"); //$NON-NLS-1$
      break;
    default:
      query.setKeyword(keyword);
      break;
    }
    return query;
  }

  @Override
  public IStatus getStatus() {
    return Status.OK_STATUS;
  }
}
