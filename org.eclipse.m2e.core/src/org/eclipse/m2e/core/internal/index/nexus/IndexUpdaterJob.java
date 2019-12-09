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

package org.eclipse.m2e.core.internal.index.nexus;

import java.util.ArrayList;
import java.util.Stack;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.jobs.IBackgroundProcessingQueue;


class IndexUpdaterJob extends Job implements IBackgroundProcessingQueue {

  public static class IndexUpdaterRule implements ISchedulingRule {

    public boolean contains(ISchedulingRule rule) {
      return rule == this;
    }

    public boolean isConflicting(ISchedulingRule rule) {
      return rule == this;
    }

  }

  public interface IndexCommand {
    abstract void run(IProgressMonitor monitor) throws CoreException;
  }

  private final Stack<IndexUpdaterJob.IndexCommand> updateQueue = new Stack<IndexUpdaterJob.IndexCommand>();

  public IndexUpdaterJob(NexusIndexManager indexManager) {
    super(Messages.IndexUpdaterJob_title);
    setRule(new IndexUpdaterRule());
  }

  public void addCommand(IndexUpdaterJob.IndexCommand indexCommand) {
    updateQueue.add(indexCommand);
  }

  public IStatus run(IProgressMonitor monitor) {
    monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);

    ArrayList<IStatus> problems = new ArrayList<IStatus>();

    while(!updateQueue.isEmpty()) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      IndexUpdaterJob.IndexCommand command = updateQueue.pop();
      try {
        command.run(monitor);
      } catch(CoreException ex) {
        problems.add(ex.getStatus());
      }
    }

    monitor.done();

    return problems.isEmpty() ? Status.OK_STATUS : new MultiStatus(IMavenConstants.PLUGIN_ID, -1,
        problems.toArray(new IStatus[problems.size()]), null, null);
  }

  public boolean isEmpty() {
    return updateQueue.isEmpty();
  }

}
