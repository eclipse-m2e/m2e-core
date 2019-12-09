/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Andrew Eisenberg - Work on Bug 350414
 *      Anton Tanasenko - Refactor marker resolutions and quick fixes (Bug #484359)
 *******************************************************************************/

package org.eclipse.m2e.editor.xml.internal.lifecycle;

import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.ui.internal.UpdateMavenProjectJob;
import org.eclipse.m2e.editor.xml.internal.Messages;


@SuppressWarnings("restriction")
public class WorkspaceLifecycleMappingResolution extends AbstractLifecycleMappingResolution {

  public WorkspaceLifecycleMappingResolution(IMarker marker, PluginExecutionAction action) {
    super(marker, action);
  }

  @Override
  public int getOrder() {
    return 60;
  }

  @Override
  protected void fix(IDocument document, List<IMarker> markers, IProgressMonitor monitor) {
    doFix(markers, monitor);
  }

  @Override
  protected void fix(IResource resource, List<IMarker> markers, IProgressMonitor monitor) {
    doFix(markers, monitor);
  }

  @Override
  public String getLabel() {
    String goal = getMarker().getAttribute(IMavenConstants.MARKER_ATTR_GOAL, ""); //$NON-NLS-1$
    return NLS.bind(Messages.LifecycleMappingProposal_workspaceIgnore_label, goal);
  }

  private void doFix(List<IMarker> markers, IProgressMonitor monitor) {
    // force reload from disk in case mapping file was modified by external process
    LifecycleMappingMetadataSource mapping = LifecycleMappingFactory.getWorkspaceMetadata(true);
    for(IMarker marker : markers) {
      addMapping(mapping, marker);
    }
    LifecycleMappingFactory.writeWorkspaceMetadata(mapping);

    // must kick off an update project job since the pom isn't modified.
    // Only update the project from where this quick fix was executed.
    // Other projects can be updated manually
    new UpdateMavenProjectJob(toArray(getProjects(markers.stream()))).schedule();
  }

  private void addMapping(LifecycleMappingMetadataSource mapping, IMarker marker) {
    String groupId = marker.getAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, ""); //$NON-NLS-1$
    String artifactId = marker.getAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, ""); //$NON-NLS-1$
    String version = marker.getAttribute(IMavenConstants.MARKER_ATTR_VERSION, ""); //$NON-NLS-1$
    String[] goals = new String[] {marker.getAttribute(IMavenConstants.MARKER_ATTR_GOAL, "")}; //$NON-NLS-1$

    LifecycleMappingFactory.addLifecyclePluginExecution(mapping, groupId, artifactId, version, goals, action);
  }

  private static IProject[] toArray(Set<IProject> projects) {
    return projects.toArray(new IProject[projects.size()]);
  }

}
