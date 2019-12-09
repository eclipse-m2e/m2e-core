/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.jdt;

import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * IMavenClassifierManager
 * 
 * @author Fred Bricon
 * @since 1.3
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IMavenClassifierManager {

  /**
   * @param mavenProjectFacade
   * @param classifier
   * @return
   */
  IClassifierClasspathProvider getClassifierClasspathProvider(IMavenProjectFacade mavenProjectFacade, String classifier);

}
