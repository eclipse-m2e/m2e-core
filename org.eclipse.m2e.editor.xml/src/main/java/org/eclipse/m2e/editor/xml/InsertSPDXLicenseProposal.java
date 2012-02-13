/*******************************************************************************
 * Copyright (c) 2012 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.performOnDOMDocument;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;
import org.eclipse.m2e.editor.xml.internal.Messages;
import org.eclipse.m2e.editor.xml.internal.XmlUtils;
import org.eclipse.m2e.editor.xml.internal.dialogs.SPDXLicense;
import org.eclipse.m2e.editor.xml.internal.dialogs.SelectSPDXLicenseDialog;


public class InsertSPDXLicenseProposal implements ICompletionProposal {
  private static final Logger log = LoggerFactory.getLogger(InsertSPDXLicenseProposal.class);

  private final ISourceViewer sourceViewer;

  private final Region region;

  private final PomTemplateContext context;

  private Point selection;

  public InsertSPDXLicenseProposal(ISourceViewer sourceViewer, PomTemplateContext context, Region region) {
    this.sourceViewer = sourceViewer;
    this.context = context;
    this.region = region;
  }

  public void apply(IDocument document) {
    IProject project = XmlUtils.extractProject(sourceViewer);
    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);
    SelectSPDXLicenseDialog dialog = new SelectSPDXLicenseDialog(sourceViewer.getTextWidget().getShell(), facade);

    if(dialog.open() == Window.OK) {
      final SPDXLicense license = dialog.getLicense();
      try {

        IMavenProjectFacade targetProject = dialog.getTargetProject();
        if(!targetProject.getPom().equals(facade.getPom())) {
          // add license to a parent
          AddLicensePomOperation operation = new AddLicensePomOperation(license, PomTemplateContext.PROJECT, null);
          performOnDOMDocument(new OperationTuple(targetProject.getPom(), operation));
        } else {
          AddLicensePomOperation operation = new AddLicensePomOperation(license, context, region);
          performOnDOMDocument(new OperationTuple(document, operation));
          this.selection = operation.getSelection();
        }

      } catch(CoreException e) {
        log.error("Failed inserting parent element", e); //$NON-NLS-1$
      } catch(IOException e) {
        log.error("Failed inserting parent element", e); //$NON-NLS-1$
      }
    }
  }

  public Point getSelection(IDocument document) {
    return selection;
  }

  public String getAdditionalProposalInfo() {
    return null;
  }

  public String getDisplayString() {
    return Messages.InsertSPDXLicenseProposal_0;
  }

  public Image getImage() {
    return MvnImages.IMG_LICENSE;
  }

  public IContextInformation getContextInformation() {
    return null;
  }

}
