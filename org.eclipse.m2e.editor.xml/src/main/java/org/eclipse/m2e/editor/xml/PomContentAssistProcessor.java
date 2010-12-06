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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.apache.maven.project.MavenProject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ISourceViewer;
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
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
import org.eclipse.wst.xml.ui.internal.contentassist.XMLContentAssistProcessor;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.project.MavenMarkerManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.editor.xml.internal.Messages;

/**
 * @author Lukas Krecan
 * @author Eugene Kuleshov
 */
@SuppressWarnings("restriction")
public class PomContentAssistProcessor extends XMLContentAssistProcessor {

  private static final ProposalComparator PROPOSAL_COMPARATOR = new ProposalComparator();
  
  private ISourceViewer sourceViewer;

  private PomStructuredTextViewConfiguration textConfig;

  public PomContentAssistProcessor(ISourceViewer sourceViewer, PomStructuredTextViewConfiguration pomStructuredTextViewConfiguration) {
    this.sourceViewer = sourceViewer;
    textConfig = pomStructuredTextViewConfiguration;
  }

    //broken
  
  protected void addTagNameProposals(ContentAssistRequest contentAssistRequest, int childPosition) {
    String currentNodeName = getCurrentNode(contentAssistRequest).getNodeName();
    PomTemplateContext context = PomTemplateContext.fromNodeName(currentNodeName);
    if(PomTemplateContext.CONFIGURATION == context) 
    {
      //this is sort of hack that makes sure the config proposals appear even
      // when you type <prefix
      // the downside is that additional typing hides the proposals popup
      // there has to be a better way though. the xml element completions seems to be coping with it fine..
      contentAssistRequest.setReplacementBeginPosition(contentAssistRequest.getReplacementBeginPosition() - 1);
      contentAssistRequest.setReplacementLength(contentAssistRequest.getReplacementLength() + 1);
      addProposals(contentAssistRequest, context, getCurrentNode(contentAssistRequest), contentAssistRequest.getMatchString());
    }
    if(PomTemplateContext.UNKNOWN == context) 
    {
      context = PomTemplateContext.fromNodeName(getCurrentNode(contentAssistRequest).getParentNode().getNodeName());
      if(PomTemplateContext.CONFIGURATION == context) 
      {
        addProposals(contentAssistRequest, context, getCurrentNode(contentAssistRequest).getParentNode(), contentAssistRequest.getMatchString());
      }
    }
    super.addTagNameProposals(contentAssistRequest, childPosition);
  }
  
  
  @Override
  protected void addTagInsertionProposals(ContentAssistRequest contentAssistRequest, int childPosition) {
    String currentNodeName = getCurrentNode(contentAssistRequest).getNodeName();
    
    addProposals(contentAssistRequest, PomTemplateContext.fromNodeName(currentNodeName));
    super.addTagInsertionProposals(contentAssistRequest, childPosition);
  }

  private Node getCurrentNode(ContentAssistRequest contentAssistRequest) {
    Node currentNode = contentAssistRequest.getNode();
    if(currentNode instanceof Text) {
      currentNode = currentNode.getParentNode();
    }
    return currentNode;
  }

