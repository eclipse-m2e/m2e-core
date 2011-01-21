package org.eclipse.m2e.editor.xml.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.xml.core.internal.parser.regions.TagNameRegion;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.project.IEditorMarkerService;
import org.eclipse.m2e.core.project.IMarkerLocationService;
import org.eclipse.m2e.core.project.IMavenMarkerManager;

import static org.eclipse.m2e.editor.xml.internal.PomEdits.*;

/**
 * a service impl used by the core module to improve marker locations and addition of our own markers
 * @author mkleint
 *
 */
public class MarkerLocationService implements IMarkerLocationService, IEditorMarkerService {
  private static final String XSI_SCHEMA_LOCATION = "xsi:schemaLocation"; //$NON-NLS-1$

  private static final String PROJECT_NODE = "project"; //$NON-NLS-1$
  private static final String OFFSET = "offset"; //$NON-NLS-1$

  public void findLocationForMarker(final IMarker marker) {
    
    String hint = marker.getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, null);
    if (IMavenConstants.EDITOR_HINT_UNKNOWN_PACKAGING.equals(hint)) {
      IDocument document = XmlUtils.getDocument(marker);
      XmlUtils.performOnRootElement(document, new NodeOperation<Element>() {
        public void process(Element root, IStructuredDocument structuredDocument) {
          Element markEl = findChild(root, "packaging");
          if (markEl == null) {
            markEl = root;
          }
          annotateMarker(marker, structuredDocument, markEl);
        }
      });
    }
    
