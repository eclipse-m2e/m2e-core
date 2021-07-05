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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;


/**
 * A single superclass for marker resolutions that can also act as completion proposals in text editor (e.g. shown when
 * called on ctrl+1)
 *
 * @author atanasenko
 */
public abstract class MavenProblemResolution extends WorkbenchMarkerResolution
    implements ICompletionProposal, ICompletionProposalExtension5 {

  protected final Logger LOG = LoggerFactory.getLogger(this.getClass());

  private final IMarker marker;

  protected MavenProblemResolution(IMarker marker) {
    this.marker = marker;
  }

  public IMarker getMarker() {
    return this.marker;
  }

  public int getOrder() {
    return Integer.MAX_VALUE;
  }

  /**
   * Run this resolution for specified markers
   */
  protected abstract void fix(IMarker[] markers, IDocument document, IProgressMonitor monitor);

  /**
   * Tells whether this resolution should only be present once in a list of resolutions for any number of supported
   * markers and will always try to resolve them all
   */
  public boolean isSingleton() {
    return false;
  }

  public abstract boolean canFix(IMarker marker) throws CoreException;

  @Override
  public Point getSelection(IDocument document) {
    return null;
  }

  @Override
  public final String getDisplayString() {
    return getLabel();
  }

  @Override
  public String getDescription() {
    return getLabel();
  }

  @Override
  public String getAdditionalProposalInfo() {
    Object o = getAdditionalProposalInfo(new NullProgressMonitor());
    return o == null ? null : o.toString();
  }

  @Override
  public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
    return getDescription();
  }

  @Override
  public IContextInformation getContextInformation() {
    return null;
  }

  @Override
  public final void run(IMarker marker) {
    run(marker, null);
  }

  @Override
  public final void apply(IDocument document) {
    run(marker, document);
  }

  @Override
  public final void run(IMarker[] markers, IProgressMonitor monitor) {
    fix(markers, null, monitor);
  }

  private void run(IMarker marker, IDocument document) {
    IMarker[] handledMarkers;

    if(isSingleton()) {
      try {
        IMarker[] allMarkers = ResourcesPlugin.getWorkspace().getRoot().findMarkers(null, true,
            IResource.DEPTH_INFINITE);
        handledMarkers = findOtherMarkers(allMarkers, true);
      } catch(CoreException e) {
        // fall back to running with a single marker
        handledMarkers = new IMarker[] {marker};
      }
    } else {
      handledMarkers = new IMarker[] {marker};
    }

    fix(handledMarkers, document, new NullProgressMonitor());
  }

  private IMarker[] findOtherMarkers(IMarker[] markers, boolean includeSelf) {
    List<IMarker> result = new ArrayList<>();
    for(IMarker marker : markers) {
      if(marker == this.marker && !includeSelf) {
        continue;
      }
      try {
        if(canFix(marker)) {
          result.add(marker);
        }
      } catch(CoreException ex) {
        LOG.error(ex.getMessage(), ex);
      }
    }
    return result.toArray(new IMarker[result.size()]);
  }

  @Override
  public final IMarker[] findOtherMarkers(IMarker[] markers) {
    return findOtherMarkers(markers, false);
  }

  public boolean includeResolution(List<? super IMarkerResolution> resolutions) {
    if(shouldBeAdded(resolutions)) {
      resolutions.add(this);
      return true;
    }
    return false;
  }

  public boolean includeProposal(List<? super ICompletionProposal> proposals) {
    if(shouldBeAdded(proposals)) {
      proposals.add(this);
      return true;
    }
    return false;
  }

  private boolean shouldBeAdded(List<?> list) {
    if(isSingleton()) {
      for(Object o : list) {
        if(o.getClass().equals(this.getClass()))
          return false;
      }
    }
    return true;
  }

  protected Set<IProject> getProjects(Stream<IMarker> markers) {
    return markers.map(m -> m.getResource().getProject()).collect(Collectors.toSet());
  }

  public static List<IMarkerResolution> getResolutions(IMarker marker) {
    IMarkerResolution[] resolutions = IDE.getMarkerHelpRegistry().getResolutions(marker);
    List<IMarkerResolution> sortedResolutions = Arrays.asList(resolutions);
    Collections.sort(sortedResolutions,
        Comparator.<IMarkerResolution, Integer> comparing(MavenProblemResolution::getOrder)
            .thenComparing(IMarkerResolution::getLabel));
    return sortedResolutions;
  }

  public static int getOrder(IMarkerResolution res) {
    if(res instanceof MavenProblemResolution) {
      MavenProblemResolution mr = (MavenProblemResolution) res;
      return mr.getOrder();
    }
    return Integer.MAX_VALUE;
  }

  public static boolean hasResolutions(IMarker marker) {
    return IDE.getMarkerHelpRegistry().hasResolutions(marker);
  }
}
