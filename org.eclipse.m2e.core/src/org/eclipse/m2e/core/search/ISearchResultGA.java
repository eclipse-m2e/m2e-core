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
package org.eclipse.m2e.core.search;

import java.util.List;

/**
 * ISearchResultGA represents a maven groupId and artifactId tuple.
 * 
 * @author Matthew Piggott
 */
public interface ISearchResultGA {
  /**
   * Returns the groupId of the component.
   */
  String getGroupId();

  /**
   * Returns the artifactId of this component.
   */
  String getArtifactId();

  /**
   * Returns the classname used to find the component.
   */
  String getClassname();

  /**
   * Returns the package name of the class.
   */
  String getPackageName();

  /**
   * Provide a list of version, extension and classifiers known for the GA. List may be incomplete and is used if the
   * user does not expand the GA.
   */
  List<ISearchResultGAVEC> getComponents();

  ISearchProvider getProvider();
}
