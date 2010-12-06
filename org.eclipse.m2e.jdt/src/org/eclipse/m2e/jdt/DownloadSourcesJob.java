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

package org.eclipse.m2e.jdt;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.jobs.IBackgroundProcessingQueue;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.jdt.internal.Messages;


/**
 * DownloadSourcesJob
 * 
 * @author igor
 */
class DownloadSourcesJob extends Job implements IBackgroundProcessingQueue {

  private static class DownloadRequest {
    final IProject project;

    final IPackageFragmentRoot fragment;

    final ArtifactKey artifact;

    final boolean downloadSources;

    final boolean downloadJavaDoc;

    public DownloadRequest(IProject project, IPackageFragmentRoot fragment, ArtifactKey artifact, boolean downloadSources,
        boolean downloadJavaDoc) {
      this.project = project;
      this.fragment = fragment;
      this.artifact = artifact;
      this.downloadSources = downloadSources;
      this.downloadJavaDoc = downloadJavaDoc;
    }

    public int hashCode() {
      int hash = 17;
      hash = hash * 31 + project.hashCode();
      hash = hash * 31 + (fragment != null? fragment.hashCode(): 0);
      hash = hash * 31 + (artifact != null? artifact.hashCode(): 0);
      hash = hash * 31 + (downloadSources ? 1 : 0);
      hash = hash * 31 + (downloadJavaDoc ? 1 : 0);
      return hash;
    }
    
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof DownloadRequest)) {
        return false;
      }
      DownloadRequest other = (DownloadRequest) o;

      return project.equals(other.project)
          && (fragment != null? fragment.equals(other.fragment): other.fragment == null)
          && (artifact != null? artifact.equals(other.artifact): other.artifact == null)
          && downloadSources == other.downloadSources
          && downloadJavaDoc == other.downloadJavaDoc;
    }
  }

  private final IMaven maven;

  private final BuildPathManager manager;

  private final MavenConsole console;

  private final MavenProjectManager projectManager;

  private final ArrayList<DownloadRequest> queue = new ArrayList<DownloadRequest>();

  public DownloadSourcesJob(BuildPathManager manager) {
    super(Messages.DownloadSourcesJob_job_download);
    this.manager = manager;

    this.maven = MavenPlugin.getDefault().getMaven();

    MavenPlugin plugin = MavenPlugin.getDefault();
    this.projectManager = plugin.getMavenProjectManager();
    this.console = plugin.getConsole();
  }

  public IStatus run(IProgressMonitor monitor) {
    ArrayList<DownloadRequest> downloadRequests;

    synchronized(this.queue) {
      downloadRequests = new ArrayList<DownloadRequest>(this.queue);
      this.queue.clear();
    }

    ArrayList<IStatus> exceptions = new ArrayList<IStatus>();

    Set<IProject> mavenProjects = new LinkedHashSet<IProject>();
    Map<IPackageFragmentRoot, File[]> nonMavenProjects = new LinkedHashMap<IPackageFragmentRoot, File[]>();

    for(DownloadRequest request : downloadRequests) {
      if ( request.artifact != null ) try {
        IMavenProjectFacade projectFacade = projectManager.create(request.project, monitor);
        
        if (projectFacade != null) {
          downloadMaven(projectFacade, request.artifact, request.downloadSources, request.downloadJavaDoc, monitor);
          mavenProjects.add(request.project);
        } else {
          List<ArtifactRepository> repositories = maven.getArtifactRepositories();

          File[] files = downloadAttachments(request.artifact, repositories, request.downloadSources, request.downloadJavaDoc, monitor);

          nonMavenProjects.put(request.fragment, files);
        }
      } catch(CoreException ex) {
        exceptions.add(ex.getStatus());
      }
    }

    if(!mavenProjects.isEmpty() || !nonMavenProjects.isEmpty()) {
      ISchedulingRule schedulingRule = ResourcesPlugin.getWorkspace().getRuleFactory().buildRule();
      getJobManager().beginRule(schedulingRule, monitor);
      try {
        for (IProject mavenProject : mavenProjects) {
          manager.updateClasspath(mavenProject, monitor);
        }

        for (Map.Entry<IPackageFragmentRoot, File[]> entry : nonMavenProjects.entrySet()) {
          File[] files = entry.getValue();
          manager.attachSourcesAndJavadoc(entry.getKey(), files[0], files[1], monitor);
        }
      } finally {
        getJobManager().endRule(schedulingRule);
      }
    }
    
    if(!exceptions.isEmpty()) {
      IStatus[] problems = exceptions.toArray(new IStatus[exceptions.size()]);
      return new MultiStatus(MavenJdtPlugin.PLUGIN_ID, -1, problems, "Could not download sources or javadoc", null);
    }

    return Status.OK_STATUS;
  }

  private void downloadMaven(IMavenProjectFacade projectFacade, ArtifactKey artifact, boolean downloadSources, boolean downloadJavadoc, IProgressMonitor monitor) throws CoreException {
    MavenProject mavenProject = projectFacade.getMavenProject(monitor);
    List<ArtifactRepository> repositories = mavenProject.getRemoteArtifactRepositories();
   
    if (artifact != null) {
      downloadAttachments(artifact, repositories, downloadSources, downloadJavadoc, monitor);
    } else {
      for (Artifact a : mavenProject.getArtifacts()) {
        ArtifactKey aKey = new ArtifactKey(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getClassifier());
        downloadAttachments(aKey, repositories, downloadSources, downloadJavadoc, monitor);
      }
    }
  }

  private File[] downloadAttachments(ArtifactKey artifact, List<ArtifactRepository> repositories, boolean downloadSources,
      boolean downloadJavadoc, IProgressMonitor monitor) throws CoreException {

    ArtifactKey[] attached = manager.getAttachedSourcesAndJavadoc(artifact, repositories, downloadSources, downloadJavadoc);

    File[] files = new File[2]; 

    if (attached[0] != null) {
      try {
        files[0] = download(attached[0], repositories, monitor);
        console.logMessage("Downloaded sources for " + artifact.toString());
      } catch (CoreException e) {
        logMessage("Could not download sources for " + artifact.toString(), e); //$NON-NLS-1$
      }
    }

    if (attached[1] != null) {
      try {
        files[1] = download(attached[1], repositories, monitor);
        console.logMessage("Downloaded javadoc for " + artifact.toString());
      } catch (CoreException e) {
        logMessage("Could not download sources for " + artifact.toString(), e); //$NON-NLS-1$
      }
    }

    return files;
  }

  private File download(ArtifactKey artifact, List<ArtifactRepository> repositories, IProgressMonitor monitor) throws CoreException {
    Artifact resolved = maven.resolve(artifact.getGroupId(), //
        artifact.getArtifactId(), //
        artifact.getVersion(), //
        "jar" /*type*/, // //$NON-NLS-1$
        artifact.getClassifier(), // 
        repositories, //
        monitor);
    return resolved.getFile();
  }

  private void logMessage(String msg, CoreException e) {
    MavenLogger.log(msg, e);
    console.logMessage(msg);
  }

  private void scheduleDownload(IProject project, IPackageFragmentRoot fragment, ArtifactKey artifact, boolean downloadSources, boolean downloadJavadoc) {
    if (project == null || !project.isAccessible()) {
      return;
    }

    synchronized(this.queue) {
      queue.add(new DownloadRequest(project, fragment, artifact, downloadSources, downloadJavadoc));
    }

    schedule(1000L);
  }

  /**
   * If artifact is not null, download sources and/or javadoc of this artifact.
   * If artifact is null, download sources and/or javadoc of all project dependencies.
   * Entire project classpath is updated after download.
   * Does nothing if both downloadSources and downloadJavadoc are false.
   */
  public void scheduleDownload(IProject project, ArtifactKey artifact, boolean downloadSources, boolean downloadJavadoc) {
    scheduleDownload(project, null, artifact, downloadSources, downloadJavadoc);
  }

  public void scheduleDownload(IPackageFragmentRoot fragment, ArtifactKey artifact, boolean downloadSources, boolean downloadJavadoc) {
    IProject project = fragment.getJavaProject().getProject();
    scheduleDownload(project, fragment, artifact, downloadSources, downloadJavadoc);
  }

  public boolean isEmpty() {
    synchronized(queue) {
      return queue.isEmpty();
    }
  }
}
