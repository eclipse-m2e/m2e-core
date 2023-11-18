/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc.
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

package org.eclipse.m2e.editor.pom;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.ARTIFACT_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCY;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.EXTENSION;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.GROUP_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.NAME;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.PARENT;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.PLUGIN;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.PROPERTIES;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.VERSION;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction;
import org.eclipse.m2e.core.ui.internal.util.XmlUtils;


/**
 * @author Eugene Kuleshov
 * @author Milos Kleint
 */
@SuppressWarnings("restriction")
public class PomHyperlinkDetector implements IHyperlinkDetector {
  @Override
  public IHyperlink[] detectHyperlinks(final ITextViewer textViewer, final IRegion region,
      boolean canShowMultipleHyperlinks) {
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
    final List<IHyperlink> hyperlinks = new ArrayList<>();
    final int offset = region.getOffset();

    XmlUtils.performOnCurrentElement(document, offset, (node, structured) -> {
      if(textViewer instanceof ISourceViewer sourceViwer) {
        IHyperlink[] links = openExternalMarkerDefinition(sourceViwer, offset);
        if(links.length > 0) {
          hyperlinks.addAll(Arrays.asList(links));
        }
      }
      //check if we have a property expression at cursor
      IHyperlink link = openPropertyDefinition(node, textViewer, offset);
      if(link != null) {
        hyperlinks.add(link);
      }
      //now check if the dependency/plugin has a version element or not, if not, try searching for it in DM/PM of effective pom
      link = openDPManagement(node, textViewer, offset);
      if(link != null) {
        hyperlinks.add(link);
      }
      //check if <module> text is selected.
      link = openModule(node, textViewer, offset);
      if(link != null) {
        hyperlinks.add(link);
      }
      link = openPOMbyID(node, textViewer);
      if(link != null) {
        hyperlinks.add(link);
      }
    });

    if(!hyperlinks.isEmpty()) {
      return hyperlinks.toArray(new IHyperlink[0]);
    }
    return null;
  }

  static ManagedArtifactRegion findManagedArtifactRegion(Node current, ITextViewer textViewer, int offset) {
    while(current != null && !(current instanceof Element)) {
      current = current.getParentNode();
    }
    if(current != null) {
      Node artNode = null;
      Node groupNode = null;
      if(ARTIFACT_ID.equals(current.getNodeName())) {
        artNode = current;
      }
      if(GROUP_ID.equals(current.getNodeName())) {
        groupNode = current;
      }
      //only on artifactid and groupid elements..
      if(artNode == null && groupNode == null) {
        return null;
      }
      Node root = current.getParentNode();
      boolean isDependency = false;
      boolean isPlugin = false;
      if(root != null) {
        String name = root.getNodeName();
        if(DEPENDENCY.equals(name)) {
          isDependency = true;
        }
        if(PLUGIN.equals(name)) {
          isPlugin = true;
        }
      } else {
        return null;
      }
      if(!isDependency && !isPlugin) {
        //some kind of other identifier
        return null;
      }
      //now see if version is missing
      NodeList childs = root.getChildNodes();
      for(int i = 0; i < childs.getLength(); i++ ) {
        Node child = childs.item(i);
        if(child instanceof Element el) {
          if(VERSION.equals(el.getNodeName())) {
            return null;
          }
          if(artNode == null && ARTIFACT_ID.equals(el.getNodeName())) {
            artNode = el;
          }
          if(groupNode == null && GROUP_ID.equals(el.getNodeName())) {
            groupNode = el;
          }
        }
      }
      if(groupNode != null && artNode != null) {
        assert groupNode instanceof IndexedRegion;
        assert artNode instanceof IndexedRegion;

        IndexedRegion groupReg = (IndexedRegion) groupNode;
        IndexedRegion artReg = (IndexedRegion) artNode;
        int startOffset = Math.min(groupReg.getStartOffset(), artReg.getStartOffset());
        int length = Math.max(groupReg.getEndOffset(), artReg.getEndOffset()) - startOffset;
        String groupId = XmlUtils.getTextValue(groupNode);
        String artifactId = XmlUtils.getTextValue(artNode);
        final MavenProject prj = XmlUtils.extractMavenProject(textViewer);
        if(prj != null) {
          //now we can create the region I guess,
          return new ManagedArtifactRegion(startOffset, length, groupId, artifactId, isDependency, isPlugin, prj);
        }
      }
    }
    return null;
  }

