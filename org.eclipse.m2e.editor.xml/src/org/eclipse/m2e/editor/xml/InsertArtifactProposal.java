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

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.ARTIFACT_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.BUILD;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.CLASSIFIER;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCIES;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCY;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCY_MANAGEMENT;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.GROUP_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.PARENT;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.PLUGIN;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.PLUGINS;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.PLUGIN_MANAGEMENT;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.PROFILE;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.RELATIVE_PATH;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.SCOPE;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.TYPE;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.VERSION;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.createElement;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.elementAtOffset;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.format;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.insertAt;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.performOnDOMDocument;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.setText;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.internal.index.IIndex;
import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.ui.internal.dialogs.MavenRepositorySearchDialog;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;
import org.eclipse.m2e.core.ui.internal.util.XmlUtils;
import org.eclipse.m2e.editor.xml.internal.Messages;


public class InsertArtifactProposal implements ICompletionProposal, ICompletionProposalExtension4,
    ICompletionProposalExtension5 {
  private static final Logger log = LoggerFactory.getLogger(InsertArtifactProposal.class);

  private final ITextViewer sourceViewer;

  private final Region region;

  private int generatedLength = 0;

  private int generatedOffset;

  private final Configuration config;

  public InsertArtifactProposal(ITextViewer sourceViewer, Region region, Configuration config) {
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
    if(config.getType() == SearchType.PLUGIN) {
      String path = XmlUtils.pathUp(config.getCurrentNode(), 2);
      boolean inPM = path.contains("pluginManagement"); //$NON-NLS-1$
      dialog = MavenRepositorySearchDialog.createSearchPluginDialog(sourceViewer.getTextWidget().getShell(), config
          .getType().getWindowTitle(), prj, eclPrj, inPM);
    }
    if(config.getType() == SearchType.PARENT) {
      dialog = MavenRepositorySearchDialog.createSearchParentDialog(sourceViewer.getTextWidget().getShell(), config
          .getType().getWindowTitle(), prj, eclPrj);
    }
    if(config.getType() == SearchType.DEPENDENCY) {
      //only populate the lists when in dependency search..
      // and when in dependency management or plugin section use the different set than elsewhere to get different visual effect.
      String path = XmlUtils.pathUp(config.getCurrentNode(), 2);
      boolean inDM = path.contains(DEPENDENCY_MANAGEMENT);
      dialog = MavenRepositorySearchDialog.createSearchDependencyDialog(sourceViewer.getTextWidget().getShell(), config
          .getType().getWindowTitle(), prj, eclPrj, inDM);
    }
    if(dialog == null) {
      throw new IllegalStateException("Wrong search type: " + config.getType());
    }
    if(config.getInitiaSearchString() != null) {
      dialog.setQuery(config.getInitiaSearchString());
    }
    final MavenRepositorySearchDialog fDialog = dialog;
    if(dialog.open() == Window.OK) {
      final IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
      int offset = region.getOffset();
      if(af != null) {
        if(config.getType() == SearchType.PARENT) {
          try {
            final int fOffset = offset;
            performOnDOMDocument(new OperationTuple(document, (Operation) doc -> {
              Element parent = insertAt(doc.createElement(PARENT), fOffset);
              setText(getChild(parent, GROUP_ID), af.group);
              setText(getChild(parent, ARTIFACT_ID), af.artifact);
              setText(getChild(parent, VERSION), af.version);
              String relativePath = PomContentAssistProcessor.findRelativePath(sourceViewer, af.group, af.artifact,
                  af.version);
              if(relativePath != null) {
                setText(getChild(parent, RELATIVE_PATH), relativePath);
              }
              format(parent);
              generatedOffset = ((IndexedRegion) parent).getStartOffset();
              generatedLength = ((IndexedRegion) parent).getEndOffset() - generatedOffset;
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
            performOnDOMDocument(new OperationTuple(document, (Operation) doc -> {
              Element currentNode = elementAtOffset(doc, fOffset);
              if(currentNode == null) {
                return;
              }
              String currentName = currentNode.getNodeName();
              Element plugin = null;
              Element toFormat = null;
              if("project".equals(currentName)) { //$NON-NLS-1$
                Element build = findChild(currentNode, BUILD);
                if(build == null) {
                  build = insertAt(doc.createElement(BUILD), fOffset);
                  toFormat = build;
                }
                plugin = createElement(getChild(build, PLUGINS), PLUGIN);
              }
              if(BUILD.equals(currentName) || PLUGIN_MANAGEMENT.equals(currentName)) {
                Element plugins = findChild(currentNode, PLUGINS);
                if(plugins == null) {
                  plugins = insertAt(doc.createElement(PLUGINS), fOffset);
                  toFormat = plugins;
                }
                plugin = createElement(plugins, PLUGIN);
              }
              if(PLUGINS.equals(currentName)) {
                plugin = insertAt(doc.createElement(PLUGIN), fOffset);
              }
              if(toFormat == null) {
                toFormat = plugin;
              }
              setText(getChild(plugin, GROUP_ID), af.group);
              setText(getChild(plugin, ARTIFACT_ID), af.artifact);
              if(af.version != null) {
                setText(getChild(plugin, VERSION), af.version);
              }
              format(toFormat);
              generatedOffset = ((IndexedRegion) toFormat).getStartOffset();
              generatedLength = ((IndexedRegion) toFormat).getEndOffset() - generatedOffset;
            }));
          } catch(IOException e) {
            log.error("Failed inserting plugin element", e); //$NON-NLS-1$
          } catch(CoreException e) {
            log.error("Failed inserting plugin element", e); //$NON-NLS-1$
          }
        }
        // dependency type

        if(config.getType() == SearchType.DEPENDENCY) {
          try {
            final int fOffset = offset;
            performOnDOMDocument(new OperationTuple(document, (Operation) doc -> {
              Element currentNode = elementAtOffset(doc, fOffset);
              if(currentNode == null) {
                return;
              }
              String currentName = currentNode.getNodeName();
              Element dependency = null;
              Element toFormat = null;
              if("project".equals(currentName) || DEPENDENCY_MANAGEMENT.equals(currentName) || PROFILE.equals(currentName)) { //$NON-NLS-1$
                Element deps = findChild(currentNode, DEPENDENCIES);
                if(deps == null) {
                  deps = insertAt(doc.createElement(DEPENDENCIES), fOffset);
                  toFormat = deps;
                }
                dependency = doc.createElement(DEPENDENCY);
                deps.appendChild(dependency);
              }
              if(DEPENDENCIES.equals(currentName)) {
                dependency = insertAt(doc.createElement(DEPENDENCY), fOffset);
              }
              if(toFormat == null) {
                toFormat = dependency;
              }
              setText(getChild(dependency, GROUP_ID), af.group);
              setText(getChild(dependency, ARTIFACT_ID), af.artifact);
              if(af.version != null) {
                setText(getChild(dependency, VERSION), af.version);
              }
              if(fDialog.getSelectedScope() != null && !"compile".equals(fDialog.getSelectedScope())) {
                setText(getChild(dependency, SCOPE), fDialog.getSelectedScope());
              }
              if(af.type != null && !"jar".equals(af.type) && !"null".equals(af.type)) { // guard against MNGECLIPSE-622 //$NON-NLS-1$)
                setText(getChild(dependency, TYPE), af.type);
              }
              if(af.classifier != null) {
                setText(getChild(dependency, CLASSIFIER), af.classifier);
              }
              format(toFormat);
              generatedOffset = ((IndexedRegion) toFormat).getStartOffset();
              generatedLength = ((IndexedRegion) toFormat).getEndOffset() - generatedOffset;
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
   *
   * @author mkleint
   */
  public enum SearchType {

    PARENT(IIndex.SEARCH_PARENTS, Messages.InsertArtifactProposal_searchDialog_title,
        Messages.InsertArtifactProposal_display_name, MvnImages.IMG_OPEN_POM,
        Messages.InsertArtifactProposal_additionals), PLUGIN(IIndex.SEARCH_PLUGIN,
        Messages.InsertArtifactProposal_insert_plugin_title,
        Messages.InsertArtifactProposal_insert_plugin_display_name, MvnImages.IMG_OPEN_POM,
        Messages.InsertArtifactProposal_insert_plugin_description), DEPENDENCY(IIndex.SEARCH_ARTIFACT,
        Messages.InsertArtifactProposal_insert_dep_title, Messages.InsertArtifactProposal_insert_dep_display_name,
        MvnImages.IMG_OPEN_POM, Messages.InsertArtifactProposal_insert_dep_desc);

    private final String type;

    private final String windowTitle;

    private final String displayName;

    private final Image image;

    private final String additionalInfo;

    SearchType(String type, String windowTitle, String dn, Image img, String addInfo) {
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
