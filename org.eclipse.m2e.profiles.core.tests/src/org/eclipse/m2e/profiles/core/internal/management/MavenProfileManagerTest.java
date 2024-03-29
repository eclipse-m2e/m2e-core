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
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.profiles.core.internal.IProfileManager;
import org.eclipse.m2e.profiles.core.internal.ProfileData;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;


@SuppressWarnings("restriction")
public class MavenProfileManagerTest extends AbstractMavenProjectTestCase {

	private static ServiceTracker<IProfileManager, IProfileManager> profileManagerTracker;

	@BeforeClass
	public static void setUpProfileManagerTracker() throws BundleException {
		BundleContext context = FrameworkUtil.getBundle(MavenProfileManagerTest.class).getBundleContext();
		profileManagerTracker = new ServiceTracker<>(context, IProfileManager.class, null);
		profileManagerTracker.open();
	}

	@AfterClass
	public static void tearDownProfileManagerTracker() {
		profileManagerTracker.close();
	}

  @Test
  public void testLoadingProfilesFromPomsResolvedViaTheirRelativePath() throws Exception {
    // -- Given...
    //
    importProject("resources/projects/relative-path-profiles/poms/parent-pom/pom.xml");
    importProject("resources/projects/relative-path-profiles/poms/classification-poms/api-classifier-pom/pom.xml");
    String pomPath = "resources/projects/relative-path-profiles/modules/module-a/pom.xml";
    IProject project = importProject(pomPath);
    waitForJobsToComplete();
    assertNotNull(pomPath + " could not be imported", project);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project.getFile(IMavenConstants.POM_FILE_NAME), true, monitor);

    // -- When...
    //
	List<ProfileData> profiles = profileManagerTracker.getService().getProfileDatas(facade, monitor);

    // -- Then...
    //
    Set<String> actualProfileIds = profiles.stream().map(ProfileData::getId).collect(Collectors.toSet());

    assertEquals(
        new HashSet<>(Arrays.asList("module-one", "module-two", "api-one", "api-two", "parent-one", "parent-two", "resolved-one", "resolved-two", "active-settings-profile")),
        actualProfileIds);
  }
}