  public static IHyperlink createHyperlink(final ManagedArtifactRegion region) {
    return new IHyperlink() {
      @Override
      public IRegion getHyperlinkRegion() {
        return region;
      }

      @Override
      public String getHyperlinkText() {
        return NLS.bind(org.eclipse.m2e.editor.internal.Messages.PomHyperlinkDetector_link_managed,
            "" + region.groupId + ":" + region.artifactId);
      }

      @Override
      public String getTypeLabel() {
        return "pom-dependency-plugin-management"; //$NON-NLS-1$
      }

      @Override
      public void open() {
        //see if we can find the plugin in plugin management of resolved project.
        MavenProject mavprj = region.project;
        if(mavprj != null) {
          InputLocation openLocation = findLocationForManagedArtifact(region, mavprj);
          if(openLocation != null) {
            File file = XmlUtils.fileForInputLocation(openLocation, mavprj);
            if(file != null) {
              IFileStore fileStore = EFS.getLocalFileSystem().getStore(file.toURI());
              XMLEditorUtility.openXmlEditor(fileStore, openLocation.getLineNumber(), openLocation.getColumnNumber(),
                  openLocation.getSource().getModelId());
            }
          }
        }
      }
    };

  }

  private IHyperlink openDPManagement(Node current, ITextViewer textViewer, int offset) {
    final ManagedArtifactRegion region = findManagedArtifactRegion(current, textViewer, offset);
    if(region != null) {
      return createHyperlink(region);
    }
    return null;
  }

