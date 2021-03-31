/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.internal.index.filter;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.m2e.core.embedder.ArtifactKey;


/**
 * Experimental extension point interface that allows filtering of maven index artifacts from appearing from various
 * places where m2e allows selection of artifacts.
 */
public interface IArtifactFilter {

  /**
   * @param project is the filtering context project. can be <code>null</code> if no project context.
   * @param artifact is the artifact to filter
   * @return <code>null</code> or OK status if the artifact should be allowed, INFO/WARNING status to allow with a
   *         message and ERROR status to block the artifact.
   */
  IStatus filter(IProject project, ArtifactKey artifact);
}
