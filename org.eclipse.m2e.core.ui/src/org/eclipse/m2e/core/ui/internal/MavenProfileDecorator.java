/*******************************************************************************
 * Copyright (c) 2026 Hélios GILLES and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal;

import static java.util.function.Predicate.not;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IProjectConfiguration;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;


/**
 * Lightweight decorator appending the Maven profiles explicitly selected for a project (as configured with
 * <em>Maven &gt; Select Maven Profiles...</em>) to its label, using the Maven command line syntax, e.g.
 * <code>my-project -P profile1,!profile2</code>.
 */
public class MavenProfileDecorator extends BaseLabelProvider
    implements ILightweightLabelDecorator, IMavenProjectChangedListener {

  private static final String PROFILES_PREFIX = " -P "; //$NON-NLS-1$

  private static final String INACTIVE_PROFILE_PREFIX = "!"; //$NON-NLS-1$

  private static final String PROFILES_SEPARATOR = ","; //$NON-NLS-1$

  public MavenProfileDecorator() {
    MavenPlugin.getMavenProjectRegistry().addMavenProjectChangedListener(this);
  }

  @Override
  public void decorate(Object element, IDecoration decoration) {
    IProject project = Adapters.adapt(element, IProject.class);
    if(project == null) {
      return;
    }
    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);
    if(facade == null) {
      return;
    }
    String profiles = getProfilesLabel(facade.getConfiguration());
    if(!profiles.isEmpty()) {
      decoration.addSuffix(PROFILES_PREFIX + profiles);
    }
  }

  /**
   * @return the selected profiles of the given configuration as a comma separated list, inactive profiles being
   *         prefixed with <code>!</code>, or an empty string if no profile is selected.
   */
  static String getProfilesLabel(IProjectConfiguration configuration) {
    if(configuration == null) {
      return ""; //$NON-NLS-1$
    }
    // profile lists may contain empty entries, e.g. when profiles are separated by ", "
    Stream<String> activeProfiles = configuration.getActiveProfileList().stream().filter(not(String::isBlank));
    Stream<String> inactiveProfiles = configuration.getInactiveProfileList().stream().filter(not(String::isBlank))
        .map(profile -> INACTIVE_PROFILE_PREFIX + profile);
    return Stream.concat(activeProfiles, inactiveProfiles).distinct().collect(Collectors.joining(PROFILES_SEPARATOR));
  }

  @Override
  public void mavenProjectChanged(List<MavenProjectChangedEvent> events, IProgressMonitor monitor) {
    IProject[] projects = events.stream()
        .flatMap(event -> Stream.of(event.getMavenProject(), event.getOldMavenProject())).filter(Objects::nonNull)
        .map(IMavenProjectFacade::getProject).filter(Objects::nonNull).distinct().toArray(IProject[]::new);
    if(projects.length > 0) {
      fireLabelProviderChanged(new LabelProviderChangedEvent(this, projects));
    }
  }

  @Override
  public void dispose() {
    MavenPlugin.getMavenProjectRegistry().removeMavenProjectChangedListener(this);
    super.dispose();
  }
}
