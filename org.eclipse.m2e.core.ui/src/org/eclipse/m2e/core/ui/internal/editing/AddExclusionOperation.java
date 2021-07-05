/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.editing;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.ARTIFACT_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.EXCLUSION;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.EXCLUSIONS;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.GROUP_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.childEquals;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.createElement;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.createElementWithText;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.format;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getChild;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.maven.model.Dependency;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;


public class AddExclusionOperation implements Operation {
  private static final Logger log = LoggerFactory.getLogger(AddExclusionOperation.class);

  private final Dependency dependency;

  private final ArtifactKey exclusion;

  public AddExclusionOperation(Dependency dependency, ArtifactKey exclusion) {
    this.dependency = dependency;
    this.exclusion = exclusion;
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation#process(org.w3c.dom.Document)
   */
  @Override
  public void process(Document document) {
    Element depElement = PomHelper.findDependency(document, dependency);

    if(depElement != null) {
      Element exclusionsElement = getChild(depElement, EXCLUSIONS);

      if(null == PomEdits.findChild(exclusionsElement, EXCLUSION, childEquals(GROUP_ID, exclusion.getGroupId()),
          childEquals(ARTIFACT_ID, exclusion.getArtifactId()))) {
        Element exclusionElement = createElement(exclusionsElement, EXCLUSION);

        createElementWithText(exclusionElement, GROUP_ID, exclusion.getGroupId());
        createElementWithText(exclusionElement, ARTIFACT_ID, exclusion.getArtifactId());
        format(exclusionElement);
      }
    } else {
      log.debug("Dependency " + dependency + " is not present for exclusion " + exclusion.toString()); //$NON-NLS-1$//$NON-NLS-2$
    }
  }
}
