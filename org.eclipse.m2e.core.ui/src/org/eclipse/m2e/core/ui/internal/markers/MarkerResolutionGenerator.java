/*******************************************************************************
 * Copyright (c) 2011-2015 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Anton Tanasenko - Refactor marker resolutions and quick fixes (Bug #484359)
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.markers;

import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.eclipse.ui.IMarkerResolutionGenerator2;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.UpdateMavenProjectJob;


@SuppressWarnings("restriction")
public class MarkerResolutionGenerator implements IMarkerResolutionGenerator, IMarkerResolutionGenerator2 {

    @Override
    public boolean hasResolutions(IMarker marker) {
    return true;
  }

  @Override
  public IMarkerResolution[] getResolutions(IMarker marker) {
    return new IMarkerResolution[] {new RefreshResolution(marker)};
  }

  private class RefreshResolution extends MavenProblemResolution {

    public RefreshResolution(IMarker marker) {
      super(marker);
    }

    @Override
    public String getDescription() {
      return Messages.MarkerResolutionGenerator_desc;
    }

    @Override
    public Image getImage() {
      return null;
    }

    @Override
    public String getLabel() {
      return Messages.MarkerResolutionGenerator_label;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }

    @Override
    public void fix(IMarker[] markers, IDocument doc, IProgressMonitor monitor) {
      final Set<IProject> projects = getProjects(Stream.of(markers));
      new UpdateMavenProjectJob(projects.toArray(new IProject[projects.size()])).schedule();
    }

    @Override
    public boolean canFix(IMarker marker) throws CoreException {
      return IMavenConstants.MARKER_CONFIGURATION_ID.equals(marker.getType());
    }
  }

}
