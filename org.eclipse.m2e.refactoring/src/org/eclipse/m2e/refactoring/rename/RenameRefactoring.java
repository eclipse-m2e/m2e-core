/*******************************************************************************
 * Copyright (c) 2008-2026 Sonatype, Inc.
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

package org.eclipse.m2e.refactoring.rename;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getTextValue;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameJavaProjectProcessor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.core.ui.internal.editing.PomHelper;
import org.eclipse.m2e.refactoring.Messages;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;


/**
 * Rename artifact refactoring implementation
 *
 * @author Anton Kraev
 */
@SuppressWarnings("restriction")
public class RenameRefactoring extends Refactoring {

  private static final String VERSION = "version"; //$NON-NLS-1$

  private static final String ARTIFACT_ID = "artifactId"; //$NON-NLS-1$

  private static final String GETARTIFACT_ID = "getArtifactId"; //$NON-NLS-1$

  private static final String GROUP_ID = "groupId"; //$NON-NLS-1$

  private static final String PROPERTIES = "properties"; //$NON-NLS-1$

  final IFile file;

  // this page contains new values
  MavenRenameWizardPage page;

  // old values
  String oldGroupId;

  String oldArtifactId;

  String oldVersion;

  // the poms of the workspace and the new values for them, while creating the change
  Map<IFile, Pom> poms;

  Edits edits;

  public RenameRefactoring(IFile file, MavenRenameWizardPage page) {
    this.file = file;
    this.page = page;
  }

  // gets element from effective model based on path
  private Object getElement(Object root, Path path) {
    if(path == null || path.path.size() == 0) {
      return root;
    }

    PathElement current = path.path.remove(0);
    String getterName = "get" + current.element; //$NON-NLS-1$

    try {
      Method getter = root.getClass().getMethod(getterName);
      root = getElement(getter.invoke(root), path);
      if(root instanceof List children) {
        for(Object child : children) {
          Method artifact = child.getClass().getMethod(GETARTIFACT_ID);
          String artifactId = (String) artifact.invoke(child);
          if(current.artifactId != null && !current.artifactId.equals(artifactId))
            continue;

          //found, names are correct
          return getElement(child, path);
        }
      } else {
        return getElement(root, path);
      }
      return null;
    } catch(Exception ex) {
      return null;
    }
  }

  /**
   * Finds all potential matched objects in model
   */
  private List<ElementWithPath> scanModel(Element model, String groupId, String artifactId, String version,
      boolean processRoot) {
    List<ElementWithPath> res = new ArrayList<>();
    Path path = new Path();
    if(processRoot) {
      scanObject(path, model, groupId, artifactId, version, res);
    } else {
      scanChildren(path, model, groupId, artifactId, version, res);
    }
    return res;
  }

  // add candidate objects with same artifactId
  private List<ElementWithPath> scanObject(Path current, Element obj, String groupId, String artifactId,
      String version, List<ElementWithPath> res) {
    if(scanFeature(obj, ARTIFACT_ID, artifactId)) {
      // System.out.println("found object " + obj + " : " + current);
      res.add(new ElementWithPath(obj, current));
    }
    scanChildren(current, obj, groupId, artifactId, version, res);
    return res;
  }

  private List<ElementWithPath> scanChildren(Path current, Element obj, String groupId, String artifactId,
      String version, List<ElementWithPath> res) {
    for(Node node = obj.getFirstChild(); node != null; node = node.getNextSibling()) {
      if(!(node instanceof Element child)) {
        continue;
      }
      Path childPath = current;
      String element = child.getNodeName();
      // the items of a list, e.g. <dependency> in <dependencies>, are found by artifactId in the list
      if(!isListItem(obj.getNodeName(), element)) {
        childPath = current.clone();
        element = element.substring(0, 1).toUpperCase() + element.substring(1);
        childPath.addElement(element, artifactId);
      }
      scanObject(childPath, child, groupId, artifactId, version, res);
    }
    return res;
  }

