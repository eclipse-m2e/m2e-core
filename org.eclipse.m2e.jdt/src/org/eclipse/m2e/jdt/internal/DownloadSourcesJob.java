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

package org.eclipse.m2e.jdt.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.jobs.IBackgroundProcessingQueue;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.jdt.MavenJdtPlugin;


/**
 * DownloadSourcesJob
 * 
 * @author igor
 */
class DownloadSourcesJob extends Job implements IBackgroundProcessingQueue {
  private static Logger log = LoggerFactory.getLogger(DownloadSourcesJob.class);

  private static final long SCHEDULE_INTERVAL = 1000L;

  private static class DownloadRequest {
    final IProject project;

    final IPackageFragmentRoot fragment;

    final ArtifactKey artifact;

    final boolean downloadSources;

    final boolean downloadJavaDoc;

    public DownloadRequest(IProject project, IPackageFragmentRoot fragment, ArtifactKey artifact,
        boolean downloadSources, boolean downloadJavaDoc) {
      this.project = project;
      this.fragment = fragment;
      this.artifact = artifact;
      this.downloadSources = downloadSources;
      this.downloadJavaDoc = downloadJavaDoc;
    }

    public int hashCode() {
      int hash = 17;
      hash = hash * 31 + project.hashCode();
      hash = hash * 31 + (fragment != null ? fragment.hashCode() : 0);
      hash = hash * 31 + (artifact != null ? artifact.hashCode() : 0);
      hash = hash * 31 + (downloadSources ? 1 : 0);
      hash = hash * 31 + (downloadJavaDoc ? 1 : 0);
      return hash;
    }

    public boolean equals(Object o) {
      if(this == o) {
        return true;
      }
      if(!(o instanceof DownloadRequest)) {
        return false;
      }
      DownloadRequest other = (DownloadRequest) o;

      return project.equals(other.project)
          && (fragment != null ? fragment.equals(other.fragment) : other.fragment == null)
          && (artifact != null ? artifact.equals(other.artifact) : other.artifact == null)
          && downloadSources == other.downloadSources && downloadJavaDoc == other.downloadJavaDoc;
    }
  }

  private final IMaven maven;

  private final BuildPathManager manager;

  private final IMavenProjectRegistry projectManager;

  private final ArrayList<DownloadRequest> queue = new ArrayList<DownloadRequest>();

  public DownloadSourcesJob(BuildPathManager manager) {
    super(Messages.DownloadSourcesJob_job_download);
    this.manager = manager;

    this.maven = MavenPlugin.getMaven();

    this.projectManager = MavenPlugin.getMavenProjectRegistry();
  }

  public IStatus run(IProgressMonitor monitor) {
    final ArrayList<DownloadRequest> downloadRequests;

    synchronized(this.queue) {
      downloadRequests = new ArrayList<DownloadRequest>(this.queue);
      this.queue.clear();
    }

    try {
      return maven.execute(new ICallable<IStatus>() {
        public IStatus call(IMavenExecutionContext context, IProgressMonitor monitor) {
          return run(downloadRequests, monitor);
        }
      }, monitor);
    } catch(CoreException ex) {
      return ex.getStatus();
    }
  }

