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

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.xml.core.internal.provisional.format.FormatProcessorXML;

import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.ui.dialogs.MavenRepositorySearchDialog;
import org.eclipse.m2e.editor.xml.internal.Messages;
import org.eclipse.m2e.editor.xml.internal.XmlUtils;

public class InsertArtifactProposal implements ICompletionProposal, ICompletionProposalExtension4, ICompletionProposalExtension5 {

  private ISourceViewer sourceViewer;
  private Region region;
  private int generatedLength = 0;
  private int generatedOffset;
  private Configuration config;

  public InsertArtifactProposal(ISourceViewer sourceViewer, Region region, Configuration config) {
    this.sourceViewer = sourceViewer;
    this.region = region;
    generatedOffset = region.getOffset();
    this.config = config;
    assert config.getType() != null;
  }

  public void apply(IDocument document) {
    MavenProject prj = XmlUtils.extractMavenProject(sourceViewer);
    Set<ArtifactKey> managedKeys = new HashSet<ArtifactKey>();
    Set<ArtifactKey> usedKeys = new HashSet<ArtifactKey>();
    if (config.getType() == SearchType.PLUGIN) {
      //only populate the lists when in plugin search..
      // and when in plugin management section use the different set than elsewhere to get different visual effect.
      String path = XmlUtils.pathUp(config.getCurrentNode(), 2);
      Set<ArtifactKey> keys = path.contains("pluginManagement") ? usedKeys : managedKeys;   //$NON-NLS-1$
      if (prj != null) {
        PluginManagement pm = prj.getPluginManagement();
        if (pm != null && pm.getPlugins() != null) {
          for (Plugin plug : pm.getPlugins()) {
            keys.add(new ArtifactKey(plug.getGroupId(), plug.getArtifactId(), plug.getVersion(), null));
          }
        }
      }
      //TODO also collect the used plugins list
    }
    if (config.getType() == SearchType.DEPENDENCY) {
      //only populate the lists when in dependency search..
      // and when in dependency management or plugin section use the different set than elsewhere to get different visual effect.
      String path = XmlUtils.pathUp(config.getCurrentNode(), 2);
      if (!path.contains("plugin")) { //$NON-NLS-1$
        Set<ArtifactKey> keys = path.contains("dependencyManagement") ? usedKeys : managedKeys; //$NON-NLS-1$
        if (prj != null) {
          DependencyManagement pm = prj.getDependencyManagement();
          if (pm != null && pm.getDependencies() != null) {
            for (Dependency dep : pm.getDependencies()) {
              keys.add(new ArtifactKey(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getClassifier()));
            }
          }
        }
      }
      //TODO also collect the used dependency list
    }
    
    MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(sourceViewer.getTextWidget().getShell(),
        config.getType().getWindowTitle(), config.getType().getIIndexType(),
        usedKeys, managedKeys, false);
    if (config.getInitiaSearchString() != null) {
      dialog.setQuery(config.getInitiaSearchString());
    }
    if(dialog.open() == Window.OK) {
      String lineDelim = document.getLegalLineDelimiters()[0];//do we care? or just append \n always? 
      IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
      int offset = region.getOffset();
      if(af != null) {
        if (config.getType() == SearchType.PARENT) {
          try {
            StringBuffer buffer = new StringBuffer();
            buffer.append("<parent>").append(lineDelim); //$NON-NLS-1$
            buffer.append("<groupId>").append(af.group).append("</groupId>").append(lineDelim); //$NON-NLS-1$ //$NON-NLS-2$
            buffer.append("<artifactId>").append(af.artifact).append("</artifactId>").append(lineDelim); //$NON-NLS-1$ //$NON-NLS-2$
            buffer.append("<version>").append(af.version).append("</version>").append(lineDelim); //$NON-NLS-1$ //$NON-NLS-2$
            String relativePath = PomContentAssistProcessor.findRelativePath(sourceViewer, af.group, af.artifact, af.version);
            if (relativePath != null) {
              buffer.append("<relativePath>").append(relativePath).append("</relativePath>").append(lineDelim); //$NON-NLS-1$ //$NON-NLS-2$
            }
            buffer.append("</parent>").append(lineDelim); //$NON-NLS-1$
            generatedLength = buffer.toString().length();
            document.replace(offset, region.getLength(), buffer.toString());
            
            Region resRegion = format(document, generatedOffset, generatedLength);
            generatedOffset = resRegion.getOffset();
            generatedLength = resRegion.getLength(); 
          } catch(BadLocationException e) {
            MavenLogger.log("Failed inserting parent element", e); //$NON-NLS-1$
          }
        }

        // plugin type
        
        if (config.getType() == SearchType.PLUGIN) {
          Node current = config.getCurrentNode();
          if ("project".equals(current.getNodeName())) { //$NON-NLS-1$
            //in project section go with build/plugins.
            Element build = XmlUtils.findChildElement((Element)current, "build"); //$NON-NLS-1$
            if (build == null) {
              try {
                StringBuffer buffer = new StringBuffer();
                generateBuild(buffer, lineDelim, af, skipVersion(current, af, managedKeys, config.getType()));
                generatedLength = buffer.toString().length();
                document.replace(offset, 0, buffer.toString());
                
                Region resRegion = format(document, generatedOffset, generatedLength);
                generatedOffset = resRegion.getOffset();
                generatedLength = resRegion.getLength(); 
              } catch (BadLocationException e) {
                MavenLogger.log("Failed inserting build element", e); //$NON-NLS-1$
              }
              return;
            } else {
              current = build;
              IndexedRegion reg = (IndexedRegion)current;
              //we need to update the offset to where we found the existing build element..
              offset = reg.getEndOffset() - "</build>".length(); //$NON-NLS-1$
            }
          }
          if ("build".equals(current.getNodeName()) || "pluginManagement".equals(current.getNodeName())) { //$NON-NLS-1$ //$NON-NLS-2$
            Element plugins = XmlUtils.findChildElement((Element)current, "plugins"); //$NON-NLS-1$
            if (plugins == null) {
              //we need to create it.
              try {
                StringBuffer buffer = new StringBuffer();
                generateArtifacts(config.getType(), buffer, lineDelim, af, skipVersion(current, af, managedKeys, config.getType()));
                generatedLength = buffer.toString().length();
                document.replace(offset, 0, buffer.toString());
                
                Region resRegion = format(document, offset, generatedLength);
                generatedOffset = resRegion.getOffset();
                generatedLength = resRegion.getLength(); 
              } catch (BadLocationException e) {
                MavenLogger.log("Failed inserting plugins element", e); //$NON-NLS-1$
              }
              return;
            } else {
              current = plugins;
              IndexedRegion reg = (IndexedRegion)current;
              //we need to update the offset to where we found the existing plugins element..
              offset = reg.getEndOffset() - "</plugins>".length(); //$NON-NLS-1$
            }
          }
          if ("plugins".equals(current.getNodeName())) { //$NON-NLS-1$
            //simple, just add the plugin here..
            //TODO we might want to look if the plugin is already defined in this section or not..
            try {
              StringBuffer buffer = new StringBuffer();
              generateArtifact(config.getType(), buffer, lineDelim, af, skipVersion(current, af, managedKeys, config.getType()));
              generatedLength = buffer.toString().length();
              document.replace(offset, 0, buffer.toString());
              Region resRegion = format(document, offset, generatedLength);
              generatedOffset = resRegion.getOffset();
              generatedLength = resRegion.getLength();
            } catch (BadLocationException e) {
              MavenLogger.log("Failed inserting plugin element", e); //$NON-NLS-1$
            }
          }
        }
          // dependency type
          
          if (config.getType() == SearchType.DEPENDENCY) {
            Node current = config.getCurrentNode();
            if ("project".equals(current.getNodeName()) || "dependencyManagement".equals(current.getNodeName()) || "profile".equals(current.getNodeName())) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
              //in project section go with dependencies section.
              Element deps = XmlUtils.findChildElement((Element)current, "dependencies"); //$NON-NLS-1$
              if (deps == null) {
                try {
                  StringBuffer buffer = new StringBuffer();
                  generateArtifacts(config.getType(), buffer, lineDelim, af, skipVersion(current, af, managedKeys, config.getType()));
                  generatedLength = buffer.toString().length();
                  document.replace(offset, 0, buffer.toString());
                  
                  Region resRegion = format(document, generatedOffset, generatedLength);
                  generatedOffset = resRegion.getOffset();
                  generatedLength = resRegion.getLength(); 
                } catch (BadLocationException e) {
                  MavenLogger.log("Failed inserting dependencies element", e); //$NON-NLS-1$
                }
                return;
              } else {
                current = deps;
                IndexedRegion reg = (IndexedRegion)current;
                //we need to update the offset to where we found the existing dependencies element..
                offset = reg.getEndOffset() - "</dependencies>".length(); //$NON-NLS-1$
              }
            }
            if ("dependencies".equals(current.getNodeName())) { //$NON-NLS-1$
              //simple, just add the dependency here..
              //TODO we might want to look if the dependency is already defined in this section or not..
              try {
                StringBuffer buffer = new StringBuffer();
                generateArtifact(config.getType(), buffer, lineDelim, af, skipVersion(current, af, managedKeys, config.getType()));
                generatedLength = buffer.toString().length();
                document.replace(offset, 0, buffer.toString());
                Region resRegion = format(document, offset, generatedLength);
                generatedOffset = resRegion.getOffset();
                generatedLength = resRegion.getLength();
              } catch (BadLocationException e) {
                MavenLogger.log("Failed inserting dependency element", e); //$NON-NLS-1$
              }
            }
        }
      }
    }
  }
  
  /**
   * decide if we want to generate the version element or not..
   * @param currentNode
   * @param af
   * @param managedList
   * @return
   */
  private boolean skipVersion(Node currentNode, IndexedArtifactFile af, Set<ArtifactKey> managedList, SearchType type) {
    String path = XmlUtils.pathUp(currentNode, 2);
    if (type == SearchType.PLUGIN) {
      if (path.contains("pluginManagement")) { //$NON-NLS-1$
        return false;
      }
    }
    if (type == SearchType.DEPENDENCY) {
      if (path.contains("dependencyManagement") || path.contains("plugin")) { //$NON-NLS-1$ //$NON-NLS-2$
        return false;
      }
    }
    ArtifactKey key = new ArtifactKey(af.group, af.artifact, af.version, af.classifier);
    return managedList.contains(key);
  }
  
  private void generateArtifact(SearchType type, StringBuffer buffer, String lineDelim, IndexedArtifactFile af, boolean skipVersion) {
    assert type == SearchType.PLUGIN || type == SearchType.DEPENDENCY;
    String rootElement = type == SearchType.PLUGIN ? "plugin" : "dependency"; //$NON-NLS-1$ //$NON-NLS-2$
    buffer.append("<" + rootElement + ">").append(lineDelim); //$NON-NLS-1$ //$NON-NLS-2$
    buffer.append("<groupId>").append(af.group).append("</groupId>").append(lineDelim); //$NON-NLS-1$ //$NON-NLS-2$
    buffer.append("<artifactId>").append(af.artifact).append("</artifactId>").append(lineDelim); //$NON-NLS-1$ //$NON-NLS-2$
    //for managed plugins (if version matches only?), don't add the version element
    if (!skipVersion) {
      buffer.append("<version>").append(af.version).append("</version>").append(lineDelim); //$NON-NLS-1$ //$NON-NLS-2$
    }
    buffer.append("</" + rootElement + ">").append(lineDelim); //$NON-NLS-1$ //$NON-NLS-2$
  }
  
  private void generateArtifacts(SearchType type, StringBuffer buffer, String lineDelim, IndexedArtifactFile af, boolean skipVersion) {
    assert type == SearchType.PLUGIN || type == SearchType.DEPENDENCY;
    String rootElement = type == SearchType.PLUGIN ? "plugins" : "dependencies"; //$NON-NLS-1$ //$NON-NLS-2$
    buffer.append("<" + rootElement + ">").append(lineDelim); //$NON-NLS-1$ //$NON-NLS-2$
    generateArtifact(type, buffer, lineDelim, af, skipVersion);
    buffer.append("</" + rootElement + ">").append(lineDelim); //$NON-NLS-1$ //$NON-NLS-2$
  }
  
  private void generateBuild(StringBuffer buffer, String lineDelim, IndexedArtifactFile af, boolean skipVersion) {
    buffer.append("<build>").append(lineDelim); //$NON-NLS-1$
    generateArtifacts(SearchType.PLUGIN, buffer, lineDelim, af, skipVersion);
    buffer.append("</build>").append(lineDelim); //$NON-NLS-1$
  } 
  
  /**
   * take the document and format the region specified by the supplied formatter.
   * operates on whole line (determined by the region specified)
   * returns the new region encompassing the original region after formatting
   */
  public static Region format(IDocument document, int offset, int length) throws BadLocationException {
    int startLine = document.getLineOfOffset(offset);
    int endLine = document.getLineOfOffset(offset + length - 1); // -1 to make sure to be before the end of line char
    int startLineOffset = document.getLineOffset(startLine);
    try {
      new FormatProcessorXML().formatDocument(document, offset, length);
    } catch(Exception e) {
      MavenLogger.log("Failed to format generated code", e);
    }    
//    formatter.format(document, new Region(startLineOffset, (document.getLineOffset(endLine) + document.getLineLength(endLine)) - startLineOffset));
    startLineOffset = document.getLineOffset(startLine); //should be same, just being paranoid
    return new Region (startLineOffset, (document.getLineOffset(endLine) + document.getLineLength(endLine)) - startLineOffset);
  }

  public Point getSelection(IDocument document) {
    return new Point(generatedOffset, generatedLength);
  }

  public String getAdditionalProposalInfo() {
    return null; //not to be used anymore
  }

  public String getDisplayString() {
    return config.getType().getDisplayName(); 
  }

  public Image getImage() {
    return config.getType().getImage();
  }

  public IContextInformation getContextInformation() {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean isAutoInsertable() {
    return false;
  }

  public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
    return config.getType().getAdditionalInfo();
  }
  
  /**
   * supported search types
   * @author mkleint
   *
   */
  public static enum SearchType {
    
    PARENT(IIndex.SEARCH_PARENTS, Messages.InsertArtifactProposal_searchDialog_title, Messages.InsertArtifactProposal_display_name, MvnImages.IMG_OPEN_POM, Messages.InsertArtifactProposal_additionals), 
    PLUGIN(IIndex.SEARCH_PLUGIN, Messages.InsertArtifactProposal_insert_plugin_title, Messages.InsertArtifactProposal_insert_plugin_display_name, MvnImages.IMG_OPEN_POM, Messages.InsertArtifactProposal_insert_plugin_description),
    DEPENDENCY(IIndex.SEARCH_ARTIFACT, Messages.InsertArtifactProposal_insert_dep_title, Messages.InsertArtifactProposal_insert_dep_display_name, MvnImages.IMG_OPEN_POM, Messages.InsertArtifactProposal_insert_dep_desc);
    
    private final String type;
    private final String windowTitle;
    private final String displayName;
    private final Image image;
    private final String additionalInfo;
    private SearchType(String type, String windowTitle, String dn, Image img, String addInfo) {
      this.type = type;
      this.windowTitle = windowTitle;
      this.displayName = dn;
      this.image = img;
      this.additionalInfo = addInfo;
    }
    
    String getIIndexType() {
      return type;
    }

    public String getWindowTitle() {
      return windowTitle;
    }

    public String getDisplayName() {
      return displayName;
    }

    public Image getImage() {
      return image;
    }

    public String getAdditionalInfo() {
      return additionalInfo;
    }
    
  }
  
  public static class Configuration {
    private final SearchType type;
    private String initiaSearchString;
    private Node node;
    
    public Configuration(SearchType type) {
      this.type = type;
    }
    
    public void setInitiaSearchString(String initiaSearchString) {
      this.initiaSearchString = initiaSearchString;
    }
    public String getInitiaSearchString() {
      return initiaSearchString;
    }
    public SearchType getType() {
      return type;
    }

    public void setCurrentNode(Node node) {
      this.node = node;
    }

    public Node getCurrentNode() {
      return node;
    }
  }

}
