/*******************************************************************************
 * Copyright (c) 2008-2012 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Andrew Eisenberg - Work on Bug 350414
 *******************************************************************************/

package org.eclipse.m2e.editor.xml.internal.lifecycle;

import java.util.HashSet;

import org.codehaus.plexus.util.xml.Xpp3Dom;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Point;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionFilter;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.ui.internal.UpdateMavenProjectJob;
import org.eclipse.m2e.editor.xml.internal.Messages;


@SuppressWarnings("restriction")
public class WorkspaceLifecycleMappingProposal extends AbstractLifecycleMappingProposal implements ICompletionProposal {

  public WorkspaceLifecycleMappingProposal(IMarker marker, PluginExecutionAction action) {
    super(marker, action);
  }

  @Override
  public void run(IMarker[] markers, IProgressMonitor monitor) {
    // force reload from disk in case mapping file was modified by external process
    LifecycleMappingMetadataSource mapping = LifecycleMappingFactory.getWorkspaceMetadata(true);
    for(IMarker marker : markers) {
      addMapping(mapping, marker);
    }
    LifecycleMappingFactory.writeWorkspaceMetadata(mapping);

    // must kick off an update project job since the pom isn't modified.
    // Only update the project from where this quick fix was executed.
    // Other projects can be updated manually
    new UpdateMavenProjectJob(new IProject[] {marker.getResource().getProject()}).schedule();
  }

  private void addMapping(LifecycleMappingMetadataSource mapping, IMarker marker) {
    String groupId = marker.getAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, ""); //$NON-NLS-1$
    String artifactId = marker.getAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, ""); //$NON-NLS-1$
    String version = marker.getAttribute(IMavenConstants.MARKER_ATTR_VERSION, ""); //$NON-NLS-1$
    String[] goals = new String[] {marker.getAttribute(IMavenConstants.MARKER_ATTR_GOAL, "")}; //$NON-NLS-1$

    PluginExecutionMetadata execution = getPluginExecutionMetadata(mapping, groupId, artifactId, version);

    if(execution == null) {
      execution = new PluginExecutionMetadata();
      execution.setSource(mapping);
      execution.setFilter(new PluginExecutionFilter(groupId, artifactId, version, new HashSet<String>()));

      Xpp3Dom actionDom = new Xpp3Dom("action");
      actionDom.addChild(new Xpp3Dom(action.toString()));
      execution.setActionDom(actionDom);

      mapping.addPluginExecution(execution);
    }

    for(String goal : goals) {
      execution.getFilter().addGoal(goal);
    }
  }

  private PluginExecutionMetadata getPluginExecutionMetadata(LifecycleMappingMetadataSource mapping, String groupId,
      String artifactId, String version) {
    for(PluginExecutionMetadata execution : mapping.getPluginExecutions()) {
      PluginExecutionFilter filter = execution.getFilter();
      if(eq(groupId, filter.getGroupId()) && eq(artifactId, filter.getArtifactId())
          && eq(version, filter.getVersionRange()) && action == execution.getAction()) {
        return execution;
      }
    }
    return null;
  }

  @Override
  public String getDisplayString() {
    String goal = marker.getAttribute(IMavenConstants.MARKER_ATTR_GOAL, ""); //$NON-NLS-1$
    return NLS.bind(Messages.LifecycleMappingProposal_workspaceIgnore_label, goal);
  }

  private static <S> boolean eq(S a, S b) {
    return a != null ? a.equals(b) : b == null;
  }

  public void apply(IDocument document) {
    run(marker);
  }

  public Point getSelection(IDocument document) {
    return null;
  }

  public String getAdditionalProposalInfo() {
    return null;
  }

  public IContextInformation getContextInformation() {
    return null;
  }
}
