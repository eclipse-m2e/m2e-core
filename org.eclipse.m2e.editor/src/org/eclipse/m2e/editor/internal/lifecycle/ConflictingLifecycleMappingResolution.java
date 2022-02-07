/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.internal.lifecycle;

import java.util.List;

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
import org.eclipse.m2e.editor.internal.Messages;


/**
 * ConflictingLifecycleMappingResolution
 */
@SuppressWarnings("restriction")
public class ConflictingLifecycleMappingResolution extends AbstractLifecycleMappingResolution {

  private String prefix;

  /**
   * @param marker
   * @param action
   */
  public ConflictingLifecycleMappingResolution(IMarker marker, int index) {
    super(marker, PluginExecutionAction.ignore);
    prefix = IMavenConstants.MARKER_DUPLICATEMAPPING_SOURCES + "." + index + ".";
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.IMarkerResolution#getLabel()
   */
  @Override
  public String getLabel() {
    String source = getMarker().getAttribute(prefix + IMavenConstants.MARKER_DUPLICATEMAPPING_SOURCES_NAME, ""); //$NON-NLS-1$
    String type = getMarker().getAttribute(IMavenConstants.MARKER_DUPLICATEMAPPING_TYPE, ""); //$NON-NLS-1$
    String value = getMarker().getAttribute(IMavenConstants.MARKER_DUPLICATEMAPPING_VALUE, ""); //$NON-NLS-1$
    return NLS.bind(Messages.LifecycleMappingProposal_sourceIgnore_label, new Object[] {source, type, value});
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.markers.EditorAwareMavenProblemResolution#fix(org.eclipse.core.resources.IResource, java.util.List, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  protected void fix(IResource resource, List<IMarker> markers, IProgressMonitor monitor) {
    fix(markers, monitor);
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.markers.EditorAwareMavenProblemResolution#fix(org.eclipse.jface.text.IDocument, java.util.List, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  protected void fix(IDocument document, List<IMarker> markers, IProgressMonitor monitor) {
    fix(markers, monitor);
  }

  private void fix(List<IMarker> markers, IProgressMonitor monitor) {
    LifecycleMappingMetadataSource mapping = LifecycleMappingFactory.getWorkspaceMetadata(true);
    for(IMarker marker : markers) {
      addMapping(mapping, marker);
    }
    LifecycleMappingFactory.writeWorkspaceMetadata(mapping);

    // must kick off an update project job since the pom isn't modified.
    // Only update the project from where this quick fix was executed.
    // Other projects can be updated manually
    new UpdateMavenProjectJob(getProjects(markers.stream()).toArray(IProject[]::new)).schedule();
  }

  /**
   * @param mapping
   * @param marker
   */
  private void addMapping(LifecycleMappingMetadataSource mapping, IMarker marker) {
    String bsn = marker.getAttribute(prefix + IMavenConstants.MARKER_DUPLICATEMAPPING_SOURCES_BSN, ""); //$NON-NLS-1$
    String version = marker.getAttribute(prefix + IMavenConstants.MARKER_DUPLICATEMAPPING_SOURCES_VERSION, ""); //$NON-NLS-1$

    String type = marker.getAttribute(IMavenConstants.MARKER_DUPLICATEMAPPING_TYPE, "");
    String value = marker.getAttribute(IMavenConstants.MARKER_DUPLICATEMAPPING_VALUE, "");
    if(IMavenConstants.MARKER_DUPLICATEMAPPING_TYPE_PACKAGING.equals(type)) {
      LifecycleMappingFactory.addLifecycleMappingPackagingFilter(mapping, bsn, version, value);
    } else if(IMavenConstants.MARKER_DUPLICATEMAPPING_TYPE_GOAL.equals(type)) {
      String executionGroupId = marker.getAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, "");
      String executionArtifactId = marker.getAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, "");
      String executionVersion = marker.getAttribute(IMavenConstants.MARKER_ATTR_VERSION, "");
      String goal = marker.getAttribute(IMavenConstants.MARKER_ATTR_GOAL, "");
      LifecycleMappingFactory.addLifecycleMappingExecutionFilter(mapping, bsn, version, executionGroupId,
          executionArtifactId, executionVersion, goal);
    }

  }

}
