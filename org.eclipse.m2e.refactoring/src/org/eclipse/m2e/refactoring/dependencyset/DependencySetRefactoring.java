/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.refactoring.dependencyset;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.ARTIFACT_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCIES;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCY;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCY_MANAGEMENT;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.GROUP_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.VERSION;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.childEquals;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.setText;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.CompoundOperation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.core.ui.internal.editing.PomHelper;


/**
 * @author mkleint
 */
public class DependencySetRefactoring extends Refactoring {
  private final IFile file;

  private final List<ArtifactKey> keys;

  public DependencySetRefactoring(IFile file, List<ArtifactKey> keys) {
    this.file = file;
    this.keys = keys;
  }

  /* (non-Javadoc)
   * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
   */
  public String getName() {
    // TODO Auto-generated method stub
    return "Set dependency version";
  }

  /* (non-Javadoc)
   * @see org.eclipse.ltk.core.refactoring.Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
   */
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    return new RefactoringStatus();
  }

  /* (non-Javadoc)
   * @see org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)
   */
  public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    return new RefactoringStatus();
  }

  /* (non-Javadoc)
   * @see org.eclipse.ltk.core.refactoring.Refactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
   */
  public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    List<Operation> operations = new ArrayList<Operation>();
    for(ArtifactKey key : keys) {
      operations.add(new OneDependency(key));
    }
    CompoundOperation compound = new CompoundOperation(operations.toArray(new Operation[0]));
    return PomHelper.createChange(file, compound, getName());
  }

  private static class OneDependency implements Operation {

    private final String groupId;

    private final String artifactId;

    private final String version;

    public OneDependency(ArtifactKey key) {
      this.groupId = key.getGroupId();
      this.artifactId = key.getArtifactId();
      this.version = key.getVersion();
    }

    /* (non-Javadoc)
     * @see org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation#process(org.w3c.dom.Document)
     */
    public void process(Document document) {
      //TODO handle activated profiles?
      Element deps = findChild(document.getDocumentElement(), DEPENDENCIES);
      //TODO expressions in fields..
      Element existing = findChild(deps, DEPENDENCY, childEquals(GROUP_ID, groupId),
          childEquals(ARTIFACT_ID, artifactId));
      if(existing != null) {
        //it's a direct dependency
        //TODO check the version value.. not to overwrite the existing version..
        //even better, have the action only available on transitive dependencies
        setText(getChild(existing, VERSION), version);
      } else {
        //is transitive dependency
        Element dm = getChild(document.getDocumentElement(), DEPENDENCY_MANAGEMENT, DEPENDENCIES);
        existing = findChild(dm, DEPENDENCY, childEquals(GROUP_ID, groupId), childEquals(ARTIFACT_ID, artifactId));
        if(existing != null) {
          setText(getChild(existing, VERSION), version);
        } else {
          PomHelper.createDependency(dm, groupId, artifactId, version);
        }
      }
    }
  }
}