  static InputLocation findLocationForManagedArtifact(final ManagedArtifactRegion region, MavenProject mavprj) {
    Model mdl = mavprj.getModel();
    InputLocation openLocation = null;
    if(region.isDependency) {
      DependencyManagement dm = mdl.getDependencyManagement();
      if(dm != null) {
        List<Dependency> list = dm.getDependencies();
        String id = region.groupId + ":" + region.artifactId + ":"; //$NON-NLS-1$ //$NON-NLS-2$
        if(list != null) {
          for(Dependency dep : list) {
            if(dep.getManagementKey().startsWith(id)) {
              InputLocation location = dep.getLocation(ARTIFACT_ID);
              //when would this be null?
              if(location != null) {
                openLocation = location;
                break;
              }
            }
          }
        }
      }
    }
    if(region.isPlugin) {
      Build build = mdl.getBuild();
      if(build != null) {
        PluginManagement pm = build.getPluginManagement();
        if(pm != null) {
          List<Plugin> list = pm.getPlugins();
          String id = Plugin.constructKey(region.groupId, region.artifactId);
          if(list != null) {
            for(Plugin plg : list) {
              if(id.equals(plg.getKey())) {
                InputLocation location = plg.getLocation(ARTIFACT_ID);
                //when would this be null?
                if(location != null) {
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
    if(current instanceof Text node) {
      String value = node.getNodeValue();
      if(value != null && node instanceof IndexedRegion reg) {
        int index = offset - reg.getStartOffset();
        String before = value.substring(0, Math.min(index + 1, value.length()));
        String after = value.substring(Math.min(index + 1, value.length()));
        int start = before.lastIndexOf("${"); //$NON-NLS-1$
        if(before.lastIndexOf("}") > start) {//$NON-NLS-1$
          //we might be in between two expressions..
          start = -1;
        }
        int end = after.indexOf("}"); //$NON-NLS-1$
        if(after.indexOf("${") != -1 && after.indexOf("${") < end) {//$NON-NLS-1$
          //we might be in between two expressions..
          end = -1;
        }
        if(start > -1 && end > -1) {
          final int startOffset = reg.getStartOffset() + start;
          final String expr = before.substring(start) + after.substring(0, end + 1);
          final int length = expr.length();
          final String prop = before.substring(start + 2) + after.substring(0, end);
// there are often properties that start with project. eg. project.build.sourceEncoding
//          if (prop.startsWith("project.") || prop.startsWith("pom.")) { //$NON-NLS-1$ //$NON-NLS-2$
//            return null; //ignore these, not in properties section.
//          }
          MavenProject prj = XmlUtils.extractMavenProject(viewer);
          if(prj != null) {
            return new ExpressionRegion(startOffset, length, prop, prj);
          }
        }
      }
    }
    return null;
  }

  public static IHyperlink createHyperlink(final ExpressionRegion region) {
    return new IHyperlink() {
      @Override
      public IRegion getHyperlinkRegion() {
        return region;
      }

      @Override
      public String getHyperlinkText() {
        return NLS.bind(org.eclipse.m2e.editor.internal.Messages.PomHyperlinkDetector_open_property, region.property);
      }

      @Override
      public String getTypeLabel() {
        return "pom-property-expression"; //$NON-NLS-1$
      }

      @Override
      public void open() {
        //see if we can find the plugin in plugin management of resolved project.
        MavenProject mavprj = region.project;
        if(mavprj != null) {
          //TODO get rid of InputLocation here and use the dom tree to find the property
          Model mdl = mavprj.getModel();
          InputLocation location = null;
          if(mdl.getProperties().containsKey(region.property)) {
            location = mdl.getLocation(PROPERTIES).getLocation(region.property);
          } else if(region.property != null && region.property.startsWith("project.")) {//$NON-NLS-1$
            if("project.version".equals(region.property)) {
              location = mdl.getLocation(VERSION);
            } else if("project.name".equals(region.property)) {
              location = mdl.getLocation(NAME);
            }
          }
          if(location != null) {
            File file = XmlUtils.fileForInputLocation(location, mavprj);
            if(file != null) {
              IFileStore fileStore = EFS.getLocalFileSystem().getStore(file.toURI());
              XMLEditorUtility.openXmlEditor(fileStore, location.getLineNumber(), location.getColumnNumber(),
                  location.getSource().getModelId());
            }
          }
        }
      }
    };
  }

  public static boolean canCreateHyperLink(final ExpressionRegion region) {
    if("project.version".equals(region.property) || "project.name".equals(region.property)) {
      return true;
    }
    return region.project != null && region.project.getModel().getProperties().containsKey(region.property);
  }

  //only create the hyperlink when the origin location for jumping is present.
  //in some cases (managed version comes from imported dependencies) we don't have the location and have nowhere to jump)
  public static boolean canCreateHyperLink(final ManagedArtifactRegion region) {
    return region.project != null
        && PomHyperlinkDetector.findLocationForManagedArtifact(region, region.project) != null;
  }

  static IHyperlink[] openExternalMarkerDefinition(ISourceViewer sourceViewer, int offset) {
    List<IHyperlink> toRet = new ArrayList<>();
    MarkerRegion[] regions = findMarkerRegions(sourceViewer, offset);
    for(MarkerRegion reg : regions) {
      if(reg.isDefinedInParent()) {
        toRet.add(createHyperlink(reg));
      }
    }
    return toRet.toArray(new IHyperlink[0]);
  }

  static MarkerRegion[] findMarkerRegions(ISourceViewer sourceViewer, int offset) {
    List<MarkerRegion> toRet = new ArrayList<>();
    IAnnotationModel model = sourceViewer.getAnnotationModel();
    if(model != null) { //eg. in tests
      Iterator<Annotation> it = model.getAnnotationIterator();
      while(it.hasNext()) {
        Annotation ann = it.next();
        if(ann instanceof MarkerAnnotation marker) {
          Position pos = sourceViewer.getAnnotationModel().getPosition(ann);
          if(pos.includes(offset)) {
            toRet.add(new MarkerRegion(pos.getOffset(), pos.getLength(), marker));
          }
        }
      }
    }
    return toRet.toArray(new MarkerRegion[0]);
  }

  public static IHyperlink createHyperlink(final MarkerRegion mark) {
    return new IHyperlink() {

      @Override
      public IRegion getHyperlinkRegion() {
        return new Region(mark.getOffset(), mark.getLength());
      }

      @Override
      public String getTypeLabel() {
        return "marker-error-defined-in-parent"; //$NON-NLS-1$;
      }

      @Override
      public String getHyperlinkText() {
        return NLS.bind("Open definition in parent for {0}", mark.getAnnotation().getText()); //TODO if there are multiple markers in one spot, how to differentiate better..
      }

      @Override
      public void open() {
        IMarker marker = mark.getAnnotation().getMarker();
        String loc = marker.getAttribute(IMavenConstants.MARKER_CAUSE_RESOURCE_PATH, null);
        if(loc != null) {
          IFileStore fileStore = EFS.getLocalFileSystem().getStore(IPath.fromOSString(loc));
          int row = marker.getAttribute(IMavenConstants.MARKER_CAUSE_LINE_NUMBER, 0);
          int column = marker.getAttribute(IMavenConstants.MARKER_CAUSE_COLUMN_START, 0);
          String name = marker.getAttribute(IMavenConstants.MARKER_CAUSE_RESOURCE_ID, null);
//          String hint = marker.getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, null);
//          if (IMavenConstants.EDITOR_HINT_NOT_COVERED_MOJO_EXECUTION.equals(hint)) {
//          }
          XMLEditorUtility.openXmlEditor(fileStore, row, column, name);
        }
      }
    };
  }

  private IHyperlink openPropertyDefinition(Node current, ITextViewer viewer, int offset) {
    final ExpressionRegion region = findExpressionRegion(current, viewer, offset);
    if(region != null && canCreateHyperLink(region)) {
      return createHyperlink(region);
    }
    return null;
  }

  private IHyperlink openModule(Node current, ITextViewer textViewer, int offset) {
    while(current != null && !(current instanceof Element)) {
      current = current.getParentNode();
    }
    if(current == null) {
      return null;
    }
    String pathUp = XmlUtils.pathUp(current, 2);
    if(!"modules/module".equals(pathUp)) { //$NON-NLS-1$
      //just in case we are in some random plugin configuration snippet..
      return null;
    }

    ITextFileBuffer buf = FileBuffers.getTextFileBufferManager().getTextFileBuffer(textViewer.getDocument());
    if(buf == null) {
      //for repository based poms..
      return null;
    }
    IFileStore folder = buf.getFileStore().getParent();

    String path = XmlUtils.getTextValue(current);
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
    if(!fileStore.fetchInfo().exists()) {
      return null;
    }
    assert current instanceof IndexedRegion;
    final IndexedRegion region = (IndexedRegion) current;

    return new IHyperlink() {
      @Override
      public IRegion getHyperlinkRegion() {
        return new Region(region.getStartOffset(), region.getEndOffset() - region.getStartOffset());
      }

      @Override
      public String getHyperlinkText() {
        return NLS.bind(org.eclipse.m2e.editor.internal.Messages.PomHyperlinkDetector_open_module, fPath);
      }

      @Override
      public String getTypeLabel() {
        return "pom-module"; //$NON-NLS-1$
      }

      @Override
      public void open() {
        XMLEditorUtility.openXmlEditor(fileStore);
      }
    };
  }

  private IHyperlink openPOMbyID(Node current, final ITextViewer viewer) {
    while(current != null && !(current instanceof Element)) {
      current = current.getParentNode();
    }
    if(current == null) {
      return null;
    }
    current = current.getParentNode();
    if(current == null || !(current instanceof Element parent)) {
      return null;
    }
    String parentName = parent.getNodeName();
    if(DEPENDENCY.equals(parentName) || PARENT.equals(parentName) || PLUGIN.equals(parentName)
        || "reportPlugin".equals(parentName) || EXTENSION.equals(parentName)) {
      final Node groupId = XmlUtils.findChild(parent, GROUP_ID);
      final Node artifactId = XmlUtils.findChild(parent, ARTIFACT_ID);
      final Node version = XmlUtils.findChild(parent, VERSION);
      final MavenProject prj = XmlUtils.extractMavenProject(viewer);

      return new IHyperlink() {
        @Override
        public IRegion getHyperlinkRegion() {
          //the goal here is to have the groupid/artifactid/version combo underscored by the link.
          //that will prevent underscoring big portions (like plugin config) underscored and
          // will also handle cases like dependencies within plugins.
          int max = groupId != null ? ((IndexedRegion) groupId).getEndOffset() : Integer.MIN_VALUE;
          int min = groupId != null ? ((IndexedRegion) groupId).getStartOffset() : Integer.MAX_VALUE;
          max = Math.max(max, artifactId != null ? ((IndexedRegion) artifactId).getEndOffset() : Integer.MIN_VALUE);
          min = Math.min(min, artifactId != null ? ((IndexedRegion) artifactId).getStartOffset() : Integer.MAX_VALUE);
          max = Math.max(max, version != null ? ((IndexedRegion) version).getEndOffset() : Integer.MIN_VALUE);
          min = Math.min(min, version != null ? ((IndexedRegion) version).getStartOffset() : Integer.MAX_VALUE);
          return new Region(min, max - min);
        }

        @Override
        public String getHyperlinkText() {
          return NLS.bind(org.eclipse.m2e.editor.internal.Messages.PomHyperlinkDetector_hyperlink_pattern,
              XmlUtils.getTextValue(groupId), XmlUtils.getTextValue(artifactId));
        }

        @Override
        public String getTypeLabel() {
          return "pom"; //$NON-NLS-1$
        }

        @Override
        public void open() {
          new Job(org.eclipse.m2e.editor.internal.Messages.PomHyperlinkDetector_job_name) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
              // TODO resolve groupId if groupId==null
              String gridString = groupId == null ? "org.apache.maven.plugins" : XmlUtils.getTextValue(groupId); //$NON-NLS-1$
              String artidString = artifactId == null ? null : XmlUtils.getTextValue(artifactId);
              String versionString = version == null ? null : XmlUtils.getTextValue(version);
              if(prj != null && gridString != null && artidString != null
                  && (versionString == null || versionString.contains("${"))) { //$NON-NLS-1$
                //TODO how do we decide here if the hyperlink is a dependency or a plugin
                // hyperlink??
                versionString = PomTemplateContext.extractVersion(prj, null, versionString, gridString, artidString,
                    PomTemplateContext.EXTRACT_STRATEGY_DEPENDENCY);
              }
              if(versionString == null) {
                return Status.OK_STATUS;
              }
              OpenPomAction.openEditor(gridString, artidString, versionString, prj, monitor);
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
    }
    return null;
  }

  public static class ExpressionRegion implements IRegion {

    final String property;

    private final int length;

    private final int offset;

    final MavenProject project;

    public ExpressionRegion(int startOffset, int length, String prop, MavenProject project) {
      this.offset = startOffset;
      this.length = length;
      this.property = prop;
      this.project = project;
      assert project != null;
    }

    @Override
    public int getLength() {
      return length;
    }

    @Override
    public int getOffset() {
      return offset;
    }
  }

  public static class ManagedArtifactRegion implements IRegion {

    private final int length;

    private final int offset;

    final MavenProject project;

    final String groupId;

    final String artifactId;

    final boolean isPlugin;

    final boolean isDependency;

    public ManagedArtifactRegion(int startOffset, int length, String groupId, String artifactId, boolean isDependency,
        boolean isPlugin, MavenProject project) {
      this.offset = startOffset;
      this.length = length;
      this.project = project;
      assert project != null;
      this.artifactId = artifactId;
      this.groupId = groupId;
      this.isDependency = isDependency;
      this.isPlugin = isPlugin;
    }

    @Override
    public int getLength() {
      return length;
    }

    @Override
    public int getOffset() {
      return offset;
    }
  }

  public static class MarkerRegion implements IRegion {

    private final MarkerAnnotation ann;

    final int offset;

    final int length;

    public MarkerRegion(int offset, int length, MarkerAnnotation applicable) {
      this.offset = offset;
      this.length = length;
      this.ann = applicable;
    }

    @Override
    public int getLength() {
      return length;
    }

    @Override
    public int getOffset() {
      return offset;
    }

    public MarkerAnnotation getAnnotation() {
      return ann;
    }

    public boolean isDefinedInParent() {
      IMarker mark = ann.getMarker();
      String isElsewhere = mark.getAttribute(IMavenConstants.MARKER_CAUSE_RESOURCE_PATH, null);
      return isElsewhere != null;
    }

  }

}
