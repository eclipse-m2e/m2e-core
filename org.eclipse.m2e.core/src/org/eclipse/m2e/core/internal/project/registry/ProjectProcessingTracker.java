/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.core.internal.project.registry;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;


/**
 * ProjectProcessingTracker keeps track of the progress in refreshing projects.
 *
 */
public class ProjectProcessingTracker {

  static final int MAX_ITERATIONS = Integer.getInteger("m2e.project.refresh.maxiterations", 5);

  static final Logger log = LoggerFactory.getLogger(ProjectProcessingTracker.class);

  private Set<IFile> processed = new LinkedHashSet<>();

  private Set<IFile> changedWhileRunning = new LinkedHashSet<>();

  private Set<IFile> seed;

  private DependencyResolutionContext context;

  private int iterations;

  /**
   * @param allProcessedPoms
   */
  public ProjectProcessingTracker(DependencyResolutionContext context) {
    this.context = context;
    this.seed = context.getCurrent();
  }

  /**
   * @param context
   * @return
   */
  public boolean needsImprovement() {
    if(changedWhileRunning.isEmpty()) {
      log.debug("Nothing changed in this cycle.");
      return false;
    }
    if(seed.isEmpty()) {
      log.debug("Seed is empty.");
      return false;
    }
    iterations++ ;
    if(iterations > MAX_ITERATIONS) {
      log.debug("Configured maximum of {} iterations reached!", iterations);
      return false;
    }
    log.debug("seed =      {}", seed);
    log.debug("processed = {}", processed);
    log.debug("changed =   {}", changedWhileRunning);
    boolean removed = false;
    for(Iterator<IFile> iterator = seed.iterator(); iterator.hasNext();) {
      IFile file = iterator.next();
      if(changedWhileRunning.contains(file)) {
        //we need to keep this in the seed...
        continue;
      }
      log.debug("{} was improved!", file);
      iterator.remove();
      removed = true;
    }
    if(removed) {
      //we have an improvement...
      context.forcePomFiles(changedWhileRunning);
      //add all possibly new ones to the seed...
      seed.addAll(changedWhileRunning);
      //clear everything for next iteration...
      changedWhileRunning.clear();
      processed.clear();
      return true;
    }
    log.debug("No new project was refreshed -> no improvement found!");
    return false;
  }

  /**
   * @param pom
   * @return
   */
  public boolean shouldProcess(IFile pom) {
    if(processed.add(pom)) {
      return true;
    }
    changedWhileRunning.add(pom);
    return false;
  }

}
