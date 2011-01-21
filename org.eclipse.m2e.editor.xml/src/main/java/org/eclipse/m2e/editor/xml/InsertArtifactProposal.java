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

import static org.eclipse.m2e.editor.xml.internal.PomEdits.elementAtOffset;
import static org.eclipse.m2e.editor.xml.internal.PomEdits.findChild;
import static org.eclipse.m2e.editor.xml.internal.PomEdits.format;
import static org.eclipse.m2e.editor.xml.internal.PomEdits.getChild;
import static org.eclipse.m2e.editor.xml.internal.PomEdits.insertAt;
import static org.eclipse.m2e.editor.xml.internal.PomEdits.performOnDOMDocument;
import static org.eclipse.m2e.editor.xml.internal.PomEdits.setText;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
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

import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.ui.dialogs.MavenRepositorySearchDialog;
import org.eclipse.m2e.editor.xml.internal.Messages;
import org.eclipse.m2e.editor.xml.internal.PomEdits.Operation;
import org.eclipse.m2e.editor.xml.internal.PomEdits.OperationTuple;
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
    final Set<ArtifactKey> managedKeys = new HashSet<ArtifactKey>();
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
      final IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
      int offset = region.getOffset();
      if(af != null) {
        if (config.getType() == SearchType.PARENT) {
          try {
              final int fOffset = offset;
              performOnDOMDocument(new OperationTuple(document, new Operation() {
                public void process(Document doc) {
                  Element parent = insertAt(doc.createElement("parent"), fOffset);
                  setText(getChild(parent, "groupId"), af.group);
                  setText(getChild(parent, "artifactId"), af.artifact);
                  setText(getChild(parent, "version"), af.version);
                  String relativePath = PomContentAssistProcessor.findRelativePath(sourceViewer, af.group, af.artifact, af.version);
                  if (relativePath != null) {
                    setText(getChild(parent, "relativePath"), relativePath);
                  }
                  format(parent);
                  generatedOffset = ((IndexedRegion)parent).getStartOffset();
                  generatedLength = ((IndexedRegion)parent).getEndOffset() - generatedOffset;
                }
              }));
          } catch(IOException e) {
            MavenLogger.log("Failed inserting parent element", e); //$NON-NLS-1$
          } catch(CoreException e) {
            MavenLogger.log("Failed inserting parent element", e); //$NON-NLS-1$
          }
        }

        // plugin type
        
        if(config.getType() == SearchType.PLUGIN) {
          try {
            final int fOffset = offset;
            performOnDOMDocument(new OperationTuple(document, new Operation() {
              public void process(Document doc) {
                Element currentNode = elementAtOffset(doc, fOffset);
                if (currentNode == null) {
                  return;
                }
                String currentName = currentNode.getNodeName();
                Element plugin = null;
                Element toFormat = null;
                if("project".equals(currentName)) { //$NON-NLS-1$
                  Element build = findChild(currentNode, "build");
                  if(build == null) {
                    build = insertAt(doc.createElement("build"), fOffset);
                    toFormat = build;
                  }
                  Element plugins = getChild(build, "plugins");
                  plugin = doc.createElement("plugin");
                  plugins.appendChild(plugin);
                }
                if("build".equals(currentName) || "pluginManagement".equals(currentName)) { //$NON-NLS-1$ //$NON-NLS-2$
                  Element plugins = findChild(currentNode, "plugins");
                  if(plugins == null) {
                    plugins = insertAt(doc.createElement("plugins"), fOffset);
                    toFormat = plugins;
                  }
                  plugin = doc.createElement("plugin");
                  plugins.appendChild(plugin);
                }
                if("plugins".equals(currentName)) {
                  plugin = insertAt(doc.createElement("plugin"), fOffset);
                }
                if (toFormat == null) {
                  toFormat = plugin;
                }
                setText(getChild(plugin, "groupId"), af.group);
                setText(getChild(plugin, "artifactId"), af.artifact);
                if (!skipVersion(plugin.getParentNode(), af, managedKeys, config.getType())) {
                  setText(getChild(plugin, "version"), af.version);
                }
                format(toFormat);
                generatedOffset = ((IndexedRegion)toFormat).getStartOffset();
                generatedLength = ((IndexedRegion)toFormat).getEndOffset() - generatedOffset;
              }
            }));
          } catch(IOException e) {
            MavenLogger.log("Failed inserting plugin element", e); //$NON-NLS-1$
          } catch(CoreException e) {
            MavenLogger.log("Failed inserting plugin element", e); //$NON-NLS-1$
          }
        }
          // dependency type
          
          if (config.getType() == SearchType.DEPENDENCY) {
            try {
              final int fOffset = offset;
              performOnDOMDocument(new OperationTuple(document, new Operation() {
                public void process(Document doc) {
                  Element currentNode = elementAtOffset(doc, fOffset);
                  if (currentNode == null) {
                    return;
                  }
                  String currentName = currentNode.getNodeName();
                  Element dependency = null;
                  Element toFormat = null;
                  if("project".equals(currentName) || "dependencyManagement".equals(currentName) || "profile".equals(currentName)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    Element deps = findChild(currentNode, "dependencies");
                    if(deps == null) {
                      deps = insertAt(doc.createElement("dependencies"), fOffset);
                      toFormat = deps;
                    }
                    dependency = doc.createElement("dependency");
                    deps.appendChild(dependency);
                  }
                  if("dependencies".equals(currentName)) {
                    dependency = insertAt(doc.createElement("dependency"), fOffset);
                  }
                  if (toFormat == null) {
                    toFormat = dependency;
                  }
                  setText(getChild(dependency, "groupId"), af.group);
                  setText(getChild(dependency, "artifactId"), af.artifact);
                  if (!skipVersion(dependency.getParentNode(), af, managedKeys, config.getType())) {
                    setText(getChild(dependency, "version"), af.version);
                  }
                  format(toFormat);
                  generatedOffset = ((IndexedRegion)toFormat).getStartOffset();
                  generatedLength = ((IndexedRegion)toFormat).getEndOffset() - generatedOffset;
                }
              }));
            } catch(IOException e) {
              MavenLogger.log("Failed inserting dependency element", e); //$NON-NLS-1$
            } catch(CoreException e) {
              MavenLogger.log("Failed inserting dependency element", e); //$NON-NLS-1$
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
