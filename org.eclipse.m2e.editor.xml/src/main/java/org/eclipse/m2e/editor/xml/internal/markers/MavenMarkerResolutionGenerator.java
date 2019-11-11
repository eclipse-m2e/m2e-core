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
 *      Anton Tanasenko - Refactor marker resolutions and quick fixes (Bug #484359)
 *******************************************************************************/

package org.eclipse.m2e.editor.xml.internal.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.eclipse.ui.IMarkerResolutionGenerator2;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.editor.xml.internal.lifecycle.LifecycleMappingResolution;
import org.eclipse.m2e.editor.xml.internal.lifecycle.WorkspaceLifecycleMappingResolution;


/**
 * MavenMarkerResolutionGenerator
 * 
 * @author dyocum
 */
@SuppressWarnings("restriction")
public class MavenMarkerResolutionGenerator implements IMarkerResolutionGenerator, IMarkerResolutionGenerator2 {

  /* (non-Javadoc)
   * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
   */
  public IMarkerResolution[] getResolutions(IMarker marker) {
    String hint = marker.getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, null);
    if(hint != null) {
      if(isMissingSchema(hint)) {
        return new IMarkerResolution[] {new SchemaCompletionResolution(marker)};
      }
      if(isUnneededParentVersion(hint)) {
        return new IMarkerResolution[] {new IdPartRemovalResolution(marker, true)};
      }
      if(isUnneededParentGroupId(hint)) {
        return new IMarkerResolution[] {new IdPartRemovalResolution(marker, false)};
      }
      if(isDependencyVersionOverride(hint)) {
        return new IMarkerResolution[] {new ManagedVersionRemovalResolution(marker, true),
            new IgnoreWarningResolution(marker, IMavenConstants.MARKER_IGNORE_MANAGED),
            new OpenManagedVersionDefinitionResolution(marker)};
      }
      if(isPluginVersionOverride(hint)) {
        return new IMarkerResolution[] {new ManagedVersionRemovalResolution(marker, false),
            new IgnoreWarningResolution(marker, IMavenConstants.MARKER_IGNORE_MANAGED),
            new OpenManagedVersionDefinitionResolution(marker)};
      }
      if(hint.equals(IMavenConstants.EDITOR_HINT_NOT_COVERED_MOJO_EXECUTION)) {
        return new IMarkerResolution[] {new LifecycleMappingResolution(marker, PluginExecutionAction.ignore),
            new WorkspaceLifecycleMappingResolution(marker, PluginExecutionAction.ignore),};
      }
      if(hint.equals(IMavenConstants.EDITOR_HINT_MISSING_CONFIGURATOR)) {
        return new IMarkerResolution[] {new LifecycleMappingResolution(marker, PluginExecutionAction.ignore),
            new WorkspaceLifecycleMappingResolution(marker, PluginExecutionAction.ignore)};
      }
      if(marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR) == IMarker.SEVERITY_ERROR
          && hint.equals(IMavenConstants.EDITOR_HINT_IMPLICIT_LIFECYCLEMAPPING)) {
        return new IMarkerResolution[] {new LifecycleMappingResolution(marker, PluginExecutionAction.ignore),
            new WorkspaceLifecycleMappingResolution(marker, PluginExecutionAction.ignore)};
      }
    }
    return new IMarkerResolution[0];
  }

  static boolean isPluginVersionOverride(String hint) {
    return hint.equals(IMavenConstants.EDITOR_HINT_MANAGED_PLUGIN_OVERRIDE);
  }

  static boolean isDependencyVersionOverride(String hint) {
    return hint.equals(IMavenConstants.EDITOR_HINT_MANAGED_DEPENDENCY_OVERRIDE);
  }

  static boolean isUnneededParentGroupId(String hint) {
    return IMavenConstants.EDITOR_HINT_PARENT_GROUP_ID.equals(hint);
  }

  static boolean isUnneededParentVersion(String hint) {
    return IMavenConstants.EDITOR_HINT_PARENT_VERSION.equals(hint);
  }

  static boolean isMissingSchema(String hint) {
    return IMavenConstants.EDITOR_HINT_MISSING_SCHEMA.equals(hint);
  }

  public boolean hasResolutions(IMarker marker) {
    String hint = marker.getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, null);
    return !(hint == null);
  }
}
