/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.editor.pom;

import org.w3c.dom.Element;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.ITextViewer;


/**
 * Use {@link org.eclipse.m2e.core.ui.internal.util.XmlUtils} instead
 *
 * @deprecated
 */
@Deprecated
public class XmlUtils {

  public static IProject extractProject(ITextViewer viewer) {
    return org.eclipse.m2e.core.ui.internal.util.XmlUtils.extractProject(viewer);
  }

  public static Element findChild(Element parent, String name) {
    return org.eclipse.m2e.core.ui.internal.util.XmlUtils.findChild(parent, name);
  }
}
