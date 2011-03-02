/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.editing;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.ARTIFACT_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCIES;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCY;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.GROUP_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.VERSION;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.createElementWithText;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.format;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getChild;

import org.apache.maven.model.Dependency;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AddDependencyOperation implements Operation {

  private Dependency dependency;

  public AddDependencyOperation(Dependency dependency) {
    this.dependency = dependency;
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation#process(org.w3c.dom.Document)
   */
  public void process(Document document) {
    Element dependencyElement = PomHelper.findDependency(document, dependency);
    if(dependencyElement == null) {
      Element dependencies = getChild(document.getDocumentElement(), DEPENDENCIES);

      // TODO Handle managed dependencies?
      dependencyElement = PomEdits.createElement(dependencies, DEPENDENCY);
      createElementWithText(dependencyElement, ARTIFACT_ID, dependency.getArtifactId());
      createElementWithText(dependencyElement, GROUP_ID, dependency.getGroupId());
      createElementWithText(dependencyElement, VERSION, dependency.getVersion());
      format(dependencyElement);
    }
    // find existing
  }
}
