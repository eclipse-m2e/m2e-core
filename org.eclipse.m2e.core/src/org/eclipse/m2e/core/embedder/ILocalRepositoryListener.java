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

package org.eclipse.m2e.core.embedder;

import java.io.File;


/**
 * ILocalRepositoryListener
 *
 * @author igor
 * @provisional This interface is provisional and can be changed or removed without notice
 */
public interface ILocalRepositoryListener {

  /**
   * New artifact has been downloaded or installed to maven local repository
   */
  void artifactInstalled(File repositoryBasedir, ArtifactKey baseArtifact, ArtifactKey artifact,
      File artifactFile);
}
