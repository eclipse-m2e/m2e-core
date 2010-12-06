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

package org.eclipse.m2e.refactoring.internal;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.m2e.core.actions.AbstractMavenMenuCreator;
import org.eclipse.m2e.core.actions.SelectionUtil;
import org.eclipse.m2e.refactoring.Messages;
import org.eclipse.m2e.refactoring.exclude.DependencyExcludeAction;

/**
 * @author Eugene Kuleshov
 */
public class RefactoringMavenMenuCreator extends AbstractMavenMenuCreator {

  public void createMenu(IMenuManager mgr) {
    int selectionType = SelectionUtil.getSelectionType(selection);
    if(selectionType == SelectionUtil.JAR_FILE) {
      mgr.appendToGroup(OPEN, getAction(new DependencyExcludeAction(), //
          DependencyExcludeAction.ID, //
          Messages.RefactoringMavenMenuCreator_action_exclude, //
          RefactoringImages.EXCLUDE));
    }
  }

}

