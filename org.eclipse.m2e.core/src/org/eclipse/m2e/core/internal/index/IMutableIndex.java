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

package org.eclipse.m2e.core.internal.index;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.m2e.core.embedder.ArtifactKey;


/**
 * @author igor
 */
public interface IMutableIndex extends IIndex {

  // index content manipulation

  void addArtifact(File pomFile, ArtifactKey artifactKey);

  void removeArtifact(File pomFile, ArtifactKey artifactKey);

  // reindexing

  void updateIndex(boolean force, IProgressMonitor monitor) throws CoreException;

}
