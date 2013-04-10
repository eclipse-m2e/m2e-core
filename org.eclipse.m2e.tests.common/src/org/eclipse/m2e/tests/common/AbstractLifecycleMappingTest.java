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
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingResult;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


@SuppressWarnings("restriction")
@RequireMavenExecutionContext
public abstract class AbstractLifecycleMappingTest extends AbstractMavenProjectTestCase {
  protected IMavenProjectRegistry mavenProjectManager;

  protected IProjectConfigurationManager projectConfigurationManager;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    mavenProjectManager = MavenPlugin.getMavenProjectRegistry();
    projectConfigurationManager = MavenPlugin.getProjectConfigurationManager();
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

  private LifecycleMappingMetadataSource loadLifecycleMappingMetadataSourceInternal(File metadataFile)
      throws IOException, XmlPullParserException {
    assertTrue("File does not exist:" + metadataFile.getAbsolutePath(), metadataFile.exists());
    InputStream in = new FileInputStream(metadataFile);
    try {
      LifecycleMappingMetadataSource lifecycleMappingMetadataSource = LifecycleMappingFactory
          .createLifecycleMappingMetadataSource(in);
      return lifecycleMappingMetadataSource;
    } finally {
      IOUtil.close(in);
    }
  }

  protected LifecycleMappingMetadataSource loadLifecycleMappingMetadataSource(String metadataFilename)
      throws IOException, XmlPullParserException {
    return loadLifecycleMappingMetadataSourceInternal(new File(metadataFilename));
  }

  /**
   * Creates new partially initialised MavenProjectFacade instance
   */
  protected MavenProjectFacade newMavenProjectFacade(IFile pom) throws CoreException {
    MavenProject mavenProject = MavenPlugin.getMaven().readProject(pom.getLocation().toFile(), monitor);
    MavenProjectFacade facade = new MavenProjectFacade(MavenPluginActivator.getDefault().getMavenProjectManagerImpl(),
        pom, mavenProject, new ResolverConfiguration());
    return facade;
  }

  protected List<MojoExecutionKey> getNotCoveredMojoExecutions(IMavenProjectFacade facade) {
    List<MojoExecutionKey> result = new ArrayList<MojoExecutionKey>();

    Map<MojoExecutionKey, List<IPluginExecutionMetadata>> executionMapping = facade.getMojoExecutionMapping();

    for(Map.Entry<MojoExecutionKey, List<IPluginExecutionMetadata>> entry : executionMapping.entrySet()) {
      if(entry.getValue() == null || entry.getValue().isEmpty()) {
        if(LifecycleMappingFactory.isInterestingPhase(entry.getKey().getLifecyclePhase())) {
          result.add(entry.getKey());
        }
      }
    }

    return result;
  }

  /**
   * @since 1.4
   */
  protected LifecycleMappingResult calculateLifecycleMapping(MavenProjectFacade facade) throws CoreException {
    MavenProject mavenProject = facade.getMavenProject(monitor);
    List<MojoExecution> mojoExecutions = facade.getMojoExecutions(monitor);
    String lifecycleMappingId = facade.getResolverConfiguration().getLifecycleMappingId();
    return LifecycleMappingFactory.calculateLifecycleMapping(mavenProject, mojoExecutions, lifecycleMappingId, monitor);
  }

}
