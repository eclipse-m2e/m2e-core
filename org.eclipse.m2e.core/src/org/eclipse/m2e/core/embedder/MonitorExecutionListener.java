/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.embedder;

import java.util.List;
import java.util.concurrent.CancellationException;

import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.LifecycleTaskSegmentCalculator;
import org.apache.maven.lifecycle.internal.TaskSegment;


/**
 * This is a wrapper from an {@link IProgressMonitor} to an {@link ExecutionListener} that could be used to report
 * progress
 */
public class MonitorExecutionListener implements ExecutionListener {

  private IProgressMonitor monitor;

  public MonitorExecutionListener(IProgressMonitor monitor) {
    this.monitor = IProgressMonitor.nullSafe(monitor);
  }

  @Override
  public void mojoStarted(ExecutionEvent event) {
    String id = event.getMojoExecution().getPlugin().getId();
    monitor.subTask("Executing " + id + "...");
  }

  void mojoFinished(ExecutionEvent event) {
    if(monitor.isCanceled()) {
      //this might not be the nicest approach here, but from the maven code the EventCatapult
      //is passing exceptions occurring in the ExecutionListener and there is no other way to stop maven
      throw new CancellationException();
    }
    monitor.worked(1);
    monitor.subTask("");
  }

  @Override
  public void mojoFailed(ExecutionEvent event) {
    mojoFinished(event);
  }

  @Override
  public void mojoSkipped(ExecutionEvent event) {
    mojoFinished(event);
  }

  @Override
  public void mojoSucceeded(ExecutionEvent event) {
    mojoFinished(event);

  }

  @Override
  public void projectStarted(ExecutionEvent event) {
    monitor.setTaskName("Building " + event.getProject().getName() + "...");
  }

  void projectFinished(ExecutionEvent event) {
    monitor.setTaskName("Building...");
  }

  @Override
  public void projectFailed(ExecutionEvent event) {
    projectFinished(event);
  }

  @Override
  public void projectSkipped(ExecutionEvent event) {
    projectFinished(event);
  }

  @Override
  public void projectSucceeded(ExecutionEvent event) {
    projectFinished(event);
  }

  @Override
  public void sessionEnded(ExecutionEvent event) {
    monitor.done();
  }

  @Override
  public void sessionStarted(ExecutionEvent event) {
    int totalWork;
    try {
      MavenSession session = event.getSession();
      LifecycleTaskSegmentCalculator calculator = (LifecycleTaskSegmentCalculator) session
          .lookup(LifecycleTaskSegmentCalculator.class.getName());
      List<TaskSegment> segments = calculator.calculateTaskSegments(session);
      totalWork = (int) (segments.stream().flatMap(seg -> seg.getTasks().stream()).count()
          * session.getProjects().size());
    } catch(Exception ex) {
      totalWork = IProgressMonitor.UNKNOWN;
    }
    monitor.beginTask("Building ...", totalWork);
  }

  //not used currently ...
  @Override
  public void projectDiscoveryStarted(ExecutionEvent event) {

  }

  @Override
  public void forkFailed(ExecutionEvent event) {
  }

  @Override
  public void forkStarted(ExecutionEvent event) {
  }

  @Override
  public void forkSucceeded(ExecutionEvent event) {
  }

  @Override
  public void forkedProjectFailed(ExecutionEvent event) {
  }

  @Override
  public void forkedProjectStarted(ExecutionEvent event) {
  }

  @Override
  public void forkedProjectSucceeded(ExecutionEvent event) {
  }

}
