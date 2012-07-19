/*******************************************************************************
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Andrew Eisenberg - Work on Bug 350414
 *******************************************************************************/


package org.eclipse.m2e.editor.xml.internal.lifecycle;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.performOnDOMDocument;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;
import org.eclipse.wst.sse.core.internal.encoding.EncodingRule;
import org.eclipse.wst.xml.core.internal.modelhandler.XMLModelLoader;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.ui.internal.UpdateMavenProjectJob;
import org.eclipse.m2e.core.ui.internal.editing.LifecycleMappingOperation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.CompoundOperation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;
import org.eclipse.m2e.editor.xml.internal.Messages;

public class LifecycleMappingProposal extends WorkbenchMarkerResolution implements ICompletionProposal, ICompletionProposalExtension5 {
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
        performIgnore(new IMarker[] {marker});
      } else {
        performOnDOMDocument(new OperationTuple(doc, createOperation(marker, false)));
      }
    } catch(IOException e) {
      log.error("Error generating code in pom.xml", e); //$NON-NLS-1$
    } catch(CoreException e) {
      log.error(e.getMessage(), e);
    }
  }

  private void performIgnore(IMarker[] marks) throws IOException, CoreException {
    final IFile[] pomFile = new IFile[1];
    final boolean[] useWorkspace = new boolean[1];
    PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
      public void run() {
        LifecycleMappingDialog dialog = new LifecycleMappingDialog(Display.getCurrent().getActiveShell(),
            (IFile) marker.getResource(), marker.getAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, ""), marker //$NON-NLS-1$
                .getAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, ""), marker.getAttribute( //$NON-NLS-1$
                IMavenConstants.MARKER_ATTR_VERSION, ""), marker.getAttribute(IMavenConstants.MARKER_ATTR_GOAL, "")); //$NON-NLS-1$ //$NON-NLS-2$
        dialog.setBlockOnOpen(true);
        if(dialog.open() == Window.OK) {
          if (dialog.useWorkspaceSettings()) {
            // store in workspace, not in pom file
            useWorkspace[0] = true;
          } else {
            pomFile[0] = dialog.getPomFile();
          }
        }
      }
    });
    
    if (pomFile[0] != null || useWorkspace[0]) {
      List<LifecycleMappingOperation> lst = new ArrayList<LifecycleMappingOperation>();
      for (IMarker m : marks) {
        lst.add(createOperation(m, useWorkspace[0]));
      }
      
      OperationTuple operationTuple;
      IDOMModel model = null;
      if (useWorkspace[0]) {
        // write to workspace preferences
        model = loadWorkspaceMappingsModel();
        operationTuple = new OperationTuple(model, new CompoundOperation(lst.toArray(new Operation[0])));
      } else {
        operationTuple = new OperationTuple(pomFile[0], new CompoundOperation(lst.toArray(new Operation[0])));
      }
      performOnDOMDocument(operationTuple);
      
      if (useWorkspace[0]) {
        // now save the workspace file if necessary
        MavenPluginActivator.getDefault().getMavenConfiguration().setWorkspaceMappings(model.getDocument().getSource());
        
        // must kick off an update project job since the pom isn't modified.
        // Only update the project from where this quick fix was executed.
        // Other projects can be updated manually
        new UpdateMavenProjectJob(new IProject[] { marker.getResource().getProject() }).schedule();
      }
    }
  }

  /**
   * Loads the workspace lifecycle mappings file as a dom model
   */
  private IDOMModel loadWorkspaceMappingsModel() throws UnsupportedEncodingException, IOException {
    IDOMModel model;
    XMLModelLoader loader = new XMLModelLoader();
    model = (IDOMModel) loader.createModel();
    loader.load(new ByteArrayInputStream(MavenPluginActivator.getDefault().getMavenConfiguration()
        .getWorkspaceMappings().getBytes()), model, EncodingRule.CONTENT_BASED);
    return model;
  }

  private LifecycleMappingOperation createOperation(IMarker mark, boolean createAtTopLevel) {
    String pluginGroupId = mark.getAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, ""); //$NON-NLS-1$
    String pluginArtifactId = mark.getAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, ""); //$NON-NLS-1$
    String pluginVersion = mark.getAttribute(IMavenConstants.MARKER_ATTR_VERSION, ""); //$NON-NLS-1$
    String[] goals = new String[] { mark.getAttribute(IMavenConstants.MARKER_ATTR_GOAL, "")}; //$NON-NLS-1$
    return new LifecycleMappingOperation(pluginGroupId, pluginArtifactId, pluginVersion, action, goals, createAtTopLevel);
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
    run(new IMarker[] {marker}, new NullProgressMonitor());
  }

  public String getDescription() {
    // TODO Auto-generated method stub
    return getDisplayString();
  }

  @Override
  public IMarker[] findOtherMarkers(IMarker[] markers) {
    List<IMarker> toRet = new ArrayList<IMarker>();
    
    for (IMarker mark : markers) {
        if (mark == this.marker) {
          continue;
        }
        try {
          if (mark.getType().equals(this.marker.getType()) && mark.getResource().equals(this.marker.getResource())) {
            toRet.add(mark);
          }
        } catch(CoreException e) {
          log.error(e.getMessage(), e);
        }
    }
    return toRet.toArray(new IMarker[0]);
    
  }

  @Override
  public void run(IMarker[] markers, IProgressMonitor monitor) {
    try {
      if(PluginExecutionAction.ignore.equals(action)) {
        performIgnore(markers);
      } else {
        List<LifecycleMappingOperation> lst = new ArrayList<LifecycleMappingOperation>();
        for (IMarker m : markers) {
          lst.add(createOperation(m, false));
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