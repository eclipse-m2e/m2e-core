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

import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.eclipse.core.resources.IProject;
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

import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.ui.internal.dialogs.MavenRepositorySearchDialog;
import org.eclipse.m2e.editor.xml.internal.Messages;
import org.eclipse.m2e.editor.xml.internal.PomEdits.Operation;
import org.eclipse.m2e.editor.xml.internal.PomEdits.OperationTuple;
import org.eclipse.m2e.editor.xml.internal.XmlUtils;

public class InsertArtifactProposal implements ICompletionProposal, ICompletionProposalExtension4, ICompletionProposalExtension5 {
  private static final Logger log = LoggerFactory.getLogger(InsertArtifactProposal.class);

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
    IProject eclPrj = XmlUtils.extractProject(sourceViewer);
    MavenRepositorySearchDialog dialog = null;
    if (config.getType() == SearchType.PLUGIN) {
      String path = XmlUtils.pathUp(config.getCurrentNode(), 2);
      boolean inPM = path.contains("pluginManagement");   //$NON-NLS-1$
      dialog = MavenRepositorySearchDialog.createSearchPluginDialog(sourceViewer.getTextWidget().getShell(),
          config.getType().getWindowTitle(), prj, eclPrj, inPM);
    }
    if (config.getType() == SearchType.PARENT) {
      dialog = MavenRepositorySearchDialog.createSearchParentDialog(sourceViewer.getTextWidget().getShell(),
          config.getType().getWindowTitle(), prj, eclPrj);
    }
    if (config.getType() == SearchType.DEPENDENCY) {
      //only populate the lists when in dependency search..
      // and when in dependency management or plugin section use the different set than elsewhere to get different visual effect.
      String path = XmlUtils.pathUp(config.getCurrentNode(), 2);
      boolean showScope = !path.contains("plugin");
      boolean inDM = path.contains("dependencyManagement");
      dialog = MavenRepositorySearchDialog.createSearchDependencyDialog(sourceViewer.getTextWidget().getShell(),
          config.getType().getWindowTitle(), prj, eclPrj, inDM);
    }
    if (dialog == null) {
      throw new IllegalStateException("Wrong search type: " + config.getType());
    }
    if (config.getInitiaSearchString() != null) {
      dialog.setQuery(config.getInitiaSearchString());
    }
    final MavenRepositorySearchDialog fDialog = dialog;
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
            log.error("Failed inserting parent element", e); //$NON-NLS-1$
          } catch(CoreException e) {
            log.error("Failed inserting parent element", e); //$NON-NLS-1$
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
                if (af.version != null) {
                  setText(getChild(plugin, "version"), af.version);
                }
                format(toFormat);
                generatedOffset = ((IndexedRegion)toFormat).getStartOffset();
                generatedLength = ((IndexedRegion)toFormat).getEndOffset() - generatedOffset;
              }
            }));
          } catch(IOException e) {
            log.error("Failed inserting plugin element", e); //$NON-NLS-1$
          } catch(CoreException e) {
            log.error("Failed inserting plugin element", e); //$NON-NLS-1$
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
                  if (af.version != null) {
                    setText(getChild(dependency, "version"), af.version);
                  }
                  if (fDialog.getSelectedScope() != null && !"compile".equals(fDialog.getSelectedScope())) {
                    setText(getChild(dependency, "scope"), fDialog.getSelectedScope());
                  }
                  format(toFormat);
                  generatedOffset = ((IndexedRegion)toFormat).getStartOffset();
                  generatedLength = ((IndexedRegion)toFormat).getEndOffset() - generatedOffset;
                }
              }));
            } catch(IOException e) {
              log.error("Failed inserting dependency element", e); //$NON-NLS-1$
            } catch(CoreException e) {
              log.error("Failed inserting dependency element", e); //$NON-NLS-1$
            }            
        }
      }
    }
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
