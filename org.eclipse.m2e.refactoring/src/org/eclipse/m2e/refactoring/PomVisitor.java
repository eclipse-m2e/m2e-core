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

package org.eclipse.m2e.refactoring;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.command.CompoundCommand;


/**
 * This interface defines refactoring visitor
 * 
 * @author Anton Kraev
 */
public interface PomVisitor {
  /**
   * Applies refactoring changes through undoable command
   * 
   * @param model - current model being visited
   * @param pm - progress monitor
   * @return command that executes changes (if any)
   * @throws Exception 
   */
  public CompoundCommand applyChanges(RefactoringModelResources model, IProgressMonitor pm) throws Exception;
}
