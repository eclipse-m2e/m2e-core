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

package org.eclipse.m2e.refactoring.rename;

import org.eclipse.core.resources.IFile;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.refactoring.AbstractPomRefactoring;


/**
 * @author Anton Kraev
 */
public class MavenRenameWizard extends RefactoringWizard {

  private static MavenRenameWizardPage page1 = new MavenRenameWizardPage();

  public MavenRenameWizard(IFile file) {
    super(new RenameRefactoring(file, page1), DIALOG_BASED_USER_INTERFACE);
  }

  @Override
  protected void addUserInputPages() {
    setDefaultPageTitle(getRefactoring().getName());
    addPage(page1);
    Model model = ((AbstractPomRefactoring) getRefactoring()).createModel();
    page1.initialize(model.getGroupId(), model.getArtifactId(), model.getVersion());
    model.eResource().unload();
  }

}
