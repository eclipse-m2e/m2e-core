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
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.StructuredTextEditor;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.actions.OpenPomAction;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.editor.xml.internal.Messages;


/**
 * @author Eugene Kuleshov
 * @author Milos Kleint
 */
public class PomHyperlinkDetector implements IHyperlinkDetector {

  private final String[] versioned = new String[] {
      "dependency>", //$NON-NLS-1$
      "parent>", //$NON-NLS-1$
      "plugin>", //$NON-NLS-1$
      "reportPlugin>", //$NON-NLS-1$
      "extension>" //$NON-NLS-1$
  };
  public IHyperlink[] detectHyperlinks(ITextViewer textViewer, final IRegion region, boolean canShowMultipleHyperlinks) {
    if(region == null || textViewer == null) {
      return null;
    }
    
    IDocument document = textViewer.getDocument();
    if(document == null) {
      return null;
    }

    IRegion lineInfo;
    String line;
    try {
      lineInfo = document.getLineInformationOfOffset(region.getOffset());
      line = document.get(lineInfo.getOffset(), lineInfo.getLength());
    } catch(BadLocationException ex) {
      return null;
    }

    if(line.length() == 0) {
      return null;
    }
    List<IHyperlink> hyperlinks = new ArrayList<IHyperlink>();
    final int offset = region.getOffset();
    final String text = document.get();
    Node current = getCurrentNode(document, offset);
    //check if we have a property expression at cursor
    IHyperlink link = openPropertyDefinition(current, textViewer, offset);
    if (link != null) {
      hyperlinks.add(link);
    }
    //now check if the dependency/plugin has a version element or not, if not, try searching for it in DM/PM of effective pom
    link = openDPManagement(current, textViewer, offset);
    if (link != null) {
      hyperlinks.add(link);
    }
    
    //first check all elements that have id (groupId+artifactId+version) combo
    Fragment fragment = null;
    //TODO rewrite to use Nodes
    for (String el : versioned) {
      fragment = getFragment(text, offset, "<" + el, "</" + el); //$NON-NLS-1$ //$NON-NLS-2$
      if (fragment != null) break;
    }
    
    if (fragment != null) {
      link = openPOMbyID(fragment, textViewer);
      if (link != null) {
        hyperlinks.add(link);
      }
    }
    //check if <module> text is selected.
    //TODO rewrite to use Nodes
    fragment = getFragment(text, offset, "<module>", "</module>"); //$NON-NLS-1$ //$NON-NLS-2$
    if (fragment != null) {
      link = openModule(fragment, textViewer);
      if (link != null) {
        hyperlinks.add(link);
      }
    }
    if (hyperlinks.size() > 0) {
      return hyperlinks.toArray(new IHyperlink[0]);
    }
    return null;
  }

  static ManagedArtifactRegion findManagedArtifactRegion(Node current, ITextViewer textViewer, int offset) {
    while (current != null && !( current instanceof Element)) {
      current = current.getParentNode(); 
    }
    if (current != null) {
      Node artNode = null;
      Node groupNode = null;
      if ("artifactId".equals(current.getNodeName())) { //$NON-NLS-1$
        artNode = current;
      }
      if ("groupId".equals(current.getNodeName())) { //$NON-NLS-1$
        groupNode = current;
      }
      //only on artifactid and groupid elements..
      if (artNode == null && groupNode == null) {
        return null;
      }
      Node root = current.getParentNode();
      boolean isDependency = false;
      boolean isPlugin = false;
      if (root != null) {
        String name = root.getNodeName();
        if ("dependency".equals(name)) { //$NON-NLS-1$
          isDependency = true;
        }
        if ("plugin".equals(name)) { //$NON-NLS-1$
          isPlugin = true;
        }
      } else {
        return null;
      }
      if (!isDependency && !isPlugin) {
        //some kind of other identifier
        return null;
      }
      //now see if version is missing
      NodeList childs = root.getChildNodes();
      for (int i = 0; i < childs.getLength(); i++) {
        Node child = childs.item(i);
        if (child instanceof Element) {
          Element el = (Element) child;
          if ("version".equals(el.getNodeName())) { //$NON-NLS-1$
            return null;
          }
          if (artNode == null && "artifactId".equals(el.getNodeName())) { //$NON-NLS-1$
            artNode = el;
          }
          if (groupNode == null && "groupId".equals(el.getNodeName())) { //$NON-NLS-1$
            groupNode = el;
          }
        }
      }
      if (groupNode != null && artNode != null) {
        assert groupNode instanceof IndexedRegion;
        assert artNode instanceof IndexedRegion;
        
        IndexedRegion groupReg = (IndexedRegion)groupNode;
        IndexedRegion artReg = (IndexedRegion)artNode;
        int startOffset = Math.min(groupReg.getStartOffset(), artReg.getStartOffset());
        int length = Math.max(groupReg.getEndOffset(), artReg.getEndOffset()) - startOffset;
        String groupId = getElementTextValue(groupNode);
        String artifactId = getElementTextValue(artNode);
        //TODO we shall rely on presence of a cached model, not project alone..
        final IProject prj = PomContentAssistProcessor.extractProject(textViewer);
        if (prj != null) {
          //now we can create the region I guess, 
          return new ManagedArtifactRegion(startOffset, length, groupId, artifactId, isDependency, isPlugin, prj);
        }
      }
    }
    return null;
  }
        