  private void addProposals(ContentAssistRequest request, PomTemplateContext context) {
    ITextSelection selection = (ITextSelection) sourceViewer.getSelectionProvider().getSelection();
    int offset = request.getReplacementBeginPosition();
    // adjust offset to end of normalized selection
    if(selection.getOffset() == offset) {
      offset = selection.getOffset() + selection.getLength();
    }

    String prefix = extractPrefix(sourceViewer, offset);
    
    addExpressionProposal(request, context, getCurrentNode(request), prefix);
    
    addGenerateProposals(request, context, getCurrentNode(request), prefix);
    
    addProposals(request, context, getCurrentNode(request), prefix);
  }
  /**
   * this is a proposal method for adding expressions when ${ is typed.. 
   * @param request
   * @param context
   * @param currentNode
   * @param prefix
   */
  private void addExpressionProposal(ContentAssistRequest request, PomTemplateContext context, Node currentNode,
      String prefix) {
    int exprStart = prefix.lastIndexOf("${"); //$NON-NLS-1$
    if (exprStart != -1) {
      //the regular prefix is separated by whitespace and <> brackets only, we need to cut the last portion
      String realExpressionPrefix = prefix.substring(exprStart);
      if (realExpressionPrefix.contains("}")) { //$NON-NLS-1$
        //the expression is not opened..
        return;
      }
      if (expressionproposalContexts.contains(context)) {
        //add all effective pom expressions
        IProject prj = extractProject(sourceViewer);
        Region region = new Region(request.getReplacementBeginPosition() - realExpressionPrefix.length(), realExpressionPrefix.length());
        if (prj != null) {
          IMavenProjectFacade mvnproject = MavenPlugin.getDefault().getMavenProjectManager().getProject(prj);
          Set<String> collect = new TreeSet<String>();
          if (mvnproject != null) {
            MavenProject mp = mvnproject.getMavenProject();
            if (mp != null) {
              Properties props = mp.getProperties();
              if (props != null) {
                for (Object key : props.keySet()) {
                  String keyString = key.toString();
                  if (("${" + keyString).startsWith(realExpressionPrefix)) { //$NON-NLS-1$
                    collect.add(keyString);
                  }
                }
              }
            }
            //add a few hardwired values as well
            if ("${basedir}".startsWith(realExpressionPrefix)) { //$NON-NLS-1$
              collect.add("basedir"); //$NON-NLS-1$
            }
            if ("${project.version}".startsWith(realExpressionPrefix)) { //$NON-NLS-1$
              collect.add("project.version"); //$NON-NLS-1$
            }
            if ("${project.groupId}".startsWith(realExpressionPrefix)) { //$NON-NLS-1$
              collect.add("project.groupId"); //$NON-NLS-1$
            }
            if ("${project.artifactId}".startsWith(realExpressionPrefix)) { //$NON-NLS-1$
              collect.add("project.artifactId"); //$NON-NLS-1$
            }
            if ("${project.build.directory}".startsWith(realExpressionPrefix)) { //$NON-NLS-1$
              collect.add("project.build.directory"); //$NON-NLS-1$
            }
            for (String key : collect) {
              ICompletionProposal proposal = new InsertExpressionProposal(sourceViewer, region, key, mvnproject); 
              if(request.shouldSeparate()) {
                request.addMacro(proposal);
              } else {
                request.addProposal(proposal);
              }
            }
          }
        }
      }
    }
  }
  
  private static List<PomTemplateContext> expressionproposalContexts = Arrays.asList(new PomTemplateContext[] {
     PomTemplateContext.ARTIFACT_ID,
     PomTemplateContext.CLASSIFIER,
//     PomTemplateContext.CONFIGURATION,
     PomTemplateContext.GOAL,
     PomTemplateContext.GROUP_ID,
     PomTemplateContext.MODULE,
     PomTemplateContext.PACKAGING,
     PomTemplateContext.PHASE,
     PomTemplateContext.PROPERTIES, //??
     PomTemplateContext.SCOPE,
     PomTemplateContext.SYSTEM_PATH,
     PomTemplateContext.TYPE,
//     PomTemplateContext.VERSION, version is intentionally not included as we have specialized handling there.. 
     PomTemplateContext.UNKNOWN //this one is both important and troubling.. but having a context for everything is weird.
  });

