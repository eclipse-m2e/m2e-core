package org.eclipse.m2e.editor.xml.internal.lifecycle;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionAction;
import org.eclipse.m2e.editor.xml.internal.Messages;
import org.eclipse.m2e.editor.xml.internal.PomEdits;

public class LifecycleMappingProposal implements ICompletionProposal, ICompletionProposalExtension5, IMarkerResolution {
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
      PomEdits.performOnDOMDocument(new PomEdits.OperationTuple(doc, createOperation()));
      marker.delete();
    } catch(IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch(CoreException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
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
    return PluginExecutionAction.ignore.equals(action) ? WorkbenchPlugin.getDefault().getImageRegistry()
        .get(org.eclipse.ui.internal.SharedImages.IMG_TOOL_DELETE)
        : WorkbenchPlugin.getDefault().getImageRegistry().get(org.eclipse.ui.internal.SharedImages.IMG_TOOL_FORWARD);
  }

  public Point getSelection(IDocument arg0) {
    return null;
  }

  public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
    if (context == null) {
      //no context in markerresolution, just to be sure..
      return null;
    }
    return PluginExecutionAction.ignore.equals(action)
      ? Messages.LifecycleMappingProposal_ignore_desc 
      : Messages.LifecycleMappingProposal_execute_desc;
  }

  public String getLabel() {
    return getDisplayString();
  }

  public void run(final IMarker marker) {
    try {
      PomEdits.performOnDOMDocument(new PomEdits.OperationTuple((IFile) marker.getResource(), createOperation()));
      marker.delete();
    } catch(IOException e) {
      MavenLogger.log("Error generating code in pom.xml", e); //$NON-NLS-1$
    } catch(CoreException e) {
      MavenLogger.log(e);
    }
    
  } 
}