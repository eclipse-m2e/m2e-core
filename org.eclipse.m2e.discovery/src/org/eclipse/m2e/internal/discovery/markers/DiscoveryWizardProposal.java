/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.discovery.markers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.internal.discovery.MavenDiscovery;
import org.eclipse.m2e.internal.discovery.Messages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;


class DiscoveryWizardProposal extends WorkbenchMarkerResolution {
  
  static final DiscoveryWizardProposal PROPOSAL = new DiscoveryWizardProposal();

  @SuppressWarnings("unchecked")
  public void run(IMarker marker) {
    String type = marker.getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, null);
    if(IMavenConstants.EDITOR_HINT_UNKNOWN_PACKAGING.equals(type)) {
      MavenDiscovery.launchWizard(Arrays.asList(new String[] {getPackageType(marker)}), Collections.EMPTY_LIST);
    } else if(IMavenConstants.EDITOR_HINT_NOT_COVERED_MOJO_EXECUTION.equals(type)) {
      MavenDiscovery
          .launchWizard(Collections.EMPTY_LIST, Arrays.asList(new MojoExecution[] {getMojoExecution(marker)}));
    }

  }

  public String getDescription() {
    return Messages.DiscoveryWizardProposal_Label;
  }

  public Image getImage() {
    return null;
  }

  @Override
  public void run(IMarker[] markers, IProgressMonitor monitor) {
    List<String> packagingTypes = new ArrayList<String>();
    List<MojoExecution> mojos = new ArrayList<MojoExecution>();
    for(IMarker marker : markers) {
      String type = marker.getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, null);
      if(IMavenConstants.EDITOR_HINT_UNKNOWN_PACKAGING.equals(type)) {
        packagingTypes.add(getPackageType(marker));
      } else if(IMavenConstants.EDITOR_HINT_NOT_COVERED_MOJO_EXECUTION.equals(type)) {
        mojos.add(getMojoExecution(marker));
      }
    }
    MavenDiscovery.launchWizard(packagingTypes, mojos);
  }

  private MojoExecution getMojoExecution(IMarker marker) {
    // TODO Which of these are actually required?
    String groupId = marker.getAttribute("groupId", null);
    String artifactId = marker.getAttribute("artifactId", null);
    String executionId = marker.getAttribute("executionId", null);
    String version = marker.getAttribute("version", null);
    String goal = marker.getAttribute("goal", null);
    if(goal != null && executionId != null && artifactId != null && groupId != null) {
      Plugin plugin = new Plugin();
      plugin.setArtifactId(artifactId);
      plugin.setGroupId(groupId);
      plugin.setVersion(version);
      return new MojoExecution(plugin, goal, executionId);
    }
    return null;
  }

  private String getPackageType(IMarker marker) {
    return marker.getAttribute(IMavenConstants.MARKER_ATTR_PACKAGING, null);
  }

  @Override
  public IMarker[] findOtherMarkers(IMarker[] markers) {
    List<IMarker> handled = new ArrayList<IMarker>();
    for(IMarker marker : markers) {
      if(MavenDiscoveryMarkerResolutionGenerator.canResolve(marker)) {
        handled.add(marker);
      }
    }
    return handled.toArray(new IMarker[handled.size()]);
  }

  public String getLabel() {
    return Messages.DiscoveryWizardProposal_Label;
  }
}