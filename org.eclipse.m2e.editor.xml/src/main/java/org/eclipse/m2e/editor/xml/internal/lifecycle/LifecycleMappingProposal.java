/*******************************************************************************
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/


package org.eclipse.m2e.editor.xml.internal.lifecycle;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.performOnDOMDocument;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;
import org.eclipse.m2e.editor.xml.internal.Messages;

public class LifecycleMappingProposal implements ICompletionProposal, ICompletionProposalExtension5, IMarkerResolution {
  private static final Logger log = LoggerFactory.getLogger(LifecycleMappingProposal.class);

  private IQuickAssistInvocationContext context;
  private final IMarker marker;

  private final PluginExecutionAction action;
  
  public LifecycleMappingProposal(IQuickAssistInvocationContext context, MarkerAnnotation mark,
      PluginExecutionAction action) {
    this.context = context;
    marker = mark.getMarker();
    this.action = action;
  }
  
  public LifecycleMappingProposal(IMarker marker, PluginExecutionAction action) {
    this.marker = marker;
    this.action = action;
  }
  
  public void apply(final IDocument doc) {
    try {
      if(PluginExecutionAction.ignore.equals(action)) {
        performIgnore();
      } else {
        performOnDOMDocument(new OperationTuple(doc, createOperation()));
      }
    } catch(IOException e) {
      log.error("Error generating code in pom.xml", e); //$NON-NLS-1$
    } catch(CoreException e) {
      log.error(e.getMessage(), e);
    }
  }

  private void performIgnore() throws IOException, CoreException {
    final IFile[] pomFile = new IFile[1];
    PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

      public void run() {
        LifecycleMappingDialog dialog = new LifecycleMappingDialog(Display.getCurrent().getActiveShell(),
            (IFile) marker.getResource(), marker.getAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, ""), marker
                .getAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, ""), marker.getAttribute(
                IMavenConstants.MARKER_ATTR_VERSION, ""), marker.getAttribute(IMavenConstants.MARKER_ATTR_GOAL, ""));
        dialog.setBlockOnOpen(true);
        if(dialog.open() == Window.OK) {
          pomFile[0] = dialog.getPomFile();
        }
      }
    });
    if(pomFile[0] != null) {
      performOnDOMDocument(new OperationTuple(pomFile[0], createOperation()));
    }
  }

  private LifecycleMappingOperation createOperation() {
    String pluginGroupId = marker.getAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, ""); //$NON-NLS-1$
    String pluginArtifactId = marker.getAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, ""); //$NON-NLS-1$
    String pluginVersion = marker.getAttribute(IMavenConstants.MARKER_ATTR_VERSION, ""); //$NON-NLS-1$
    String[] goals = new String[] { marker.getAttribute(IMavenConstants.MARKER_ATTR_GOAL, "")}; //$NON-NLS-1$
    return new LifecycleMappingOperation(pluginGroupId, pluginArtifactId, pluginVersion, action, goals);
  }


  public String getAdditionalProposalInfo() {
    return null;
  }

  public IContextInformation getContextInformation() {
    return null;
  }

  public String getDisplayString() {
    String goal = marker.getAttribute(IMavenConstants.MARKER_ATTR_GOAL, ""); //$NON-NLS-1$
    return PluginExecutionAction.ignore.equals(action) ? NLS.bind(Messages.LifecycleMappingProposal_ignore_label, goal)
        : NLS.bind(Messages.LifecycleMappingProposal_execute_label, goal);
  }

  public Image getImage() {
    return PluginExecutionAction.ignore.equals(action) ? PlatformUI.getWorkbench().getSharedImages()
        .getImage(org.eclipse.ui.ISharedImages.IMG_TOOL_DELETE) : PlatformUI.getWorkbench().getSharedImages()
        .getImage(org.eclipse.ui.ISharedImages.IMG_TOOL_FORWARD);
  }

  public Point getSelection(IDocument arg0) {
    return null;
  }

  public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
    if (context == null) {
      //no context in markerresolution, just to be sure..
      return null;
    }
    String pluginGroupId = marker.getAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, ""); //$NON-NLS-1$
    String pluginArtifactId = marker.getAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, ""); //$NON-NLS-1$
    String pluginVersion = marker.getAttribute(IMavenConstants.MARKER_ATTR_VERSION, ""); //$NON-NLS-1$
    String goal = marker.getAttribute(IMavenConstants.MARKER_ATTR_GOAL, ""); //$NON-NLS-1$
    String execution = marker.getAttribute(IMavenConstants.MARKER_ATTR_EXECUTION_ID, "-"); //$NON-NLS-1$
    String phase = marker.getAttribute(IMavenConstants.MARKER_ATTR_LIFECYCLE_PHASE, "-"); //$NON-NLS-1$
    String info = NLS.bind(Messages.LifecycleMappingProposal_all_desc, 
        new Object[] {goal, execution, phase, pluginGroupId + ":" + pluginArtifactId + ":" + pluginVersion,  //$NON-NLS-1$ //$NON-NLS-2$
        (PluginExecutionAction.ignore.equals(action)
            ? Messages.LifecycleMappingProposal_ignore_desc 
            : Messages.LifecycleMappingProposal_execute_desc)});
    
    return info;
  }

  public String getLabel() {
    return getDisplayString();
  }

  public void run(final IMarker marker) {
    try {
      if(PluginExecutionAction.ignore.equals(action)) {
        performIgnore();
      } else {
        performOnDOMDocument(new OperationTuple((IFile) marker.getResource(), createOperation()));
      }
    } catch(IOException e) {
      log.error("Error generating code in pom.xml", e); //$NON-NLS-1$
    } catch(CoreException e) {
      log.error(e.getMessage(), e);
    }
  }
}