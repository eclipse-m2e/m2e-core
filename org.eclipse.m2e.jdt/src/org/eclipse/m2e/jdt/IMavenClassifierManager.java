/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