  IStatus run(ArrayList<DownloadRequest> downloadRequests, IProgressMonitor monitor) {
    SubMonitor subMonitor = SubMonitor.convert(monitor, 3 * downloadRequests.size() + 5);
    final ArrayList<IStatus> exceptions = new ArrayList<IStatus>();
    final Set<IProject> mavenProjects = new LinkedHashSet<IProject>();
    final Map<IPackageFragmentRoot, File[]> nonMavenProjects = new LinkedHashMap<IPackageFragmentRoot, File[]>();

    for(DownloadRequest request : downloadRequests) {
      SubMonitor requestMonitor = subMonitor.split(3);
      try {
        if(request.artifact != null) {
          requestMonitor.setTaskName(getName() + ": " + request.artifact.getArtifactId());
        } else if(request.project != null) {
          requestMonitor.setTaskName(getName() + ": " + request.project.getName());
        }
        IMavenProjectFacade projectFacade = projectManager.create(request.project, requestMonitor.split(1));
        if(projectFacade != null) {
          boolean hasDownloadedFiles = downloadMaven(projectFacade, request.artifact, request.downloadSources,
              request.downloadJavaDoc, requestMonitor.split(2));
          if(hasDownloadedFiles) {
            //only perform later classpath update if something changed
            mavenProjects.add(request.project);
          }
        } else if(request.artifact != null) {
          List<ArtifactRepository> repositories = maven.getArtifactRepositories();
          File[] files = downloadAttachments(request.artifact, repositories, request.downloadSources,
              request.downloadJavaDoc, requestMonitor.split(2));
          if(request.fragment == null) {
            log.warn(
                "IPackageFragmentRoot is missing, skipping javadoc/source attachment for project " + request.project);
          } else {
            nonMavenProjects.put(request.fragment, files);
          }
        }
      } catch(CoreException ex) {
        exceptions.add(ex.getStatus());
      }
      requestMonitor.done();
    }

    // consider update classpath after each individual download?
    // pro: user gets sources progressively (then faster)
    // con: more save operations
    SubMonitor updateMonitor = SubMonitor.convert(subMonitor.split(5),
        1 + mavenProjects.size() + nonMavenProjects.size());
    if(!mavenProjects.isEmpty() || !nonMavenProjects.isEmpty()) {
      ISchedulingRule schedulingRule = ResourcesPlugin.getWorkspace().getRuleFactory().buildRule();
      getJobManager().beginRule(schedulingRule, updateMonitor.split(1));
      try {
        for(IProject mavenProject : mavenProjects) {
          manager.updateClasspath(mavenProject, updateMonitor.split(1));
        }

        for(Map.Entry<IPackageFragmentRoot, File[]> entry : nonMavenProjects.entrySet()) {
          File[] files = entry.getValue();
          manager.attachSourcesAndJavadoc(entry.getKey(), files[0], files[1], updateMonitor.split(1));
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

  private boolean downloadMaven(IMavenProjectFacade projectFacade, ArtifactKey artifact, boolean downloadSources,
      boolean downloadJavadoc, IProgressMonitor monitor) throws CoreException {
    MavenProject mavenProject = projectFacade.getMavenProject(monitor);
    List<ArtifactRepository> repositories = mavenProject.getRemoteArtifactRepositories();
    boolean hasDownloadedFiles = false;
    File[] files = null;
    if(artifact != null) {
      files = downloadAttachments(artifact, repositories, downloadSources, downloadJavadoc, monitor);
      hasDownloadedFiles = isNotEmpty(files);
    } else {
      for(Artifact a : mavenProject.getArtifacts()) {
        ArtifactKey aKey = new ArtifactKey(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getClassifier());
        files = downloadAttachments(aKey, repositories, downloadSources, downloadJavadoc, monitor);
        hasDownloadedFiles = hasDownloadedFiles || isNotEmpty(files);
      }
    }
    return hasDownloadedFiles;
  }

  private boolean isNotEmpty(File[] files) {
    return files != null && (files[0] != null || files[1] != null);
  }

  private File[] downloadAttachments(ArtifactKey artifact, List<ArtifactRepository> repositories,
      boolean downloadSources, boolean downloadJavadoc, IProgressMonitor monitor) throws CoreException {
    if(monitor != null && monitor.isCanceled()) {
      String message = "Downloading of sources/javadocs was canceled"; //$NON-NLS-1$
      log.debug(message);
      synchronized(queue) {
        queue.clear();
      }
      throw new OperationCanceledException(message);
    }
    ArtifactKey[] attached = manager.getAttachedSourcesAndJavadoc(artifact, repositories, downloadSources,
        downloadJavadoc);

    File[] files = new File[2];

    if(attached[0] != null) {
      try {
        files[0] = download(attached[0], repositories, monitor);
        log.info("Downloaded sources for " + artifact.toString());
      } catch(CoreException e) {
        log.error("Could not download sources for " + artifact.toString(), e); //$NON-NLS-1$
      }
    }
    if(monitor != null) {
      monitor.worked(1);
    }
    if(attached[1] != null) {
      try {
        files[1] = download(attached[1], repositories, monitor);
        log.info("Downloaded javadoc for " + artifact.toString());
      } catch(CoreException e) {
        log.error("Could not download javadoc for " + artifact.toString(), e); //$NON-NLS-1$
      }
    }
    if(monitor != null) {
      monitor.worked(1);
    }
    return files;
  }

  private File download(ArtifactKey artifact, List<ArtifactRepository> repositories, IProgressMonitor monitor)
      throws CoreException {
    Artifact resolved = maven.resolve(artifact.getGroupId(), //
        artifact.getArtifactId(), //
        artifact.getVersion(), //
        "jar" /*type*/, // //$NON-NLS-1$
        artifact.getClassifier(), // 
        repositories, //
        monitor);
    return resolved.getFile();
  }

  private void scheduleDownload(IProject project, IPackageFragmentRoot fragment, ArtifactKey artifact,
      boolean downloadSources, boolean downloadJavadoc) {
    addDownloadRequest(project, fragment, artifact, downloadSources, downloadJavadoc);

    schedule(SCHEDULE_INTERVAL);
  }

  public void addDownloadRequest(IProject project, IPackageFragmentRoot fragment, ArtifactKey artifact,
      boolean downloadSources, boolean downloadJavadoc) {
    if(project == null || !project.isAccessible()) {
      return;
    }

    synchronized(this.queue) {
      queue.add(new DownloadRequest(project, fragment, artifact, downloadSources, downloadJavadoc));
    }
  }

  /**
   * If artifact is not null, download sources and/or javadoc of this artifact. If artifact is null, download sources
   * and/or javadoc of all project dependencies. Entire project classpath is updated after download. Does nothing if
   * both downloadSources and downloadJavadoc are false.
   */
  public void scheduleDownload(IProject project, ArtifactKey artifact, boolean downloadSources,
      boolean downloadJavadoc) {
    scheduleDownload(project, null, artifact, downloadSources, downloadJavadoc);
  }

  public void scheduleDownload(IPackageFragmentRoot fragment, ArtifactKey artifact, boolean downloadSources,
      boolean downloadJavadoc) {
    IProject project = fragment.getJavaProject().getProject();
    scheduleDownload(project, fragment, artifact, downloadSources, downloadJavadoc);
  }

  public boolean isEmpty() {
    synchronized(queue) {
      return queue.isEmpty();
    }
  }
}