  private static boolean isListItem(String list, String item) {
    return list.equals(item + "s") //$NON-NLS-1$
        || item.endsWith("y") && list.equals(item.substring(0, item.length() - 1) + "ies"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private boolean scanFeature(Element obj, String featureName, String value) {
    //not searching on this
    if(value == null) {
      return false;
    }
    String val = getValue(obj, featureName);
    if(value.equals(val)) {
      return true;
    }
    return false;
  }

  private String getValue(Element obj, String featureName) {
    Element feature = findChild(obj, featureName);
    if(feature == null) {
      return null;
    }
    return getTextValue(feature);
  }

  public Model createModel() {
    try {
      return MavenPlugin.getMavenModelManager().readMavenModel(file);
    } catch(CoreException ex) {
      ILog.of(getClass()).error(Messages.AbstractPomRefactoring_error, ex);
      return null;
    }
  }

  public String getNewProjectName() {
    return page.getRenameEclipseProject() ? page.getNewArtifactId() : null;
  }

  /**
   * Applies new values in model
   *
   * @param editingDomain
   * @param renameProject
   * @throws NoSuchMethodException
   * @throws Exception
   */
  public void applyModel(Pom model, String newGroupId, String newArtifactId, String newVersion, boolean processRoot)
      throws Exception {
    // find all affected objects in the pom
    List<ElementWithPath> affected = scanModel(model.document().getDocumentElement(), this.oldGroupId,
        this.oldArtifactId, this.oldVersion, processRoot);

    // go through all affected objects, check in effective model
    Iterator<ElementWithPath> i = affected.iterator();
    while(i.hasNext()) {
      ElementWithPath obj = i.next();
      Object effectiveObj = getElement(model.effective(), obj.path.clone());
      if(effectiveObj == null) {
        // System.out.println("cannot find effective for: " + obj.object);
        continue;
      }
      String effectiveVersion;
      String effectiveGroupId;
      switch(effectiveObj) {
        case Model m -> {
          effectiveVersion = m.getVersion();
          effectiveGroupId = m.getGroupId();
        }
        case Parent p -> {
          effectiveVersion = p.getVersion();
          effectiveGroupId = p.getGroupId();
        }
        case Dependency d -> {
          effectiveVersion = d.getVersion();
          effectiveGroupId = d.getGroupId();
        }
        case Plugin p -> {
          effectiveVersion = p.getVersion();
          effectiveGroupId = p.getGroupId();
        }
        case ReportPlugin r -> {
          effectiveVersion = r.getVersion();
          effectiveGroupId = r.getGroupId();
        }
        case Extension e -> {
          effectiveVersion = e.getVersion();
          effectiveGroupId = e.getGroupId();
        }
        default -> {
          // not an artifact reference, e.g. an exclusion
          continue;
        }
      }
      // if version from effective POM is different from old version, skip it
      if(this.oldVersion != null && !this.oldVersion.equals(effectiveVersion)) {
        continue;
      }

      // only set groupId if effective group id is the same as old group id
      if(oldGroupId != null && oldGroupId.equals(effectiveGroupId))
        applyFeature(model, GROUP_ID, newGroupId, obj);
      // set artifact id unconditionally
      applyFeature(model, ARTIFACT_ID, newArtifactId, obj);
      // only set version if effective version is the same (already checked by the above)
      // and new version is not empty
      if(!"".equals(newVersion)) { //$NON-NLS-1$
        applyFeature(model, VERSION, newVersion, obj);
      }
    }
  }

  // apply the value, considering properties
  private void applyFeature(Pom model, String feature, String newValue, ElementWithPath obj) {
    String old = getValue(obj.object, feature);
    if(old != null && old.startsWith("${")) { //$NON-NLS-1$
      // this is a property, go find it in the pom or its workspace parents
      String pName = old.substring(2);
      pName = pName.substring(0, pName.length() - 1).trim();
      for(Pom current = model; current != null; current = getWorkspaceParent(current)) {
        Element property = findChild(findChild(current.document().getDocumentElement(), PROPERTIES), pName);
        if(property != null) {
          edits.add(current.pomFile(), property, newValue);
          return;
        }
      }
    }
    applyObject(model, obj.object, feature, newValue);
  }

  private void applyObject(Pom model, Element obj, String featureName, String value) {
    Element feature = findChild(obj, featureName);
    if(feature == null) {
      return;
    }
    String old = getTextValue(feature);
    if(old.equals(value)) {
      return;
    }
    edits.add(model.pomFile(), feature, value);
  }

  private Pom getWorkspaceParent(Pom pom) {
    Parent parent = pom.effective().getParent();
    if(parent == null) {
      return null;
    }
    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getMavenProject(parent.getGroupId(),
        parent.getArtifactId(), parent.getVersion());
    return facade != null ? poms.get(facade.getPom()) : null;
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
      throws CoreException, OperationCanceledException {
    Model model = MavenPlugin.getMavenModelManager().readMavenModel(file);
    this.oldArtifactId = model.getArtifactId();
    this.oldGroupId = model.getGroupId();
    this.oldVersion = model.getVersion();
    RefactoringStatus res = new RefactoringStatus();
    return res;
  }

  @Override
  public RefactoringStatus checkFinalConditions(IProgressMonitor pm) {
    return new RefactoringStatus();
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    CompositeChange res = new CompositeChange(getTitle());
    List<IMavenProjectFacade> projects = MavenPlugin.getMavenProjectRegistry().getProjects();
    pm.beginTask(Messages.AbstractPomRefactoring_task, projects.size());
    try {
      poms = new LinkedHashMap<>();
      for(IMavenProjectFacade facade : projects) {
        if(facade.getProject().isAccessible() && facade.getPom().isAccessible()) {
          pm.setTaskName(NLS.bind(Messages.AbstractPomRefactoring_loading, facade.getProject().getName()));
          poms.put(facade.getPom(),
              new Pom(facade.getPom(), loadDocument(facade.getPom()), facade.getMavenProject(pm).getModel()));
        }
        pm.worked(1);
      }

      edits = new Edits();
      for(Pom current : poms.values()) {
        //process <project> element only for the refactored file itself
        boolean processRoot = current.pomFile().equals(file);
        applyModel(current, page.getNewGroupId(), page.getNewArtifactId(), page.getNewVersion(), processRoot);
      }
      // the refactored file first
      List<IFile> changed = new ArrayList<>(edits.values.keySet());
      if(changed.remove(file)) {
        changed.add(0, file);
      }
      for(IFile pomFile : changed) {
        res.add(PomHelper.createChange(pomFile, edits.operation(pomFile), pomFile.getProject().getName()));
      }

      String newName = getNewProjectName();
      if(newName != null) {
        RenameJavaProjectProcessor processor = new RenameJavaProjectProcessor(JavaCore.create(file.getProject()));
        org.eclipse.ltk.core.refactoring.participants.RenameRefactoring refactoring = new org.eclipse.ltk.core.refactoring.participants.RenameRefactoring(
            processor);
        processor.setNewElementName(newName);
        RefactoringStatus status = new RefactoringStatus();
        status.merge(refactoring.checkInitialConditions(pm));
        if(!status.hasFatalError()) {
          status.merge(refactoring.checkFinalConditions(pm));
          if(!status.hasFatalError()) {
            res.add(refactoring.createChange(pm));
          }
        }
      }
    } catch(CoreException | OperationCanceledException ex) {
      throw ex;
    } catch(Exception ex) {
      throw new CoreException(Status.error(Messages.AbstractPomRefactoring_error, ex));
    } finally {
      poms = null;
      edits = null;
      pm.done();
    }
    return res;
  }

  private static Document loadDocument(IFile pom) throws CoreException {
    try (InputStream contents = pom.getContents()) {
      return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(contents);
    } catch(CoreException ex) {
      throw ex;
    } catch(Exception ex) {
      throw new CoreException(Status.error(NLS.bind("Can't load model {0}", pom), ex)); //$NON-NLS-1$
    }
  }

  @Override
  public String getName() {
    return Messages.RenameRefactoring_name;
  }

  static class Path {
    List<PathElement> path = new ArrayList<>();

    public void addElement(String element, String artifactId) {
      path.add(new PathElement(element, artifactId));
    }

    @Override
    public String toString() {
      return path.toString();
    }

    @Override
    public Path clone() {
      Path res = new Path();
      res.path = new ArrayList<>(this.path);
      return res;
    }
  }

  // path (built during traversal of the pom, used to find in effective model)
  static class PathElement {
    String element;

    String artifactId;

    public PathElement(String element, String artifactId) {
      this.element = element;
      this.artifactId = artifactId;
    }

    @Override
    public String toString() {
      return "/" + element + "[artifactId=" + artifactId + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
  }

  static class ElementWithPath {
    public Element object;

    public Path path;

    public ElementWithPath(Element object, Path path) {
      this.object = object;
      this.path = path;
    }
  }

  // a pom of the workspace with its effective model
  record Pom(IFile pomFile, Document document, Model effective) {
  }

  // the new text values per pom, by the position of their element, applied to the pom documents in the changes
  static class Edits {
    final Map<IFile, Map<List<Integer>, String>> values = new LinkedHashMap<>();

    void add(IFile pom, Element element, String value) {
      values.computeIfAbsent(pom, p -> new LinkedHashMap<>()).put(position(element), value);
    }

    PomEdits.Operation operation(IFile pom) {
      Map<List<Integer>, String> pomValues = values.get(pom);
      return document -> pomValues.forEach((position, value) -> {
        Element element = document.getDocumentElement();
        for(int index : position) {
          element = childElements(element).get(index);
        }
        PomEdits.setText(element, value);
      });
    }

    private static List<Integer> position(Element element) {
      List<Integer> position = new ArrayList<>();
      for(Node node = element; node.getParentNode() instanceof Element parent; node = parent) {
        position.add(0, childElements(parent).indexOf(node));
      }
      return position;
    }

    private static List<Element> childElements(Element element) {
      List<Element> children = new ArrayList<>();
      for(Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
        if(node instanceof Element child) {
          children.add(child);
        }
      }
      return children;
    }
  }

  public String getTitle() {
    return NLS.bind(Messages.RenameRefactoring_title, file.getParent().getName());
  }

}
