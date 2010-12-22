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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.InputLocation;
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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.StructuredTextEditor;

import org.eclipse.m2e.core.actions.OpenPomAction;
import org.eclipse.m2e.core.actions.OpenPomAction.MavenPathStorageEditorInput;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.editor.xml.internal.Messages;
import org.eclipse.m2e.editor.xml.internal.NodeOperation;
import org.eclipse.m2e.editor.xml.internal.XmlUtils;


/**
 * @author Eugene Kuleshov
 * @author Milos Kleint
 */
public class PomHyperlinkDetector implements IHyperlinkDetector {


  public IHyperlink[] detectHyperlinks(final ITextViewer textViewer, final IRegion region, boolean canShowMultipleHyperlinks) {
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
    final List<IHyperlink> hyperlinks = new ArrayList<IHyperlink>();
    final int offset = region.getOffset();
    XmlUtils.performOnCurrentElement(document, offset, new NodeOperation<Node>() {
      public void process(Node node) {
        //check if we have a property expression at cursor
        IHyperlink link = openPropertyDefinition(node, textViewer, offset);
        if (link != null) {
          hyperlinks.add(link);
        }
        //now check if the dependency/plugin has a version element or not, if not, try searching for it in DM/PM of effective pom
        link = openDPManagement(node, textViewer, offset);
        if (link != null) {
          hyperlinks.add(link);
        }
        //check if <module> text is selected.
        link = openModule(node, textViewer, offset);
        if (link != null) {
          hyperlinks.add(link);
        }
        link = openPOMbyID(node, textViewer, offset);
        if (link != null) {
          hyperlinks.add(link);
        }
      }
    });
    
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
        String groupId = XmlUtils.getElementTextValue(groupNode);
        String artifactId = XmlUtils.getElementTextValue(artNode);
        final MavenProject prj = XmlUtils.extractMavenProject(textViewer);
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
            MavenProject mavprj = region.project;
            if (mavprj != null) {
              InputLocation openLocation = findLocationForManagedArtifact(region, mavprj);
              if (openLocation != null) {
                File file = XmlUtils.fileForInputLocation(openLocation);
                if (file != null) {
                  IFileStore fileStore = EFS.getLocalFileSystem().getStore(file.toURI());
                  openXmlEditor(fileStore, openLocation.getLineNumber(), openLocation.getColumnNumber(), openLocation.getSource().getModelId());
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
          MavenProject prj = XmlUtils.extractMavenProject(viewer);
          if (prj != null) {
            return new ExpressionRegion(startOffset, length, prop, prj);
          }
        }
      }
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
            MavenProject mavprj = region.project;
            if(mavprj != null) {
              Model mdl = mavprj.getModel();
              if (mdl.getProperties().containsKey(region.property)) {
                InputLocation location = mdl.getLocation( "properties" ).getLocation( region.property ); //$NON-NLS-1$
                if (location != null) {
                  File file = XmlUtils.fileForInputLocation(location);
                  if (file != null) {
                    IFileStore fileStore = EFS.getLocalFileSystem().getStore(file.toURI());
                    openXmlEditor(fileStore, location.getLineNumber(), location.getColumnNumber(), location.getSource().getModelId());
                  }
                }
              }
            }
          }
        };
    }
    return null;
  }

  private IHyperlink openModule(Node current, ITextViewer textViewer, int offset) {
    while (current != null && !( current instanceof Element)) {
      current = current.getParentNode(); 
    }
    if (current == null) {
      return null;
    }
    String pathUp = XmlUtils.pathUp(current, 2);
    if (! "modules/module".equals(pathUp)) { //$NON-NLS-1$
      //just in case we are in some random plugin configuration snippet..
      return null;
    }
    
    ITextFileBuffer buf = FileBuffers.getTextFileBufferManager().getTextFileBuffer(textViewer.getDocument());
    if (buf == null) {
      //for repository based poms..
      return null;
    }
    IFileStore folder = buf.getFileStore().getParent();

    String path = XmlUtils.getElementTextValue(current);
    final String fPath = path;
    //construct IPath for the child pom file, handle relative paths..
    while(folder != null && path.startsWith("../")) { //$NON-NLS-1$
      folder = folder.getParent();
      path = path.substring("../".length());//$NON-NLS-1$
    }
    if(folder == null) {
      return null;
    }
    IFileStore modulePom = folder.getChild(path);
    if(!modulePom.getName().endsWith("xml")) { //$NON-NLS-1$
      modulePom = modulePom.getChild("pom.xml");//$NON-NLS-1$
    }
    final IFileStore fileStore = modulePom;
    if (!fileStore.fetchInfo().exists()) {
      return null;
    }
    assert current instanceof IndexedRegion;
    final IndexedRegion region = (IndexedRegion) current;

    return new IHyperlink() {
      public IRegion getHyperlinkRegion() {
        return new Region(region.getStartOffset(), region.getEndOffset() - region.getStartOffset());
      }

      public String getHyperlinkText() {
        return NLS.bind(Messages.PomHyperlinkDetector_open_module, fPath);
      }

      public String getTypeLabel() {
        return "pom-module"; //$NON-NLS-1$
      }

      public void open() {
        openXmlEditor(fileStore);
      }
    };
  }

  private IHyperlink openPOMbyID(Node current, final ITextViewer viewer, int offset) {
    while (current != null && !( current instanceof Element)) {
      current = current.getParentNode(); 
    }
    if (current == null) {
      return null;
    }
    Element parent = (Element) current.getParentNode();
    if (parent == null) {
      return null;
    }
    String parentName = parent.getNodeName();
    if ("dependency".equals(parentName) || "parent".equals(parentName)
        || "plugin".equals(parentName) || "reportPlugin".equals(parentName)
        || "extension".equals(parentName)) {
      final Node groupId = XmlUtils.findChildElement(parent, "groupId"); 
      final Node artifactId = XmlUtils.findChildElement(parent, "artifactId"); 
      final Node version = XmlUtils.findChildElement(parent, "version"); 
      final MavenProject prj = XmlUtils.extractMavenProject(viewer);
    
    
    IHyperlink pomHyperlink = new IHyperlink() {
      public IRegion getHyperlinkRegion() {
        //the goal here is to have the groupid/artifactid/version combo underscored by the link.
        //that will prevent underscoring big portions (like plugin config) underscored and
        // will also handle cases like dependencies within plugins.
        int max = groupId != null ? ((IndexedRegion)groupId).getEndOffset() : Integer.MIN_VALUE;
        int min = groupId != null ? ((IndexedRegion)groupId).getStartOffset() : Integer.MAX_VALUE;
        max = Math.max(max, artifactId != null ? ((IndexedRegion)artifactId).getEndOffset() : Integer.MIN_VALUE);
        min = Math.min(min, artifactId != null ? ((IndexedRegion)artifactId).getStartOffset() : Integer.MAX_VALUE);
        max = Math.max(max, version != null ? ((IndexedRegion)version).getEndOffset() : Integer.MIN_VALUE);
        min = Math.min(min, version != null ? ((IndexedRegion)version).getStartOffset() : Integer.MAX_VALUE);
        return new Region(min, max - min);
      }

      public String getHyperlinkText() {
        return NLS.bind(Messages.PomHyperlinkDetector_hyperlink_pattern, XmlUtils.getElementTextValue(groupId), XmlUtils.getElementTextValue(artifactId));
      }

      public String getTypeLabel() {
        return "pom"; //$NON-NLS-1$
      }

      public void open() {
        new Job(Messages.PomHyperlinkDetector_job_name) {
          protected IStatus run(IProgressMonitor monitor) {
            // TODO resolve groupId if groupId==null
            String gridString = groupId == null ? "org.apache.maven.plugins" : XmlUtils.getElementTextValue(groupId); //$NON-NLS-1$      
            String artidString = artifactId == null ? null : XmlUtils.getElementTextValue(artifactId);
            String versionString = version == null ? null : XmlUtils.getElementTextValue(version);
            if (prj != null && gridString != null && artidString != null && (versionString == null || versionString.contains("${"))) { //$NON-NLS-1$
              try {
                //TODO how do we decide here if the hyperlink is a dependency or a plugin
                // hyperlink??
                versionString = PomTemplateContext.extractVersion(prj, null, versionString, gridString, artidString, PomTemplateContext.EXTRACT_STRATEGY_DEPENDENCY);
                
              } catch(CoreException e) {
                versionString = null;
              }
            }
            if (versionString == null) {
              return Status.OK_STATUS;
            }
            final IEditorPart page = OpenPomAction.openEditor(gridString,  
                                     artidString, 
                                     versionString, monitor);
// TODO: it's preferable to open the xml page, but this code will blink and open overview first and later switch. looks bad            
//            Display.getDefault().syncExec(new Runnable() {
//              public void run() {
//                selectEditorPage(page);
//              }
//            });
            return Status.OK_STATUS;
          }
        }.schedule();
      }

    };
    return pomHyperlink;
    }
    return null;
  }

  
  private void openXmlEditor(final IFileStore fileStore) {
    openXmlEditor(fileStore, -1, -1, fileStore.getName());
  }
  
  private void openXmlEditor(final IFileStore fileStore, int line, int column, String name) {
    assert fileStore != null;
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if(window != null) {
      IWorkbenchPage page = window.getActivePage();
      if(page != null) {
        try {
          if(fileStore.getName().equals("pom.xml")) {
            IEditorPart part = IDE.openEditorOnFileStore(page, fileStore);
            reveal(selectEditorPage(part), line, column);
          } else {
            //we need special EditorInput for stuff from repository
            name = name +  ".pom"; //$NON-NLS-1$
            File file = new File(fileStore.toURI());
            try {
              IEditorInput input = new MavenPathStorageEditorInput(name, name, file.getAbsolutePath(),
                  readStream(new FileInputStream(file)));
              IEditorPart part = OpenPomAction.openEditor(input, name);
              reveal(selectEditorPage(part), line, column);
            } catch(IOException e) {
              MavenLogger.log("failed opening editor", e);
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
  
  private StructuredTextEditor selectEditorPage(IEditorPart part) {
    if (part == null) {
      return null;
    }
    if (part instanceof FormEditor) {
      FormEditor ed = (FormEditor) part;
      ed.setActivePage(null); //null means source, always or just in the case of MavenPomEditor?
      if (ed.getActiveEditor() instanceof StructuredTextEditor) {
        return (StructuredTextEditor) ed.getActiveEditor();
      }      
    }
    return null;
  }
  
  private void reveal(StructuredTextEditor structured, int line, int column) {
    if (structured == null || line < 0 || column < 0) {
      return;
    }
    IDocument doc = structured.getTextViewer().getDocument();
    if (doc instanceof IStructuredDocument) {
      IStructuredDocument document = (IStructuredDocument) doc;
      try {
        int offset = document.getLineOffset(line - 1);
        structured.selectAndReveal(offset + column - 1, 0);
      } catch(BadLocationException e) {
        MavenLogger.log("failed selecting part of editor", e);
      }
    }
  }
  
  /**
   * duplicate of OpenPomAction method
   * @param is
   * @return
   * @throws IOException
   */
  private static byte[] readStream(InputStream is) throws IOException {
    byte[] b = new byte[is.available()];
    int len = 0;
    while(true) {
      int n = is.read(b, len, b.length - len);
      if(n == -1) {
        if(len < b.length) {
          byte[] c = new byte[len];
          System.arraycopy(b, 0, c, 0, len);
          b = c;
        }
        return b;
      }
      len += n;
      if(len == b.length) {
        byte[] c = new byte[b.length + 1000];
        System.arraycopy(b, 0, c, 0, len);
        b = c;
      }
    }
  }
  

  
  static class ExpressionRegion implements IRegion {

    final String property;
    private int length;
    private int offset;
    final MavenProject project;

    public ExpressionRegion(int startOffset, int length, String prop, MavenProject project) {
      this.offset = startOffset;
      this.length = length;
      this.property = prop;
      this.project = project;
      assert project != null;
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
    final MavenProject project;
    final String groupId;
    final String artifactId;
    final boolean isPlugin;
    final boolean isDependency;

    public ManagedArtifactRegion(int startOffset, int length, String groupId, String artifactId, boolean isDependency, boolean isPlugin, MavenProject project) {
      this.offset = startOffset;
      this.length = length;
      this.project = project;
      assert project != null;
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
