/*******************************************************************************
 * Copyright (c) 2017 Walmartlabs
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Anton Tanasenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.core.project;

import org.eclipse.core.runtime.IPath;


/**
 * Resolves workspace projects based on a requested classifier
 *
 * @author atanasenko
 * @since 1.9
 */
public interface IWorkspaceClassifierResolver {

  /**
   * Returns a path relative to workspace which corresponds to a requested classifier
   */
  IPath resolveClassifier(IMavenProjectFacade project, String classifier);

  int getPriority();

}
