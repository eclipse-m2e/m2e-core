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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.sse.ui.internal.contentassist.IRelevanceCompletionProposal;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
import org.eclipse.wst.xml.ui.internal.contentassist.DefaultXMLCompletionProposalComputer;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.editor.xml.internal.Messages;
import org.eclipse.m2e.editor.xml.internal.XmlUtils;


/**
 * @author Lukas Krecan
 * @author Eugene Kuleshov
 */
@SuppressWarnings("restriction")
public class PomContentAssistProcessor extends DefaultXMLCompletionProposalComputer {

  private static Set<PomTemplateContext> expressionproposalContexts = EnumSet.of( //
      PomTemplateContext.GROUP_ID, PomTemplateContext.ARTIFACT_ID, //
      //version is intentionally not included as we have specialized handling there..
      //PomTemplateContext.VERSION,
      PomTemplateContext.PACKAGING, PomTemplateContext.TYPE, //
      PomTemplateContext.CLASSIFIER, PomTemplateContext.SCOPE, PomTemplateContext.SYSTEM_PATH, //
      PomTemplateContext.PROPERTIES, PomTemplateContext.MODULE, //
      PomTemplateContext.PHASE, PomTemplateContext.GOAL, PomTemplateContext.CONFIGURATION, //
      //this one is both important and troubling.. but having a context for everything is weird.
      PomTemplateContext.UNKNOWN);

  protected void addTagNameProposals(ContentAssistRequest contentAssistRequest, int childPosition,
      CompletionProposalInvocationContext ctx) {
    PomTemplateContext context = PomTemplateContext.fromNode(contentAssistRequest.getParent());

    if(PomTemplateContext.CONFIGURATION == context) {
      addTemplateProposals(contentAssistRequest, context, ctx.getViewer(), true);
    }
  }

  protected void addTagInsertionProposals(ContentAssistRequest contentAssistRequest, int childPosition,
      CompletionProposalInvocationContext ctx) {
    PomTemplateContext context = PomTemplateContext.fromNode(contentAssistRequest.getParent());

    // wst content assist doesn't provide matchString in text content
    int offset = contentAssistRequest.getReplacementBeginPosition();
    String prefix = extractPrefix(ctx.getViewer(), offset);
    contentAssistRequest.setMatchString(prefix);
    contentAssistRequest.setReplacementBeginPosition(offset - prefix.length());
    contentAssistRequest.setReplacementLength(prefix.length());

    addExpressionProposals(contentAssistRequest, context, ctx.getViewer());
    addGenerateProposals(contentAssistRequest, context, ctx.getViewer());
    addTemplateProposals(contentAssistRequest, context, ctx.getViewer(), false);
  }

  /**
   * this is a proposal method for adding expressions when ${ is typed..
   * 
   * @param request
   * @param context
   * @param currentNode
   * @param prefix
   */
  private void addExpressionProposals(ContentAssistRequest request, PomTemplateContext context,
      ITextViewer sourceViewer) {
    String prefix = request.getMatchString();
    int exprStart = prefix.lastIndexOf("${"); //$NON-NLS-1$
    if(exprStart != -1) {
      //the regular prefix is separated by whitespace and <> brackets only, we need to cut the last portion
      String realExpressionPrefix = prefix.substring(exprStart);
      if(realExpressionPrefix.contains("}")) { //$NON-NLS-1$
        //the expression is not opened..
        return;
      }

      if(expressionproposalContexts.contains(context)) {
        //add all effective pom expressions
        MavenProject prj = XmlUtils.extractMavenProject(sourceViewer);
        Region region = new Region(request.getReplacementBeginPosition() + exprStart, realExpressionPrefix.length());
        Set<String> collect = new TreeSet<String>();
        if(prj != null) {
          Properties props = prj.getProperties();
          if(props != null) {
            for(Object key : props.keySet()) {
              String keyString = key.toString();
              if(("${" + keyString).startsWith(realExpressionPrefix)) { //$NON-NLS-1$
                collect.add(keyString);
              }
            }
          }
        }

        //add a few hardwired values as well
        if("${basedir}".startsWith(realExpressionPrefix)) { //$NON-NLS-1$
          collect.add("basedir"); //$NON-NLS-1$
        }
        if("${project.version}".startsWith(realExpressionPrefix)) { //$NON-NLS-1$
          collect.add("project.version"); //$NON-NLS-1$
        }
        if("${project.groupId}".startsWith(realExpressionPrefix)) { //$NON-NLS-1$
          collect.add("project.groupId"); //$NON-NLS-1$
        }
        if("${project.artifactId}".startsWith(realExpressionPrefix)) { //$NON-NLS-1$
          collect.add("project.artifactId"); //$NON-NLS-1$
        }
        if("${project.build.directory}".startsWith(realExpressionPrefix)) { //$NON-NLS-1$
          collect.add("project.build.directory"); //$NON-NLS-1$
        }
        for(String key : collect) {
          request.addProposal(new InsertExpressionProposal(region, key, prj));
        }
      }
    }
  }

