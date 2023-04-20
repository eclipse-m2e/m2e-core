/*******************************************************************************
 * Copyright (c) 2023 Ben Gilbert and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core;

import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.m2e.core.embedder.ArtifactRef;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class MavenDependencyTest extends AbstractMavenProjectTestCase {

    @After
    public void clearWorkspace() throws Exception {
        for (IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
            p.delete(true, null);
        }
    }

    @Test
    public void testArtifactsIncludeClassifier() throws Exception {
        IProject project = importProject("resources/projects/classifier/pom.xml");
        project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
        waitForJobsToComplete(monitor);

        IMavenProjectFacade facade = MavenPluginActivator.getDefault().getMavenProjectManagerImpl().create(project, monitor);
        Assert.assertNotNull(facade);

        Set<ArtifactRef> artifacts = facade.getMavenProjectArtifacts();
        Assert.assertNotNull(artifacts);

        List<String> portableRefs = artifacts.stream()
            .map(ref -> ref.artifactKey().toPortableString())
            .toList();
        // Confirm two different artifacts
        Assert.assertEquals("com.example:dependency-1:0.0.1-SNAPSHOT::", portableRefs.get(0));
        Assert.assertEquals("com.example:dependency-1:0.0.1-SNAPSHOT:classifier:", portableRefs.get(1));
    }

}
