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

package org.eclipse.m2e.editor.internal.lifecycle;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.performOnDOMDocument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.ui.internal.editing.LifecycleMappingOperation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.CompoundOperation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;
import org.eclipse.m2e.editor.internal.Messages;


@SuppressWarnings("restriction")
public class LifecycleMappingResolution extends AbstractLifecycleMappingResolution {

  public LifecycleMappingResolution(IMarker marker, PluginExecutionAction action) {
    super(marker, action);
  }

  @Override
  public int getOrder() {
    return 50;
  }

  @Override
  protected void fix(IDocument document, List<IMarker> markers, IProgressMonitor monitor) {
    try {
      if(PluginExecutionAction.ignore.equals(action)) {
        performIgnore(markers);
      } else {
        performOnDOMDocument(new OperationTuple(document, createOperation(markers)));
      }
    } catch(IOException e) {
      LOG.error("Error generating code in pom.xml", e); //$NON-NLS-1$
    } catch(CoreException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  @Override
  protected void fix(IResource resource, List<IMarker> markers, IProgressMonitor monitor) {
    try {
      if(PluginExecutionAction.ignore.equals(action)) {
        performIgnore(markers);
      } else {
        performOnDOMDocument(new OperationTuple((IFile) resource, createOperation(markers)));
      }
    } catch(IOException e) {
      LOG.error("Error generating code in pom.xml", e); //$NON-NLS-1$
    } catch(CoreException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  private void performIgnore(List<IMarker> markers) throws IOException, CoreException {
    final IFile[] pomFile = new IFile[1];
    PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
      LifecycleMappingDialog dialog = new LifecycleMappingDialog(Display.getCurrent().getActiveShell(),
          (IFile) getMarker().getResource(), getMarker().getAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, ""),
          getMarker().getAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, ""),
          getMarker().getAttribute(IMavenConstants.MARKER_ATTR_GOAL, ""));
      dialog.setBlockOnOpen(true);
      if(dialog.open() == Window.OK) {
        pomFile[0] = dialog.getPomFile();
      }
    });
    if(pomFile[0] != null) {
      performOnDOMDocument(new OperationTuple(pomFile[0], createOperation(markers)));
    }
  }

  private Operation createOperation(List<IMarker> markers) {
    List<LifecycleMappingOperation> lst = new ArrayList<>();
    for(IMarker m : markers) {
      String pluginGroupId = m.getAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, ""); //$NON-NLS-1$
      String pluginArtifactId = m.getAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, ""); //$NON-NLS-1$
      String pluginVersion = m.getAttribute(IMavenConstants.MARKER_ATTR_VERSION, ""); //$NON-NLS-1$
      String[] goals = new String[] {m.getAttribute(IMavenConstants.MARKER_ATTR_GOAL, "")}; //$NON-NLS-1$
      lst.add(new LifecycleMappingOperation(pluginGroupId, pluginArtifactId, pluginVersion, action, goals));
    }
    return new CompoundOperation(lst.toArray(new Operation[lst.size()]));
  }

  @Override
  public String getLabel() {
    String goal = getMarker().getAttribute(IMavenConstants.MARKER_ATTR_GOAL, ""); //$NON-NLS-1$
    return PluginExecutionAction.ignore.equals(action) ? NLS.bind(Messages.LifecycleMappingProposal_ignore_label, goal)
        : NLS.bind(Messages.LifecycleMappingProposal_execute_label, goal);
  }

  @Override
  public Point getSelection(IDocument document) {
    return null;
  }

  @Override
  public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
    if(getQuickAssistContext() == null) {
      //no context in markerresolution, just to be sure..
      return null;
    }
    String pluginGroupId = getMarker().getAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, ""); //$NON-NLS-1$
    String pluginArtifactId = getMarker().getAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, ""); //$NON-NLS-1$
    String pluginVersion = getMarker().getAttribute(IMavenConstants.MARKER_ATTR_VERSION, ""); //$NON-NLS-1$
    String goal = getMarker().getAttribute(IMavenConstants.MARKER_ATTR_GOAL, ""); //$NON-NLS-1$
    String execution = getMarker().getAttribute(IMavenConstants.MARKER_ATTR_EXECUTION_ID, "-"); //$NON-NLS-1$
    String phase = getMarker().getAttribute(IMavenConstants.MARKER_ATTR_LIFECYCLE_PHASE, "-"); //$NON-NLS-1$
    return NLS.bind(Messages.LifecycleMappingProposal_all_desc,
        new Object[] {goal, execution, phase, pluginGroupId + ":" + pluginArtifactId + ":" + pluginVersion, //$NON-NLS-1$ //$NON-NLS-2$
            (PluginExecutionAction.ignore.equals(action) ? Messages.LifecycleMappingProposal_ignore_desc
                : Messages.LifecycleMappingProposal_execute_desc)});
  }

}
