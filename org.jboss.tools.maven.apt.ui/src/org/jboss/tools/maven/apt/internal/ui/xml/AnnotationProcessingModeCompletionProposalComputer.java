/*******************************************************************************
 * Copyright (c) 2008-2016 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial implementation of PomContentAssistProcessor
 *      Fred Bricon (Red Hat, Inc.) - adapted for &lt;m2e.apt.activation&gt;
 *******************************************************************************/
package org.jboss.tools.maven.apt.internal.ui.xml;

import java.util.ArrayList;
import java.util.Collection;

import org.jboss.tools.maven.apt.preferences.AnnotationProcessingMode;
import org.jboss.tools.maven.apt.ui.preferences.PreferenceMessages;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.sse.ui.internal.contentassist.IRelevanceCompletionProposal;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
import org.eclipse.wst.xml.ui.internal.contentassist.DefaultXMLCompletionProposalComputer;

import org.eclipse.m2e.editor.xml.MvnIndexPlugin;


/**
 * &lt;m2e.apt.activation&gt; completion proposal for the pom.xml editor.
 *
 * This class is heavily inspired from m2e's <a href="http://git.eclipse.org/c/m2e/m2e-core.git/tree/org.eclipse.m2e.editor.xml/src/main/java/org/eclipse/m2e/editor/xml/PomContentAssistProcessor.java?h=releases/1.6/1.6.2.20150902-0002&id=bb1a4dd26b66d9840a36ef337661188b99f17e7b">
 * org.eclipse.m2e.editor.xml.PomContentAssistProcessor</a>
 * 
 * @author Fred Bricon
 * @author Lukas Krecan
 * @author Eugene Kuleshov
 */
@SuppressWarnings("restriction")
public class AnnotationProcessingModeCompletionProposalComputer extends DefaultXMLCompletionProposalComputer {
  
  private static final String M2E_PROPERTIES_CONTEXT_TYPE = "org.eclipse.m2e.editor.xml.templates.contextType.properties";

  @Override
  protected void addTagInsertionProposals(ContentAssistRequest contentAssistRequest, int childPosition,
      CompletionProposalInvocationContext ctx) {
    if (contentAssistRequest.getParent() == null || contentAssistRequest.getParent().getParentNode() == null) {
      return;
    }
    String containerName = contentAssistRequest.getParent().getParentNode().getNodeName();
    String propertyName = contentAssistRequest.getParent().getNodeName();
    if (!"properties".equals(containerName) && !"m2e.apt.activation".equals(propertyName)) {
      return;
    }
    int offset = contentAssistRequest.getReplacementBeginPosition();
    String prefix = extractPrefix(ctx.getViewer(), offset);
    contentAssistRequest.setMatchString(prefix);
    contentAssistRequest.setReplacementBeginPosition(offset - prefix.length());
    contentAssistRequest.setReplacementLength(prefix.length());
    addTemplateProposals(contentAssistRequest, ctx.getViewer());
  }
  
  public static final String extractPrefix(ITextViewer viewer, int offset) {
    int i = offset;
    IDocument document = viewer.getDocument();
    if(i > document.getLength()) {
      return ""; //$NON-NLS-1$
    }

    try {
      while(i > 0) {
        char ch = document.getChar(i - 1);
        if(ch == '>' || ch == '<' || ch == ' ' || ch == '\n' || ch == '\t') {
          break;
        }
        i-- ;
      }
      return document.get(i, offset - i);
    } catch(BadLocationException e) {
      return ""; //$NON-NLS-1$
    }
  }
  
  private Collection<Template> getTemplates(String prefix) {
    Collection<Template> proposals = new ArrayList<>(3);
    checkAndAdd(proposals, prefix, AnnotationProcessingMode.disabled, PreferenceMessages.AnnotationProcessingSettingsPage_Disabled_Mode_Label);
    checkAndAdd(proposals, prefix, AnnotationProcessingMode.jdt_apt, PreferenceMessages.AnnotationProcessingSettingsPage_Jdt_Apt_Mode_Label);
    checkAndAdd(proposals, prefix, AnnotationProcessingMode.maven_execution, PreferenceMessages.AnnotationProcessingSettingsPage_Maven_Execution_Mode);
    return proposals;
  }
  
  protected void checkAndAdd(Collection<Template> proposals, String prefix, AnnotationProcessingMode mode, String description) {
    String name = mode.name();
    if(name.startsWith(prefix)) {
      proposals.add(new Template(name, description, M2E_PROPERTIES_CONTEXT_TYPE, name, false));
    }
  }

  private void addTemplateProposals(ContentAssistRequest request, ITextViewer sourceViewer) {
    int offset = request.getReplacementBeginPosition();
    String prefix = request.getMatchString();
    int len = prefix.length();

    Region region = new Region(offset, len);
    TemplateContext templateContext = createContext(sourceViewer, region);
    if(templateContext == null) {
      return;
    }

    Collection<Template> proposals = getTemplates(prefix);
    for(Template template : proposals) {
      TemplateProposal proposal = createProposalForTemplate(prefix, region, templateContext, template);
      request.addProposal(proposal);
    }
  }
  
  private TemplateProposal createProposalForTemplate(String prefix, Region region, TemplateContext context, final Template template) {
    
      return new AnnotationProcessingModePropertyProposal(template, context, region) {
        public String getAdditionalProposalInfo() {
          return getTemplate().getDescription();
        }

        public String getDisplayString() {
          return template.getName();
        }
      };
  }
  
  private static class AnnotationProcessingModePropertyProposal extends TemplateProposal implements IRelevanceCompletionProposal {

    public AnnotationProcessingModePropertyProposal(Template template, TemplateContext context, IRegion region) {
      super(template, context, region, null, 2000);
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
          return getTemplate().getName().toLowerCase().startsWith(content.toLowerCase());
        }
      } catch(BadLocationException e) {
        // concurrent modification - ignore
      }
      return false;
    }
  }
  
  private TemplateContext createContext(ITextViewer viewer, IRegion region) {
    TemplateContextType contextType = MvnIndexPlugin.getDefault()
                                                    .getTemplateContextRegistry()
                                                    .getContextType(M2E_PROPERTIES_CONTEXT_TYPE);
    if(contextType != null) {
      IDocument document = viewer.getDocument();
      return new DocumentTemplateContext(contextType, document, region.getOffset(), region.getLength());
    }
    return null;
  }
}
