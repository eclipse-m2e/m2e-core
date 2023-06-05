/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.jdt.ui.internal;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCIES;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.performOnDOMDocument;

import java.io.IOException;

import org.w3c.dom.Element;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;
import org.eclipse.m2e.core.ui.internal.editing.PomHelper;


@SuppressWarnings("restriction")
public class AddDependencyJavaCompletionProposal implements IJavaCompletionProposal {

  private ArtifactKey artifactKey;

  private IFile pomfile;

  private int relevance;

  public AddDependencyJavaCompletionProposal(ArtifactKey artifactKey, IFile pomfile, int relevance) {
    this.artifactKey = artifactKey;
    this.pomfile = pomfile;
    this.relevance = relevance;
  }

  public void apply(IDocument javaDocument) {
    try {
      performOnDOMDocument(new OperationTuple(pomfile, (Operation) document -> {
        Element depsEl = getChild(document.getDocumentElement(), DEPENDENCIES);
        PomHelper.addOrUpdateDependency(depsEl, artifactKey.groupId(), artifactKey.artifactId(), artifactKey.version(),
            null, "compile", null);
      }));
    } catch(IOException ex) {
      MavenJdtUiPlugin.getDefault().getLog().error("Can't modify file " + pomfile, ex);
    } catch(CoreException ex) {
      MavenJdtUiPlugin.getDefault().getLog().log(ex.getStatus());
    }

  }

  public Point getSelection(IDocument document) {
    return null;
  }

  public String getAdditionalProposalInfo() {
    return null;
  }

  public String getDisplayString() {
    return "Add " + artifactKey.groupId() + ":" + artifactKey.artifactId() + ":" + artifactKey.version()
        + " as dependency";
  }

  public Image getImage() {
    return MavenJdtUiPlugin.getDefault().getImageRegistry().get(MavenJdtUiPlugin.M2E_ICON);
  }

  public IContextInformation getContextInformation() {
    return null;
  }

  public int getRelevance() {
    return relevance;
  }

}
