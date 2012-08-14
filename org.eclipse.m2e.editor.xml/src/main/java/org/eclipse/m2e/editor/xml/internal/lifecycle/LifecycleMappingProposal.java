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
import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.ui.internal.editing.LifecycleMappingOperation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.CompoundOperation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;
import org.eclipse.m2e.editor.xml.internal.Messages;


public class LifecycleMappingProposal extends AbstractLifecycleMappingProposal implements ICompletionProposal,
    ICompletionProposalExtension5 {
  private static final Logger log = LoggerFactory.getLogger(LifecycleMappingProposal.class);

  private IQuickAssistInvocationContext context;
  
  public LifecycleMappingProposal(IQuickAssistInvocationContext context, MarkerAnnotation mark,
      PluginExecutionAction action) {
    super(mark.getMarker(), action);
    this.context = context;
  }
  
  public LifecycleMappingProposal(IMarker marker, PluginExecutionAction action) {
    super(marker, action);
  }
  
  public void apply(final IDocument doc) {
    try {
      if(PluginExecutionAction.ignore.equals(action)) {
        performIgnore(new IMarker[] {marker});
      } else {
        performOnDOMDocument(new OperationTuple(doc, createOperation(marker)));
      }
    } catch(IOException e) {
      log.error("Error generating code in pom.xml", e); //$NON-NLS-1$
    } catch(CoreException e) {
      log.error(e.getMessage(), e);
    }
  }

  private void performIgnore(IMarker[] marks) throws IOException, CoreException {
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
      List<LifecycleMappingOperation> lst = new ArrayList<LifecycleMappingOperation>();
      for (IMarker m : marks) {
        lst.add(createOperation(m));
      }
      performOnDOMDocument(new OperationTuple(pomFile[0], new CompoundOperation(lst.toArray(new Operation[0]))));
    }
  }

  private LifecycleMappingOperation createOperation(IMarker mark) {
    String pluginGroupId = mark.getAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, ""); //$NON-NLS-1$
    String pluginArtifactId = mark.getAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, ""); //$NON-NLS-1$
    String pluginVersion = mark.getAttribute(IMavenConstants.MARKER_ATTR_VERSION, ""); //$NON-NLS-1$
    String[] goals = new String[] { mark.getAttribute(IMavenConstants.MARKER_ATTR_GOAL, "")}; //$NON-NLS-1$
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

  public Point getSelection(IDocument document) {
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

  @Override
  public void run(IMarker[] markers, IProgressMonitor monitor) {
    try {
      if(PluginExecutionAction.ignore.equals(action)) {
        performIgnore(markers);
      } else {
        List<LifecycleMappingOperation> lst = new ArrayList<LifecycleMappingOperation>();
        for (IMarker m : markers) {
          lst.add(createOperation(m));
        }
        performOnDOMDocument(new OperationTuple((IFile) marker.getResource(), new CompoundOperation(lst.toArray(new Operation[0]))));
      }
    } catch(IOException e) {
      log.error("Error generating code in pom.xml", e); //$NON-NLS-1$
    } catch(CoreException e) {
      log.error(e.getMessage(), e);
    }
  }
  
  
}