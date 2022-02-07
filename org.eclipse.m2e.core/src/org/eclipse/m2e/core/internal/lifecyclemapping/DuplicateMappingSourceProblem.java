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

package org.eclipse.m2e.core.internal.lifecyclemapping;

import java.util.Arrays;
import java.util.Objects;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.markers.MavenProblemInfo;
import org.eclipse.m2e.core.internal.markers.SourceLocation;


/**
 * DuplicateMappingSourceProblem
 *
 * @author christoph
 */
public class DuplicateMappingSourceProblem extends MavenProblemInfo {

  private LifecycleMappingMetadataSource[] conflictingSources;

  private String type;

  private String value;

  /**
   * @param location
   * @param message
   * @param error
   */
  public DuplicateMappingSourceProblem(SourceLocation location, String message, String type, String value,
      DuplicateMappingException error) {
    super(location, message, error);
    this.type = type;
    this.value = value;
    this.conflictingSources = error.getConflictingSources();
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.internal.markers.MavenProblemInfo#processMarker(org.eclipse.core.resources.IMarker)
   */
  @Override
  public void processMarker(IMarker marker) throws CoreException {
    super.processMarker(marker);
    marker.setAttribute(IMavenConstants.MARKER_DUPLICATEMAPPING_TYPE, type);
    marker.setAttribute(IMavenConstants.MARKER_DUPLICATEMAPPING_VALUE, value);

    Bundle[] bundles = Arrays.stream(conflictingSources).map(LifecycleMappingMetadataSource::getSource)
        .filter(Bundle.class::isInstance).map(Bundle.class::cast).toArray(Bundle[]::new);
    marker.setAttribute(IMavenConstants.MARKER_DUPLICATEMAPPING_SOURCES, bundles.length);
    for(int i = 0; i < bundles.length; i++ ) {
      Bundle bundle = bundles[i];
      String prefix = IMavenConstants.MARKER_DUPLICATEMAPPING_SOURCES + "." + i + ".";
      Version version = bundle.getVersion();
      String v = version.getMajor() + "." + version.getMinor() + "." + version.getMicro();
      marker.setAttribute(prefix + IMavenConstants.MARKER_DUPLICATEMAPPING_SOURCES_NAME,
          Objects.requireNonNullElse(bundle.getHeaders().get(Constants.BUNDLE_NAME), bundle.getSymbolicName()));
      marker.setAttribute(prefix + IMavenConstants.MARKER_DUPLICATEMAPPING_SOURCES_BSN, bundle.getSymbolicName());
      marker.setAttribute(prefix + IMavenConstants.MARKER_DUPLICATEMAPPING_SOURCES_VERSION, v);
    }
    marker.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT,
        IMavenConstants.EDITOR_HINT_CONFLICTING_LIFECYCLEMAPPING);
  }

}
