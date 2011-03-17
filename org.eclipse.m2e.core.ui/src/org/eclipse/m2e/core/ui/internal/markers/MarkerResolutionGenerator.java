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

package org.eclipse.m2e.core.ui.internal.markers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.UpdateConfigurationJob;

public class MarkerResolutionGenerator implements IMarkerResolutionGenerator, IMarkerResolutionGenerator2 {

  private static final Logger LOG = LoggerFactory.getLogger(MarkerResolutionGenerator.class);
  
  public boolean hasResolutions(IMarker marker) {
    // TODO is the resolution for all configuration markers??
    return true;
  }

  public IMarkerResolution[] getResolutions(IMarker marker) {
    // TODO is the resolution for all configuration markers??
    return new IMarkerResolution[] { new RefreshResolution() };
  }
  
  private class RefreshResolution extends WorkbenchMarkerResolution {


    /* (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolution2#getDescription()
     */
    public String getDescription() {
      return Messages.MarkerResolutionGenerator_desc;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolution2#getImage()
     */
    public Image getImage() {
      // TODO Auto-generated method getImage
      return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolution#getLabel()
     */
    public String getLabel() {
      return Messages.MarkerResolutionGenerator_label;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
     */
    public void run(IMarker marker) {
      final Set<IProject> projects = getProjects(marker);
      new UpdateConfigurationJob(MavenPlugin.getDefault(), projects.toArray(new IProject[projects.size()])).schedule();
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.views.markers.WorkbenchMarkerResolution#run(org.eclipse.core.resources.IMarker[], org.eclipse.core.runtime.IProgressMonitor)
     */
    public void run(IMarker[] markers, IProgressMonitor monitor) {
      final Set<IProject> projects = getProjects(markers);
      new UpdateConfigurationJob(MavenPlugin.getDefault(), projects.toArray(new IProject[projects.size()])).schedule();
    }
    

    /**
     * @param markers
     * @return
     */
    private Set<IProject> getProjects(IMarker... markers) {
      Set<IProject> toRet = new HashSet<IProject>();
      for (IMarker mark : markers) {
        IResource res = mark.getResource();
        IProject prj = res.getProject();
        if (prj != null) {
          toRet.add(prj);
        }
      }
      return toRet;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.views.markers.WorkbenchMarkerResolution#findOtherMarkers(org.eclipse.core.resources.IMarker[])
     */
    public IMarker[] findOtherMarkers(IMarker[] markers) {
      List<IMarker> toRet = new ArrayList<IMarker>();
      for (IMarker m : markers) {
        try {
          if (IMavenConstants.MARKER_ATTR_CONFIGURATOR_ID.equals(m.getType())) {
            //TODO is this the only condition
            toRet.add(m);
          }
        } catch(CoreException ex) {
          LOG.error(ex.getMessage(), ex);
        }
      }
      return toRet.toArray(new IMarker[0]);
    }

    
  }

}
