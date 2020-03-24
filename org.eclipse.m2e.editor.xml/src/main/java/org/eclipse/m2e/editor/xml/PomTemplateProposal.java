/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.sse.ui.internal.contentassist.IRelevanceCompletionProposal;

import org.eclipse.m2e.editor.pom.PomTemplate;


@SuppressWarnings("restriction")
public class PomTemplateProposal extends TemplateProposal implements IRelevanceCompletionProposal {

  public PomTemplateProposal(Template template, TemplateContext context, IRegion region, Image image, int relevance) {
    super(template, context, region, image, relevance);
  }

  @Override
  public boolean validate(IDocument document, int offset, DocumentEvent event) {
    try {
      int replaceOffset = getReplaceOffset();
      if(offset >= replaceOffset) {
        String content = document.get(replaceOffset, offset - replaceOffset);
        if(!content.isEmpty() && content.charAt(0) == '<') {
          content = content.substring(1);
        }
        return getMatchValue().toLowerCase().startsWith(content.toLowerCase());
      }
    } catch(BadLocationException e) {
      // concurrent modification - ignore
    }
    return false;
  }

  public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
    super.apply(viewer, trigger, stateMask, offset);
    if(retriggerOnApply()) {
      Display.getDefault()
          .asyncExec(() -> ((ITextOperationTarget) viewer).doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS));
    }
  }

  private boolean retriggerOnApply() {
    if(getTemplate() instanceof PomTemplate) {
      return ((PomTemplate) getTemplate()).isRetriggerOnApply();
    }
    return false;
  }

  private String getMatchValue() {
    String matchValue = null;
    if(getTemplate() instanceof PomTemplate) {
      matchValue = ((PomTemplate) getTemplate()).getMatchValue();
    }
    if(matchValue == null) {
      return getTemplate().getName();
    }
    return matchValue;
  }
}
