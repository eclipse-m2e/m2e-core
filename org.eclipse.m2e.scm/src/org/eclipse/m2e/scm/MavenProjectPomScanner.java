/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.scm;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Scm;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.project.AbstractProjectScanner;


/**
 * Maven project scanner using dependency list
 * 
 * @author Eugene Kuleshov
 */
public class MavenProjectPomScanner<T> extends AbstractProjectScanner<MavenProjectScmInfo> {
  private static final Logger log = LoggerFactory.getLogger(MavenProjectPomScanner.class);

  private final boolean developer;

  private final Dependency[] dependencies;

  private IMaven maven;

  public MavenProjectPomScanner(boolean developer, Dependency[] dependencies) {
    this.developer = developer;
    this.dependencies = dependencies;
    this.maven = MavenPlugin.getMaven();
  }

  public String getDescription() {
    if(dependencies.length == 1) {
      Dependency d = dependencies[0];
      return d.getGroupId()
          + ":" + d.getArtifactId() + ":" + d.getVersion() + (d.getClassifier() == null ? "" : ":" + d.getClassifier()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }
    return "" + dependencies.length + " projects"; //$NON-NLS-1$
  }

  public void run(IProgressMonitor monitor) throws InterruptedException {
    for(Dependency d : dependencies) {
      if(monitor.isCanceled()) {
        throw new InterruptedException();
      }

      try {
        Model model = resolveModel(d.getGroupId(), d.getArtifactId(), d.getVersion(), monitor);
        if(model == null) {
          String msg = "Can't resolve " + d.getArtifactId();
          Exception error = new Exception(msg);
          log.error(msg, error);
          addError(error);
          continue;
        }

        Scm scm = resolveScm(model, monitor);
        if(scm == null) {
          String msg = "No SCM info for " + d.getArtifactId();
          Exception error = new Exception(msg);
          log.error(msg, error);
          addError(error);
          continue;
        }

        String tag = scm.getTag();

        log.info(d.getArtifactId());
        log.info("Connection: " + scm.getConnection());
        log.info("       dev: " + scm.getDeveloperConnection());
        log.info("       url: " + scm.getUrl());
        log.info("       tag: " + tag);

        String connection;
        if(developer) {
          connection = scm.getDeveloperConnection();
          if(connection == null) {
            String msg = d.getArtifactId() + " doesn't specify developer SCM connection";
            Exception error = new Exception(msg);
            log.error(msg, error);
            addError(error);
            continue;
          }
        } else {
          connection = scm.getConnection();
          if(connection == null) {
            String msg = d.getArtifactId() + " doesn't specify SCM connection";
            Exception error = new Exception(msg);
            log.error(msg, error);
            addError(error);
            continue;
          }
        }

        // connection: scm:svn:https://svn.apache.org/repos/asf/incubator/wicket/branches/wicket-1.2.x/wicket
        //        dev: scm:svn:https://svn.apache.org/repos/asf/incubator/wicket/branches/wicket-1.2.x/wicket
        //        url: http://svn.apache.org/viewvc/incubator/wicket/branches/wicket-1.2.x/wicket
        //        tag: HEAD  

        // TODO add an option to select all modules/projects and optimize scan 

        if(connection.endsWith("/")) { //$NON-NLS-1$
          connection = connection.substring(0, connection.length() - 1);
        }

        int n = connection.lastIndexOf("/"); //$NON-NLS-1$
        String label = (n == -1 ? connection : connection.substring(n)) + "/" + IMavenConstants.POM_FILE_NAME; //$NON-NLS-1$

        addProject(new MavenProjectScmInfo(label, model, null, tag, connection, connection));

      } catch(Exception ex) {
        addError(ex);
        String msg = "Error reading " + d.getArtifactId();
        log.error(msg, ex);
      }
    }
  }

  private Scm resolveScm(Model model, IProgressMonitor monitor) throws ArtifactResolutionException,
      ArtifactNotFoundException, XmlPullParserException, IOException, CoreException {
    Scm scm = model.getScm();
    if(scm != null) {
      return scm;
    }

    Parent parent = model.getParent();
    if(parent == null) {
      return null;
    }

    Model parentModel = resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), monitor);
    if(parentModel == null) {
      return null;
    }

    Scm parentScm = resolveScm(parentModel, monitor);
    if(parentScm == null) {
      return null;
    }

    Set<String> modules = new HashSet<String>(parentModel.getModules());
    List<Profile> parentModelProfiles = parentModel.getProfiles();
    for(Profile profile : parentModelProfiles) {
      modules.addAll(profile.getModules());
    }

    // heuristics for matching module names to artifactId
    String artifactId = model.getArtifactId();
    for(String module : modules) {
      if(module.equals(artifactId) || module.endsWith("/" + artifactId)) { //$NON-NLS-1$
        if(parentScm.getConnection() != null) {
          parentScm.setConnection(parentScm.getConnection() + "/" + module); //$NON-NLS-1$
        }
        if(parentScm.getDeveloperConnection() != null) {
          parentScm.setDeveloperConnection(parentScm.getDeveloperConnection() + "/" + module); //$NON-NLS-1$
        }
        return parentScm;
      }
    }

    // XXX read modules from profiles

    return parentScm;
  }

  private Model resolveModel(String groupId, String artifactId, String version, IProgressMonitor monitor)
      throws CoreException {
    monitor.subTask(NLS.bind(Messages.MavenProjectPomScanner_task_resolving,
        new Object[] {groupId, artifactId, version}));

    List<ArtifactRepository> repositories = maven.getArtifactRepositories();
    Artifact artifact = maven.resolve(groupId, artifactId, version, "pom", null, repositories, monitor); //$NON-NLS-1$

    File file = artifact.getFile();
    if(file == null) {
      return null;
    }

    // XXX this fail on reading extensions
    // MavenProject project = embedder.readProject(file);

    monitor.subTask(NLS.bind(Messages.MavenProjectPomScanner_23, new Object[] {groupId, artifactId, version}));
    return maven.readModel(file);
  }

}
