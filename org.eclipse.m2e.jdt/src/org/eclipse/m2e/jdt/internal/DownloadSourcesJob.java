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

package org.eclipse.m2e.jdt.internal;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.jobs.IBackgroundProcessingQueue;
import org.eclipse.m2e.core.internal.jobs.MavenJob;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.jdt.MavenJdtPlugin;


/**
 * DownloadSourcesJob
 *
 * @author igor
 */
@SuppressWarnings("restriction")
class DownloadSourcesJob extends MavenJob implements IBackgroundProcessingQueue {
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

    @Override
    public int hashCode() {
      return Objects.hash(project, fragment, artifact, downloadSources, downloadJavaDoc);
    }

    @Override
    public boolean equals(Object o) {
      if(this == o) {
        return true;
      }
      return o instanceof DownloadRequest other && //
          project.equals(other.project) && Objects.equals(fragment, other.fragment)
          && Objects.equals(artifact, other.artifact) && downloadSources == other.downloadSources
          && downloadJavaDoc == other.downloadJavaDoc;
    }
  }

  private static final class Attachments {
    public final File javadoc;

    public final File sources;

    public Attachments(File javadoc, File sources) {
      this.javadoc = javadoc;
      this.sources = sources;
    }

    public boolean isNotEmpty() {
      return sources != null || javadoc != null;
    }
  }

  private final IMaven maven;

  private final BuildPathManager manager;

  private final IMavenProjectRegistry projectManager;

  private final BlockingQueue<DownloadRequest> queue = new LinkedBlockingQueue<>();

  private Set<DownloadRequest> requests = new HashSet<>();

  private final Set<IProject> toUpdateMavenProjects = new HashSet<>();

  private final Map<IPackageFragmentRoot, Attachments> toUpdateAttachments = new HashMap<>();

  public DownloadSourcesJob(BuildPathManager manager) {
    super(Messages.DownloadSourcesJob_job_download);
    this.manager = manager;

    this.maven = MavenPlugin.getMaven();

    this.projectManager = MavenPlugin.getMavenProjectRegistry();
  }

  @Override
  public IStatus run(IProgressMonitor monitor) {
    int totalWork = 2 * queue.size();
    SubMonitor subMonitor = SubMonitor.convert(monitor, totalWork);
    while(!queue.isEmpty() && !monitor.isCanceled()) {
      final DownloadRequest request = queue.poll();
      try {
        // Process requests one by one to not fill the maven context with too many projects at once and retain a lot of RAM
        IStatus status = maven.execute((context, aMonitor) -> downloadFilesAndPopulateToUpdate(request, aMonitor),
            subMonitor.split(1));
        if(!status.isOK()) {
          // or maybe just log and ignore?
          queue.clear();
          requests.clear();
          toUpdateAttachments.clear();
          toUpdateMavenProjects.clear();
          return status;
        }
      } catch(CoreException ex) {
        return ex.getStatus();
      }
    }
    if(monitor.isCanceled()) {
      queue.clear();
      requests.clear();
      toUpdateAttachments.clear();
      toUpdateMavenProjects.clear();
      return Status.CANCEL_STATUS;
    }

    if(!toUpdateAttachments.isEmpty() || !toUpdateMavenProjects.isEmpty()) {
      // consider update classpath after each individual download?
      // pro: user gets sources progressively (then faster)
      // con: more save operations
      updateClasspath(manager, toUpdateMavenProjects, toUpdateAttachments, subMonitor.split(totalWork / 2));
      toUpdateAttachments.clear();
      toUpdateMavenProjects.clear();
    }
    // updateClasspath might has added new requests to the queue.
    requests.clear(); // Retain in requests all elements in queue (in an efficient manner)
    requests.addAll(queue);
    subMonitor.done();
    return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
  }

  private static void updateClasspath(BuildPathManager manager, Set<IProject> toUpdateMavenProjects,
      Map<IPackageFragmentRoot, Attachments> toUpdateAttachments, IProgressMonitor monitor) {
    SubMonitor updateMonitor = SubMonitor.convert(monitor, Messages.DownloadSourcesJob_job_associateWithClasspath,
        1 + toUpdateMavenProjects.size() + toUpdateAttachments.size());
    ISchedulingRule schedulingRule = ResourcesPlugin.getWorkspace().getRuleFactory().buildRule();
    getJobManager().beginRule(schedulingRule, updateMonitor.split(1));
    try {
      for(IProject mavenProject : toUpdateMavenProjects) {
        updateMonitor
            .setTaskName(Messages.DownloadSourcesJob_job_associateWithClasspath + " - " + mavenProject.getName());
        manager.updateClasspath(mavenProject, updateMonitor.split(1));
      }
      for(Map.Entry<IPackageFragmentRoot, Attachments> entry : toUpdateAttachments.entrySet()) {
        updateMonitor.setTaskName(
            Messages.DownloadSourcesJob_job_associateWithClasspath + " - " + entry.getKey().getElementName());
        manager.attachSourcesAndJavadoc(entry.getKey(), entry.getValue().sources, entry.getValue().javadoc,
            updateMonitor.split(1));
      }
    } finally {
      getJobManager().endRule(schedulingRule);
      updateMonitor.done();
    }
  }

  IStatus downloadFilesAndPopulateToUpdate(DownloadRequest request, IProgressMonitor monitor) {
    SubMonitor requestMonitor = SubMonitor.convert(monitor, 33);
    try {
      if(request.artifact != null) {
        requestMonitor.setTaskName(getName() + ": " + request.artifact.artifactId());
      } else if(request.project != null) {
        requestMonitor.setTaskName(getName() + ": " + request.project.getName());
      }
      IMavenProjectFacade projectFacade = projectManager.create(request.project, requestMonitor.split(1));
      if(projectFacade != null) {
        Attachments files = downloadMaven(projectFacade, request.artifact, request.downloadSources,
            request.downloadJavaDoc, requestMonitor.split(2));
        if(files != null && files.isNotEmpty()) {
          //only perform later classpath update if something changed
          toUpdateMavenProjects.add(request.project);
        }
      } else if(request.artifact != null) {
        List<ArtifactRepository> repositories = maven.getArtifactRepositories();
        Attachments files = downloadAttachments(request.artifact, repositories, request.downloadSources,
            request.downloadJavaDoc, requestMonitor.split(2));
        if(request.fragment == null) {
          log.warn(
              "IPackageFragmentRoot is missing, skipping javadoc/source attachment for project " + request.project);
        } else {
          toUpdateAttachments.put(request.fragment, files);
        }
      }
      return Status.OK_STATUS;
    } catch(CoreException ex) {
      return new MultiStatus(MavenJdtPlugin.PLUGIN_ID, -1, new IStatus[] {ex.getStatus()},
          "Could not download sources or javadoc", null);
    } finally {
      requestMonitor.done();
    }
  }

  private Attachments downloadMaven(IMavenProjectFacade projectFacade, ArtifactKey artifact, boolean downloadSources,
      boolean downloadJavadoc, IProgressMonitor monitor) throws CoreException {
    MavenProject mavenProject = projectFacade.getMavenProject(monitor);
    List<ArtifactRepository> repositories = mavenProject.getRemoteArtifactRepositories();
    Attachments files = null;
    if(artifact != null) {
      files = downloadAttachments(artifact, repositories, downloadSources, downloadJavadoc, monitor);
    } else {
      for(Artifact a : mavenProject.getArtifacts()) {
        ArtifactKey aKey = new ArtifactKey(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getClassifier());
        files = downloadAttachments(aKey, repositories, downloadSources, downloadJavadoc, monitor);
      }
    }
    if(files != null && files.isNotEmpty()) {
      return files;
    }
    return null;
  }

  /**
   * @return null if no attachment was found, the found attachments otherwise
   * @throws CoreException
   */
  private Attachments downloadAttachments(ArtifactKey artifact, List<ArtifactRepository> repositories,
      boolean downloadSources, boolean downloadJavadoc, IProgressMonitor monitor) throws CoreException {
    if(monitor != null && monitor.isCanceled()) {
      String message = "Downloading of sources/javadocs was canceled"; //$NON-NLS-1$
      log.debug(message);
      throw new OperationCanceledException(message);
    }
    ArtifactKey[] attached = manager.getAttachedSourcesAndJavadoc(artifact, repositories, downloadSources,
        downloadJavadoc);

    File source = null;
    if(attached[0] != null) {
      try {
        source = download(attached[0], repositories, monitor);
        log.info("Downloaded sources for " + artifact.toString());
      } catch(CoreException e) {
        log.error("Could not download sources for " + artifact.toString(), e); //$NON-NLS-1$
      }
    }
    if(monitor != null) {
      monitor.worked(1);
    }
    File javadoc = null;
    if(attached[1] != null) {
      try {
        javadoc = download(attached[1], repositories, monitor);
        log.info("Downloaded javadoc for " + artifact.toString());
      } catch(CoreException e) {
        log.error("Could not download javadoc for " + artifact.toString(), e); //$NON-NLS-1$
      }
    }
    if(source == null && javadoc == null) {
      return null;
    }
    return new Attachments(javadoc, source);
  }

  private File download(ArtifactKey artifact, List<ArtifactRepository> repositories, IProgressMonitor monitor)
      throws CoreException {
    Artifact resolved = maven.resolve(artifact.groupId(), //
        artifact.artifactId(), //
        artifact.version(), //
        "jar" /*type*/, // //$NON-NLS-1$
        artifact.classifier(), //
        repositories, //
        monitor);
    return resolved.getFile();
  }

  private void scheduleDownload(IProject project, IPackageFragmentRoot fragment, ArtifactKey artifact,
      boolean downloadSources, boolean downloadJavadoc) {
    if(project == null || !project.isAccessible()) {
      return;
    }
    DownloadRequest request = new DownloadRequest(project, fragment, artifact, downloadSources, downloadJavadoc);
    if(requests.add(request)) { // guard against new requests that are/will be already downloaded in this run to prevent endless loops
      queue.add(request);
      schedule(SCHEDULE_INTERVAL);
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

  @Override
  public boolean isEmpty() {
    return queue.isEmpty();
  }
}
