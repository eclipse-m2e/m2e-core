/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.editing;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.ARTIFACT_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.EXCLUSION;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.EXCLUSIONS;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.GROUP_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.createElement;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.createElementWithText;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.format;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getChild;

import org.apache.maven.model.Dependency;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AddExclusionOperation implements Operation {

  private Dependency dependency;

  private ArtifactKey exclusion;

  public AddExclusionOperation(Dependency dependency, ArtifactKey exclusion) {
    this.dependency = dependency;
    this.exclusion = exclusion;
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation#process(org.w3c.dom.Document)
   */
  public void process(Document document) {
    Element depElement = PomHelper.findDependency(document, dependency);

    if(depElement != null) {
      Element exclusionsElement = getChild(depElement, EXCLUSIONS);

      Element exclusionElement = createElement(exclusionsElement, EXCLUSION);

      createElementWithText(exclusionElement, ARTIFACT_ID, exclusion.getArtifactId());
      createElementWithText(exclusionElement, GROUP_ID, exclusion.getGroupId());
      format(exclusionElement);
    }
  }
}