  private void addGenerateProposals(ContentAssistRequest request, PomTemplateContext context, Node node, String prefix) {
    if (prefix.trim().length() != 0) {
      //only provide these generate proposals when there is no prefix.
      return;
    }
    if (context == PomTemplateContext.PROJECT) {
      //check if we have a parent defined..
      Node project = node;
      if (project != null && project instanceof Element) {
        Element parent = MavenMarkerManager.findChildElement((Element)project, "parent"); //$NON-NLS-1$
        if (parent == null) {
          //now add the proposal for parent inclusion
          Region region = new Region(request.getReplacementBeginPosition(), 0);
          Element groupId = MavenMarkerManager.findChildElement((Element)project, "groupId"); //$NON-NLS-1$
          String groupString = null;
          if (groupId != null) {
            groupString = MavenMarkerManager.getElementTextValue(groupId);
          }
          InsertArtifactProposal.Configuration config = new InsertArtifactProposal.Configuration(InsertArtifactProposal.SearchType.PARENT);
          config.setInitiaSearchString(groupString);
          ICompletionProposal proposal = new InsertArtifactProposal(sourceViewer, region, config, this.textConfig); 
          if(request.shouldSeparate()) {
            request.addMacro(proposal);
          } else {
            request.addProposal(proposal);
          }
        } 
      }
    }
    if (context == PomTemplateContext.PARENT && node.getNodeName().equals("parent")) { //$NON-NLS-1$
      Element parent = (Element)node;
      Element relPath = MavenMarkerManager.findChildElement(parent, "relativePath"); //$NON-NLS-1$
      if (relPath == null) {
        //only show when no relpath already defined..
        String relative = findRelativePath(sourceViewer, parent);
        if (relative != null) {
          Region region = new Region(request.getReplacementBeginPosition(), 0);
          ICompletionProposal proposal = new CompletionProposal("<relativePath>" + relative + "</relativePath>",  //$NON-NLS-1$ //$NON-NLS-2$
              region.getOffset(), region.getLength(), 0, 
              WorkbenchPlugin.getDefault().getImageRegistry().get(org.eclipse.ui.internal.SharedImages.IMG_OBJ_ADD), 
              NLS.bind(Messages.PomContentAssistProcessor_insert_relPath_title, relative), null, null);
          if (request.shouldSeparate()) {
            request.addMacro(proposal);
          } else {
            request.addProposal(proposal);
          }
        }
      }
    }
    if (context == PomTemplateContext.RELATIVE_PATH) {
      //completion in the text portion of relative path
      Element parent = (Element) node.getParentNode();
      if (parent != null && "parent".equals(parent.getNodeName())) { //$NON-NLS-1$
        String relative = findRelativePath(sourceViewer, parent);
        String textContent = MavenMarkerManager.getElementTextValue(node); 
        if (relative != null && !relative.equals(textContent)) {
          Region region = new Region(request.getReplacementBeginPosition() - prefix.length(), prefix.length());
          if (request.getNode() instanceof IndexedRegion && request.getNode() instanceof Text) { 
            //for <relativePath>|</relativePath> the current node is the element node and not the text node
            //only replace the text node content..
            IndexedRegion index = (IndexedRegion)request.getNode();
            region = new Region(index.getStartOffset(), index.getEndOffset() - index.getStartOffset());
          }
          ICompletionProposal proposal = new CompletionProposal(relative, 
              region.getOffset(), region.getLength(), 0, 
              WorkbenchPlugin.getDefault().getImageRegistry().get(org.eclipse.ui.internal.SharedImages.IMG_OBJ_ADD), 
              NLS.bind(Messages.PomContentAssistProcessor_set_relPath_title, relative), null, null);
          if (request.shouldSeparate()) {
            request.addMacro(proposal);
          } else {
            request.addProposal(proposal);
          }
        }
      }
    }
    if (context == PomTemplateContext.PLUGINS || context == PomTemplateContext.BUILD 
        || context == PomTemplateContext.PLUGIN_MANAGEMENT || context == PomTemplateContext.PROJECT) {
      //now add the proposal for plugin inclusion
      Region region = new Region(request.getReplacementBeginPosition(), 0);
      InsertArtifactProposal.Configuration config = new InsertArtifactProposal.Configuration(InsertArtifactProposal.SearchType.PLUGIN);
      config.setCurrentNode(node);
      
      ICompletionProposal proposal = new InsertArtifactProposal(sourceViewer, region, config, this.textConfig); 
      if(request.shouldSeparate()) {
        request.addMacro(proposal);
      } else {
        request.addProposal(proposal);
      }

    }
    
  }
  /*
   * calculates the path of the node up in the hierarchy, example of result is project/build/plugins/plugin
   * level parameter designates the number of parents to climb eg. for level 2 the result would be plugins/plugin
   * level -1 means all the way to the top. 
   */
  static String pathUp(Node node, int level) {
    StringBuffer buf = new StringBuffer();
    int current = level;
    while (node != null && level > 0) {
      if (node instanceof Element) {
        if (buf.length() > 0) {
          buf.insert(0, "/");
        }
        buf.insert(0, node.getNodeName());
        current = current -1;
      }
      node = node.getParentNode();
    }
    return buf.toString();
  }
  
