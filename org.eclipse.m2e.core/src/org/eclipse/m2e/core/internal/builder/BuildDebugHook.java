/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

  public void buildStart(IMavenProjectFacade projectFacade, int kind, Map<String, String> args,
      Map<MojoExecutionKey, List<AbstractBuildParticipant>> participants, IResourceDelta delta, IProgressMonitor monitor);

  public void buildParticipant(IMavenProjectFacade projectFacade, MojoExecutionKey mojoExecutionKey,
      AbstractBuildParticipant participant, Set<File> files, IProgressMonitor monitor);

}
