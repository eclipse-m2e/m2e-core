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

package org.eclipse.m2e.refactoring.exclude;

import org.eclipse.core.resources.IFile;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;


/**
 * @author Anton Kraev
 */
public class MavenExcludeWizard extends RefactoringWizard {

  public MavenExcludeWizard(IFile file, String excludedGroupId, String excludedArtifactId) {
    super(new ExcludeRefactoring(file, excludedGroupId, excludedArtifactId), DIALOG_BASED_USER_INTERFACE);
  }

  @Override
  protected void addUserInputPages() {
    setDefaultPageTitle(getRefactoring().getName());
  }

}