  private static String findRelativePath(ISourceViewer viewer, Element parent) {
    String groupId = MavenMarkerManager.getElementTextValue(MavenMarkerManager.findChildElement(parent, "groupId")); //$NON-NLS-1$
    String artifactId = MavenMarkerManager.getElementTextValue(MavenMarkerManager.findChildElement(parent, "artifactId")); //$NON-NLS-1$
    String version = MavenMarkerManager.getElementTextValue(MavenMarkerManager.findChildElement(parent, "version")); //$NON-NLS-1$
    return findRelativePath(viewer, groupId, artifactId, version);
  }
  
  public static String findRelativePath(ISourceViewer viewer, String groupId, String artifactId, String version) {
    if (groupId != null && artifactId != null && version != null) {
      IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager().getMavenProject(groupId, artifactId, version);
      if (facade != null) {
        //now add the proposal for relativePath
        IFile parentPomFile = facade.getPom();
        IPath path = parentPomFile.getLocation();
        IProject prj = extractProject(viewer);
        if (prj != null && path != null) {
          IPath path2 = prj.getLocation();
          IPath relative = path.makeRelativeTo(path2);
          if (relative != path) {
              return relative.toString();
          }
        }
      }
    }
    return null;
  }

  
  
  private void addProposals(ContentAssistRequest request, PomTemplateContext context, Node currentNode, String prefix) {
    if(request != null) {
      IProject prj = extractProject(sourceViewer);

      ICompletionProposal[] templateProposals = getTemplateProposals(prj, sourceViewer,
          request.getReplacementBeginPosition(), context.getContextTypeId(), currentNode, prefix);
      for(ICompletionProposal proposal : templateProposals) {
        if(request.shouldSeparate()) {
          request.addMacro(proposal);
        } else {
          request.addProposal(proposal);
        }
      }
    }
  }

  /**
   * what is this method supposed to do? for the sourceViewer find the associated file on disk and for
   * that one find the IProject it belongs to. The required condition for the IProject instance is that
   * project relative path of the file shall only be pom.xml (thus no nested, unopened maven pom). 
   * So that when MavenPlugin.getDefault().getMavenProjectManager().getProject(prj); is called later on
   * the instance, it actually returns the maven model facade for the pom.xml backing the sourceViewer.
   * @param sourceViewer
   * @return
   */
  public static IProject extractProject(ITextViewer sourceViewer) {
    ITextFileBuffer buf = FileBuffers.getTextFileBufferManager().getTextFileBuffer(sourceViewer.getDocument());
    IFileStore folder = buf.getFileStore();
    File file = new File(folder.toURI());
    IPath path = Path.fromOSString(file.getAbsolutePath());
    Stack<IResource> stack = new Stack<IResource>();
    //here we need to find the most inner project to the path.
    //we do so by shortening the path and remembering all the resources identified.
    // at the end we pick the last one from the stack. is there a catch to it?
    IResource ifile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(path);
    if (ifile != null) {
      stack.push(ifile);
    } else {
      while(path.segmentCount() > 1) {
        ResourcesPlugin.getWorkspace().getRoot().findMember(path);
        if(ifile != null) {
          stack.push(ifile);
        }
        path = path.removeFirstSegments(1);
      }
    }
    IResource res = stack.empty() ? null : stack.pop();
    if (res != null) {
      IProject prj = res.getProject();
    //the project returned is in a way unrelated to nested child poms that don't have an opened project,
    //in that case we pass along a wrong parent/aggregator
      if (res.getProjectRelativePath().segmentCount() != 1) { 
        //if the project were the pom's project, the relative path would be just "pom.xml", if it's not just throw it out of the window..
        prj = null;
      }
      return prj;
    }
    return null;
  }
  
