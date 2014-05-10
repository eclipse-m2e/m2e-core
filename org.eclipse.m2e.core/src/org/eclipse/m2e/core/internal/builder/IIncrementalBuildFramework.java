/*******************************************************************************
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Takari, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.builder;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
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
  public static interface BuildResultCollector {

    public void refresh(File file);

    public void addMessage(File file, int line, int column, String message, int severity, Throwable cause);

    public void removeMessages(File file);
  }

  /**
   * @experimental this interface is part of work in progress and can be changed or removed without notice.
   * @since 1.6
   */
  public static interface BuildContext {
    public void release();
  }

  public BuildContext setupProjectBuildContext(IProject project, int kind, IResourceDelta delta,
      BuildResultCollector results) throws CoreException;

}
