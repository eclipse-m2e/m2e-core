package org.eclipse.m2e.editor.xml;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatterExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.project.MavenMarkerManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.dialogs.MavenRepositorySearchDialog;
import org.eclipse.m2e.editor.xml.InsertArtifactProposal.Configuration;
import org.eclipse.m2e.editor.xml.internal.Messages;

public class InsertArtifactProposal implements ICompletionProposal, ICompletionProposalExtension4, ICompletionProposalExtension5 {

  private ISourceViewer sourceViewer;
  private Region region;
  private int generatedLength = 0;
  private int generatedOffset;
  private Configuration config;
  private PomStructuredTextViewConfiguration textConfig;

  public InsertArtifactProposal(ISourceViewer sourceViewer, Region region, Configuration config, PomStructuredTextViewConfiguration config2) {
    this.sourceViewer = sourceViewer;
    this.region = region;
    generatedOffset = region.getOffset();
    this.config = config;
    this.textConfig = config2;
    assert config.getType() != null;
  }

  public void apply(IDocument document) {
    IProject prj = PomContentAssistProcessor.extractProject(sourceViewer);
    Set<ArtifactKey> managedKeys = new HashSet<ArtifactKey>();
    Set<ArtifactKey> usedKeys = new HashSet<ArtifactKey>();
    if (prj != null) {
      IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager().getProject(prj);
      if (facade != null) {
        MavenProject mp = facade.getMavenProject();
        if (mp != null) {
          PluginManagement pm = mp.getPluginManagement();
          if (pm != null && pm.getPlugins() != null) {
            for (Plugin plug : pm.getPlugins()) {
              managedKeys.add(new ArtifactKey(plug.getGroupId(), plug.getArtifactId(), plug.getVersion(), null));
            }
          }
        }
      }
    }
    //TODO also collect the used plugin's list
    
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
            
            IContentFormatter formatter = textConfig.getContentFormatter(sourceViewer);
            Region resRegion = format(formatter, document, generatedOffset, generatedLength);
            generatedOffset = resRegion.getOffset();
            generatedLength = resRegion.getLength(); 
          } catch(BadLocationException e) {
            MavenLogger.log("Failed inserting parent element", e); //$NON-NLS-1$
          }
        }
        if (config.getType() == SearchType.PLUGIN) {
          Node current = config.getCurrentNode();
          if ("project".equals(current.getNodeName())) { //$NON-NLS-1$
            //in project section go with build/plugins.
            Element build = MavenMarkerManager.findChildElement((Element)current, "build"); //$NON-NLS-1$
            if (build == null) {
              try {
                StringBuffer buffer = new StringBuffer();
                generateBuild(buffer, lineDelim, af, skipVersion(current, af, managedKeys));
                generatedLength = buffer.toString().length();
                document.replace(offset, 0, buffer.toString());
                
                IContentFormatter formatter = textConfig.getContentFormatter(sourceViewer);
                Region resRegion = format(formatter, document, generatedOffset, generatedLength);
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
            Element plugins = MavenMarkerManager.findChildElement((Element)current, "plugins"); //$NON-NLS-1$
            if (plugins == null) {
              //we need to create it.
              try {
                StringBuffer buffer = new StringBuffer();
                generatePlugins(buffer, lineDelim, af, skipVersion(current, af, managedKeys));
                generatedLength = buffer.toString().length();
                document.replace(offset, 0, buffer.toString());
                
                IContentFormatter formatter = textConfig.getContentFormatter(sourceViewer);
                Region resRegion = format(formatter, document, offset, generatedLength);
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
              generatePlugin(buffer, lineDelim, af, skipVersion(current, af, managedKeys));
              generatedLength = buffer.toString().length();
              document.replace(offset, 0, buffer.toString());
              IContentFormatter formatter = textConfig.getContentFormatter(sourceViewer);
              Region resRegion = format(formatter, document, offset, generatedLength);
              generatedOffset = resRegion.getOffset();
              generatedLength = resRegion.getLength();
            } catch (BadLocationException e) {
              MavenLogger.log("Failed inserting plugin element", e); //$NON-NLS-1$
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
  private boolean skipVersion(Node currentNode, IndexedArtifactFile af, Set<ArtifactKey> managedList) {
    if ("pluginManagement".equals(currentNode.getNodeName()) || "pluginManagement".equals(currentNode.getParentNode().getNodeName())) {
      return false;
    }
    ArtifactKey key = new ArtifactKey(af.group, af.artifact, af.version, null);
    return managedList.contains(key);
  }
  
  private void generatePlugin(StringBuffer buffer, String lineDelim, IndexedArtifactFile af, boolean skipVersion) {
    buffer.append("<plugin>").append(lineDelim); //$NON-NLS-1$
    buffer.append("<groupId>").append(af.group).append("</groupId>").append(lineDelim); //$NON-NLS-1$ //$NON-NLS-2$
    buffer.append("<artifactId>").append(af.artifact).append("</artifactId>").append(lineDelim); //$NON-NLS-1$ //$NON-NLS-2$
    //for managed plugins (if version matches only?), don't add the version element
    if (!skipVersion) {
      buffer.append("<version>").append(af.version).append("</version>").append(lineDelim); //$NON-NLS-1$ //$NON-NLS-2$
    }
    buffer.append("</plugin>").append(lineDelim); //$NON-NLS-1$
  }
  
  private void generatePlugins(StringBuffer buffer, String lineDelim, IndexedArtifactFile af, boolean skipVersion) {
    buffer.append("<plugins>").append(lineDelim); //$NON-NLS-1$
    generatePlugin(buffer, lineDelim, af, skipVersion);
    buffer.append("</plugins>").append(lineDelim); //$NON-NLS-1$
  }
  
  private void generateBuild(StringBuffer buffer, String lineDelim, IndexedArtifactFile af, boolean skipVersion) {
    buffer.append("<build>").append(lineDelim); //$NON-NLS-1$
    generatePlugins(buffer, lineDelim, af, skipVersion);
    buffer.append("</build>").append(lineDelim); //$NON-NLS-1$
  }  
  
  /**
   * take the document and format the region specified by the supplied formatter.
   * operates on whole line (determined by the region specified)
   * returns the new region encompassing the original region after formatting
   */
  public static Region format(IContentFormatter formatter, IDocument document, int offset, int length) throws BadLocationException {
    int startLine = document.getLineOfOffset(offset);
    int endLine = document.getLineOfOffset(offset + length - 1); // -1 to make sure to be before the end of line char
    int startLineOffset = document.getLineOffset(startLine);
    formatter.format(document, new Region(startLineOffset, (document.getLineOffset(endLine) + document.getLineLength(endLine)) - startLineOffset));
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
    PLUGIN(IIndex.SEARCH_PLUGIN, Messages.InsertArtifactProposal_insert_plugin_title, Messages.InsertArtifactProposal_insert_plugin_display_name, MvnImages.IMG_OPEN_POM, Messages.InsertArtifactProposal_insert_plugin_description);
    
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
