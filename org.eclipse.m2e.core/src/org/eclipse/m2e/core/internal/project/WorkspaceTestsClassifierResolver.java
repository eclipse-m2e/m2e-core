/*******************************************************************************
 * Copyright (c) 2017 Walmartlabs
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * {@link IWorkspaceClassifierResolver} which resolves artifacts with 'tests' classifier
 *
 * @author atanasenko
 * @since 1.9
 */
public class WorkspaceTestsClassifierResolver extends AbstractWorkspaceClassifierResolver {

  public IPath resolveClassifier(IMavenProjectFacade project, String classifier) {
    if("tests".equals(classifier)) { // //$NON-NLS-1$
      return project.getTestOutputLocation();
    }
    return null;
  }

  public int getPriority() {
    return Integer.MAX_VALUE;
  }

}
