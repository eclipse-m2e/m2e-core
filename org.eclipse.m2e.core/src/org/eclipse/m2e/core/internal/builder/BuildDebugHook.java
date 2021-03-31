/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.builder;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


/**
 * @noreference This internal interface can be changed or removed without notice.
 * @since 1.4
 */
public interface BuildDebugHook {

  void buildStart(IMavenProjectFacade projectFacade, int kind, Map<String, String> args,
      Map<MojoExecutionKey, List<AbstractBuildParticipant>> participants, IResourceDelta delta, IProgressMonitor monitor);

  void buildParticipant(IMavenProjectFacade projectFacade, MojoExecutionKey mojoExecutionKey,
      AbstractBuildParticipant participant, Set<File> files, IProgressMonitor monitor);

}