  private ICompletionProposal[] getTemplateProposals(IProject project, ITextViewer viewer, int offset, String contextTypeId, Node currentNode, String prefix) {
    ITextSelection selection = (ITextSelection) viewer.getSelectionProvider().getSelection();

    // adjust offset to end of normalized selection
    if(selection.getOffset() == offset) {
      offset = selection.getOffset() + selection.getLength();
    }

//    String prefix = extractPrefix(viewer, offset);
    Region region = new Region(offset - prefix.length(), prefix.length()); 
    TemplateContext context = createContext(viewer, region, contextTypeId);
    if(context == null) {
      return new ICompletionProposal[0];
    }

    // name of the selection variables {line, word}_selection 
    context.setVariable("selection", selection.getText()); //$NON-NLS-1$

    PomTemplateContext templateContext = PomTemplateContext.fromId(contextTypeId);
    
    // add the user defined templates - separate them from the rest of the templates
    // so that we know what they are and can assign proper icon to them.
    Image image = MvnImages.IMG_USER_TEMPLATE;
    List<TemplateProposal> matches = new ArrayList<TemplateProposal>();
    TemplateStore store = MvnIndexPlugin.getDefault().getTemplateStore();
    if(store != null) {
      Template[] templates = store.getTemplates(contextTypeId);
      for(Template template : templates) {
        TemplateProposal proposal = createProposalForTemplate(prefix, region, context, image, template, true);
        if (proposal != null) {
          matches.add(proposal);
        }
      }
    }    
    if (templateContext == PomTemplateContext.CONFIGURATION) {
      image = MvnImages.IMG_PARAMETER;
    } else {
      //other suggestions from the templatecontext are to be text inside the element, not actual
      //elements..
      image = null;
    }
    
    Template[] templates = templateContext.getTemplates(project, currentNode, prefix);
    for(Template template : templates) {
      TemplateProposal proposal = createProposalForTemplate(prefix, region, context, image, template, false);
      if (proposal != null) {
        matches.add(proposal);
      }
    }
    
    
    if (templateContext!=PomTemplateContext.VERSION) {
      // versions are already sorted with o.a.m.artifact.versioning.ComparableVersion
      Collections.sort(matches, PROPOSAL_COMPARATOR);
    }

    return (ICompletionProposal[]) matches.toArray(new ICompletionProposal[matches.size()]);

  }

  private TemplateProposal createProposalForTemplate(String prefix, Region region, TemplateContext context, Image image,
      final Template template, boolean isUserTemplate) {
    try {
      context.getContextType().validate(template.getPattern());
      if(template.matches(prefix, context.getContextType().getId())) {
        if (isUserTemplate) {
          //for templates defined by users, preserve the default behaviour..
          return new TemplateProposal(template, context, region, image, getRelevance(template, prefix)) {
            public String getAdditionalProposalInfo() {
              return StringUtils.convertToHTMLContent(super.getAdditionalProposalInfo());
            }
          };
        } else {
          return new TemplateProposal(template, context, region, image, getRelevance(template, prefix)) {
            public String getAdditionalProposalInfo() {
              return getTemplate().getDescription();
            }
  
            public String getDisplayString() {
              return template.getName();
            }
          };
        }
      }
    } catch(TemplateException e) {
      // ignore
    }
    
    return null;
  }

  protected TemplateContext createContext(ITextViewer viewer, IRegion region, String contextTypeId) {
    TemplateContextType contextType= getContextType(viewer, region, contextTypeId);
    if (contextType != null) {
      IDocument document= viewer.getDocument();
      return new DocumentTemplateContext(contextType, document, region.getOffset(), region.getLength());
    }
    return null;
  }

  //TODO we should have different relevance for user defined templates and generated proposals..
  protected int getRelevance(Template template, String prefix) {
    if (template.getName().startsWith(prefix))
      return 90;
    return 0;
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

  
  static final class ProposalComparator implements Comparator<TemplateProposal> {
    public int compare(TemplateProposal o1, TemplateProposal o2) {
      int res = o2.getRelevance() - o1.getRelevance();
      if(res == 0) {
        res = o1.getDisplayString().compareTo(o2.getDisplayString());
      }
      return res;
    }
  }
  
}
