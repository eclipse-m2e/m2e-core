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

package org.eclipse.m2e.core.project.configurator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.ResolverConfiguration;


/**
 * Generic project configurator that using Maven plugins
 * 
 * @author Eugene Kuleshov
 * @see AbstractProjectConfigurator
 */
public class MavenProjectConfigurator extends AbstractProjectConfigurator {

  String pluginKey;

  List<String> goals;

  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
    if(pluginKey == null || goals == null) {
      return;
    }

    MavenProject mavenProject = request.getMavenProject();
    Build build = mavenProject.getBuild();
    if(build != null) {
      Map<String, Plugin> pluginMap = build.getPluginsAsMap();
      Plugin mavenPlugin = pluginMap.get(pluginKey);
      if(mavenPlugin != null) {
        IFile pomFile = request.getPom();
        ResolverConfiguration resolverConfiguration = request.getResolverConfiguration();
        // MavenPlugin plugin = MavenPlugin.getDefault();
        try {
          IMaven maven = MavenPlugin.getDefault().getMaven();
          MavenExecutionRequest executionRequest = projectManager.createExecutionRequest(pomFile, resolverConfiguration, monitor);
          executionRequest.setGoals(goals);
          maven.execute(executionRequest, monitor);
        } catch(Exception ex) {
          String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
          console.logError(msg);
          MavenLogger.log(msg, ex);
        }

        try {
          request.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
        } catch(CoreException ex) {
          IStatus status = ex.getStatus();
          String msg = status.getMessage();
          Throwable t = status.getException();
          console.logError(msg + (t == null ? "" : "; " + t.toString())); //$NON-NLS-1$ //$NON-NLS-2$
          MavenLogger.log(ex);
        }
      }
    }
  }

  public String getPluginKey() {
    return this.pluginKey;
  }

  public List<String> getGoals() {
    return this.goals;
  }

  // IExecutableExtension  

  @Override
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    super.setInitializationData(config, propertyName, data);

    Pattern pattern = Pattern.compile("(.+?)\\:(.+?)\\|(.+)"); //$NON-NLS-1$
    String params = (String) data;
    if(params != null) {
      Matcher matcher = pattern.matcher(params);
      if(matcher.find() && matcher.groupCount() == 3) {
        pluginKey = matcher.group(1) + ":" + matcher.group(2); //$NON-NLS-1$
        goals = Arrays.asList(matcher.group(3).split("\\|")); //$NON-NLS-1$
        return;
      }
    }
    MavenLogger.log("Unable to parse configuration for project configurator " + getId() + "; " + data, null); //$NON-NLS-2$
  }

  @Override
  public String toString() {
    return super.toString() + " " + pluginKey + goals; //$NON-NLS-1$
  }
  
}
