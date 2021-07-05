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

package org.eclipse.m2e.editor.xml;

import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateContextType;


/**
 * @author Lukas Krecan
 */
public class PomTemplateContextType extends TemplateContextType {
  public PomTemplateContextType() {
    addResolver(new GlobalTemplateVariables.Cursor());
    addResolver(new GlobalTemplateVariables.Date());
    addResolver(new GlobalTemplateVariables.Dollar());
    addResolver(new GlobalTemplateVariables.LineSelection());
    addResolver(new GlobalTemplateVariables.Time());
    addResolver(new GlobalTemplateVariables.User());
    addResolver(new GlobalTemplateVariables.WordSelection());
    addResolver(new GlobalTemplateVariables.Year());
  }
}
