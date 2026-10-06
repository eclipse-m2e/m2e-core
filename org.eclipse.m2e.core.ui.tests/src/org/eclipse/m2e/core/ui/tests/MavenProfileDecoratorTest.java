/*******************************************************************************
 * Copyright (c) 2026 Hélios GILLES and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core.ui.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.ui.internal.MavenProfileDecorator;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


@SuppressWarnings("restriction")
public class MavenProfileDecoratorTest extends AbstractMavenProjectTestCase {

  private static final String PROJECT_POM = "resources/projects/profiles/pom.xml";

  private MavenProfileDecorator decorator;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    decorator = new MavenProfileDecorator();
  }

  @Override
  @After
  public void tearDown() throws Exception {
    try {
      decorator.dispose();
    } finally {
      super.tearDown();
    }
  }

  @Test
  public void testNoDecorationWithoutSelectedProfiles() throws Exception {
    IProject project = importProject(PROJECT_POM);
    waitForJobsToComplete();

    assertEquals(List.of(), decorate(project));
  }

  @Test
  public void testActiveAndInactiveProfiles() throws Exception {
    IProject project = importProject(PROJECT_POM, configuration("profile1, !profile2"));
    waitForJobsToComplete();

    assertEquals(List.of(" -P profile1,!profile2"), decorate(project));
  }

  @Test
  public void testDuplicateProfilesAreIgnored() throws Exception {
    IProject project = importProject(PROJECT_POM, configuration("profile1,profile2 profile1"));
    waitForJobsToComplete();

    assertEquals(List.of(" -P profile1,profile2"), decorate(project));
  }

  @Test
  public void testNoDecorationForNonMavenElement() throws Exception {
    assertEquals(List.of(), decorate(new Object()));
    assertEquals(List.of(), decorate(workspace.getRoot().getProject("notMaven")));
  }

  @Test
  public void testLabelUpdatedWhenProfilesChange() throws Exception {
    IProject project = importProject(PROJECT_POM);
    waitForJobsToComplete();

    List<LabelProviderChangedEvent> events = new CopyOnWriteArrayList<>();
    ILabelProviderListener listener = events::add;
    decorator.addListener(listener);
    try {
      MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, configuration("profile2"),
          monitor);
      waitForJobsToComplete();
    } finally {
      decorator.removeListener(listener);
    }

    assertTrue("Expected a label change notification for " + project, events.stream()
        .anyMatch(event -> event.getElements() != null && Arrays.asList(event.getElements()).contains(project)));
    assertEquals(List.of(" -P profile2"), decorate(project));
  }

  private List<String> decorate(Object element) {
    RecordingDecoration decoration = new RecordingDecoration();
    decorator.decorate(element, decoration);
    assertEquals(List.of(), decoration.prefixes);
    return decoration.suffixes;
  }

  private static ResolverConfiguration configuration(String selectedProfiles) {
    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setSelectedProfiles(selectedProfiles);
    return configuration;
  }
}
