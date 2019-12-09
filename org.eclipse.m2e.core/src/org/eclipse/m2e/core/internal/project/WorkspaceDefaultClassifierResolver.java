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
package org.eclipse.m2e.core.internal.project;

import org.eclipse.core.runtime.IPath;

import org.eclipse.m2e.core.project.AbstractWorkspaceClassifierResolver;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IWorkspaceClassifierResolver;


/**
 * {@link IWorkspaceClassifierResolver} which resolves artifacts with null or empty classifier
 *
 * @author atanasenko
 * @since 1.9
 */
public class WorkspaceDefaultClassifierResolver extends AbstractWorkspaceClassifierResolver {

  public IPath resolveClassifier(IMavenProjectFacade project, String classifier) {
    if(classifier == null || "".equals(classifier)) { // //$NON-NLS-1$
      return project.getOutputLocation();
    }
    return null;
  }

  public int getPriority() {
    return Integer.MAX_VALUE;
  }

}
