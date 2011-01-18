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

package org.eclipse.m2e.core.internal.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.views.markers.MarkerViewUtil;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.xml.core.internal.parser.regions.TagNameRegion;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.core.Messages;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.project.IMavenMarkerManager;


@SuppressWarnings("restriction")
public class MavenMarkerManager implements IMavenMarkerManager {
  private static final String XSI_SCHEMA_LOCATION = "xsi:schemaLocation"; //$NON-NLS-1$

  private static final String PROJECT_NODE = "project"; //$NON-NLS-1$
  public static final String OFFSET = "offset"; //$NON-NLS-1$
  
  private final MavenConsole console;
  private final IMavenConfiguration mavenConfiguration; 

  public MavenMarkerManager(MavenConsole console, IMavenConfiguration mavenConfiguration) {
    this.console = console;
    this.mavenConfiguration = mavenConfiguration;
  }
  
  public void addMarkers(IResource pomFile, String type, MavenExecutionResult result) {
    List<Throwable> exceptions = result.getExceptions();
    
    for(Throwable ex : exceptions) {
      if(ex instanceof ProjectBuildingException) {
        handleProjectBuildingException(pomFile, type, (ProjectBuildingException) ex);
      } else if(ex instanceof AbstractArtifactResolutionException) {
        AbstractArtifactResolutionException rex = (AbstractArtifactResolutionException) ex;
        String errorMessage = getArtifactId(rex) + " " + getRootErrorMessage(ex); //$NON-NLS-1$
        addMarker(pomFile, type, errorMessage, 1, IMarker.SEVERITY_ERROR);
      } else {
        handleBuildException(pomFile, type, ex);
      }
    }

    DependencyResolutionResult resolutionResult = result.getDependencyResolutionResult();
    if(resolutionResult != null) {
      // @see also addMissingArtifactMarkers
      addErrorMarkers(pomFile, type, org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_metadata_resolution,
          resolutionResult.getCollectionErrors());
      for(org.sonatype.aether.graph.Dependency dependency : resolutionResult.getUnresolvedDependencies()) {
        addErrorMarkers(pomFile, type, org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_error_artifact,
            resolutionResult.getResolutionErrors(dependency));
      }
    }

    MavenProject mavenProject = result.getProject();
    if (mavenProject != null) {
      addMissingArtifactMarkers(pomFile, type, mavenProject);
    }
  }
  
  public void addEditorHintMarkers(IResource pomFile, MavenProject mavenProject, String type) {
    checkForSchema(pomFile, type);
    //mkleint: adding here but I'm sort of not entirely clear what the usage patter of this class is.
    checkVarious(pomFile, mavenProject, type);
  }

