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

package org.eclipse.m2e.core.ui.internal.search.util;

import java.util.Collection;


/**
 * Interface to be implemented by a SearchEngine.
 * 
 * @author Lukas Krecan
 */
public interface SearchEngine {

  /**
   * Finds groupIds for given expression.
   * 
   * @param searchExpression
   * @param packaging
   * @param containingArtifact When looking for exclusion, contains information about artifact we are excluding from.
   * @return
   */
  public Collection<String> findGroupIds(String searchExpression, Packaging packaging, ArtifactInfo containingArtifact);

  /**
   * Finds artifactIds for given expression
   * 
   * @param groupId
   * @param searchExpression
   * @param packaging
   * @param containingArtifact When looking for exclusion, contains information about artifact we are excluding from.
   * @return
   */
  public Collection<String> findArtifactIds(String groupId, String searchExpression, Packaging packaging,
      ArtifactInfo containingArtifact);

  public Collection<String> findVersions(String groupId, String artifactId, String searchExpression, Packaging packaging);

  public Collection<String> findClassifiers(String groupId, String artifactId, String version, String prefix,
      Packaging packaging);

  public Collection<String> findTypes(String groupId, String artifactId, String version, String prefix,
      Packaging packaging);

}
