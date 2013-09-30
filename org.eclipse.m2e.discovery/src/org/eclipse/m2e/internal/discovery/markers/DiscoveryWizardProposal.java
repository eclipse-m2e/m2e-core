/*******************************************************************************
 * Copyright (c) 2011-2013 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Red Hat, Inc. - refactored getMojoExecution(IMarker) out to MarkerUtils
 *******************************************************************************/

package org.eclipse.m2e.internal.discovery.markers;

import static org.eclipse.m2e.core.internal.markers.MarkerUtils.getMojoExecution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.internal.discovery.MavenDiscovery;
import org.eclipse.m2e.internal.discovery.MavenDiscoveryIcons;
import org.eclipse.m2e.internal.discovery.Messages;


//IMPORANT: if you decide to rename the class please correct code in PomQuickAssistProcessor as well..

public class DiscoveryWizardProposal extends WorkbenchMarkerResolution {

  public DiscoveryWizardProposal() {
  }

  @SuppressWarnings("unchecked")
  public void run(IMarker marker) {
    //by default we want save people some time by resolving discovery issues in one project/file in one shot..
    try {
      IMarker[] fileMarkers = marker.getResource().findMarkers(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID, true,
          IResource.DEPTH_INFINITE);
      run(fileMarkers, new NullProgressMonitor());
      return;
    } catch(CoreException e) {
      //doesn't matter, as a fallback run the one marker variant only
    }

    String type = marker.getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, null);
    if(IMavenConstants.EDITOR_HINT_NOT_COVERED_MOJO_EXECUTION.equals(type)) {
      MavenDiscovery.launchWizard(Collections.EMPTY_LIST, Collections.singleton(getMojoExecution(marker)),
          Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    } else if(IMavenConstants.EDITOR_HINT_UNKNOWN_LIFECYCLE_ID.equals(type)) {
      MavenDiscovery.launchWizard(Collections.EMPTY_LIST, Collections.EMPTY_LIST,
          Collections.singleton(getLifecycleId(marker)), Collections.EMPTY_LIST);
    } else if(IMavenConstants.EDITOR_HINT_MISSING_CONFIGURATOR.equals(type)) {
      MavenDiscovery.launchWizard(Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
          Collections.singleton(getConfiguratorId(marker)));
    }
  }

  public String getDescription() {
    return Messages.DiscoveryWizardProposal_description;
  }

  public Image getImage() {
    return MavenDiscoveryIcons.getImage(MavenDiscoveryIcons.QUICK_FIX_ICON);
  }

  @Override
  public void run(IMarker[] markers, IProgressMonitor monitor) {
    List<String> lifecycleIds = new ArrayList<String>();
    List<String> packagingTypes = new ArrayList<String>();
    List<MojoExecutionKey> mojos = new ArrayList<MojoExecutionKey>();
    List<String> configuratorIds = new ArrayList<String>();
    for(IMarker marker : markers) {
      String type = marker.getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, null);
      if(IMavenConstants.EDITOR_HINT_NOT_COVERED_MOJO_EXECUTION.equals(type)) {
        mojos.add(getMojoExecution(marker));
      } else if(IMavenConstants.EDITOR_HINT_UNKNOWN_LIFECYCLE_ID.equals(type)) {
        lifecycleIds.add(getLifecycleId(marker));
      } else if(IMavenConstants.EDITOR_HINT_MISSING_CONFIGURATOR.equals(type)) {
        configuratorIds.add(getConfiguratorId(marker));
      }
    }
    MavenDiscovery.launchWizard(packagingTypes, mojos, lifecycleIds, configuratorIds);
  }

  private String getLifecycleId(IMarker marker) {
    return marker.getAttribute(IMavenConstants.MARKER_ATTR_LIFECYCLE_PHASE, null);
  }

  private String getConfiguratorId(IMarker marker) {
    return marker.getAttribute(IMavenConstants.MARKER_ATTR_CONFIGURATOR_ID, null);
  }

  @Override
  public IMarker[] findOtherMarkers(IMarker[] markers) {
    List<IMarker> handled = new ArrayList<IMarker>();
    for(IMarker marker : markers) {
      if(MavenDiscoveryMarkerResolutionGenerator.canResolve(marker)) {
        //a way to filter out the markers with the current proposal
        try {
          IMarkerResolution[] cached = (IMarkerResolution[]) marker.getResource().getSessionProperty(
              MavenDiscoveryMarkerResolutionGenerator.QUALIFIED);
          if(cached == null || cached[0] != this) {
            handled.add(marker);
          }
        } catch(CoreException e) {
          handled.add(marker);
        }
      }
    }
    return handled.toArray(new IMarker[handled.size()]);
  }

  public String getLabel() {
    return Messages.DiscoveryWizardProposal_Label;
  }
}