   private IHyperlink openDPManagement(Node current, ITextViewer textViewer, int offset) {
      final ManagedArtifactRegion region = findManagedArtifactRegion(current, textViewer, offset);
      if (region != null) {
        return new IHyperlink() {
          public IRegion getHyperlinkRegion() {
            return region;
          }

          public String getHyperlinkText() {
            return NLS.bind(Messages.PomHyperlinkDetector_link_managed, "" + region.groupId + ":" + region.artifactId);
          }

          public String getTypeLabel() {
            return "pom-dependency-plugin-management"; //$NON-NLS-1$
          }

          public void open() {
            //see if we can find the plugin in plugin management of resolved project.
            IMavenProjectFacade mvnproject = MavenPlugin.getDefault().getMavenProjectManager().getProject(region.project);
            if (mvnproject != null) {
              MavenProject mavprj = mvnproject.getMavenProject();
              if (mavprj != null) {
                InputLocation openLocation = findLocationForManagedArtifact(region, mavprj);
                if (openLocation != null) {
                  File file = fileForInputLocation(openLocation);
                  if (file != null) {
                    IFileStore fileStore = EFS.getLocalFileSystem().getStore(file.toURI());
                    openXmlEditor(fileStore, openLocation.getLineNumber(), openLocation.getColumnNumber());
                  }
                }
              }
            }
          }
        };
    }
    return null;
  }
  
   
  static InputLocation findLocationForManagedArtifact(final ManagedArtifactRegion region, MavenProject mavprj) {
     Model mdl = mavprj.getModel();
     InputLocation openLocation = null;
     if (region.isDependency) {
       DependencyManagement dm = mdl.getDependencyManagement();
       if (dm != null) {
         List<Dependency> list = dm.getDependencies();
         String id = region.groupId + ":" + region.artifactId + ":"; //$NON-NLS-1$ //$NON-NLS-2$
         if (list != null) {
           for (Dependency dep : list) {
             if (dep.getManagementKey().startsWith(id)) {
               InputLocation location = dep.getLocation("artifactId"); //$NON-NLS-1$
               //when would this be null?
               if (location != null) {
                 openLocation = location;
                 break;
               }
             }
           }
         }
       }
     }
     if (region.isPlugin) {
       Build build = mdl.getBuild();
       if (build != null) {
         PluginManagement pm = build.getPluginManagement();
         if (pm != null) {
           List<Plugin> list = pm.getPlugins();
           String id = Plugin.constructKey(region.groupId, region.artifactId);
           if (list != null) {
             for (Plugin plg : list) {
               if (id.equals(plg.getKey())) {
                 InputLocation location = plg.getLocation("artifactId"); //$NON-NLS-1$
                 //when would this be null?
                 if (location != null) {
                   openLocation = location;
                   break;
                 }
               }
             }
           }
         }
       }
     }
     return openLocation;
   }
   
