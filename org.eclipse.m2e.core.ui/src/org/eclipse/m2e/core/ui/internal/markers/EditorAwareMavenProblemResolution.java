/*******************************************************************************
 * Copyright (c) 2015 Anton Tanasenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Anton Tanasenko - Refactor marker resolutions and quick fixes (Bug #484359)
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.markers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;


/**
 * @author atanasenko
 */
public abstract class EditorAwareMavenProblemResolution extends MavenProblemResolution {

  private IQuickAssistInvocationContext context;

  protected EditorAwareMavenProblemResolution(IMarker marker) {
    super(marker);
  }

  public IQuickAssistInvocationContext getQuickAssistContext() {
    return this.context;
  }

  public void setQuickAssistContext(IQuickAssistInvocationContext context) {
    this.context = context;
  }

  @Override
  protected final void fix(IMarker[] markers, IDocument document, IProgressMonitor monitor) {

    Map<IResource, List<IMarker>> resourceMap = Stream.of(markers).collect(Collectors.groupingBy(IMarker::getResource));

    // assume that the document comes from that resource
    if(resourceMap.size() == 1 && document != null) {
      fix(document, resourceMap.values().iterator().next(), monitor);
      return;
    }

    for(Map.Entry<IResource, List<IMarker>> e : resourceMap.entrySet()) {
      fix(e.getKey(), e.getValue(), monitor);
    }
  }

  protected abstract void fix(IResource resource, List<IMarker> markers, IProgressMonitor monitor);

  protected abstract void fix(IDocument document, List<IMarker> markers, IProgressMonitor monitor);

}
