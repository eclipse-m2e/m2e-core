/*******************************************************************************
 * Copyright (c) 2008-2014 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Rob Newton - added warning preferences page for disabling warnings
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.markers;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.childEquals;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.childMissingOrEqual;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChilds;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getTextValue;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.textEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputLocationTracker;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.markers.IEditorMarkerService;
import org.eclipse.m2e.core.internal.markers.IMarkerLocationService;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.internal.preferences.ProblemSeverity;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Matcher;
import org.eclipse.m2e.core.ui.internal.util.XmlUtils;
import org.eclipse.m2e.model.edit.pom.util.NodeOperation;


/**
 * a service impl used by the core module to improve marker locations and addition of our own markers
 *
 * @author mkleint
 * @since 1.16
 */
@SuppressWarnings("restriction")
public class MarkerLocationService implements IMarkerLocationService, IEditorMarkerService {
  private static final Logger log = LoggerFactory.getLogger(MarkerLocationService.class);

  private static final String XSI_SCHEMA_LOCATION = "xsi:schemaLocation"; //$NON-NLS-1$

  private static final String PROJECT_NODE = "project"; //$NON-NLS-1$

  private static final String OFFSET = "offset"; //$NON-NLS-1$

  public static final String ATTR_MANAGED_VERSION_LOCATION = "managedVersionLocation"; //$NON-NLS-1$

  @Override
  public void findLocationForMarker(final IMarker marker) {
    IDOMModel domModel = null;
    try {
      Integer lineNumber = (Integer) marker.getAttribute(IMarker.LINE_NUMBER);
      if(lineNumber == null) {
        return;
      }
      Integer columnStart = (Integer) marker.getAttribute(IMavenConstants.MARKER_COLUMN_START);
      if(columnStart == null) {
        return;
      }
      Integer columnEnd = (Integer) marker.getAttribute(IMavenConstants.MARKER_COLUMN_END);
      if(columnEnd == null) {
        return;
      }

      IFile resource = (IFile) marker.getResource();
      domModel = (IDOMModel) StructuredModelManager.getModelManager().getModelForRead(resource);
      if(domModel == null) {
        throw new IllegalArgumentException("Document is not structured: " + resource);
      }
      IStructuredDocument document = domModel.getStructuredDocument();
      int charStart = document.getLineOffset(lineNumber - 1) + columnStart - 1;
      marker.setAttribute(IMarker.CHAR_START, charStart);
      int charEnd;
      if(columnEnd > columnStart) {
        charEnd = document.getLineOffset(lineNumber - 1) + columnEnd;
      } else {
        IRegion line = document.getLineInformation(lineNumber - 1);
        charEnd = line.getOffset() + line.getLength();
      }
      marker.setAttribute(IMarker.CHAR_END, charEnd);
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      if(domModel != null) {
        domModel.releaseFromRead();
      }
    }
  }

  public void findLocationForMarker_(final IMarker marker) {

    String hint = marker.getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, null);