    if (IMavenConstants.EDITOR_HINT_NOT_COVERED_MOJO_EXECUTION.equals(hint)) {
      IDocument document = XmlUtils.getDocument(marker);
      XmlUtils.performOnRootElement(document, new NodeOperation<Element>() {
        public void process(Element root, IStructuredDocument structuredDocument) {
          String groupId = marker.getAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, "");
          String artifactId = marker.getAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, "");
          String exec = marker.getAttribute(IMavenConstants.MARKER_ATTR_EXECUTION_ID, "");
          String goal = marker.getAttribute(IMavenConstants.MARKER_ATTR_GOAL, "");
          Element build = findChild(root, "build");
          Element plugin = findPlugin(build, groupId, artifactId);
          Element ourMarkerPlacement = null;
          if (plugin == null) {
            //look in profiles
            List<Element> profiles = findChilds(findChild(root, "profiles"), "profile");
            //TODO eventually we should only process the activated profiles.. but need MavenProject for it.
            for (Element profile : profiles) {
              Element profBuild = findChild(profile, "build");
              plugin = findPlugin(profBuild, groupId, artifactId);
              if (plugin != null) {
                //TODO what is multiple profiles have the plugin with same or different execution ids?
                break;
              }
            }
          }
          if (plugin != null) {
            Element execution = findChild(findChild(plugin, "executions"), "execution", childEquals("id", exec));
            if (execution != null) {
              Element goalEl = findChild(findChild(execution, "goals"), "goal", textEquals(goal));
              if (goalEl != null) {
                ourMarkerPlacement = goalEl;
              } else {
                ourMarkerPlacement = findChild(execution, "id");
                if (ourMarkerPlacement == null) { //just old plain paranoia
                  ourMarkerPlacement = execution;
                }
              }
            } else {
              //execution not here (eg. in PM or parent PM), just mark the plugin's artifactId
              ourMarkerPlacement = findChild(plugin, "artifactId");
              if (ourMarkerPlacement == null) { //just old plain paranoia
                ourMarkerPlacement = plugin;
              }
            }
          } else {
            //what are the strategies for placement when no plugin is found?
            // we could.. search pluginManagement, but it's unlikely to be there..
            ourMarkerPlacement = build != null ? build : root;
          }
          annotateMarker(marker, structuredDocument, ourMarkerPlacement);
        }


        private Element findPlugin(Element build, String groupId, String artifactId) {
          return findChild(findChild(build, "plugins"), "plugin", childEquals("groupId", groupId), childEquals("artifactId", artifactId));
        }
      });
    }
  }

  private void annotateMarker(final IMarker marker, IStructuredDocument structuredDocument, Element ourMarkerPlacement) {
    if (ourMarkerPlacement instanceof IndexedRegion) {
      IndexedRegion region = (IndexedRegion) ourMarkerPlacement;
      try {
        marker.setAttribute(IMarker.CHAR_START, region.getStartOffset());
        //as end, mark just the end of line where the region starts to prevent marking the entire <build> section.
        IRegion line;
        try {
          line = structuredDocument.getLineInformationOfOffset(region.getStartOffset());
          int end = Math.min(region.getEndOffset(), line.getOffset() + line.getLength());
          marker.setAttribute(IMarker.CHAR_END, end);
        } catch(BadLocationException e) {
          marker.setAttribute(IMarker.CHAR_END, region.getStartOffset() + region.getLength());
        }
        marker.setAttribute(IMarker.LINE_NUMBER, structuredDocument.getLineOfOffset(region.getStartOffset()) + 1);
      } catch(CoreException e) {
        MavenLogger.log(e);
      }
    }
  }
  

  public void addEditorHintMarkers(IMavenMarkerManager markerManager, IFile pom, MavenProject mavenProject, String type) {
    checkForSchema(markerManager, pom, type);
    checkVarious(markerManager, pom, mavenProject, type);
  }

  /**
   * The xsi:schema info is not part of the model, it is stored in the xml only. Need to open the DOM
   * and look for the project node to see if it has this schema defined
   * @param mavenMarkerManager 
   * @param pomFile
   */
  static void checkForSchema(IMavenMarkerManager mavenMarkerManager, IResource pomFile, String type) {
    IDOMModel domModel = null;
    try{
      if(!(pomFile instanceof IFile)){
        return;
      }
      domModel = (IDOMModel)StructuredModelManager.getModelManager().getModelForRead((IFile)pomFile);
      IStructuredDocument document = domModel.getStructuredDocument();
      
      // iterate through document regions
      documentLoop:for(IStructuredDocumentRegion documentRegion : document.getStructuredDocumentRegions()) {
        // only check tag regions
        if (DOMRegionContext.XML_TAG_NAME.equals(documentRegion.getType())){
          for(ITextRegion textRegion: documentRegion.getRegions().toArray()){
            // find a project tag
            if(textRegion instanceof TagNameRegion && PROJECT_NODE.equals(documentRegion.getText(textRegion))){
              // check if schema is missing
              if (documentRegion.getText().lastIndexOf(XSI_SCHEMA_LOCATION) == -1) {
                int offset = documentRegion.getStartOffset();
                int lineNumber = document.getLineOfOffset(offset) + 1;
                IMarker marker = mavenMarkerManager.addMarker(pomFile, type,
                    org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_error_noschema, lineNumber,
                    IMarker.SEVERITY_WARNING);
                //the quick fix in the marker view needs to know the offset, since it doesn't have access to the
                //editor/source viewer
                if(marker != null){
                  marker.setAttribute(OFFSET, offset);
                  marker.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT,
                      IMavenConstants.EDITOR_HINT_MISSING_SCHEMA);
                  marker.setAttribute(IMarker.CHAR_START, documentRegion.getStartOffset());
                  marker.setAttribute(IMarker.CHAR_END, documentRegion.getEndOffset());
                  marker.setAttribute("problemType", "pomhint"); //only imporant in case we enable the generic xml quick fixes //$NON-NLS-1$ //$NON-NLS-2$
                }
              }
              // there could only be one project tag
              break documentLoop;
            }
          }
        }
      }
    } catch(Exception ex) {
      MavenLogger.log("Error checking for schema", ex); //$NON-NLS-1$
    }
    finally {
      if ( domModel != null ) {
        domModel.releaseFromRead();
      }
    }
  }

  private static void checkManagedDependencies(IMavenMarkerManager mavenMarkerManager, Element root, IResource pomFile, MavenProject mavenproject, String type, IStructuredDocument document)
      throws CoreException {
    List<Element> candidates = new ArrayList<Element>();
    
    Element dependencies = findChild(root, "dependencies"); //$NON-NLS-1$
    if (dependencies != null) {
      for (Element el : findChilds(dependencies, "dependency")) { //$NON-NLS-1$
        Element version = findChild(el, "version"); //$NON-NLS-1$
        if (version != null) {
          candidates.add(el);
        }
      }
    }
    //we should also consider <dependencies> section in the profiles, but profile are optional and so is their
    // dependencyManagement section.. that makes handling our markers more complex.
    // see MavenProject.getInjectedProfileIds() for a list of currently active profiles in effective pom
    String currentProjectKey = mavenproject.getGroupId() + ":" + mavenproject.getArtifactId() + ":" + mavenproject.getVersion(); //$NON-NLS-1$ //$NON-NLS-2$
    List<String> activeprofiles = mavenproject.getInjectedProfileIds().get(currentProjectKey);
    //remember what profile we found the dependency in.
    Map<Element, String> candidateProfile = new HashMap<Element, String>();
    Element profiles = findChild(root, "profiles"); //$NON-NLS-1$
    if (profiles != null) {
      for (Element profile : findChilds(profiles, "profile")) { //$NON-NLS-1$
        String idString = getTextValue(findChild(profile, "id")); //$NON-NLS-1$
        if (idString != null && activeprofiles.contains(idString)) {
          dependencies = findChild(profile, "dependencies"); //$NON-NLS-1$
          if (dependencies != null) {
            for (Element el : findChilds(dependencies, "dependency")) { //$NON-NLS-1$
              Element version = findChild(el, "version"); //$NON-NLS-1$
              if (version != null) {
                candidates.add(el);
                candidateProfile.put(el, idString);
              }
            }
          }
        }
      }
    }
    //collect the managed dep ids
    Map<String, String> managed = new HashMap<String, String>();
    DependencyManagement dm = mavenproject.getDependencyManagement();
    if (dm != null) {
      List<Dependency> deps = dm.getDependencies();
      if (deps != null) {
        for (Dependency dep : deps) {
          //shall we be using geManagementkey() here? but it contains also the type, not only the gr+art ids..
          managed.put(dep.getGroupId() + ":" + dep.getArtifactId(), dep.getVersion()); //$NON-NLS-1$
        }
      }
    }
    
    //now we have all the candidates, match them against the effective managed set 
    for(Element dep : candidates) {
      Element version = findChild(dep, "version"); //$NON-NLS-1$
      String grpString = getTextValue(findChild(dep, "groupId")); //$NON-NLS-1$
      String artString = getTextValue(findChild(dep, "artifactId")); //$NON-NLS-1$
      String versionString = getTextValue(version);
      if(grpString != null && artString != null && versionString != null) {
        String id = grpString + ":" + artString; //$NON-NLS-1$
        if(managed.containsKey(id)) {
          String managedVersion = managed.get(id);
          if(version instanceof IndexedRegion) {
            IndexedRegion off = (IndexedRegion) version;
            if(lookForIgnoreMarker(document, version, off, IMavenConstants.MARKER_IGNORE_MANAGED)) {
              continue;
            }
  
            IMarker mark = mavenMarkerManager.addMarker(pomFile, type, NLS.bind(
                org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_managed_title, managedVersion, artString),
                document.getLineOfOffset(off.getStartOffset()) + 1, IMarker.SEVERITY_WARNING);
            mark.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT,
                IMavenConstants.EDITOR_HINT_MANAGED_DEPENDENCY_OVERRIDE);
            mark.setAttribute(IMarker.CHAR_START, off.getStartOffset());
            mark.setAttribute(IMarker.CHAR_END, off.getEndOffset());
            mark.setAttribute("problemType", "pomhint"); //only imporant in case we enable the generic xml quick fixes //$NON-NLS-1$ //$NON-NLS-2$
            //add these attributes to easily and deterministicaly find the declaration in question
            mark.setAttribute("groupId", grpString); //$NON-NLS-1$
            mark.setAttribute("artifactId", artString); //$NON-NLS-1$
            String profile = candidateProfile.get(dep);
            if(profile != null) {
              mark.setAttribute("profile", profile); //$NON-NLS-1$
            }
          }
        }
      }
    }
  }

  private static void checkManagedPlugins(IMavenMarkerManager mavenMarkerManager, Element root, IResource pomFile, MavenProject mavenproject, String type, IStructuredDocument document)
      throws CoreException {
    List<Element> candidates = new ArrayList<Element>();
    Element build = findChild(root, "build"); //$NON-NLS-1$
    if (build == null) {
      return;
    }
    Element plugins = findChild(build, "plugins"); //$NON-NLS-1$
    if (plugins != null) {
      for (Element el : findChilds(plugins, "plugin")) { //$NON-NLS-1$
        Element version = findChild(el, "version"); //$NON-NLS-1$
        if (version != null) {
          candidates.add(el);
        }
      }
    }
    //we should also consider <plugins> section in the profiles, but profile are optional and so is their
    // pluginManagement section.. that makes handling our markers more complex.
    // see MavenProject.getInjectedProfileIds() for a list of currently active profiles in effective pom
    String currentProjectKey = mavenproject.getGroupId() + ":" + mavenproject.getArtifactId() + ":" + mavenproject.getVersion(); //$NON-NLS-1$ //$NON-NLS-2$
    List<String> activeprofiles = mavenproject.getInjectedProfileIds().get(currentProjectKey);
    //remember what profile we found the dependency in.
    Map<Element, String> candidateProfile = new HashMap<Element, String>();
    Element profiles = findChild(root, "profiles"); //$NON-NLS-1$
    if (profiles != null) {
      for (Element profile : findChilds(profiles, "profile")) { //$NON-NLS-1$
        String idString = getTextValue(findChild(profile, "id")); //$NON-NLS-1$
        if (idString != null && activeprofiles.contains(idString)) {
          build = findChild(profile, "build"); //$NON-NLS-1$
          if (build == null) {
            continue;
          }
          plugins = findChild(build, "plugins"); //$NON-NLS-1$
          if (plugins != null) {
            for (Element el : findChilds(plugins, "plugin")) { //$NON-NLS-1$
              Element version = findChild(el, "version"); //$NON-NLS-1$
              if (version != null) {
                candidates.add(el);
                candidateProfile.put(el, idString);
              }
            }
          }
        }
      }
    }
    //collect the managed plugin ids
    Map<String, String> managed = new HashMap<String, String>();
    PluginManagement pm = mavenproject.getPluginManagement();
    if (pm != null) {
      List<Plugin> plgs = pm.getPlugins();
      if (plgs != null) {
        for (Plugin plg : plgs) {
          managed.put(plg.getKey(), plg.getVersion());
        }
      }
    }
    
    //now we have all the candidates, match them against the effective managed set 
    for(Element dep : candidates) {
      String grpString = getTextValue(findChild(dep, "groupId")); //$NON-NLS-1$
      if (grpString == null) {
        grpString = "org.apache.maven.plugins"; //$NON-NLS-1$
      }
      String artString = getTextValue(findChild(dep, "artifactId")); //$NON-NLS-1$
      Element version = findChild(dep, "version"); //$NON-NLS-1$
      String versionString = getTextValue(version);
      if(artString != null && versionString != null) {
        String id = Plugin.constructKey(grpString, artString);
        if(managed.containsKey(id)) {
          String managedVersion = managed.get(id);
          if(version instanceof IndexedRegion) {
            IndexedRegion off = (IndexedRegion) version;
            if(lookForIgnoreMarker(document, version, off, IMavenConstants.MARKER_IGNORE_MANAGED)) {
              continue;
            }
  
            IMarker mark = mavenMarkerManager.addMarker(pomFile, type, NLS.bind(
                org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_managed_title, managedVersion, artString),
                document.getLineOfOffset(off.getStartOffset()) + 1, IMarker.SEVERITY_WARNING);
            mark.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT,
                IMavenConstants.EDITOR_HINT_MANAGED_PLUGIN_OVERRIDE);
            mark.setAttribute(IMarker.CHAR_START, off.getStartOffset());
            mark.setAttribute(IMarker.CHAR_END, off.getEndOffset());
            mark.setAttribute("problemType", "pomhint"); //only imporant in case we enable the generic xml quick fixes //$NON-NLS-1$ //$NON-NLS-2$
            //add these attributes to easily and deterministicaly find the declaration in question
            mark.setAttribute("groupId", grpString); //$NON-NLS-1$
            mark.setAttribute("artifactId", artString); //$NON-NLS-1$
            String profile = candidateProfile.get(dep);
            if(profile != null) {
              mark.setAttribute("profile", profile); //$NON-NLS-1$
            }
          }
        }
      }
    }
  }

  private static void checkParentMatchingGroupIdVersion(IMavenMarkerManager mavenMarkerManager, Element root, IResource pomFile, String type,
      IStructuredDocument document) throws CoreException {
    Element parent = findChild(root, "parent"); //$NON-NLS-1$
    Element groupId = findChild(root, "groupId"); //$NON-NLS-1$
    if(parent != null && groupId != null) {
      //now compare the values of parent and project groupid..
      String parentString = getTextValue(findChild(parent, "groupId")); //$NON-NLS-1$
      String childString = getTextValue(groupId);
      if(parentString != null && parentString.equals(childString)) {
        //now figure out the offset
        if(groupId instanceof IndexedRegion) {
          IndexedRegion off = (IndexedRegion) groupId;
          IMarker mark = mavenMarkerManager.addMarker(pomFile, type,
              org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_duplicate_groupid,
              document.getLineOfOffset(off.getStartOffset()) + 1, IMarker.SEVERITY_WARNING);
          mark.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, IMavenConstants.EDITOR_HINT_PARENT_GROUP_ID);
          mark.setAttribute(IMarker.CHAR_START, off.getStartOffset());
          mark.setAttribute(IMarker.CHAR_END, off.getEndOffset());
          mark.setAttribute("problemType", "pomhint"); //only important in case we enable the generic xml quick fixes //$NON-NLS-1$ //$NON-NLS-2$
        }
      }
    }
    Element version = findChild(root, "version"); //$NON-NLS-1$
    if(parent != null && version != null) {
      //now compare the values of parent and project version..
      String parentString = getTextValue(findChild(parent, "version")); //$NON-NLS-1$
      String childString = getTextValue(version);
      if(parentString != null && parentString.equals(childString)) {
        //now figure out the offset
        if(version instanceof IndexedRegion) {
          IndexedRegion off = (IndexedRegion) version;
          IMarker mark = mavenMarkerManager.addMarker(pomFile, type,
              org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_duplicate_version,
              document.getLineOfOffset(off.getStartOffset()) + 1, IMarker.SEVERITY_WARNING);
          mark.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, IMavenConstants.EDITOR_HINT_PARENT_VERSION);
          mark.setAttribute(IMarker.CHAR_START, off.getStartOffset());
          mark.setAttribute(IMarker.CHAR_END, off.getEndOffset());
          mark.setAttribute("problemType", "pomhint"); //only important in case we enable the generic xml quick fixes //$NON-NLS-1$ //$NON-NLS-2$
        }
      }
    }
  }

  /**
   * @param mavenMarkerManager 
   * @param pomFile
   * @param mavenProject can be null
   */
  static void checkVarious(IMavenMarkerManager mavenMarkerManager, IResource pomFile, MavenProject mavenProject, String type) {
    IDOMModel domModel = null;
    try {
      if(!(pomFile instanceof IFile)) {
        return;
      }
      domModel = (IDOMModel) StructuredModelManager.getModelManager().getModelForRead((IFile) pomFile);
      IStructuredDocument document = domModel.getStructuredDocument();
      Element root = domModel.getDocument().getDocumentElement();
  
      if(root.getNodeName().equals("project")) { //$NON-NLS-1$
        //now check parent version and groupid against the current project's ones..
        checkParentMatchingGroupIdVersion(mavenMarkerManager, root, pomFile, type, document);
        if (mavenProject != null) {
          checkManagedDependencies(mavenMarkerManager, root, pomFile, mavenProject, type, document);
          checkManagedPlugins(mavenMarkerManager, root, pomFile, mavenProject, type, document);
        }
      }
    } catch(Exception t) {
      MavenLogger.log("Error checking for warnings", t); //$NON-NLS-1$
    } finally {
      if(domModel != null) {
        domModel.releaseFromRead();
      }
    }
  }
  
  private static boolean lookForIgnoreMarker(IStructuredDocument document, Element version, IndexedRegion off, String ignoreString) {
    Node reg = version;
    int line = document.getLineOfOffset(off.getStartOffset());
    try {
      int lineend = document.getLineOffset(line) + document.getLineLength(line) - 1;
      int start = off.getStartOffset();
      while (reg != null && start < lineend) {
        reg = reg.getNextSibling();
        if (reg != null && reg instanceof Comment) {
          Comment comm = (Comment)reg;
          String data =comm.getData(); 
          if (data != null && data.contains(ignoreString)) {
            return true;
          }
        }
        if (reg != null) {
            start = ((IndexedRegion)reg).getStartOffset();
        }
      }
    } catch(BadLocationException ex) {
      //not possible IMHO we ask for line offset of line we know is in the document.
    }
    return false;
  }


}
