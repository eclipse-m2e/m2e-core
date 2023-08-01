/*******************************************************************************
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Takari, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.builder;

import java.io.File;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;


/**
 * @experimental this interface is part of work in progress and can be changed or removed without notice.
 * @since 1.6
 */
public interface IIncrementalBuildFramework {

  /**
   * @experimental this interface is part of work in progress and can be changed or removed without notice.
   * @since 1.6
   */
  public interface BuildResultCollector {

    void refresh(File file);

    void addMessage(File file, int line, int column, String message, int severity, Throwable cause);

    void removeMessages(File file);

    /**
     * @since 1.6.2
     */
    Set<File> getFiles();
  }

  /**
   * @experimental this interface is part of work in progress and can be changed or removed without notice.
   * @since 2.4
   */
  public interface BuildDelta {
    boolean hasDelta(File file);
  }

  /**
   * @experimental this interface is part of work in progress and can be changed or removed without notice.
   * @since 1.6
   */
  public interface BuildContext {
    void release();
  }

  BuildContext setupProjectBuildContext(IProject project, int kind, BuildDelta delta, BuildResultCollector results)
      throws CoreException;

}
