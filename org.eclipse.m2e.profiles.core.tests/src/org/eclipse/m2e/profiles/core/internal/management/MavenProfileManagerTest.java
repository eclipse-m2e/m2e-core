/*************************************************************************************
 * Copyright (c) 2011-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/

package org.eclipse.m2e.profiles.core.internal.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import org.eclipse.core.resources.IProject;

import org.apache.maven.model.Profile;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.profiles.core.internal.ProfileData;
import org.eclipse.m2e.profiles.core.internal.ProfileState;


public class MavenProfileManagerTest extends AbstractMavenProfileTest {

  @Test
  public void testUserSelectedProfiles() throws Exception {
    String pomPath = "projects/embedded-profiles/pom.xml";
    IProject project = importProject(pomPath);
    waitForJobsToComplete();
    assertNotNull(pomPath + " could not be imported", project);

    IMavenProjectFacade facade = getFacade(project);
    List<ProfileData> profiles = profileManager.getProfileDatas(facade, monitor);
    assertEquals(3, facade.getMavenProject(monitor).getActiveProfiles().size());
    assertEquals(profiles.toString(), 5, profiles.size());

    //Check default profile status when no user profile is selected
    for(ProfileData p : profiles) {
      String pid = p.getId();
      if("active-by-default".equals(pid)) {
        assertTrue(p.isAutoActive());//has <activeByDefault>true</activeByDefault>
        assertEquals(ProfileState.Active, p.getActivationState());
        assertFalse(p.isUserSelected());
      } else if("inactive-settings-profile".equals(pid)) {
        assertFalse(p.isAutoActive());
        assertEquals(ProfileState.Inactive, p.getActivationState());
        assertFalse(p.isUserSelected());
      } else if("active-settings-profile".equals(pid)) {
        assertTrue(p.isAutoActive());
        assertEquals(ProfileState.Active, p.getActivationState());
        assertFalse(p.isUserSelected());
      } else if("activebydefault-settings-profile".equals(pid)) {
        assertTrue(p.isAutoActive());
        assertEquals(ProfileState.Active, p.getActivationState());
        assertFalse(p.isUserSelected());
      } else if("my-profile".equals(pid)) {
        assertFalse(p.isAutoActive());
        assertEquals(ProfileState.Inactive, p.getActivationState());
        assertFalse(p.isUserSelected());
      }
    }

    profileManager.updateActiveProfiles(facade, Arrays.asList("my-profile"), true, false, monitor);

    facade = getFacade(project);
    assertEquals(3, facade.getMavenProject(monitor).getActiveProfiles().size());

    profiles = profileManager.getProfileDatas(facade, monitor);
    //When a user manually selects a profile, all profiles enabled by default in the pom 
    // are rendered inactive, the ones from settings are still active
    for(ProfileData p : profiles) {
      String pid = p.getId();
      if("active-by-default".equals(pid)) {
        // <activeByDefault> profiles are disabled when user explicitly use profiles
        assertFalse(p.isAutoActive());
        assertEquals(ProfileState.Inactive, p.getActivationState());
        assertFalse(p.isUserSelected());
      } else if("inactive-settings-profile".equals(pid)) {
        assertFalse(p.isAutoActive());
        assertEquals(ProfileState.Inactive, p.getActivationState());
        assertFalse(p.isUserSelected());
      } else if("active-settings-profile".equals(pid)) {
        assertTrue(p.isAutoActive());
        assertEquals(ProfileState.Active, p.getActivationState());
        assertFalse(p.isUserSelected());
      } else if("activebydefault-settings-profile".equals(pid)) {
        assertTrue(p.isAutoActive());
        assertEquals(ProfileState.Active, p.getActivationState());
        assertFalse(p.isUserSelected());
      } else if("my-profile".equals(pid)) {
        assertFalse(p.isAutoActive());
        assertEquals(ProfileState.Active, p.getActivationState());
        assertTrue(p.isUserSelected());
      }
    }

  }

  @Test
  public void testDisabledProfiles() throws Exception {
    String pomPath = "projects/disabled-profiles/pom.xml";
    IProject project = importProject(pomPath);
    waitForJobsToComplete();
    assertNotNull(pomPath + " could not be imported", project);

    IMavenProjectFacade facade = getFacade(project);

    List<ProfileData> profiles = profileManager.getProfileDatas(facade, monitor);

    assertEquals(profiles.toString(), 4, profiles.size());
    for(ProfileData p : profiles) {
      assertNotEquals(ProfileState.Disabled, p.getActivationState());
    }
    List<String> changedProfiles = Arrays
        .asList("!test-disabled-profile, active-settings-profile, inactive-settings-profile,!activebydefault-settings-profile");

    profileManager.updateActiveProfiles(facade, changedProfiles, true, false, monitor);

    facade = getFacade(project);

    assertEquals(2, facade.getMavenProject(monitor).getActiveProfiles().size());

    profiles = profileManager.getProfileDatas(facade, monitor);

    for(ProfileData p : profiles) {
      String pid = p.getId();
      if("inactive-settings-profile".equals(pid) || "active-settings-profile".equals(pid)) {
        assertEquals(p.toString(), ProfileState.Active, p.getActivationState());
      } else {
        assertEquals(p.toString(), ProfileState.Disabled, p.getActivationState());
      }
      assertTrue(p.isUserSelected());
    }
  }

  @Test
  public void testParentFromRemote() throws Exception {
    String pomPath = "projects/profiles-from-parent/pom.xml";
    IProject project = importProject(pomPath);
    waitForJobsToComplete();
    assertNotNull(pomPath + " could not be imported", project);

    IMavenProjectFacade facade = getFacade(project);
    List<ProfileData> profiles = profileManager.getProfileDatas(facade, monitor);
    assertEquals(profiles.toString(), 5, profiles.size());
    for(ProfileData p : profiles) {
      String pid = p.getId();
      if("other-parent-profile".equals(pid)) {
        assertFalse(p.isAutoActive());//parent profile activation is not inherited
        assertEquals(ProfileState.Inactive, p.getActivationState());
      } else if("inactive-settings-profile".equals(pid)) {
        assertFalse(p.isAutoActive());
        assertEquals(ProfileState.Inactive, p.getActivationState());
      } else if("active-settings-profile".equals(pid)) {
        assertTrue(p.isAutoActive());
        assertEquals(ProfileState.Active, p.getActivationState());
      } else if("activebydefault-settings-profile".equals(pid)) {
        assertTrue(p.isAutoActive());
        assertEquals(ProfileState.Active, p.getActivationState());
      } else if("parent-profile".equals(pid)) {
        assertTrue(p.isAutoActive());// True since Bug #441112 fix
      } else {
        fail("Unexpected profile " + pid);
      }
    }
  }

  @Test
  public void testGetAvailableSettingsProfiles() throws Exception {
    Map<Profile, Boolean> profiles = profileManager.getAvailableSettingsProfiles();
    assertEquals(3, profiles.size());
    for(Entry<Profile, Boolean> p : profiles.entrySet()) {
      String pid = p.getKey().getId();
      if("inactive-settings-profile".equals(pid)) {
        assertFalse(p.getValue());
      } else if("active-settings-profile".equals(pid)) {
        assertTrue(p.getValue());
      } else if("activebydefault-settings-profile".equals(pid)) {
        assertTrue(p.getValue());
      } else {
        fail("Unexpected profile " + pid);
      }
    }
  }

  @Test
  public void test441112_InheritParentActiveProfiles() throws Exception {
    String projectsRoot = "projects/com.mygroup.test.bug.parent";

    IProject[] projects = importProjects(projectsRoot, new String[] {"pom.xml", "com.mygroup.test.bug.itest/pom.xml"},
        new ResolverConfiguration());
    waitForJobsToComplete();
    assertNotNull(projectsRoot + " could not be imported", projects);
    assertEquals(2, projects.length);

    IMavenProjectFacade facade = getFacade(projects[1]);
    List<ProfileData> profiles = profileManager.getProfileDatas(facade, monitor);
    assertEquals(profiles.toString(), 7 /*from projects*/+ 3 /*from settings*/, profiles.size());
    for(ProfileData p : profiles) {
      String pid = p.getId();

      switch(pid) {
        case "whenIsIntegrationTestsProjectInsideEclipse":
        case "whenIsIntegrationTestsProjectInParent":
        case "whenIsIntegrationTestsProjectInParentWithPropertyNegation":
        case "whenIsIntegrationTestsProjectInChildPOM":
        case "whenIsIntegrationTestsProjectInChildWithPropertyNegation":
          assertTrue(p.getId() + " should be activated automatically", p.isAutoActive());
          assertEquals(ProfileState.Active, p.getActivationState());
          break;
        case "whenIsIntegrationTestsProjectInParentWithoutProperty":
        case "whenIsIntegrationTestsProjectInChildWithoutProperty":
          assertFalse(p.getId() + " should not be activated automatically", p.isAutoActive());
          assertEquals(ProfileState.Inactive, p.getActivationState());
          break;
        default:
          //ignore settings profiles
      }
    }
  }

  @Test
  public void testLoadingProfilesFromPomsResolvedViaTheirRelativePath() throws Exception {
    // -- Given...
    //
    importProject("projects/relative-path-profiles/poms/parent-pom/pom.xml");
    importProject("projects/relative-path-profiles/poms/classification-poms/api-classifier-pom/pom.xml");
    String pomPath = "projects/relative-path-profiles/modules/module-a/pom.xml";
    IProject project = importProject(pomPath);
    waitForJobsToComplete();
    assertNotNull(pomPath + " could not be imported", project);

    IMavenProjectFacade facade = getFacade(project);

    // -- When...
    //
    List<ProfileData> profiles = profileManager.getProfileDatas(facade, monitor);

    // -- Then...
    //
    Set<String> actualProfileIds = profiles.stream().map(ProfileData::getId).collect(Collectors.toSet());

    assertEquals(
        new HashSet<>(Arrays.asList("module-one", "module-two", "api-one", "api-two", "parent-one", "parent-two",
            "activebydefault-settings-profile", "inactive-settings-profile", "active-settings-profile")),
        actualProfileIds);
  }
}