    if(IMavenConstants.EDITOR_HINT_NOT_COVERED_MOJO_EXECUTION.equals(hint)) {
      try {
        final boolean lookInPM = false;
        final String groupId = marker.getAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, "");
        final String artifactId = marker.getAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, "");
        final String exec = marker.getAttribute(IMavenConstants.MARKER_ATTR_EXECUTION_ID, "");
        final String goal = marker.getAttribute(IMavenConstants.MARKER_ATTR_GOAL, "");
        XmlUtils.performOnRootElement((IFile) marker.getResource(), new NodeOperation<Element>() {
            @Override
            public void process(Element root, IStructuredDocument structuredDocument) {
            Element build = findChild(root, PomEdits.BUILD);
            List<Element> candidates = new ArrayList<>();
            Element plugin = findPlugin(build, groupId, artifactId);
            if(plugin != null) {
              candidates.add(plugin);
            }
            if(lookInPM) {
              plugin = findPlugin(findChild(build, PomEdits.PLUGIN_MANAGEMENT), groupId, artifactId);
              if(plugin != null) {
                candidates.add(plugin);
              }
            }
            //look in profiles
            List<Element> profiles = findChilds(findChild(root, PomEdits.PROFILES), PomEdits.PROFILE);
            //TODO eventually we should only process the activated profiles.. but need MavenProject for it.
            for(Element profile : profiles) {
              Element profBuild = findChild(profile, PomEdits.BUILD);
              plugin = findPlugin(profBuild, groupId, artifactId);
              if(plugin != null) {
                candidates.add(plugin);
              }
              if(lookInPM) {
                plugin = findPlugin(findChild(profBuild, PomEdits.PLUGIN_MANAGEMENT), groupId, artifactId);
                if(plugin != null) {
                  candidates.add(plugin);
                }
              }
            }
            Element ourMarkerPlacement = null;
            for(Element candid : candidates) {
              Matcher match = "default".equals(exec) ? childMissingOrEqual(PomEdits.ID, "default")
                  : childEquals(PomEdits.ID, exec);
              Element execution = findChild(findChild(candid, PomEdits.EXECUTIONS), PomEdits.EXECUTION, match);
              if(execution != null) {
                Element goalEl = findChild(findChild(execution, PomEdits.GOALS), PomEdits.GOAL, textEquals(goal));
                if(goalEl != null) {
                  ourMarkerPlacement = goalEl;
                  break;
                }
                //only remember the first execution match
                if(ourMarkerPlacement == null) {
                  ourMarkerPlacement = findChild(execution, PomEdits.ID);
                  if(ourMarkerPlacement == null) { //just old plain paranoia
                    ourMarkerPlacement = execution;
                  }
                }
              }
            }
            if(ourMarkerPlacement == null) {
              plugin = candidates.size() > 0 ? candidates.get(0) : null;
              //executions not here (eg. in PM or parent PM), just mark the plugin's artifactId
              ourMarkerPlacement = findChild(plugin, PomEdits.ARTIFACT_ID);
              if(ourMarkerPlacement == null && plugin != null) { //just old plain paranoia
                ourMarkerPlacement = plugin;
              } else {
                //what are the strategies for placement when no plugin is found?
                // we could.. search pluginManagement, but it's unlikely to be there..
                ourMarkerPlacement = build != null ? build : root;
              }
            }

            annotateMarker(marker, structuredDocument, ourMarkerPlacement);
          }

          private Element findPlugin(Element build, String groupId, String artifactId) {
            Matcher grIdmatch = "org.apache.maven.plugins".equals(groupId)
                ? childMissingOrEqual(PomEdits.GROUP_ID, groupId)
                : childEquals(PomEdits.GROUP_ID, groupId);
            return findChild(findChild(build, PomEdits.PLUGINS), PomEdits.PLUGIN, grIdmatch,
                childEquals(PomEdits.ARTIFACT_ID, artifactId));
          }
        });
      } catch(IOException e) {
        log.error("Error locating marker", e);
      } catch(CoreException e) {
        log.error("Error locating marker", e);
      }
    }
  }

  private void annotateMarker(final IMarker marker, IStructuredDocument structuredDocument,
      Element ourMarkerPlacement) {
    if(ourMarkerPlacement instanceof IndexedRegion) {
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
        log.error(e.getMessage(), e);
      }
    }
  }

  @Override
  public void addEditorHintMarkers(IMavenMarkerManager markerManager, IFile pom, MavenProject mavenProject,
      String type) {
    checkForSchema(markerManager, pom, type);
    checkVarious(markerManager, pom, mavenProject, type);
  }

  /**
   * The xsi:schema info is not part of the model, it is stored in the xml only. Need to open the DOM and look for the
   * project node to see if it has this schema defined
   *
   * @param mavenMarkerManager
   * @param pomFile
   */
  static void checkForSchema(IMavenMarkerManager mavenMarkerManager, IResource pomFile, String type) {
    IDOMModel domModel = null;
    try {
      if(!(pomFile instanceof IFile)) {
        return;
      }
      domModel = (IDOMModel) StructuredModelManager.getModelManager().getModelForRead((IFile) pomFile);
      IStructuredDocument document = domModel.getStructuredDocument();

      // iterate through document regions
      documentLoop: for(IStructuredDocumentRegion documentRegion : document.getStructuredDocumentRegions()) {
        // only check tag regions
        if(DOMRegionContext.XML_TAG_NAME.equals(documentRegion.getType())) {
          for(ITextRegion textRegion : documentRegion.getRegions().toArray()) {
            // find a project tag
            if(textRegion instanceof TagNameRegion && PROJECT_NODE.equals(documentRegion.getText(textRegion))) {
              // check if schema is missing
              if(documentRegion.getText().lastIndexOf(XSI_SCHEMA_LOCATION) == -1) {
                int offset = documentRegion.getStartOffset();
                int lineNumber = document.getLineOfOffset(offset) + 1;
                IMarker marker = mavenMarkerManager.addMarker(pomFile, type,
                    org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_error_noschema, lineNumber,
                    IMarker.SEVERITY_WARNING);
                //the quick fix in the marker view needs to know the offset, since it doesn't have access to the
                //editor/source viewer
                if(marker != null) {
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
      log.error("Error checking for schema", ex); //$NON-NLS-1$
    } finally {
      if(domModel != null) {
        domModel.releaseFromRead();
      }
    }
  }

  private static void checkManagedDependencies(IMavenMarkerManager mavenMarkerManager, Element root, IResource pomFile,
      MavenProject mavenproject, String type, IStructuredDocument document) throws CoreException {
    ProblemSeverity overridingManagedVersionSeverity = getOverridingManagedVersionSeverity();
    if(ProblemSeverity.ignore.equals(overridingManagedVersionSeverity)) {
      return;
    }
    List<Element> candidates = new ArrayList<>();

    Element dependencies = findChild(root, PomEdits.DEPENDENCIES);
    if(dependencies != null) {
      for(Element el : findChilds(dependencies, PomEdits.DEPENDENCY)) {
        Element version = findChild(el, PomEdits.VERSION);
        if(version != null) {
          candidates.add(el);
        }
      }
    }
    //we should also consider <dependencies> section in the profiles, but profile are optional and so is their
    // dependencyManagement section.. that makes handling our markers more complex.
    // see MavenProject.getInjectedProfileIds() for a list of currently active profiles in effective pom
    String currentProjectKey = mavenproject.getGroupId() + ":" + mavenproject.getArtifactId() + ":" //$NON-NLS-1$//$NON-NLS-2$
        + mavenproject.getVersion();
    List<String> activeprofiles = mavenproject.getInjectedProfileIds().get(currentProjectKey);
    //remember what profile we found the dependency in.
    Map<Element, String> candidateProfile = new HashMap<>();
    Element profiles = findChild(root, PomEdits.PROFILES);
    if(profiles != null) {
      for(Element profile : findChilds(profiles, PomEdits.PROFILE)) {
        String idString = getTextValue(findChild(profile, PomEdits.ID));
        if(idString != null && activeprofiles.contains(idString)) {
          dependencies = findChild(profile, PomEdits.DEPENDENCIES);
          if(dependencies != null) {
            for(Element el : findChilds(dependencies, PomEdits.DEPENDENCY)) {
              Element version = findChild(el, PomEdits.VERSION);
              if(version != null) {
                candidates.add(el);
                candidateProfile.put(el, idString);
              }
            }
          }
        }
      }
    }
    //collect the managed dep ids
    Map<String, Dependency> managed = new HashMap<>();
    DependencyManagement dm = mavenproject.getDependencyManagement();
    if(dm != null) {
      List<Dependency> deps = dm.getDependencies();
      if(deps != null) {
        for(Dependency dep : deps) {
          if(dep.getVersion() != null) { //#335366
            //355882 use dep.getManagementKey() to prevent false positives
            //when type or classifier doesn't match
            managed.put(dep.getManagementKey(), dep);
          }
        }
      }
    }

    //now we have all the candidates, match them against the effective managed set
    for(Element dep : candidates) {
      Element version = findChild(dep, PomEdits.VERSION);
      String grpString = getTextValue(findChild(dep, PomEdits.GROUP_ID));
      String artString = getTextValue(findChild(dep, PomEdits.ARTIFACT_ID));
      String versionString = getTextValue(version);
      if(grpString != null && artString != null && versionString != null) {
        String typeString = getTextValue(findChild(dep, PomEdits.TYPE));
        String classifier = getTextValue(findChild(dep, PomEdits.CLASSIFIER));
        String id = getDependencyKey(grpString, artString, typeString, classifier);
        if(managed.containsKey(id)) {
          Dependency managedDep = managed.get(id);
          String managedVersion = managedDep == null ? null : managedDep.getVersion();
          if(version instanceof IndexedRegion) {
            IndexedRegion off = (IndexedRegion) version;
            if(lookForIgnoreMarker(document, version, off, IMavenConstants.MARKER_IGNORE_MANAGED)) {
              continue;
            }
            String msg = versionString.equals(managedVersion)
                ? org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_redundant_managed_title
                : org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_managed_title;
            IMarker mark = mavenMarkerManager.addMarker(pomFile, type, NLS.bind(msg, managedVersion, artString),
                document.getLineOfOffset(off.getStartOffset()) + 1, overridingManagedVersionSeverity.getSeverity());
            mark.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT,
                IMavenConstants.EDITOR_HINT_MANAGED_DEPENDENCY_OVERRIDE);
            mark.setAttribute(IMarker.CHAR_START, off.getStartOffset());
            mark.setAttribute(IMarker.CHAR_END, off.getEndOffset());
            mark.setAttribute("problemType", "pomhint"); //only important in case we enable the generic xml quick fixes //$NON-NLS-1$ //$NON-NLS-2$
            //add these attributes to easily and deterministically find the declaration in question
            mark.setAttribute("groupId", grpString); //$NON-NLS-1$
            mark.setAttribute("artifactId", artString); //$NON-NLS-1$
            setManagedVersionAttributes(mark, mavenproject, managedDep);
            String profile = candidateProfile.get(dep);
            if(profile != null) {
              mark.setAttribute("profile", profile); //$NON-NLS-1$
            }
          }
        }
      }
    }
  }

  private static void setManagedVersionAttributes(IMarker mark, MavenProject mavenproject,
      InputLocationTracker dependencyOrPlugin) throws CoreException {
    InputLocation loc = dependencyOrPlugin == null ? null : dependencyOrPlugin.getLocation("version");
    File file = loc == null ? null : XmlUtils.fileForInputLocation(loc, mavenproject);

    if(file != null) {
      mark.setAttribute(ATTR_MANAGED_VERSION_LOCATION, file.toURI().toString());
      int lineNumber = loc != null ? loc.getLineNumber() : -1;
      if(lineNumber > 0) {
        mark.setAttribute("managedVersionLine", lineNumber);
      }
      int columnNumber = loc != null ? loc.getColumnNumber() : -1;
      if(columnNumber > 0) {
        mark.setAttribute("managedVersionColumn", columnNumber);
      }
    }
  }

  private static String getDependencyKey(String groupId, String artifactId, String type, String classifier) {
    StringBuilder key = new StringBuilder(groupId).append(":").append(artifactId).append(":") //$NON-NLS-1$ //$NON-NLS-2$
        .append(type == null ? "jar" : type);//$NON-NLS-1$
    if(classifier != null) {
      key.append(":").append(classifier);//$NON-NLS-1$
    }
    return key.toString();
  }

  private static void checkManagedPlugins(IMavenMarkerManager mavenMarkerManager, Element root, IResource pomFile,
      MavenProject mavenproject, String type, IStructuredDocument document) throws CoreException {
    ProblemSeverity overridingManagedVersionSeverity = getOverridingManagedVersionSeverity();
    if(ProblemSeverity.ignore.equals(overridingManagedVersionSeverity)) {
      return;
    }
    List<Element> candidates = new ArrayList<>();
    Element build = findChild(root, PomEdits.BUILD);
    if(build == null) {
      return;
    }
    Element plugins = findChild(build, PomEdits.PLUGINS);
    if(plugins != null) {
      for(Element el : findChilds(plugins, PomEdits.PLUGIN)) {
        Element version = findChild(el, PomEdits.VERSION);
        if(version != null) {
          candidates.add(el);
        }
      }
    }
    //we should also consider <plugins> section in the profiles, but profile are optional and so is their
    // pluginManagement section.. that makes handling our markers more complex.
    // see MavenProject.getInjectedProfileIds() for a list of currently active profiles in effective pom
    String currentProjectKey = mavenproject.getGroupId() + ":" + mavenproject.getArtifactId() + ":" //$NON-NLS-1$//$NON-NLS-2$
        + mavenproject.getVersion();
    List<String> activeprofiles = mavenproject.getInjectedProfileIds().get(currentProjectKey);
    //remember what profile we found the dependency in.
    Map<Element, String> candidateProfile = new HashMap<>();
    Element profiles = findChild(root, PomEdits.PROFILES);
    if(profiles != null) {
      for(Element profile : findChilds(profiles, PomEdits.PROFILE)) {
        String idString = getTextValue(findChild(profile, PomEdits.ID));
        if(idString != null && activeprofiles.contains(idString)) {
          build = findChild(profile, PomEdits.BUILD);
          if(build == null) {
            continue;
          }
          plugins = findChild(build, PomEdits.PLUGINS);
          if(plugins != null) {
            for(Element el : findChilds(plugins, PomEdits.PLUGIN)) {
              Element version = findChild(el, PomEdits.VERSION);
              if(version != null) {
                candidates.add(el);
                candidateProfile.put(el, idString);
              }
            }
          }
        }
      }
    }
    //collect the managed plugin ids
    Map<String, Plugin> managed = new HashMap<>();
    PluginManagement pm = mavenproject.getPluginManagement();
    if(pm != null) {
      List<Plugin> plgs = pm.getPlugins();
      if(plgs != null) {
        for(Plugin plg : plgs) {
          InputLocation loc = plg.getLocation("version");
          //#350203 skip plugins defined in the superpom
          String modelID = loc == null ? null : (loc.getSource() == null ? null : loc.getSource().getModelId());
          if(loc != null && (modelID == null
              || !(modelID.startsWith("org.apache.maven:maven-model-builder:") && modelID.endsWith(":super-pom")))) {
            managed.put(plg.getKey(), plg);
          }
        }
      }
    }

    //now we have all the candidates, match them against the effective managed set
    for(Element dep : candidates) {
      String grpString = getTextValue(findChild(dep, PomEdits.GROUP_ID));
      if(grpString == null) {
        grpString = "org.apache.maven.plugins"; //$NON-NLS-1$
      }
      String artString = getTextValue(findChild(dep, PomEdits.ARTIFACT_ID));
      Element version = findChild(dep, PomEdits.VERSION);
      String versionString = getTextValue(version);
      if(artString != null && versionString != null) {
        String id = Plugin.constructKey(grpString, artString);
        if(managed.containsKey(id)) {
          Plugin managedPlugin = managed.get(id);
          String managedVersion = managedPlugin == null ? null : managedPlugin.getVersion();
          if(version instanceof IndexedRegion) {
            IndexedRegion off = (IndexedRegion) version;
            if(lookForIgnoreMarker(document, version, off, IMavenConstants.MARKER_IGNORE_MANAGED)) {
              continue;
            }

            String msg = versionString.equals(managedVersion)
                ? org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_redundant_managed_title
                : org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_managed_title;
            IMarker mark = mavenMarkerManager.addMarker(pomFile, type, NLS.bind(msg, managedVersion, artString),
                document.getLineOfOffset(off.getStartOffset()) + 1, overridingManagedVersionSeverity.getSeverity());
            mark.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT,
                IMavenConstants.EDITOR_HINT_MANAGED_PLUGIN_OVERRIDE);
            mark.setAttribute(IMarker.CHAR_START, off.getStartOffset());
            mark.setAttribute(IMarker.CHAR_END, off.getEndOffset());
            mark.setAttribute("problemType", "pomhint"); //only imporant in case we enable the generic xml quick fixes //$NON-NLS-1$ //$NON-NLS-2$
            //add these attributes to easily and deterministicaly find the declaration in question
            mark.setAttribute("groupId", grpString); //$NON-NLS-1$
            mark.setAttribute("artifactId", artString); //$NON-NLS-1$
            setManagedVersionAttributes(mark, mavenproject, managedPlugin);
            String profile = candidateProfile.get(dep);
            if(profile != null) {
              mark.setAttribute("profile", profile); //$NON-NLS-1$
            }
          }
        }
      }
    }
  }

  private static void checkParentMatchingGroupIdVersion(IMavenMarkerManager mavenMarkerManager, Element root,
      IResource pomFile, String type, IStructuredDocument document) throws CoreException {
    Element parent = findChild(root, PomEdits.PARENT);
    Element groupId = findChild(root, PomEdits.GROUP_ID);
    ProblemSeverity matchingParentGroupIdSeverity = getMatchingParentGroupIdSeverity();
    if(parent != null && groupId != null && !ProblemSeverity.ignore.equals(matchingParentGroupIdSeverity)) {
      //now compare the values of parent and project groupid..
      String parentString = getTextValue(findChild(parent, PomEdits.GROUP_ID));
      String childString = getTextValue(groupId);
      if(parentString != null && parentString.equals(childString)) {
        //now figure out the offset
        if(groupId instanceof IndexedRegion) {
          IndexedRegion off = (IndexedRegion) groupId;
          IMarker mark = mavenMarkerManager.addMarker(pomFile, type,
              org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_duplicate_groupid,
              document.getLineOfOffset(off.getStartOffset()) + 1, matchingParentGroupIdSeverity.getSeverity());
          mark.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, IMavenConstants.EDITOR_HINT_PARENT_GROUP_ID);
          mark.setAttribute(IMarker.CHAR_START, off.getStartOffset());
          mark.setAttribute(IMarker.CHAR_END, off.getEndOffset());
          mark.setAttribute("problemType", "pomhint"); //only important in case we enable the generic xml quick fixes //$NON-NLS-1$ //$NON-NLS-2$
        }
      }
    }
    Element version = findChild(root, PomEdits.VERSION);
    ProblemSeverity matchingParentVersionSeverity = getMatchingParentVersionSeverity();
    if(parent != null && version != null && !ProblemSeverity.ignore.equals(matchingParentVersionSeverity)) {
      //now compare the values of parent and project version..
      String parentString = getTextValue(findChild(parent, PomEdits.VERSION));
      String childString = getTextValue(version);
      if(parentString != null && parentString.equals(childString)) {
        //now figure out the offset
        if(version instanceof IndexedRegion) {
          IndexedRegion off = (IndexedRegion) version;
          IMarker mark = mavenMarkerManager.addMarker(pomFile, type,
              org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_duplicate_version,
              document.getLineOfOffset(off.getStartOffset()) + 1, matchingParentVersionSeverity.getSeverity());
          mark.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, IMavenConstants.EDITOR_HINT_PARENT_VERSION);
          mark.setAttribute(IMarker.CHAR_START, off.getStartOffset());
          mark.setAttribute(IMarker.CHAR_END, off.getEndOffset());
          mark.setAttribute("problemType", "pomhint"); //only important in case we enable the generic xml quick fixes //$NON-NLS-1$ //$NON-NLS-2$
        }
      }
    }
  }

  private static ProblemSeverity getMatchingParentGroupIdSeverity() {
    return ProblemSeverity.get(M2EUIPluginActivator.getDefault().getPreferenceStore()
        .getString(MavenPreferenceConstants.P_DUP_OF_PARENT_GROUPID_PB));
  }

  private static ProblemSeverity getMatchingParentVersionSeverity() {
    return ProblemSeverity.get(M2EUIPluginActivator.getDefault().getPreferenceStore()
        .getString(MavenPreferenceConstants.P_DUP_OF_PARENT_VERSION_PB));
  }

  private static ProblemSeverity getOverridingManagedVersionSeverity() {
    return ProblemSeverity.get(M2EUIPluginActivator.getDefault().getPreferenceStore()
        .getString(MavenPreferenceConstants.P_OVERRIDING_MANAGED_VERSION_PB));
  }

  /**
   * @param mavenMarkerManager
   * @param pomFile
   * @param mavenProject can be null
   */
  static void checkVarious(IMavenMarkerManager mavenMarkerManager, IResource pomFile, MavenProject mavenProject,
      String type) {
    IDOMModel domModel = null;
    try {
      if(!(pomFile instanceof IFile)) {
        return;
      }
      domModel = (IDOMModel) StructuredModelManager.getModelManager().getModelForRead((IFile) pomFile);
      IStructuredDocument document = domModel.getStructuredDocument();
      Element root = domModel.getDocument().getDocumentElement();

      if(root != null && "project".equals(root.getNodeName())) { //$NON-NLS-1$
        //now check parent version and groupid against the current project's ones..
        checkParentMatchingGroupIdVersion(mavenMarkerManager, root, pomFile, type, document);
        if(mavenProject != null) {
          checkManagedDependencies(mavenMarkerManager, root, pomFile, mavenProject, type, document);
          checkManagedPlugins(mavenMarkerManager, root, pomFile, mavenProject, type, document);
        }
      }
    } catch(Exception t) {
      log.error("Error checking for warnings", t); //$NON-NLS-1$
    } finally {
      if(domModel != null) {
        domModel.releaseFromRead();
      }
    }
  }

  private static boolean lookForIgnoreMarker(IStructuredDocument document, Element version, IndexedRegion off,
      String ignoreString) {
    Node reg = version;
    int line = document.getLineOfOffset(off.getStartOffset());
    try {
      int lineend = document.getLineOffset(line) + document.getLineLength(line) - 1;
      int start = off.getStartOffset();
      while(reg != null && start < lineend) {
        reg = reg.getNextSibling();
        if(reg instanceof Comment) {
          Comment comm = (Comment) reg;
          String data = comm.getData();
          if(data != null && data.contains(ignoreString)) {
            return true;
          }
        }
        if(reg != null) {
          start = ((IndexedRegion) reg).getStartOffset();
        }
      }
    } catch(BadLocationException ex) {
      //not possible IMHO we ask for line offset of line we know is in the document.
    }
    return false;
  }

}
