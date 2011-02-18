/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.editing;

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

    if(depElement == null) {
      throw new IllegalArgumentException("Dependency does not exist in this pom");
    }
    Element exclusionsElement = PomEdits.getChild(depElement, PomHelper.EXCLUSIONS);

    Element exclusionElement = PomEdits.createElement(exclusionsElement, PomHelper.EXCLUSION);

    PomEdits.createElementWithText(exclusionElement, PomHelper.ARTIFACT_ID, exclusion.getArtifactId());
    PomEdits.createElementWithText(exclusionElement, PomHelper.GROUP_ID, exclusion.getGroupId());
    PomEdits.createElementWithText(exclusionElement, PomHelper.VERSION, exclusion.getVersion());
  }
}
