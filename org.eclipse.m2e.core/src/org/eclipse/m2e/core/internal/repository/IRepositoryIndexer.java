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

  public String getIndexerId();

  /**
   * This method is called from a background thread which does not keep any workspace locks.
   */
  public void initialize(IProgressMonitor monitor) throws CoreException;

  /**
   * Called by repository registry when new repository is added. 
   * 
   * This method is called from a background thread which does not keep any workspace locks.
   */
  public void repositoryAdded(IRepository repository, IProgressMonitor monitor) throws CoreException;

  /**
   * Called by repository registry when a repository is removed. 
   * 
   * This method is called from a background thread which does not keep any workspace locks.
   */
  public void repositoryRemoved(IRepository repository, IProgressMonitor monitor) throws CoreException;
}
