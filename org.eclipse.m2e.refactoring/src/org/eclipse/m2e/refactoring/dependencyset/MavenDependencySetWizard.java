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

package org.eclipse.m2e.refactoring.dependencyset;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.m2e.core.embedder.ArtifactKey;


/**
 * @author Milos Kleint
 */
public class MavenDependencySetWizard extends RefactoringWizard {

  public MavenDependencySetWizard(IFile file, List<ArtifactKey> keys) {
    super(new DependencySetRefactoring(file, keys), DIALOG_BASED_USER_INTERFACE);
  }

  @Override
  protected void addUserInputPages() {
    setDefaultPageTitle(getRefactoring().getName());
  }

}
