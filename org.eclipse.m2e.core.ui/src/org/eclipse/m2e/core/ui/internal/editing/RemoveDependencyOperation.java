/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.editing;

import org.apache.maven.model.Dependency;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RemoveDependencyOperation implements Operation {

  private Dependency dependency;

  public RemoveDependencyOperation(Dependency dependency) {
    this.dependency = dependency;
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation#process(org.w3c.dom.Document)
   */
  public void process(Document document) {
    Element dependencyElement = PomHelper.findDependency(document, dependency);
    if(dependencyElement == null) {
      throw new IllegalArgumentException("Dependency does not exist in pom");
    }
    Element dependencies = PomEdits.findChild(document.getDocumentElement(), PomHelper.DEPENDENCIES);
    PomEdits.removeChild(dependencies, dependencyElement);
    // Remove dependencies element if it is empty

    if(PomEdits.findDependencies(document.getDocumentElement()).isEmpty()) {
      PomEdits.removeChild(document.getDocumentElement(), dependencies);
    }
  }
}
