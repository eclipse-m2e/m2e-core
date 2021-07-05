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

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCIES;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getChild;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;


public class AddDependencyOperation implements Operation {

  private final Dependency dependency;

  public AddDependencyOperation(Dependency dependency) {
    this.dependency = dependency;
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation#process(org.w3c.dom.Document)
   */
  @Override
  public void process(Document document) {
    Element dependencies = getChild(document.getDocumentElement(), DEPENDENCIES);

    PomHelper.addOrUpdateDependency(dependencies, dependency.getGroupId(), dependency.getArtifactId(),
        (dependency.getVersion() == null || dependency.getVersion().length() == 0) ? null : dependency.getVersion(),
        null, null, null);

    for(Exclusion exclusion : dependency.getExclusions()) {
      new AddExclusionOperation(dependency, new ArtifactKey(exclusion.getGroupId(), exclusion.getArtifactId(), null,
          null)).process(document);
    }
  }
}
