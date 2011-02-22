/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecycle;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.markers.MarkerLocation;
import org.eclipse.m2e.core.internal.markers.MavenProblemInfo;


public class MissingLifecyclePackaging extends MavenProblemInfo {
  private final String packaging;

  MissingLifecyclePackaging(String packaging, MarkerLocation markerLocation) {
    super(NLS.bind(Messages.LifecycleMissing, packaging), markerLocation);
    this.packaging = packaging;
  }

  public String getPackaging() {
    return packaging;
  }

  @Override
  public void processMarker(IMarker marker) throws CoreException {
    super.processMarker(marker);
    marker.setAttribute(IMavenConstants.MARKER_ATTR_PACKAGING, getPackaging());
    marker.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, IMavenConstants.EDITOR_HINT_UNKNOWN_PACKAGING);
  }
}
