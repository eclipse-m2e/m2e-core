/********************************************************************************
 * Copyright (c) 2023, 2023 Michael Keppler and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Michael Keppler - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.core.internal.jobs;

import org.eclipse.core.runtime.jobs.Job;


/**
 * Specialization of {@link Job} which displays an m2 icon in the progress UI.
 */
public abstract class MavenJob extends Job {

  public static final Object FAMILY_M2 = new Object();

  /**
   * Creates a new job with the specified name. The job name is a human-readable value that is displayed to users. The
   * name does not need to be unique, but it must not be <code>null</code>.
   *
   * @param name the name of the job.
   */
  public MavenJob(String name) {
    super(name);
  }

  @Override
  public boolean belongsTo(Object family) {
    if(FAMILY_M2.equals(family)) {
      return true;
    }
    return super.belongsTo(family);
  }

}
