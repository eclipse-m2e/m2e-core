/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.editing;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCIES;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.removeChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.removeIfNoChildElement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.maven.model.Dependency;

import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;


public class RemoveDependencyOperation implements Operation {

  private final Dependency dependency;

  public RemoveDependencyOperation(Dependency dependency) {
    this.dependency = dependency;
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation#process(org.w3c.dom.Document)
   */
  @Override
  public void process(Document document) {
    Element dependencyElement = PomHelper.findDependency(document, dependency);
    if(dependencyElement != null) {
      Element dependencies = findChild(document.getDocumentElement(), DEPENDENCIES);
      removeChild(dependencies, dependencyElement);
      // Remove dependencies element if it is empty

      removeIfNoChildElement(dependencies);
    }
  }
}
