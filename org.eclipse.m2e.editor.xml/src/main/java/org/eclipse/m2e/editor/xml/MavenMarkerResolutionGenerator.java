/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.eclipse.ui.IMarkerResolutionGenerator2;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.editor.xml.internal.lifecycle.LifecycleMappingProposal;


/**
 * MavenMarkerResolutionGenerator
 * 
 * @author dyocum
 */
public class MavenMarkerResolutionGenerator implements IMarkerResolutionGenerator, IMarkerResolutionGenerator2 {

  /* (non-Javadoc)
   * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
   */
  public IMarkerResolution[] getResolutions(IMarker marker) {
    String hint = marker.getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, null);
    if(hint != null) {
      //only provide a quickfix for the schema marker
      if(IMavenConstants.EDITOR_HINT_MISSING_SCHEMA.equals(hint)) {
        return new IMarkerResolution[] {new XMLSchemaMarkerResolution()};
      }
      if(IMavenConstants.EDITOR_HINT_PARENT_VERSION.equals(hint)) {
        return new IMarkerResolution[] {new PomQuickAssistProcessor.IdPartRemovalProposal(marker, true)};
      }
      if(IMavenConstants.EDITOR_HINT_PARENT_GROUP_ID.equals(hint)) {
        return new IMarkerResolution[] {new PomQuickAssistProcessor.IdPartRemovalProposal(marker, false)};
      }
      if(hint.equals(IMavenConstants.EDITOR_HINT_MANAGED_DEPENDENCY_OVERRIDE)) {
        return new IMarkerResolution[] {new PomQuickAssistProcessor.ManagedVersionRemovalProposal(marker, true),
            new PomQuickAssistProcessor.IgnoreWarningProposal(marker, IMavenConstants.MARKER_IGNORE_MANAGED)};
      }
      if(hint.equals(IMavenConstants.EDITOR_HINT_MANAGED_PLUGIN_OVERRIDE)) {
        return new IMarkerResolution[] {new PomQuickAssistProcessor.ManagedVersionRemovalProposal(marker, false),
            new PomQuickAssistProcessor.IgnoreWarningProposal(marker, IMavenConstants.MARKER_IGNORE_MANAGED)};
      }
      if(hint.equals(IMavenConstants.EDITOR_HINT_NOT_COVERED_MOJO_EXECUTION)) {
        return new IMarkerResolution[] {
            new LifecycleMappingProposal(marker, PluginExecutionAction.ignore),
//            new LifecycleMappingProposal(marker, PluginExecutionAction.execute)
        };
      }
      if(marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR) == IMarker.SEVERITY_ERROR
          && hint.equals(IMavenConstants.EDITOR_HINT_IMPLICIT_LIFECYCLEMAPPING)) {
        return new IMarkerResolution[] {new LifecycleMappingProposal(marker, PluginExecutionAction.ignore)};
      }
    }
    return new IMarkerResolution[0];
  }

  public boolean hasResolutions(IMarker marker) {
    String hint = marker.getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, null); //$NON-NLS-1$
    return !(hint == null);
  }
}
