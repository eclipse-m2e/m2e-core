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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.ide.IDE;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.editor.xml.internal.Messages;

/**
 * MavenMarkerResolution
 * TODO mkleint: this class shall be eventually merged with the class doing the same in POMQuickAssistProcessor
 * @author dyocum
 */
public class XMLSchemaMarkerResolution implements IMarkerResolution {

  /* (non-Javadoc)
   * @see org.eclipse.ui.IMarkerResolution#getLabel()
   */
  public String getLabel() {
    return Messages.MavenMarkerResolution_schema_label;
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
   */
  public void run(final IMarker marker) {
    if(marker.getResource().getType() == IResource.FILE){
      try {
        IDOMModel domModel = (IDOMModel)StructuredModelManager.getModelManager().getModelForEdit((IFile)marker.getResource());
        int offset = ((Integer)marker.getAttribute("offset")); //$NON-NLS-1$
        IStructuredDocumentRegion regionAtCharacterOffset = domModel.getStructuredDocument().getRegionAtCharacterOffset(offset);
        if(regionAtCharacterOffset != null && regionAtCharacterOffset.getText() != null &&
            regionAtCharacterOffset.getText().lastIndexOf("<project") >=0){ //$NON-NLS-1$
          //in case there are unsaved changes, find the current offset of the <project> node before inserting
          offset = regionAtCharacterOffset.getStartOffset();
          IDE.openEditor(MvnIndexPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage(), (IFile)marker.getResource());
          InsertEdit edit = new InsertEdit(offset+8, PomQuickAssistProcessor.XSI_VALUE);
          try {
            edit.apply(domModel.getStructuredDocument());
            IEditorPart activeEditor = MvnIndexPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            MvnIndexPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().saveEditor(activeEditor, false);
          } catch(Exception e){
            MavenLogger.log("Unable to insert schema info", e); //$NON-NLS-1$
          }
        } else {
          String msg = Messages.MavenMarkerResolution_error;
          MessageDialog.openError(MvnIndexPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.MavenMarkerResolution_error_title, msg);
        }
      } catch(Exception e) {
        MavenLogger.log("Unable to run quick fix for maven marker", e); //$NON-NLS-1$
      }
    }
  }

}
