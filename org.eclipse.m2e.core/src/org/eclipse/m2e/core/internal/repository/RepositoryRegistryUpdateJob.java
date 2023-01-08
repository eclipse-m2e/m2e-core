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

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.jobs.IBackgroundProcessingQueue;
import org.eclipse.m2e.core.internal.jobs.MavenJob;


/**
 * RepositoryRegistryUpdateJob
 *
 * @author igor
 */
public class RepositoryRegistryUpdateJob extends MavenJob implements IBackgroundProcessingQueue {

  private final RepositoryRegistry registry;

  private final ArrayList<Object> queue = new ArrayList<>();

  public RepositoryRegistryUpdateJob(RepositoryRegistry registry) {
    super(Messages.RepositoryRegistryUpdateJob_title);
    this.registry = registry;
  }

  @Override
  public IStatus run(IProgressMonitor monitor) {
    synchronized(queue) {
      queue.clear();
    }
    try {
      registry.updateRegistry(monitor);
    } catch(CoreException ex) {
      return ex.getStatus();
    }
    return Status.OK_STATUS;
  }

  @Override
  public boolean isEmpty() {
    synchronized(queue) {
      return queue.isEmpty();
    }
  }

  public void updateRegistry() {
    synchronized(queue) {
      queue.add(new Object());
      schedule(1000L);
    }
  }
}
