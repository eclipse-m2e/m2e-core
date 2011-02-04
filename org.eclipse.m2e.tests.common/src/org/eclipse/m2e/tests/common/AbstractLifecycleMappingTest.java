/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.lifecycle.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecycle.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.internal.lifecycle.model.io.xpp3.LifecycleMappingMetadataSourceXpp3Reader;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


@SuppressWarnings("restriction")
public abstract class AbstractLifecycleMappingTest extends AbstractMavenProjectTestCase {
  protected MavenProjectManager mavenProjectManager;

  protected IProjectConfigurationManager projectConfigurationManager;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    mavenProjectManager = MavenPlugin.getDefault().getMavenProjectManager();
    projectConfigurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
  }

  @Override
  protected void tearDown() throws Exception {
    projectConfigurationManager = null;
    mavenProjectManager = null;

    super.tearDown();
  }

  protected IMavenProjectFacade importMavenProject(String basedir, String pomName) throws Exception {
    ResolverConfiguration configuration = new ResolverConfiguration();
    IProject[] project = importProjects(basedir, new String[] {pomName}, configuration);
    waitForJobsToComplete();

    return mavenProjectManager.create(project[0], monitor);
  }

  protected LifecycleMappingMetadataSource loadLifecycleMappingMetadataSource(String metadataFilename)
      throws IOException, XmlPullParserException {
    File metadataFile = new File(metadataFilename);
    assertTrue("File does not exist:" + metadataFile.getAbsolutePath(), metadataFile.exists());
    InputStream in = new FileInputStream(metadataFile);
    try {
      LifecycleMappingMetadataSource lifecycleMappingMetadataSource = new LifecycleMappingMetadataSourceXpp3Reader()
          .read(in);
      return lifecycleMappingMetadataSource;
    } finally {
      IOUtil.close(in);
    }
  }

  /**
   * Creates new partially initialised MavenProjectFacade instance
   */
  protected MavenProjectFacade newMavenProjectFacade(IFile pom) throws CoreException {
    MavenProject mavenProject = plugin.getMaven().readProject(pom.getLocation().toFile(), monitor);
    MavenExecutionRequest request = plugin.getMaven().createExecutionRequest(monitor);
    MavenSession session = plugin.getMaven().createSession(request, mavenProject);
    MavenExecutionPlan executionPlan = plugin.getMaven().calculateExecutionPlan(session, mavenProject,
        Arrays.asList("deploy"), false, monitor);
    MavenProjectFacade facade = new MavenProjectFacade(plugin.getMavenProjectManagerImpl(), pom, mavenProject,
        executionPlan.getMojoExecutions(), new ResolverConfiguration());
    return facade;
  }

  protected List<MojoExecutionKey> getNotCoveredMojoExecutions(IMavenProjectFacade facade) {
    List<MojoExecutionKey> result = new ArrayList<MojoExecutionKey>();

    Map<MojoExecutionKey, List<PluginExecutionMetadata>> executionMapping = facade.getMojoExecutionMapping();

    for(Map.Entry<MojoExecutionKey, List<PluginExecutionMetadata>> entry : executionMapping.entrySet()) {
      if(entry.getValue() == null || entry.getValue().isEmpty()) {
        if (LifecycleMappingFactory.isInterestingPhase(entry.getKey().getLifecyclePhase())) {
          result.add(entry.getKey());
        }
      }
    }

    return result;
  }

}
