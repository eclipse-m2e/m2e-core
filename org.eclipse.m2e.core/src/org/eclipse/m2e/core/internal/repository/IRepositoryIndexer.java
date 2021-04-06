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

package org.eclipse.m2e.core.internal.repository;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.m2e.core.repository.IRepository;


/**
 * IRepositoryIndexer
 *
 * @author igor
 */
public interface IRepositoryIndexer {

  String getIndexerId();

  /**
   * This method is called from a background thread which does not keep any workspace locks.
   */
  void initialize(IProgressMonitor monitor) throws CoreException;

  /**
   * Called by repository registry when new repository is added. This method is called from a background thread which
   * does not keep any workspace locks.
   */
  void repositoryAdded(IRepository repository, IProgressMonitor monitor) throws CoreException;

  /**
   * Called by repository registry when a repository is removed. This method is called from a background thread which
   * does not keep any workspace locks.
   */
  void repositoryRemoved(IRepository repository, IProgressMonitor monitor) throws CoreException;
}
