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

package org.eclipse.m2e.refactoring.rename;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.util.PomResourceImpl;
import org.eclipse.m2e.refactoring.AbstractPomRefactoring;
import org.eclipse.m2e.refactoring.Messages;
import org.eclipse.m2e.refactoring.PomVisitor;
import org.eclipse.m2e.refactoring.RefactoringModelResources;
import org.eclipse.m2e.refactoring.RefactoringModelResources.PropertyInfo;
import org.eclipse.osgi.util.NLS;


/**
 * Rename artifact refactoring implementation
 *
 * @author Anton Kraev
 */
public class RenameRefactoring extends AbstractPomRefactoring {

  private static final String VERSION = "version"; //$NON-NLS-1$

  private static final String GETVERSION = Messages.RenameRefactoring_1;

  private static final String ARTIFACT_ID = "artifactId"; //$NON-NLS-1$

  private static final String GETARTIFACT_ID = "getArtifactId"; //$NON-NLS-1$

  private static final String GROUP_ID = "groupId"; //$NON-NLS-1$

  private static final String GETGROUP_ID = "getGroupId"; //$NON-NLS-1$

  // this page contains new values
  MavenRenameWizardPage page;

  // old values
  String oldGroupId;

  String oldArtifactId;

  String oldVersion;

  public RenameRefactoring(IFile file, MavenRenameWizardPage page) {
    super(file);
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
  private List<EObjectWithPath> scanModel(Model model, String groupId, String artifactId, String version,
      boolean processRoot) {
    List<EObjectWithPath> res = new ArrayList<>();
    Path path = new Path();
    if(processRoot) {
      scanObject(path, model, groupId, artifactId, version, res);
    } else {
      scanChildren(path, model, groupId, artifactId, version, res);
    }
    return res;
  }

  // add candidate objects with same artifactId
  private List<EObjectWithPath> scanObject(Path current, EObject obj, String groupId, String artifactId, String version,
      List<EObjectWithPath> res) {
    if(scanFeature(obj, ARTIFACT_ID, artifactId)) {
      // System.out.println("found object " + obj + " : " + current);
      res.add(new EObjectWithPath(obj, current));
    }
    scanChildren(current, obj, groupId, artifactId, version, res);
    return res;
  }

  private List<EObjectWithPath> scanChildren(Path current, EObject obj, String groupId, String artifactId,
      String version, List<EObjectWithPath> res) {
    Iterator<EObject> it = obj.eContents().iterator();
    while(it.hasNext()) {
      obj = it.next();
      Path child = current.clone();
      String element = obj.eContainingFeature().getName();
      element = element.substring(0, 1).toUpperCase() + element.substring(1);
      child.addElement(element, artifactId);
      scanObject(child, obj, groupId, artifactId, version, res);
    }
    return res;
  }

  private boolean scanFeature(EObject obj, String featureName, String value) {
    //not searching on this
    if(value == null) {
      return false;
    }
    EStructuralFeature feature = obj.eClass().getEStructuralFeature(featureName);
    if(feature == null) {
      return false;
    }
    String val = obj.eGet(feature) == null ? null : obj.eGet(feature).toString();
    if(value.equals(val)) {
      return true;
    }
    return false;
  }

  private String getValue(EObject obj, String featureName) {
    EStructuralFeature feature = obj.eClass().getEStructuralFeature(featureName);
    if(feature == null) {
      return null;
    }
    return obj.eGet(feature) == null ? null : obj.eGet(feature).toString();
  }

  @Override
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
  public CompoundCommand applyModel(RefactoringModelResources model, String newGroupId, String newArtifactId,
      String newVersion, boolean processRoot) throws Exception {
    // find all affected objects in EMF model
    List<EObjectWithPath> affected = scanModel(model.getTmpModel(), this.oldGroupId, this.oldArtifactId,
        this.oldVersion, processRoot);

    // go through all affected objects, check in effective model
    Iterator<EObjectWithPath> i = affected.iterator();
    CompoundCommand command = new CompoundCommand();
    while(i.hasNext()) {
      EObjectWithPath obj = i.next();
      Object effectiveObj = getElement(model.getEffective(), obj.path.clone());
      if(effectiveObj == null) {
        // System.out.println("cannot find effective for: " + obj.object);
        continue;
      }
      Method method = effectiveObj.getClass().getMethod(GETVERSION);
      String effectiveVersion = (String) method.invoke(effectiveObj);
      method = effectiveObj.getClass().getMethod(GETGROUP_ID);
      String effectiveGroupId = (String) method.invoke(effectiveObj);
      // if version from effective POM is different from old version, skip it
      if(this.oldVersion != null && !this.oldVersion.equals(effectiveVersion)) {
        continue;
      }

      // only set groupId if effective group id is the same as old group id
      if(oldGroupId != null && oldGroupId.equals(effectiveGroupId))
        applyFeature(editingDomain, model, GROUP_ID, newGroupId, command, obj);
      // set artifact id unconditionally
      applyFeature(editingDomain, model, ARTIFACT_ID, newArtifactId, command, obj);
      // only set version if effective version is the same (already checked by the above)
      // and new version is not empty
      if(!"".equals(newVersion)) { //$NON-NLS-1$
        applyFeature(editingDomain, model, VERSION, newVersion, command, obj);
      }
    }

    return command.isEmpty() ? null : command;
  }

  // apply the value, considering properties
  private void applyFeature(AdapterFactoryEditingDomain editingDomain, RefactoringModelResources model, String feature,
      String newValue, CompoundCommand command, EObjectWithPath obj) {
    PropertyInfo info = null;
    String old = getValue(obj.object, feature);
    if(old != null && old.startsWith("${")) { //$NON-NLS-1$
      // this is a property, go find it
      String pName = old.substring(2);
      pName = pName.substring(0, pName.length() - 1).trim();
      info = model.getProperties().get(pName);
    }
    if(info != null)
      info.setNewValue(new SetCommand(editingDomain, info.getPair(),
          info.getPair().eClass().getEStructuralFeature("value"), newValue)); //$NON-NLS-1$
    else
      applyObject(editingDomain, command, obj.object, feature, newValue);
  }

  private void applyObject(AdapterFactoryEditingDomain editingDomain, CompoundCommand command, EObject obj,
      String featureName, String value) {
    EStructuralFeature feature = obj.eClass().getEStructuralFeature(featureName);
    if(feature == null) {
      return;
    }
    Object old = obj.eGet(feature);
    if(old == null || old.equals(value)) {
      return;
    }
    command.append(new SetCommand(editingDomain, obj, feature, value));
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
      throws CoreException, OperationCanceledException {
    PomResourceImpl resource = AbstractPomRefactoring.loadResource(file);
    try {
      Model model = (Model) resource.getContents().get(0);
      this.oldArtifactId = model.getArtifactId();
      this.oldGroupId = model.getGroupId();
      this.oldVersion = model.getVersion();
    } finally {
      resource.unload();
    }
    RefactoringStatus res = new RefactoringStatus();
    return res;
  }

  @Override
  public String getName() {
    return Messages.RenameRefactoring_name;
  }

  @Override
  public PomVisitor getVisitor() {
    return (current, pm) -> {
      //process <project> element only for the refactored file itself
      boolean processRoot = current.getPomFile().equals(file);
      return RenameRefactoring.this.applyModel(current, page.getNewGroupId(), page.getNewArtifactId(),
          page.getNewVersion(), processRoot);
    };
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

  // path (built during traversal of EMF model, used to find in effective model)
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

  static class EObjectWithPath {
    public EObject object;

    public Path path;

    public EObjectWithPath(EObject object, Path path) {
      this.object = object;
      this.path = path;
    }
  }

  // XXX move stuff UP after implementing another refactoring
  // after moving up, use this
  interface ScanVisitor {
    boolean interested(EObject obj);
  }

  @Override
  public boolean scanAllArtifacts() {
    return true;
  }

  @Override
  public String getTitle() {
    return NLS.bind(Messages.RenameRefactoring_title, file.getParent().getName());
  }

}
