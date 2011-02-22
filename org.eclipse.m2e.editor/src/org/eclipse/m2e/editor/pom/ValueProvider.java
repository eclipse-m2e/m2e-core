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

package org.eclipse.m2e.editor.pom;

import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * Value provider for retrieving and creating holder element values
 * 
 * @author Eugene Kuleshov
 * @deprecated to be removed soon
 */
public abstract class ValueProvider<T> {

  public abstract T getValue();
  
  public T create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
    return null;
  }

  public boolean isEmpty() {
    return false;
  }

  
}