  private void addGenerateProposals(ContentAssistRequest request, PomTemplateContext context,
      ITextViewer sourceViewer) {

    String prefix = request.getMatchString();
    if(prefix.trim().length() != 0) {
      //only provide these generate proposals when there is no prefix.
      return;
    }

    Node node = request.getParent();

    if(context == PomTemplateContext.PARENT && node.getNodeName().equals("parent")) { //$NON-NLS-1$
      Element parent = (Element) node;
      Element relPath = XmlUtils.findChild(parent, "relativePath"); //$NON-NLS-1$
      if(relPath == null) {
        //only show when no relpath already defined..
        String relative = findRelativePath(sourceViewer, parent);
        if(relative != null) {
          Region region = new Region(request.getReplacementBeginPosition(), 0);
          ICompletionProposal proposal = new CompletionProposal("<relativePath>" + relative + "</relativePath>", //$NON-NLS-1$ //$NON-NLS-2$
              region.getOffset(), region.getLength(), 0, //
              PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD), //
              NLS.bind(Messages.PomContentAssistProcessor_insert_relPath_title, relative), null, null);
          request.addProposal(proposal);
        }
      }
    }
    if(context == PomTemplateContext.RELATIVE_PATH) {
      //completion in the text portion of relative path
      Element parent = (Element) node.getParentNode();
      if(parent != null && "parent".equals(parent.getNodeName())) { //$NON-NLS-1$
        String relative = findRelativePath(sourceViewer, parent);
        String textContent = XmlUtils.getTextValue(node);
        if(relative != null && !relative.equals(textContent)) {
          Region region = new Region(request.getReplacementBeginPosition() - prefix.length(), prefix.length());
          if(request.getNode() instanceof IndexedRegion && request.getNode() instanceof Text) {
            //for <relativePath>|</relativePath> the current node is the element node and not the text node
            //only replace the text node content..
            IndexedRegion index = (IndexedRegion) request.getNode();
            region = new Region(index.getStartOffset(), index.getEndOffset() - index.getStartOffset());
          }
          ICompletionProposal proposal = new CompletionProposal(relative, region.getOffset(), region.getLength(), 0,
              PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD),
              NLS.bind(Messages.PomContentAssistProcessor_set_relPath_title, relative), null, null);
          request.addProposal(proposal);
        }
      }
    }
    if(context == PomTemplateContext.DEPENDENCIES || context == PomTemplateContext.PROFILE
        || context == PomTemplateContext.DEPENDENCY_MANAGEMENT || context == PomTemplateContext.PROJECT) {
      //now add the proposal for dependency inclusion
      Region region = new Region(request.getReplacementBeginPosition(), 0);
      InsertArtifactProposal.Configuration config = new InsertArtifactProposal.Configuration(
          InsertArtifactProposal.SearchType.DEPENDENCY);
      config.setCurrentNode(node);

      request.addProposal(new InsertArtifactProposal(sourceViewer, region, config));
    }

    if(context == PomTemplateContext.PLUGINS || context == PomTemplateContext.BUILD
        || context == PomTemplateContext.PLUGIN_MANAGEMENT || context == PomTemplateContext.PROJECT) {
      //now add the proposal for plugin inclusion
      Region region = new Region(request.getReplacementBeginPosition(), 0);
      InsertArtifactProposal.Configuration config = new InsertArtifactProposal.Configuration(
          InsertArtifactProposal.SearchType.PLUGIN);
      config.setCurrentNode(node);

      request.addProposal(new InsertArtifactProposal(sourceViewer, region, config));

    }
    //comes after dependency and plugin.. the dep and plugin ones are guessed to be more likely hits..
    if(context == PomTemplateContext.PROJECT) {
      //check if we have a parent defined..
      Node project = node;
      if(project != null && project instanceof Element) {
        Element parent = XmlUtils.findChild((Element) project, "parent"); //$NON-NLS-1$
        if(parent == null) {
          //now add the proposal for parent inclusion
          Region region = new Region(request.getReplacementBeginPosition(), 0);
          Element groupId = XmlUtils.findChild((Element) project, "groupId"); //$NON-NLS-1$
          String groupString = null;
          if(groupId != null) {
            groupString = XmlUtils.getTextValue(groupId);
          }
          InsertArtifactProposal.Configuration config = new InsertArtifactProposal.Configuration(
              InsertArtifactProposal.SearchType.PARENT);
          config.setInitiaSearchString(groupString);
          request.addProposal(new InsertArtifactProposal(sourceViewer, region, config));
        }
      }
    }
    if((context == PomTemplateContext.PROJECT && XmlUtils.findChild((Element) node, "licenses") == null)
        || context == PomTemplateContext.LICENSES) {
      Region region = new Region(request.getReplacementBeginPosition(), 0);
      request.addProposal(new InsertSPDXLicenseProposal(sourceViewer, context, region));
    }
  }

  private static String findRelativePath(ITextViewer viewer, Element parent) {
    String groupId = XmlUtils.getTextValue(XmlUtils.findChild(parent, "groupId")); //$NON-NLS-1$
    String artifactId = XmlUtils.getTextValue(XmlUtils.findChild(parent, "artifactId")); //$NON-NLS-1$
    String version = XmlUtils.getTextValue(XmlUtils.findChild(parent, "version")); //$NON-NLS-1$
    return findRelativePath(viewer, groupId, artifactId, version);
  }

  public static String findRelativePath(ITextViewer viewer, String groupId, String artifactId, String version) {
    if(groupId != null && artifactId != null && version != null) {
      IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getMavenProject(groupId, artifactId, version);
      if(facade != null) {
        //now add the proposal for relativePath
        IFile parentPomFile = facade.getPom();
        IPath path = parentPomFile.getLocation();
        //TODO we might not need the IPRoject instance at all..
        IProject prj = XmlUtils.extractProject(viewer);
        if(prj != null && path != null) {
          IPath path2 = prj.getLocation();
          IPath relative = path.makeRelativeTo(path2);
          if(relative != path) {
            return relative.toString();
          }
        }
      }
    }
    return null;
  }

  private void addTemplateProposals(ContentAssistRequest request, PomTemplateContext context, ITextViewer sourceViewer,
      boolean tagProposals) {
    MavenProject prj = XmlUtils.extractMavenProject(sourceViewer);
    IProject eclipseprj = XmlUtils.extractProject(sourceViewer);
    ITextSelection selection = (ITextSelection) sourceViewer.getSelectionProvider().getSelection();

    Node parentNode = request.getParent();
    int offset = request.getReplacementBeginPosition();
    String prefix = request.getMatchString();
    int len = prefix.length();

    // also replace opening '<'
    if(tagProposals) {
      offset-- ;
      len++ ;
    }

    Region region = new Region(offset, len);
    TemplateContext templateContext = createContext(sourceViewer, region, context.getContextTypeId());
    if(templateContext == null) {
      return;
    }

    // name of the selection variables {line, word}_selection 
    templateContext.setVariable("selection", selection.getText()); //$NON-NLS-1$

    // add the user defined templates - separate them from the rest of the templates
    // so that we know what they are and can assign proper icon to them.
    Image image = MvnImages.IMG_USER_TEMPLATE;
    List<TemplateProposal> matches = new ArrayList<TemplateProposal>();
    TemplateStore store = MvnIndexPlugin.getDefault().getTemplateStore();
    if(store != null) {
      Template[] templates = store.getTemplates(context.getContextTypeId());
      for(Template template : templates) {
        TemplateProposal proposal = createProposalForTemplate(prefix, region, templateContext, image, template, true);
        if(proposal != null) {
          matches.add(proposal);
        }
      }
    }
    if(context == PomTemplateContext.CONFIGURATION) {
      image = MvnImages.IMG_PARAMETER;
    } else {
      //other suggestions from the templatecontext are to be text inside the element, not actual
      //elements..
      image = null;
    }

    Template[] templates = context.getTemplates(prj, eclipseprj, parentNode, prefix);
    for(Template template : templates) {
      TemplateProposal proposal = createProposalForTemplate(prefix, region, templateContext, image, template, false);
      if(proposal != null) {
        matches.add(proposal);
      }
    }

    for(ICompletionProposal proposal : matches) {
      request.addProposal(proposal);
    }
  }

  private TemplateProposal createProposalForTemplate(String prefix, Region region, TemplateContext context, Image image,
      final Template template, boolean isUserTemplate) {
    try {
      context.getContextType().validate(template.getPattern());
      if(template.matches(prefix, context.getContextType().getId())) {
        if(isUserTemplate) {
          //for templates defined by users, preserve the default behaviour..
          return new PomTemplateProposal(template, context, region, image, getRelevance(template, prefix)) {
            public String getAdditionalProposalInfo() {
              return StringUtils.convertToHTMLContent(super.getAdditionalProposalInfo());
            }
          };
        }
        return new PomTemplateProposal(template, context, region, image, getRelevance(template, prefix)) {
          public String getAdditionalProposalInfo() {
            return getTemplate().getDescription();
          }

          public String getDisplayString() {
            return template.getName();
          }
        };
      }
    } catch(TemplateException e) {
      // ignore
    }

    return null;
  }

  protected TemplateContext createContext(ITextViewer viewer, IRegion region, String contextTypeId) {
    TemplateContextType contextType = getContextType(viewer, region, contextTypeId);
    if(contextType != null) {
      IDocument document = viewer.getDocument();
      return new DocumentTemplateContext(contextType, document, region.getOffset(), region.getLength());
    }
    return null;
  }

  //TODO we should have different relevance for user defined templates and generated proposals..
  protected int getRelevance(Template template, String prefix) {
    if(template.getName().startsWith(prefix))
      return 1900;
    return 1500;
  }

  protected TemplateContextType getContextType(ITextViewer viewer, IRegion region, String contextTypeId) {
    ContextTypeRegistry registry = MvnIndexPlugin.getDefault().getTemplateContextRegistry();
    if(registry != null) {
      return registry.getContextType(contextTypeId);
    }
    return null;
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

  private static class PomTemplateProposal extends TemplateProposal implements IRelevanceCompletionProposal {

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
          return getTemplate().getName().toLowerCase().startsWith(content.toLowerCase());
        }
      } catch(BadLocationException e) {
        // concurrent modification - ignore
      }
      return false;
    }
  }

}
