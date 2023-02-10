/*******************************************************************************
 * Copyright (c) 2011-2015 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Red Hat, Inc. - refactored getMojoExecution(IMarker) out to MarkerUtils
 *      Anton Tanasenko - Refactor marker resolutions and quick fixes (Bug #484359)
 *******************************************************************************/

package org.eclipse.m2e.internal.discovery.markers;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;

import org.eclipse.m2e.core.ui.internal.markers.MavenProblemResolution;
import org.eclipse.m2e.core.ui.internal.wizards.MappingDiscoveryJob;
import org.eclipse.m2e.internal.discovery.MavenDiscoveryIcons;
import org.eclipse.m2e.internal.discovery.Messages;


@SuppressWarnings("restriction")
public class DiscoveryWizardResolution extends MavenProblemResolution {

  public DiscoveryWizardResolution(IMarker marker) {
    super(marker);
  }

  public int getOrder() {
    return 10;
  }

  public String getDescription() {
    return Messages.DiscoveryWizardProposal_description;
  }

  public String getLabel() {
    return Messages.DiscoveryWizardProposal_Label;
  }

  public Image getImage() {
    return MavenDiscoveryIcons.getImage(MavenDiscoveryIcons.QUICK_FIX_ICON);
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  @Override
  public boolean canFix(IMarker marker) {
    return MavenDiscoveryMarkerResolutionGenerator.canResolve(marker);
  }

  @Override
  public void fix(IMarker[] markers, IDocument document, IProgressMonitor monitor) {
    Set<IProject> projects = Stream.of(markers).map(m -> m.getResource().getProject()).collect(Collectors.toSet());

    MappingDiscoveryJob discoveryJob = new MappingDiscoveryJob(projects, false);
    discoveryJob.schedule();
  }
}
