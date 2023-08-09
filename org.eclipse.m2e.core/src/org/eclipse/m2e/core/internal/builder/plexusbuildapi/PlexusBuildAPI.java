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

package org.eclipse.m2e.core.internal.builder.plexusbuildapi;

import static org.eclipse.core.resources.IncrementalProjectBuilder.AUTO_BUILD;
import static org.eclipse.core.resources.IncrementalProjectBuilder.CLEAN_BUILD;
import static org.eclipse.core.resources.IncrementalProjectBuilder.INCREMENTAL_BUILD;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant2;
import org.sonatype.plexus.build.incremental.ThreadBuildContext;


/**
 * @since 1.6
 */
public class PlexusBuildAPI implements IIncrementalBuildFramework {
  public static QualifiedName BUILD_CONTEXT_KEY = new QualifiedName(IMavenConstants.PLUGIN_ID, "BuildContext"); //$NON-NLS-1$

  @Override
  public BuildContext setupProjectBuildContext(IProject project, int kind, BuildDelta delta,
      IIncrementalBuildFramework.BuildResultCollector results) throws CoreException {
    @SuppressWarnings("unchecked")
    Map<String, Object> contextState = (Map<String, Object>) project.getSessionProperty(BUILD_CONTEXT_KEY);
    BuildContext buildContext;
    if(delta != null && contextState != null && (INCREMENTAL_BUILD == kind || AUTO_BUILD == kind)) {
      buildContext = new EclipseIncrementalBuildContext(delta, contextState, results, project.getLocation().toFile());
    } else if(CLEAN_BUILD == kind) {
      project.setSessionProperty(BUILD_CONTEXT_KEY, null); // clean context state
      buildContext = new EclipseBuildContext(project, new HashMap<>(), results);
    } else {
      contextState = new HashMap<>();
      project.setSessionProperty(BUILD_CONTEXT_KEY, contextState);
      if(AbstractBuildParticipant2.PRECONFIGURE_BUILD == kind) {
        buildContext = new EclipseEmptyBuildContext(project, contextState, results);
      } else {
        // must be full build
        buildContext = new EclipseBuildContext(project, contextState, results);
      }
    }
    ThreadBuildContext.setThreadBuildContext((org.sonatype.plexus.build.incremental.BuildContext) buildContext);
    return buildContext;
  }

}