  /**
   * @param pomFile
   * @param mavenProject can be null
   */
  private void checkVarious(IResource pomFile, MavenProject mavenProject, String type) {
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
        checkParentMatchingGroupIdVersion(root, pomFile, type, document);
        if (mavenProject != null) {
          checkManagedDependencies(root, pomFile, mavenProject, type, document);
          checkManagedPlugins(root, pomFile, mavenProject, type, document);
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
  
  private void checkManagedDependencies(Element root, IResource pomFile, MavenProject mavenproject, String type, IStructuredDocument document)
      throws CoreException {
    List<Element> candidates = new ArrayList<Element>();
    
    Element dependencies = findChildElement(root, "dependencies"); //$NON-NLS-1$
    if (dependencies != null) {
      for (Element el : findChildElements(dependencies, "dependency")) { //$NON-NLS-1$
        Element version = findChildElement(el, "version"); //$NON-NLS-1$
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
    Element profiles = findChildElement(root, "profiles"); //$NON-NLS-1$
    if (profiles != null) {
      for (Element profile : findChildElements(profiles, "profile")) { //$NON-NLS-1$
        String idString = getElementTextValue(findChildElement(profile, "id")); //$NON-NLS-1$
        if (idString != null && activeprofiles.contains(idString)) {
          dependencies = findChildElement(profile, "dependencies"); //$NON-NLS-1$
          if (dependencies != null) {
            for (Element el : findChildElements(dependencies, "dependency")) { //$NON-NLS-1$
              Element version = findChildElement(el, "version"); //$NON-NLS-1$
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
      Element version = findChildElement(dep, "version"); //$NON-NLS-1$
      String grpString = getElementTextValue(findChildElement(dep, "groupId")); //$NON-NLS-1$
      String artString = getElementTextValue(findChildElement(dep, "artifactId")); //$NON-NLS-1$
      String versionString = getElementTextValue(version);
      if(grpString != null && artString != null && versionString != null) {
        String id = grpString + ":" + artString; //$NON-NLS-1$
        if(managed.containsKey(id)) {
          String managedVersion = managed.get(id);
          if(version instanceof IndexedRegion) {
            IndexedRegion off = (IndexedRegion) version;
            if(lookForIgnoreMarker(document, version, off, IMavenConstants.MARKER_IGNORE_MANAGED)) {
              continue;
            }

            IMarker mark = addMarker(pomFile, type, NLS.bind(
                org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_managed_title, managedVersion, artString),
                document.getLineOfOffset(off.getStartOffset()) + 1, IMarker.SEVERITY_WARNING, false /*isTransient*/);
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

  static boolean lookForIgnoreMarker(IStructuredDocument document, Element version, IndexedRegion off, String ignoreString) {
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
  
  private void checkManagedPlugins(Element root, IResource pomFile, MavenProject mavenproject, String type, IStructuredDocument document)
      throws CoreException {
    List<Element> candidates = new ArrayList<Element>();
    Element build = findChildElement(root, "build"); //$NON-NLS-1$
    if (build == null) {
      return;
    }
    Element plugins = findChildElement(build, "plugins"); //$NON-NLS-1$
    if (plugins != null) {
      for (Element el : findChildElements(plugins, "plugin")) { //$NON-NLS-1$
        Element version = findChildElement(el, "version"); //$NON-NLS-1$
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
    Element profiles = findChildElement(root, "profiles"); //$NON-NLS-1$
    if (profiles != null) {
      for (Element profile : findChildElements(profiles, "profile")) { //$NON-NLS-1$
        String idString = getElementTextValue(findChildElement(profile, "id")); //$NON-NLS-1$
        if (idString != null && activeprofiles.contains(idString)) {
          build = findChildElement(profile, "build"); //$NON-NLS-1$
          if (build == null) {
            continue;
          }
          plugins = findChildElement(build, "plugins"); //$NON-NLS-1$
          if (plugins != null) {
            for (Element el : findChildElements(plugins, "plugin")) { //$NON-NLS-1$
              Element version = findChildElement(el, "version"); //$NON-NLS-1$
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
      String grpString = getElementTextValue(findChildElement(dep, "groupId")); //$NON-NLS-1$
      if (grpString == null) {
        grpString = "org.apache.maven.plugins"; //$NON-NLS-1$
      }
      String artString = getElementTextValue(findChildElement(dep, "artifactId")); //$NON-NLS-1$
      Element version = findChildElement(dep, "version"); //$NON-NLS-1$
      String versionString = getElementTextValue(version);
      if(artString != null && versionString != null) {
        String id = Plugin.constructKey(grpString, artString);
        if(managed.containsKey(id)) {
          String managedVersion = managed.get(id);
          if(version instanceof IndexedRegion) {
            IndexedRegion off = (IndexedRegion) version;
            if(lookForIgnoreMarker(document, version, off, IMavenConstants.MARKER_IGNORE_MANAGED)) {
              continue;
            }

            IMarker mark = addMarker(pomFile, type, NLS.bind(
                org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_managed_title, managedVersion, artString),
                document.getLineOfOffset(off.getStartOffset()) + 1, IMarker.SEVERITY_WARNING, false /*isTransient*/);
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

  private void checkParentMatchingGroupIdVersion(Element root, IResource pomFile, String type,
      IStructuredDocument document) throws CoreException {
    Element parent = findChildElement(root, "parent"); //$NON-NLS-1$
    Element groupId = findChildElement(root, "groupId"); //$NON-NLS-1$
    if(parent != null && groupId != null) {
      //now compare the values of parent and project groupid..
      String parentString = getElementTextValue(findChildElement(parent, "groupId")); //$NON-NLS-1$
      String childString = getElementTextValue(groupId);
      if(parentString != null && parentString.equals(childString)) {
        //now figure out the offset
        if(groupId instanceof IndexedRegion) {
          IndexedRegion off = (IndexedRegion) groupId;
          IMarker mark = addMarker(pomFile, type,
              org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_duplicate_groupid,
              document.getLineOfOffset(off.getStartOffset()) + 1, IMarker.SEVERITY_WARNING, false /*isTransient*/);
          mark.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, IMavenConstants.EDITOR_HINT_PARENT_GROUP_ID);
          mark.setAttribute(IMarker.CHAR_START, off.getStartOffset());
          mark.setAttribute(IMarker.CHAR_END, off.getEndOffset());
          mark.setAttribute("problemType", "pomhint"); //only important in case we enable the generic xml quick fixes //$NON-NLS-1$ //$NON-NLS-2$
        }
      }
    }
    Element version = findChildElement(root, "version"); //$NON-NLS-1$
    if(parent != null && version != null) {
      //now compare the values of parent and project version..
      String parentString = getElementTextValue(findChildElement(parent, "version")); //$NON-NLS-1$
      String childString = getElementTextValue(version);
      if(parentString != null && parentString.equals(childString)) {
        //now figure out the offset
        if(version instanceof IndexedRegion) {
          IndexedRegion off = (IndexedRegion) version;
          IMarker mark = addMarker(pomFile, type,
              org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_duplicate_version,
              document.getLineOfOffset(off.getStartOffset()) + 1, IMarker.SEVERITY_WARNING, false);
          mark.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, IMavenConstants.EDITOR_HINT_PARENT_VERSION);
          mark.setAttribute(IMarker.CHAR_START, off.getStartOffset());
          mark.setAttribute(IMarker.CHAR_END, off.getEndOffset());
          mark.setAttribute("problemType", "pomhint"); //only important in case we enable the generic xml quick fixes //$NON-NLS-1$ //$NON-NLS-2$
        }
      }
    }
  }
  
  public static Element findChildElement(Element parent, String name) {
    NodeList rootList = parent.getChildNodes(); 
    for (int i = 0; i < rootList.getLength(); i++) {
        Node nd = rootList.item(i);
        if (nd instanceof Element) {
          Element el = (Element)nd;
          if (name.equals(el.getNodeName())) {
            return el;
          }
        }
    }
    return null;
  }
  public static List<Element> findChildElements(Element parent, String name) {
    NodeList rootList = parent.getChildNodes();
    List<Element> toRet = new ArrayList<Element>();
    for (int i = 0; i < rootList.getLength(); i++) {
        Node nd = rootList.item(i);
        if (nd instanceof Element) {
          Element el = (Element)nd;
          if (name.equals(el.getNodeName())) {
            toRet.add(el);
          }
        }
    }
    return toRet;
  }
  
  /**
   * gets the element text value, accepts null as parameter
   * @param element
   * @return
   */
  public static String getElementTextValue(Node element) {
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

  /**
   * The xsi:schema info is not part of the model, it is stored in the xml only. Need to open the DOM
   * and look for the project node to see if it has this schema defined
   * @param pomFile
   */
  protected void checkForSchema(IResource pomFile, String type) {
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
                IMarker marker = addMarker(pomFile, type,
                    org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_error_noschema, lineNumber,
                    IMarker.SEVERITY_WARNING, false /*isTransient*/);
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
  
  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.project.IMavenMarkerManager#addMarker(org.eclipse.core.resources.IResource, java.lang.String, int, int)
   */
  public IMarker addMarker(IResource resource, String type, String message, int lineNumber, int severity) {
    return addMarker(resource, type, message, lineNumber, severity, false /*isTransient*/);
  }

  private IMarker addMarker(IResource resource, String type, String message, int lineNumber, int severity, boolean isTransient) {
    IMarker marker = null;
    try {
      if(resource.isAccessible()) {
        marker = findMarker(resource, type, message, lineNumber, severity, isTransient);
        if(marker != null) {
          // This marker already exists
          return marker;
        }
        marker= resource.createMarker(type);
        marker.setAttribute(IMarker.MESSAGE, message);
        marker.setAttribute(IMarker.SEVERITY, severity);
        marker.setAttribute(IMarker.TRANSIENT, isTransient);
        
        if(lineNumber == -1) {
          lineNumber = 1;
        }
        marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
        marker.setAttribute(MarkerViewUtil.NAME_ATTRIBUTE, resource.getFullPath().toPortableString());
      }
    } catch(CoreException ex) {
      console.logError("Unable to add marker; " + ex.toString()); //$NON-NLS-1$
    }
    return marker;
  }

  private static <T> boolean eq(T a, T b) {
    if(a == null) {
      if(b == null) {
        return true;
      }
      return false;
    }
    return a.equals(b);
  }

  private IMarker findMarker(IResource resource, String type, String message, int lineNumber, int severity,
      boolean isTransient) throws CoreException {
    IMarker[] markers = resource.findMarkers(type, false /*includeSubtypes*/, IResource.DEPTH_ZERO);
    if(markers == null || markers.length == 0) {
      return null;
    }
    for(IMarker marker : markers) {
      if(eq(message, marker.getAttribute(IMarker.MESSAGE)) && eq(lineNumber, marker.getAttribute(IMarker.LINE_NUMBER))
          && eq(severity, marker.getAttribute(IMarker.SEVERITY))
          && eq(isTransient, marker.getAttribute(IMarker.TRANSIENT))) {
        return marker;
      }
    }
    return null;
  }

  private void handleProjectBuildingException(IResource pomFile, String type, ProjectBuildingException ex) {
    Throwable cause = ex.getCause();
    if(cause instanceof ModelBuildingException) {
      ModelBuildingException mbe = (ModelBuildingException) cause;
      for (ModelProblem problem : mbe.getProblems()) {
        String msg = Messages.getString("plugin.markerBuildError", problem.getMessage()); //$NON-NLS-1$
//      console.logError(msg);
        int severity = (Severity.WARNING == problem.getSeverity())? IMarker.SEVERITY_WARNING: IMarker.SEVERITY_ERROR;
        addMarker(pomFile, type, msg, 1, severity);
      }
    } else {
      handleBuildException(pomFile, type, ex);
    }
  }

  private void handleBuildException(IResource pomFile, String type, Throwable ex) {
    String msg = getErrorMessage(ex);
    addMarker(pomFile, type, msg, 1, IMarker.SEVERITY_ERROR);
  }

  private String getArtifactId(AbstractArtifactResolutionException rex) {
    String id = rex.getGroupId() + ":" + rex.getArtifactId() + ":" + rex.getVersion(); //$NON-NLS-1$ //$NON-NLS-2$
    if(rex.getClassifier() != null) {
      id += ":" + rex.getClassifier(); //$NON-NLS-1$
    }
    if(rex.getType() != null) {
      id += ":" + rex.getType(); //$NON-NLS-1$
    }
    return id;
  }

  private String getRootErrorMessage(Throwable ex) {
    return getRootCause(ex).getMessage();
  }

  private String getErrorMessage(Throwable ex) {
    StringBuilder message = new StringBuilder();
    while(ex != null) {
      if(ex.getMessage() != null && message.indexOf(ex.getMessage()) < 0) {
        if(message.length() > 0) {
          message.append(": ");
        }
        message.append(ex.getClass().getSimpleName()).append(": ").append(ex.getMessage());
      }
      ex = ex.getCause();
    }
    return message.toString();
  }

  private Throwable getRootCause(Throwable ex) {
    Throwable lastCause = ex;
    Throwable cause = lastCause.getCause();
    while(cause != null && cause != lastCause) {
      if(cause instanceof ArtifactNotFoundException) {
        cause = null;
      } else {
        lastCause = cause;
        cause = cause.getCause();
      }
    }
    return cause == null ? lastCause : cause;
  }

  
  private void addErrorMarkers(IResource pomFile, String type, String msg, List<? extends Exception> exceptions) {
    if(exceptions != null) {
      for(Exception ex : exceptions) {
        if(ex instanceof org.sonatype.aether.transfer.ArtifactNotFoundException) {
          // ignored here, handled by addMissingArtifactMarkers
        } else if(ex instanceof AbstractArtifactResolutionException) {
          AbstractArtifactResolutionException rex = (AbstractArtifactResolutionException) ex;
          String errorMessage = getArtifactId(rex) + " " + getRootErrorMessage(ex); //$NON-NLS-1$
          addMarker(pomFile, type, errorMessage, 1, IMarker.SEVERITY_ERROR);
//          console.logError(errorMessage);

        } else {
          addMarker(pomFile, type, ex.getMessage(), 1, IMarker.SEVERITY_ERROR);
//          console.logError(msg + "; " + ex.toString());
        }
      }
    }
  }

  public void deleteMarkers(IResource resource, String type) throws CoreException {
    deleteMarkers(resource, true /*includeSubtypes*/, type);
  }

  public void deleteMarkers(IResource resource, boolean includeSubtypes, String type) throws CoreException {
    if (resource != null && resource.exists()) {
      resource.deleteMarkers(type, includeSubtypes, IResource.DEPTH_INFINITE);
    }
  }

  public void deleteMarkers(IResource resource, String type, int severity, String attrName, String attrValue)
      throws CoreException {
    if(resource == null || !resource.exists()) {
      return;
    }

    IMarker[] markers = resource.findMarkers(type, false /*includeSubtypes*/, IResource.DEPTH_ZERO);
    for(IMarker marker : markers) {
      if(eq(severity, marker.getAttribute(IMarker.SEVERITY)) && eq(attrValue, marker.getAttribute(attrName))) {
        marker.delete();
      }
    }
  }

  private void addMissingArtifactMarkers(IResource pomFile, String type, MavenProject mavenProject) {
//    Set<Artifact> directDependencies = mavenProject.getDependencyArtifacts();
    Set<Artifact> artifacts = mavenProject.getArtifacts();
    for(Artifact artifact : artifacts) {
      if (!artifact.isResolved()) {
        String errorMessage;
//        if (directDependencies.contains(artifact)) {
          errorMessage = NLS.bind(org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_error_missing, artifact.toString());
//        } else {
//          errorMessage = "Missing indirectly referenced artifact " + artifact.toString();
//        }
        
        if(mavenConfiguration.isOffline()) {
          errorMessage = NLS.bind(org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_error_offline, errorMessage); 
        }
        
        addMarker(pomFile, type, errorMessage, 1, IMarker.SEVERITY_ERROR);
        console.logError(errorMessage);
      }
    }
  }

  public void addErrorMarkers(IResource resource, String type, Exception ex) {
    Throwable cause = getRootCause(ex);
    if(cause instanceof CoreException) {
      CoreException cex = (CoreException) cause;
      IStatus status = cex.getStatus();
      if(status != null) {
        addMarker(resource, type, status.getMessage(), 1, IMarker.SEVERITY_ERROR, false /*isTransient*/); //$NON-NLS-1$
        IStatus[] children = status.getChildren();
        if(children != null) {
          for(IStatus childStatus : children) {
            addMarker(resource, type, childStatus.getMessage(), 1, IMarker.SEVERITY_ERROR, false /*isTransient*/); //$NON-NLS-1$
          }
        }
      }
    } else {
      addMarker(resource, type, cause.getMessage(), 1, IMarker.SEVERITY_ERROR, false /*isTransient*/); //$NON-NLS-1$
    }
  }
}
