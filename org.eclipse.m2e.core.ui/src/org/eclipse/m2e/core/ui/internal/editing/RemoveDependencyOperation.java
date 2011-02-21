/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.editing;

import org.apache.maven.model.Dependency;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.*;
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
      //TODO we shall not throw exceptions from operations..
      throw new IllegalArgumentException("Dependency does not exist in pom");
    }
    Element dependencies = findChild(document.getDocumentElement(), DEPENDENCIES);
    removeChild(dependencies, dependencyElement);
    // Remove dependencies element if it is empty

    removeIfNoChildElement(dependencies);
  }
}