  static String getElementTextValue(Node element) {
    if (element == null) return null;
    StringBuffer buff = new StringBuffer();
    NodeList list = element.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      Node child = list.item(i);
      if (child instanceof Text) {
        Text text = (Text)child;
        buff.append(text.getData());
      }
    }
    return buff.toString();
  }

  static ExpressionRegion findExpressionRegion(Node current, ITextViewer viewer, int offset) {
    if (current != null && current instanceof Text) {
      Text node = (Text)current;
      String value = node.getNodeValue();
      if (value != null) {
        assert node instanceof IndexedRegion;
        IndexedRegion reg = (IndexedRegion)node;
        int index = offset - reg.getStartOffset();
        String before = value.substring(0, Math.min (index + 1, value.length()));
        String after = value.substring(Math.min (index + 1, value.length()));
        int start = before.lastIndexOf("${"); //$NON-NLS-1$
        if (before.lastIndexOf("}") > start) {//$NON-NLS-1$
          //we might be in between two expressions..
          start = -1;
        }
        int end = after.indexOf("}"); //$NON-NLS-1$
        if (after.indexOf("${") != -1 && after.indexOf("${") < end) {//$NON-NLS-1$
          //we might be in between two expressions..
          end = -1;
        }
        if (start > -1 && end > -1) {
          final int startOffset = reg.getStartOffset() + start;
          final String expr = before.substring(start) + after.substring(0, end + 1);
          final int length = expr.length();
          final String prop = before.substring(start + 2) + after.substring(0, end);
// there are often properties that start with project. eg. project.build.sourceEncoding          
//          if (prop.startsWith("project.") || prop.startsWith("pom.")) { //$NON-NLS-1$ //$NON-NLS-2$
//            return null; //ignore these, not in properties section.
//          }
          final IProject prj = PomContentAssistProcessor.extractProject(viewer);
          //TODO we shall rely on presence of a cached model, not project alone.. ]MNGECLIPSE-2540
          if (prj != null) {
            return new ExpressionRegion(startOffset, length, prop, prj);
          }
        }
      }
    }
    return null;
  }
  /**
   * converts an InputLocation to a file path on the local disk, null if not available.
   * still the input source's model value can be used further..
   * @param location
   * @return
   */
  static  File fileForInputLocation(InputLocation location) {
    InputSource source = location.getSource();
    if (source != null) {
      //MNGECLIPSE-2539 apparently if maven can't resolve the model from local storage,
      //the location will be empty. not only applicable to local repo models but
      //apparently also to models in workspace not reachable by relativePath 
      String loc = source.getLocation();
      File file = null;
      if (loc != null) {
        file = new File(loc);
      } else {
        //try to find pom by coordinates..
        String modelId = source.getModelId();
        String[] splitStrings = modelId.split(":");
        assert splitStrings.length == 3;
        IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager().getMavenProject(splitStrings[0], splitStrings[1], splitStrings[2]);
        if (facade != null) {
          file = facade.getPomFile();
        }
      }
      return file;
    }
    return null;
  }
    
        
   private IHyperlink openPropertyDefinition(Node current, ITextViewer viewer, int offset) {
     final ExpressionRegion region = findExpressionRegion(current, viewer, offset);
     if (region != null) {
        return new IHyperlink() {
          public IRegion getHyperlinkRegion() {
            return region;
          }

          public String getHyperlinkText() {
            return NLS.bind(Messages.PomHyperlinkDetector_open_property, region.property);
          }

          public String getTypeLabel() {
            return "pom-property-expression"; //$NON-NLS-1$
          }

          public void open() {
            //see if we can find the plugin in plugin management of resolved project.
            IMavenProjectFacade mvnproject = MavenPlugin.getDefault().getMavenProjectManager().getProject(region.project);
            if(mvnproject != null) {
              MavenProject mavprj = mvnproject.getMavenProject();
              if(mavprj != null) {
                Model mdl = mavprj.getModel();
                if (mdl.getProperties().containsKey(region.property)) {
                  InputLocation location = mdl.getLocation( "properties" ).getLocation( region.property ); //$NON-NLS-1$
                  if (location != null) {
                    File file = fileForInputLocation(location);
                    if (file != null) {
                      IFileStore fileStore = EFS.getLocalFileSystem().getStore(file.toURI());
                      openXmlEditor(fileStore, location.getLineNumber(), location.getColumnNumber());
                    }
                  }
                }
              }
            }
          }
        };
    }
    return null;
  }

  private IHyperlink openModule(Fragment fragment, ITextViewer textViewer) {
    final Fragment module = getValue(fragment, "<module>", "</module>"); //$NON-NLS-1$ //$NON-NLS-2$

    ITextFileBuffer buf = FileBuffers.getTextFileBufferManager().getTextFileBuffer(textViewer.getDocument());
    IFileStore folder = buf.getFileStore().getParent();

    String path = module.text;
    //construct IPath for the child pom file, handle relative paths..
    while(folder != null && path.startsWith("../")) { //NOI18N //$NON-NLS-1$
      folder = folder.getParent();
      path = path.substring("../".length());//NOI18N //$NON-NLS-1$
    }
    if(folder == null) {
      return null;
    }
    IFileStore modulePom = folder.getChild(path);
    if(!modulePom.getName().endsWith("xml")) {//NOI18N //$NON-NLS-1$
      modulePom = modulePom.getChild("pom.xml");//NOI18N //$NON-NLS-1$
    }
    final IFileStore fileStore = modulePom;
    if (!fileStore.fetchInfo().exists()) {
      return null;
    }

    IHyperlink pomHyperlink = new IHyperlink() {
      public IRegion getHyperlinkRegion() {
        return new Region(module.offset, module.length);
      }

      public String getHyperlinkText() {
        return NLS.bind(Messages.PomHyperlinkDetector_open_module, module.text);
      }

      public String getTypeLabel() {
        return "pom-module"; //$NON-NLS-1$
      }

      public void open() {
        openXmlEditor(fileStore);
      }
    };

    return pomHyperlink;

  }

  private IHyperlink openPOMbyID(Fragment fragment, final ITextViewer viewer) {
    final Fragment groupId = getValue(fragment, "<groupId>", "</groupId>"); //$NON-NLS-1$ //$NON-NLS-2$
    final Fragment artifactId = getValue(fragment, "<artifactId>", Messages.PomHyperlinkDetector_23); //$NON-NLS-1$
    final Fragment version = getValue(fragment, "<version>", "</version>"); //$NON-NLS-1$ //$NON-NLS-2$
    final IProject prj = PomContentAssistProcessor.extractProject(viewer);
    
    IHyperlink pomHyperlink = new IHyperlink() {
      public IRegion getHyperlinkRegion() {
        //the goal here is to have the groupid/artifactid/version combo underscored by the link.
        //that will prevent underscoring big portions (like plugin config) underscored and
        // will also handle cases like dependencies within plugins.
        int max = groupId != null ? groupId.offset + groupId.length : Integer.MIN_VALUE;
        int min = groupId != null ? groupId.offset : Integer.MAX_VALUE;
        max = Math.max(max, artifactId != null ? artifactId.offset + artifactId.length : Integer.MIN_VALUE);
        min = Math.min(min, artifactId != null ? artifactId.offset : Integer.MAX_VALUE);
        max = Math.max(max, version != null ? version.offset + version.length : Integer.MIN_VALUE);
        min = Math.min(min, version != null ? version.offset : Integer.MAX_VALUE);
        return new Region(min, max - min);
      }

      public String getHyperlinkText() {
        return NLS.bind(Messages.PomHyperlinkDetector_hyperlink_pattern, groupId, artifactId);
      }

      public String getTypeLabel() {
        return "pom"; //$NON-NLS-1$
      }

      public void open() {
        new Job(Messages.PomHyperlinkDetector_job_name) {
          protected IStatus run(IProgressMonitor monitor) {
            // TODO resolve groupId if groupId==null
            String gridString = groupId == null ? "org.apache.maven.plugins" : groupId.text; //$NON-NLS-1$      
            String artidString = artifactId == null ? null : artifactId.text;
            String versionString = version == null ? null : version.text;
            if (prj != null && gridString != null && artidString != null && (version == null || version.text.contains("${"))) { //$NON-NLS-1$
              try {
                //TODO how do we decide here if the hyperlink is a dependency or a plugin
                // hyperlink??
                versionString = PomTemplateContext.extractVersion(prj, versionString, gridString, artidString, PomTemplateContext.EXTRACT_STRATEGY_DEPENDENCY);
                
              } catch(CoreException e) {
                versionString = null;
              }
            }
            if (versionString == null) {
              return Status.OK_STATUS;
            }
            OpenPomAction.openEditor(gridString,  
                                     artidString, 
                                     versionString, monitor);
            return Status.OK_STATUS;
          }
        }.schedule();
      }

    };

    return pomHyperlink;
  }

  /**
   * fragment offset returned contains the xml elements 
   * while the text only includes the element text value
   */
  private Fragment getValue(Fragment section, String startTag, String endTag) {
    int start = section.text.indexOf(startTag);
    if(start == -1) {
      return null;
    }
    int end = section.text.indexOf(endTag);
    if(end == -1) {
      return null;
    }

    return new Fragment(section.text.substring(start + startTag.length(), end).trim(), section.offset + start, end + endTag.length() - start);
  }

  /**
   * returns the text, offset and length of the xml element. text includes the xml tags. 
   */
  private Fragment getFragment(String text, int offset, String startTag, String endTag) {
    int start = text.substring(0, offset).lastIndexOf(startTag);
    if(start == -1) {
      return null;
    }

    int end = text.indexOf(endTag, start);
    if(end == -1 || end <= offset) {
      return null;
    }
    end = end + endTag.length();
    return new Fragment(text.substring(start, end), start, end - start);
  }
  
  private static class Fragment {
    final int length;
    final int offset;
    final String text;
    
    Fragment(String text, int start, int len) {
      this.text = text;
      this.offset = start;
      
      this.length = len;
      
    }

    @Override
    public String toString() {
      return text;
    }
  }
  
  
  /**
   * copied from org.eclipse.wst.xml.ui.internal.hyperlink.XMLHyperlinkDetector
   * Returns the node the cursor is currently on in the document. null if no
   * node is selected
   * 
   * returned value is also an instance of IndexedRegion
   * 
   * @param offset
   * @return Node either element, doctype, text, or null
   */
  static Node getCurrentNode(IDocument document, int offset) {
    // get the current node at the offset (returns either: element,
    // doctype, text)
    IndexedRegion inode = null;
    IStructuredModel sModel = null;
    try {
      sModel = StructuredModelManager.getModelManager().getExistingModelForRead(document);
      if (sModel != null) {
        inode = sModel.getIndexedRegion(offset);
        if (inode == null) {
          inode = sModel.getIndexedRegion(offset - 1);
        }
      }
    }
    finally {
      if (sModel != null) {
        sModel.releaseFromRead();
      }
    }

    if (inode instanceof Node) {
      return (Node) inode;
    }
    return null;
  }

  private void openXmlEditor(final IFileStore fileStore) {
    openXmlEditor(fileStore, -1, -1);
  }
  
  private void openXmlEditor(final IFileStore fileStore, int line, int column) {
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if(window != null) {
      IWorkbenchPage page = window.getActivePage();
      if(page != null) {
        try {
          IEditorPart part = IDE.openEditorOnFileStore(page, fileStore);
          if(part instanceof FormEditor) {
            FormEditor ed = (FormEditor) part;
            ed.setActivePage(null); //null means source, always or just in the case of MavenPomEditor?
            if(line != -1) {
              if(ed.getActiveEditor() instanceof StructuredTextEditor) {
                StructuredTextEditor structured = (StructuredTextEditor) ed.getActiveEditor();
                // convert the line and Column numbers to an offset:
                IDocument doc = structured.getTextViewer().getDocument();
                if (doc instanceof IStructuredDocument) {
                  IStructuredDocument document = (IStructuredDocument) doc;
                  try {
                    int offset = document.getLineOffset(line - 1);
                    structured.selectAndReveal(offset + column - 1, 0);
                  } catch(BadLocationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                  }
                }
              }
            }
          }
        } catch(PartInitException e) {
          MessageDialog.openInformation(
              Display.getDefault().getActiveShell(), //
              Messages.PomHyperlinkDetector_error_title,
              NLS.bind(Messages.PomHyperlinkDetector_error_message, fileStore, e.toString()));

        }
      }
    }
  }

  
  static class ExpressionRegion implements IRegion {

    final String property;
    private int length;
    private int offset;
    final IProject project;

    public ExpressionRegion(int startOffset, int length, String prop, IProject project) {
      this.offset = startOffset;
      this.length = length;
      this.property = prop;
      this.project = project;
    }

    public int getLength() {
      return length;
    }

    public int getOffset() {
      return offset;
    }
  }
  
  static class ManagedArtifactRegion implements IRegion {

    private int length;
    private int offset;
    final IProject project;
    final String groupId;
    final String artifactId;
    final boolean isPlugin;
    final boolean isDependency;

    public ManagedArtifactRegion(int startOffset, int length, String groupId, String artifactId, boolean isDependency, boolean isPlugin, IProject project) {
      this.offset = startOffset;
      this.length = length;
      this.project = project;
      this.artifactId = artifactId;
      this.groupId = groupId;
      this.isDependency = isDependency;
      this.isPlugin = isPlugin;
    }

    public int getLength() {
      return length;
    }

    public int getOffset() {
      return offset;
    }
  }
  
  
}
