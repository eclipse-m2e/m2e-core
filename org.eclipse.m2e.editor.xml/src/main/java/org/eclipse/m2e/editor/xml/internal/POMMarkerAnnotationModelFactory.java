package org.eclipse.m2e.editor.xml.internal;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModelFactory;

/**
 * created this file to get the proper lightbulb icon for the warnings with hint
 * is almost exact copy of the wst one..
 * @author mkleint
 */
public class POMMarkerAnnotationModelFactory extends ResourceMarkerAnnotationModelFactory {
  public POMMarkerAnnotationModelFactory() {
    super();
  }

  /*
   * @see org.eclipse.core.filebuffers.IAnnotationModelFactory#createAnnotationModel(org.eclipse.core.runtime.IPath)
   */
  public IAnnotationModel createAnnotationModel(IPath location) {
    IAnnotationModel model = null;
    IFile file = FileBuffers.getWorkspaceFileAtLocation(location);
    if (file != null) {
      model = new POMMarkerAnnotationModel(file);
    }
    else {
      model = new POMMarkerAnnotationModel(ResourcesPlugin.getWorkspace().getRoot(), location.toString());
    }
    return model;
  }
}
