package org.eclipse.m2e.editor.xml.internal.lifecycle;

import java.io.IOException;

import org.w3c.dom.Element;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.editor.xml.internal.Messages;
import org.eclipse.m2e.editor.xml.internal.PomEdits;
import org.eclipse.m2e.editor.xml.internal.XmlUtils;

public class LifecycleMappingProposal implements ICompletionProposal, ICompletionProposalExtension5, IMarkerResolution {

  public static final String EXECUTE = "execute";

  public static final String IGNORE = "ignore";
  
  private IQuickAssistInvocationContext context;
  private final IMarker marker;
  private final String action;
  
  public LifecycleMappingProposal(IQuickAssistInvocationContext context, MarkerAnnotation mark, String action) {
    this.context = context;
    marker = mark.getMarker();
    this.action = action;
  }
  
  public LifecycleMappingProposal(IMarker marker, String action) {
    this.marker = marker;
    this.action = action;
  }
  
  public void apply(final IDocument doc) {
    try {
      PomEdits.performOnDOMDocument(new PomEdits.OperationTuple(doc, createOperation()));
    } catch(IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch(CoreException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  private LifecycleMappingOperation createOperation() {
    String pluginGroupId = marker.getAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, "");
    String pluginArtifactId = marker.getAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, "");
    String pluginVersion = marker.getAttribute(IMavenConstants.MARKER_ATTR_VERSION, "");
    String[] goals = new String[] { marker.getAttribute(IMavenConstants.MARKER_ATTR_GOAL, "")};
    return new LifecycleMappingOperation(pluginGroupId, pluginArtifactId, pluginVersion, action, goals);
  }


  public String getAdditionalProposalInfo() {
    return null;
  }

  public IContextInformation getContextInformation() {
    return null;
  }

  public String getDisplayString() {
    return IGNORE.equals(action) ? "Ignore TODO" : "Execute TODO";
  }

  public Image getImage() {
    return IGNORE.equals(action) ? WorkbenchPlugin.getDefault().getImageRegistry().get(org.eclipse.ui.internal.SharedImages.IMG_TOOL_DELETE)
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
    final IDocument doc = context.getSourceViewer().getDocument();
    //oh, how do I miss scala here..
    final String[] toRet = new String[1];
//    XmlUtils.performOnRootElement(doc, new NodeOperation<Element>() {
//      public void process(Element root) {
//        //now check parent version and groupid against the current project's ones..
//        if (root.getNodeName().equals(PomQuickAssistProcessor.PROJECT_NODE)) { //$NON-NLS-1$
//          Element value = XmlUtils.findChildElement(root, isVersion ? VERSION_NODE : GROUP_ID_NODE); //$NON-NLS-1$ //$NON-NLS-2$
//          toRet[0] = previewForRemovedElement(doc, value);
//      }
//    }});
    if (toRet[0] != null) {
      return toRet[0];
    }
     
    return "TODO";
  }

  public String getLabel() {
    return getDisplayString();
  }

  public void run(final IMarker marker) {
    try {
      PomEdits.performOnDOMDocument(new PomEdits.OperationTuple((IFile) marker.getResource(), createOperation()));
    } catch(IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch(CoreException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
  } 
}