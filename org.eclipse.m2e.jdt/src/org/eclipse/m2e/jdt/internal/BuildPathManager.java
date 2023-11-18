/*******************************************************************************
 * Copyright (c) 2008-2021 Sonatype, Inc.
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.jdt.IClasspathManagerDelegate;


/**
 * This class is responsible for mapping Maven classpath to JDT and back.
 */
@SuppressWarnings("restriction")
public class BuildPathManager implements IMavenProjectChangedListener, IResourceChangeListener, IClasspathManager {
  private static final Logger log = LoggerFactory.getLogger(BuildPathManager.class);

  public static final int SOURCE_DOWNLOAD_PRIORITY = Job.DECORATE;//Low priority

  // local repository variable
  public static final String M2_REPO = "M2_REPO"; //$NON-NLS-1$

  private static final String PROPERTY_SRC_ROOT = ".srcRoot"; //$NON-NLS-1$

  private static final String PROPERTY_SRC_ENCODING = ".srcEncoding"; //$NON-NLS-1$

  private static final String PROPERTY_SRC_PATH = ".srcPath"; //$NON-NLS-1$

  private static final String PROPERTY_JAVADOC_URL = ".javadoc"; //$NON-NLS-1$

  public static final String CLASSIFIER_SOURCES = "sources"; //$NON-NLS-1$

  public static final String CLASSIFIER_JAVADOC = "javadoc"; //$NON-NLS-1$

  public static final String CLASSIFIER_TESTS = "tests"; //$NON-NLS-1$

  public static final String CLASSIFIER_TESTSOURCES = "test-sources"; //$NON-NLS-1$

  public static final ArtifactFilter SCOPE_FILTER_RUNTIME = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);

  public static final ArtifactFilter SCOPE_FILTER_TEST = new ScopeArtifactFilter(Artifact.SCOPE_TEST);

  final IMavenProjectRegistry projectManager;

  final IMavenConfiguration mavenConfiguration;

  final BundleContext bundleContext;

  final IMaven maven;

  final File stateLocationDir;

  final Map<URI, InternalModuleInfo> moduleInfosMap = new ConcurrentHashMap<>();

  private final DownloadSourcesJob downloadSourcesJob;

  private final DefaultClasspathManagerDelegate defaultDelegate;

  public BuildPathManager(IMavenProjectRegistry projectManager, BundleContext bundleContext, File stateLocationDir) {
    this.projectManager = projectManager;
    this.mavenConfiguration = MavenPlugin.getMavenConfiguration();
    this.bundleContext = bundleContext;
    this.stateLocationDir = stateLocationDir;
    this.maven = MavenPlugin.getMaven();
    this.downloadSourcesJob = new DownloadSourcesJob(this);
    downloadSourcesJob.setPriority(SOURCE_DOWNLOAD_PRIORITY);
    this.defaultDelegate = new DefaultClasspathManagerDelegate();
  }

  public static IClasspathEntry getMavenContainerEntry(IJavaProject javaProject) {
    if(javaProject != null) {
      try {
        for(IClasspathEntry entry : javaProject.getRawClasspath()) {
          if(MavenClasspathHelpers.isMaven2ClasspathContainer(entry.getPath())) {
            return entry;
          }
        }
      } catch(JavaModelException ex) {
        return null;
      }
    }
    return null;
  }

  public static IClasspathContainer getMaven2ClasspathContainer(IJavaProject project) throws JavaModelException {
    IClasspathEntry[] entries = project.getRawClasspath();
    for(IClasspathEntry entry : entries) {
      if(entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER
          && MavenClasspathHelpers.isMaven2ClasspathContainer(entry.getPath())) {
        return JavaCore.getClasspathContainer(entry.getPath(), project);
      }
    }
    return null;
  }

  @Override
  public void mavenProjectChanged(List<MavenProjectChangedEvent> events, IProgressMonitor monitor) {
    Set<IProject> projects = new HashSet<>();
    monitor.setTaskName(Messages.BuildPathManager_monitor_setting_cp);
    for(MavenProjectChangedEvent event : events) {
      IFile pom = event.getSource();
      IProject project = pom.getProject();
      if(project.isAccessible() && projects.add(project)) {
        updateClasspath(project, monitor);
      }
    }
  }

  @Override
  public void updateClasspath(IProject project, IProgressMonitor monitor) {
    IJavaProject javaProject = JavaCore.create(project);
    if(javaProject != null) {
      try {
        IClasspathEntry containerEntry = getMavenContainerEntry(javaProject);
        IPath path = containerEntry != null ? containerEntry.getPath() : IPath.fromOSString(CONTAINER_ID);
        IClasspathEntry[] classpath = getClasspath(project, monitor);
        IClasspathContainer container = new MavenClasspathContainer(path, classpath);
        JavaCore.setClasspathContainer(container.getPath(), new IJavaProject[] {javaProject},
            new IClasspathContainer[] {container}, monitor);
        saveContainerState(project, container);
      } catch(CoreException ex) {
        log.error(ex.getMessage(), ex);
      }
    }
  }

  private void saveContainerState(IProject project, IClasspathContainer container) {
    File containerStateFile = getContainerStateFile(project);
    try (FileOutputStream is = new FileOutputStream(containerStateFile)) {
      new MavenClasspathContainerSaveHelper().writeContainer(container, is);
    } catch(IOException ex) {
      log.error("Can't save classpath container state for " + project.getName(), ex); //$NON-NLS-1$
    }
  }

  public IClasspathContainer getSavedContainer(IProject project) throws CoreException {
    File containerStateFile = getContainerStateFile(project);
    if(!containerStateFile.exists()) {
      return null;
    }

    try (FileInputStream is = new FileInputStream(containerStateFile)) {
      ;
      return new MavenClasspathContainerSaveHelper().readContainer(is);
    } catch(IOException | ClassNotFoundException ex) {
      throw new CoreException(Status.error("Can't read classpath container state for " + project.getName(), ex));
    }
  }

  private IClasspathEntry[] getClasspath(IMavenProjectFacade projectFacade, final int kind,
      final Properties sourceAttachment, boolean uniquePaths, final IProgressMonitor monitor) throws CoreException {

    final ClasspathDescriptor classpath = new ClasspathDescriptor(uniquePaths);

    getDelegate(projectFacade, monitor).populateClasspath(classpath, projectFacade, kind, monitor);

    configureAttachedSourcesAndJavadoc(projectFacade, sourceAttachment, classpath, monitor);

    IClasspathEntry[] entries = classpath.getEntries();

    if(uniquePaths) {
      Map<IPath, IClasspathEntry> paths = new LinkedHashMap<>();
      for(IClasspathEntry entry : entries) {
        if(!paths.containsKey(entry.getPath())) {
          paths.put(entry.getPath(), entry);
        }
      }
      return paths.values().toArray(new IClasspathEntry[paths.size()]);
    }

    return entries;
  }

  private IClasspathManagerDelegate getDelegate(IMavenProjectFacade projectFacade, IProgressMonitor monitor) {
    ILifecycleMapping lifecycleMapping = LifecycleMappingFactory.getLifecycleMapping(projectFacade);
    return lifecycleMapping instanceof IClasspathManagerDelegate classpathManager ? classpathManager : defaultDelegate;
  }

  private void configureAttachedSourcesAndJavadoc(IMavenProjectFacade facade, Properties sourceAttachment,
      ClasspathDescriptor classpath, IProgressMonitor monitor) throws CoreException {
    for(IClasspathEntryDescriptor desc : classpath.getEntryDescriptors()) {
      if(IClasspathEntry.CPE_LIBRARY == desc.getEntryKind() && desc.getSourceAttachmentPath() == null) {
        ArtifactKey a = desc.getArtifactKey();
        String key = desc.getPath().toPortableString();

        IPath srcPath = desc.getSourceAttachmentPath();
        IPath srcRoot = desc.getSourceAttachmentRootPath();
        if(srcPath == null && sourceAttachment != null && sourceAttachment.containsKey(key + PROPERTY_SRC_PATH)) {
          srcPath = IPath.fromPortableString((String) sourceAttachment.get(key + PROPERTY_SRC_PATH));
          if(sourceAttachment.containsKey(key + PROPERTY_SRC_ROOT)) {
            srcRoot = IPath.fromPortableString((String) sourceAttachment.get(key + PROPERTY_SRC_ROOT));
          }
        }
        if(srcPath == null && a != null) {
          srcPath = getSourcePath(a);
        }
        if(sourceAttachment != null) {
          String srcEncoding = sourceAttachment.getProperty(key + PROPERTY_SRC_ENCODING);
          if(srcEncoding != null) {
            desc.getClasspathAttributes().put(IClasspathAttribute.SOURCE_ATTACHMENT_ENCODING, srcEncoding);
          }
        }

        // configure javadocs if available
        String javaDocUrl = desc.getJavadocUrl();
        if(javaDocUrl == null && sourceAttachment != null && sourceAttachment.containsKey(key + PROPERTY_JAVADOC_URL)) {
          javaDocUrl = (String) sourceAttachment.get(key + PROPERTY_JAVADOC_URL);
        }
        if(javaDocUrl == null && a != null) {
          javaDocUrl = getJavaDocUrl(a);
        }

        desc.setSourceAttachment(srcPath, srcRoot);
        desc.setJavadocUrl(javaDocUrl);

        ArtifactKey aKey = desc.getArtifactKey();
        if(aKey != null) { // maybe we should try to find artifactKey little harder here?
          boolean isSnapshot = aKey.version().endsWith("-SNAPSHOT");
          // We should update a sources/javadoc jar for a snapshot in case they're already downloaded.
          File mainFile = desc.getPath() != null ? desc.getPath().toFile() : null;
          File srcFile = srcPath != null ? srcPath.toFile() : null;
          boolean downloadSources = (srcPath == null && mavenConfiguration.isDownloadSources())
              || (isSnapshot && isLastModifiedBefore(srcFile, mainFile));
          File javaDocFile = javaDocUrl != null ? getAttachedArtifactFile(aKey, CLASSIFIER_JAVADOC) : null;
          boolean downloadJavaDoc = (javaDocUrl == null && mavenConfiguration.isDownloadJavaDoc())
              || (isSnapshot && isLastModifiedBefore(javaDocFile, mainFile));
          scheduleDownload(facade.getProject(), facade.getMavenProject(monitor), aKey, downloadSources,
              downloadJavaDoc);
        }
      }
    }
  }

  private static boolean isLastModifiedBefore(File file, File ref) {
    return ref != null && ref.canRead() && file != null && file.canRead() && file.lastModified() < ref.lastModified();
  }

  private static final String ARTIFACT_TYPE_JAR = "jar";

  private boolean isUnavailable(ArtifactKey a, List<ArtifactRepository> repositories) throws CoreException {
    return maven.isUnavailable(a.groupId(), a.artifactId(), a.version(), ARTIFACT_TYPE_JAR /*type*/, a.classifier(),
        repositories);
  }

//  public void downloadSources(IProject project, ArtifactKey artifact, boolean downloadSources, boolean downloadJavaDoc) throws CoreException {
//    List<ArtifactRepository> repositories = null;
//    IMavenProjectFacade facade = projectManager.getProject(project);
//    if (facade != null) {
//      MavenProject mavenProject =  facade.getMavenProject();
//      if (mavenProject != null) {
//        repositories = mavenProject.getRemoteArtifactRepositories();
//      }
//    }
//    doDownloadSources(project, artifact, downloadSources, downloadJavaDoc, repositories);
//  }

  public IClasspathEntry[] getClasspath(IProject project, int scope, IProgressMonitor monitor) throws CoreException {
    return getClasspath(project, scope, true, monitor);
  }

  @Override
  public IClasspathEntry[] getClasspath(IProject project, int scope, boolean uniquePaths, IProgressMonitor monitor)
      throws CoreException {
    IMavenProjectFacade facade = projectManager.create(project, monitor);
    if(facade == null) {
      return new IClasspathEntry[0];
    }
    try {
      Properties props = new Properties();
      File file = getSourceAttachmentPropertiesFile(project);
      if(file.canRead()) {
        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
          props.load(is);
        }
      }
      return getClasspath(facade, scope, props, uniquePaths, monitor);
    } catch(IOException e) {
      throw new CoreException(Status.error("Can't save classpath container changes", e));
    }
  }

  public IClasspathEntry[] getClasspath(IProject project, IProgressMonitor monitor) throws CoreException {
    return getClasspath(project, CLASSPATH_DEFAULT, monitor);
  }

  /**
   * Downloads artifact sources using background job. If path is null, downloads sources for all classpath entries of
   * the project, otherwise downloads sources for the first classpath entry with the given path.
   */
//  public void downloadSources(IProject project, IPath path) throws CoreException {
//    downloadSourcesJob.scheduleDownload(project, path, findArtifacts(project, path), true, false);
//  }

  /**
   * Downloads artifact JavaDocs using background job. If path is null, downloads sources for all classpath entries of
   * the project, otherwise downloads sources for the first classpath entry with the given path.
   */
//  public void downloadJavaDoc(IProject project, IPath path) throws CoreException {
//    downloadSourcesJob.scheduleDownload(project, path, findArtifacts(project, path), false, true);
//  }

  private Set<ArtifactKey> findArtifacts(IProject project, IPath path) throws CoreException {
    ArrayList<IClasspathEntry> entries = findClasspathEntries(project, path);

    Set<ArtifactKey> artifacts = new LinkedHashSet<>();

    for(IClasspathEntry entry : entries) {
      ArtifactKey artifact = findArtifactByArtifactKey(entry);

      if(artifact != null) {
        artifacts.add(artifact);
      }
    }

    return artifacts;
  }

  public ArtifactKey findArtifact(IProject project, IPath path) throws CoreException {
    if(path != null) {
      Set<ArtifactKey> artifacts = findArtifacts(project, path);
      // it is not possible to have more than one classpath entry with the same path
      if(!artifacts.isEmpty()) {
        return artifacts.iterator().next();
      }
    }
    return null;
  }

  private ArtifactKey findArtifactByArtifactKey(IClasspathEntry entry) {
    IClasspathAttribute[] attributes = entry.getExtraAttributes();
    String groupId = null;
    String artifactId = null;
    String version = null;
    String classifier = null;
    for(IClasspathAttribute attribute : attributes) {
      if(GROUP_ID_ATTRIBUTE.equals(attribute.getName())) {
        groupId = attribute.getValue();
      } else if(ARTIFACT_ID_ATTRIBUTE.equals(attribute.getName())) {
        artifactId = attribute.getValue();
      } else if(VERSION_ATTRIBUTE.equals(attribute.getName())) {
        version = attribute.getValue();
      } else if(CLASSIFIER_ATTRIBUTE.equals(attribute.getName())) {
        classifier = attribute.getValue();
      }
    }

    if(groupId != null && artifactId != null && version != null) {
      return new ArtifactKey(groupId, artifactId, version, classifier);
    }
    return null;
  }

  // TODO should it be just one entry?
  private ArrayList<IClasspathEntry> findClasspathEntries(IProject project, IPath path) throws JavaModelException {
    ArrayList<IClasspathEntry> entries = new ArrayList<>();

    IJavaProject javaProject = JavaCore.create(project);
    addEntries(entries, javaProject.getRawClasspath(), path);

    IClasspathContainer container = getMaven2ClasspathContainer(javaProject);
    if(container != null) {
      addEntries(entries, container.getClasspathEntries(), path);
    }
    return entries;
  }

  private void addEntries(Collection<IClasspathEntry> collection, IClasspathEntry[] entries, IPath path) {
    for(IClasspathEntry entry : entries) {
      if(entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY && (path == null || path.equals(entry.getPath()))) {
        collection.add(entry);
      }
    }
  }

  /**
   * Extracts and persists custom source/javadoc attachment info
   */
  public void persistAttachedSourcesAndJavadoc(IJavaProject project, IClasspathContainer containerSuggestion,
      IProgressMonitor monitor) throws CoreException {
    IFile pom = project.getProject().getFile(IMavenConstants.POM_FILE_NAME);
    IMavenProjectFacade facade = projectManager.create(pom, false, null);
    if(facade == null) {
      return;
    }

    // collect all source/javadoc attachement
    Properties props = new Properties();
    IClasspathEntry[] entries = containerSuggestion.getClasspathEntries();
    for(IClasspathEntry entry : entries) {
      if(IClasspathEntry.CPE_LIBRARY == entry.getEntryKind()) {
        String path = entry.getPath().toPortableString();
        if(entry.getSourceAttachmentPath() != null) {
          props.put(path + PROPERTY_SRC_PATH, entry.getSourceAttachmentPath().toPortableString());
        }
        if(entry.getSourceAttachmentRootPath() != null) {
          props.put(path + PROPERTY_SRC_ROOT, entry.getSourceAttachmentRootPath().toPortableString());
        }
        String sourceAttachmentEncoding = getSourceAttachmentEncoding(entry);
        if(sourceAttachmentEncoding != null) {
          props.put(path + PROPERTY_SRC_ENCODING, sourceAttachmentEncoding);
        }
        String javadocUrl = getJavadocLocation(entry);
        if(javadocUrl != null) {
          props.put(path + PROPERTY_JAVADOC_URL, javadocUrl);
        }
      }
    }

    // eliminate all "standard" source/javadoc attachement we get from local repo
    entries = getClasspath(facade, CLASSPATH_DEFAULT, null, true, monitor);
    for(IClasspathEntry entry : entries) {
      if(IClasspathEntry.CPE_LIBRARY == entry.getEntryKind()) {
        String path = entry.getPath().toPortableString();
        String value = (String) props.get(path + PROPERTY_SRC_PATH);
        if(value != null && entry.getSourceAttachmentPath() != null
            && value.equals(entry.getSourceAttachmentPath().toPortableString())) {
          props.remove(path + PROPERTY_SRC_PATH);
        }
        value = (String) props.get(path + PROPERTY_SRC_ROOT);
        if(value != null && entry.getSourceAttachmentRootPath() != null
            && value.equals(entry.getSourceAttachmentRootPath().toPortableString())) {
          props.remove(path + PROPERTY_SRC_ROOT);
        }
      }
    }

    // persist custom source/javadoc attachement info
    File file = getSourceAttachmentPropertiesFile(project.getProject());
    try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
      props.store(os, null);
    } catch(IOException e) {
      throw new CoreException(Status.error("Can't save classpath container changes", e));
    }

    // update classpath container. suboptimal as this will re-calculate classpath
    updateClasspath(project.getProject(), monitor);
  }

  /** public for unit tests only */
  public String getJavadocLocation(IClasspathEntry entry) {
    return MavenClasspathHelpers.getAttribute(entry, IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME);
  }

  public String getSourceAttachmentEncoding(IClasspathEntry entry) {
    return MavenClasspathHelpers.getAttribute(entry, IClasspathAttribute.SOURCE_ATTACHMENT_ENCODING);
  }

  /** public for unit tests only */
  public File getSourceAttachmentPropertiesFile(IProject project) {
    return new File(stateLocationDir, project.getName() + ".sources"); //$NON-NLS-1$
  }

  /** public for unit tests only */
  public File getContainerStateFile(IProject project) {
    return new File(stateLocationDir, project.getName() + ".container"); //$NON-NLS-1$
  }

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    int type = event.getType();
    if(IResourceChangeEvent.PRE_DELETE == type) {
      // remove custom source and javadoc configuration
      IProject project = (IProject) event.getResource();
      File attachmentProperties = getSourceAttachmentPropertiesFile(project);
      if(attachmentProperties.exists() && !attachmentProperties.delete()) {
        log.error("Can't delete " + attachmentProperties.getAbsolutePath()); //$NON-NLS-1$
      }

      // remove classpath container state
      File containerState = getContainerStateFile(project);
      if(containerState.exists() && !containerState.delete()) {
        log.error("Can't delete " + containerState.getAbsolutePath()); //$NON-NLS-1$
      }

      moduleInfosMap.remove(project.getLocationURI());

    } else if(IResourceChangeEvent.POST_CHANGE == type) {

      IResourceDelta delta = event.getDelta(); // workspace delta
      IResourceDelta[] resourceDeltas = delta.getAffectedChildren();
      final Set<IProject> affectedProjects = new LinkedHashSet<>(resourceDeltas.length);
      ModuleInfoDetector visitor = new ModuleInfoDetector(affectedProjects);
      for(IResourceDelta d : resourceDeltas) {
        IProject project = (IProject) d.getResource();
        if(!ModuleSupport.isMavenJavaProject(project)) {
          continue;
        }
        try {
          d.accept(visitor, false);
        } catch(CoreException ex) {
          log.error(ex.getMessage(), ex);
        }
      }
      if(affectedProjects.isEmpty()) {
        return;
      }

      Job job = new WorkspaceJob(Messages.BuildPathManager_update_module_path_job_name) {
        @Override
        public IStatus runInWorkspace(IProgressMonitor monitor) {
          SubMonitor subMonitor = SubMonitor.convert(monitor, affectedProjects.size());
          for(IProject p : affectedProjects) {
            if(monitor.isCanceled()) {
              return Status.CANCEL_STATUS;
            }
            if(requiresUpdate(p, subMonitor)) {
              monitor.setTaskName(p.getName());
              updateClasspath(p, subMonitor.newChild(1));
            }
          }
          return Status.OK_STATUS;
        }

        private boolean requiresUpdate(IProject p, IProgressMonitor monitor) {
          if(!ModuleSupport.isMavenJavaProject(p)) {
            return false;
          }

          IJavaProject jp = JavaCore.create(p);
          try {
            IModuleDescription moduleDescription = jp.getModuleDescription();
            if(moduleDescription == null) {
              return false;
            }
            URI location = p.getLocationURI();
            InternalModuleInfo newModuleInfo = ModuleSupport.getModuleInfo(jp, monitor);
            if(monitor.isCanceled()) {
              return false;
            }
            // Probably not the best way to detect if module path has changed, like, on the very 1st time a
            // module-info.java is modified, there will be no previous state to compare to, but should work
            // well enough the rest of the time, for cases that don't involve obscure module path configs
            InternalModuleInfo oldModuleInfo = moduleInfosMap.get(location);
            if(Objects.equals(newModuleInfo, oldModuleInfo)) {
              return false;
            }
            moduleInfosMap.put(location, newModuleInfo);
            return true;
          } catch(JavaModelException ex) {
            log.error(ex.getMessage(), ex);
          }
          return false;
        }
      };
      job.setRule(MavenPlugin.getProjectConfigurationManager().getRule());
      job.schedule();
    }
  }

  public boolean setupVariables() {
    boolean changed = false;
    try {
      File localRepositoryDir = new File(maven.getLocalRepository().getBasedir());
      IPath oldPath = JavaCore.getClasspathVariable(M2_REPO);
      IPath newPath = IPath.fromOSString(localRepositoryDir.getAbsolutePath());
      JavaCore.setClasspathVariable(M2_REPO, //
          newPath, //
          new NullProgressMonitor());
      changed = !newPath.equals(oldPath);
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
      changed = false;
    }
    return changed;
  }

  public boolean variablesAreInUse() {
    try {
      IJavaModel model = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
      IJavaProject[] projects = model.getJavaProjects();
      for(IJavaProject project : projects) {
        IClasspathEntry[] entries = project.getRawClasspath();
        for(IClasspathEntry curr : entries) {
          if(curr.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
            String var = curr.getPath().segment(0);
            if(M2_REPO.equals(var)) {
              return true;
            }
          }
        }
      }
    } catch(JavaModelException e) {
      return true;
    }
    return false;
  }

  static String getSourcesClassifier(String baseClassifier) {
    return BuildPathManager.CLASSIFIER_TESTS.equals(baseClassifier) ? BuildPathManager.CLASSIFIER_TESTSOURCES
        : BuildPathManager.CLASSIFIER_SOURCES;
  }

  private IPath getSourcePath(ArtifactKey a) {
    File file = getAttachedArtifactFile(a, getSourcesClassifier(a.classifier()));

    if(file != null) {
      return IPath.fromOSString(file.getAbsolutePath());
    }

    return null;
  }

  /**
   * Resolves artifact from local repository. Returns null if the artifact is not available locally
   */
  private File getAttachedArtifactFile(ArtifactKey a, String classifier) {
    // can't use Maven resolve methods since they mark artifacts as not-found even if they could be resolved remotely
    try {
      ArtifactRepository localRepository = maven.getLocalRepository();
      String relPath = maven.getArtifactPath(localRepository, a.groupId(), a.artifactId(), a.version(), "jar", //$NON-NLS-1$
          classifier);
      File file = new File(localRepository.getBasedir(), relPath).getCanonicalFile();
      if(file.canRead()) {
        return file;
      }
    } catch(CoreException ex) {
      // fall through
    } catch(IOException ex) {
      // fall through
    }
    return null;
  }

  private String getJavaDocUrl(ArtifactKey base) {
    File file = getAttachedArtifactFile(base, CLASSIFIER_JAVADOC);

    return getJavaDocUrl(file);
  }

  static String getJavaDocUrl(File file) {
    try {
      if(file != null) {
        URL fileUrl = file.toURI().toURL();
        return "jar:" + fileUrl.toExternalForm() + "!/" + getJavaDocPathInArchive(file); //$NON-NLS-1$ //$NON-NLS-2$
      }
    } catch(MalformedURLException ex) {
      // fall through
    }

    return null;
  }

  private static String getJavaDocPathInArchive(File file) {
    try (ZipFile jarFile = new ZipFile(file);) {
      String marker = "package-list"; //$NON-NLS-1$
      for(Enumeration<? extends ZipEntry> en = jarFile.entries(); en.hasMoreElements();) {
        ZipEntry entry = en.nextElement();
        String entryName = entry.getName();
        if(entryName.endsWith(marker)) {
          return entry.getName().substring(0, entryName.length() - marker.length());
        }
      }
    } catch(IOException ex) {
      // ignore
    }

    return ""; //$NON-NLS-1$
  }

  /**
   * this is for unit tests only!
   */
  public Job getDownloadSourcesJob() {
    return downloadSourcesJob;
  }

  @Override
  public void scheduleDownload(IPackageFragmentRoot fragment, boolean downloadSources, boolean downloadJavadoc) {
    if(fragment == null) {
      return;
    }
    ArtifactKey artifact = fragment.getAdapter(ArtifactKey.class);
    if(artifact == null) {
      // we don't know anything about this JAR/ZIP
      return;
    }
    scheduleDownload(fragment, artifact, downloadSources, downloadJavadoc);
  }

  /**
   * Download sources for an {@link IPackageFragmentRoot} that has already been identified as the given
   * <code>artifact</code>. <br/>
   * TODO promote to API in {@link IClasspathManager} once this as been battle-tested.
   *
   * @since 1.16.0
   */
  public void scheduleDownload(IPackageFragmentRoot fragment, ArtifactKey artifact, boolean downloadSources,
      boolean downloadJavadoc) {
    if(fragment == null || artifact == null) {
      return;
    }

    IProject project = fragment.getJavaProject().getProject();

    try {
      if(project.hasNature(IMavenConstants.NATURE_ID)) {
        IMavenProjectFacade facade = projectManager.getProject(project);
        MavenProject mavenProject = facade != null ? facade.getMavenProject() : null;
        if(mavenProject != null) {
          scheduleDownload(project, mavenProject, artifact, downloadSources, downloadJavadoc);
        } else {
          downloadSourcesJob.scheduleDownload(project, artifact, downloadSources, downloadJavadoc);
        }
      } else {
        // this is a non-maven project
        List<ArtifactRepository> repositories = maven.getArtifactRepositories();
        ArtifactKey[] attached = getAttachedSourcesAndJavadoc(artifact, repositories, downloadSources, downloadJavadoc);

        if(attached[0] != null || attached[1] != null) {
          downloadSourcesJob.scheduleDownload(fragment, artifact, downloadSources, downloadJavadoc);
        }
      }
    } catch(CoreException e) {
      log.error("Could not schedule sources/javadoc download", e); //$NON-NLS-1$
    }

  }

  @Override
  public void scheduleDownload(final IProject project, final boolean downloadSources, final boolean downloadJavadoc) {
    try {
      if(project != null && project.isAccessible() && project.hasNature(IMavenConstants.NATURE_ID)) {
        IMavenProjectFacade facade = projectManager.getProject(project);
        MavenProject mavenProject = facade != null ? facade.getMavenProject() : null;
        if(mavenProject != null) {
          for(Artifact artifact : mavenProject.getArtifacts()) {
            ArtifactKey artifactKey = new ArtifactKey(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getBaseVersion(), artifact.getClassifier());
            scheduleDownload(project, mavenProject, artifactKey, downloadSources, downloadJavadoc);
          }
        } else {
          // project is not in the cache, push all processing to the background job
          downloadSourcesJob.scheduleDownload(project, null, downloadSources, downloadJavadoc);
        }
      }
    } catch(CoreException e) {
      log.error("Could not schedule sources/javadoc download", e); //$NON-NLS-1$
    }
  }

  private void scheduleDownload(IProject project, MavenProject mavenProject, ArtifactKey artifact,
      boolean downloadSources, boolean downloadJavadoc) throws CoreException {
    ArtifactKey[] attached = getAttachedSourcesAndJavadoc(artifact, mavenProject.getRemoteArtifactRepositories(),
        downloadSources, downloadJavadoc);

    if(attached[0] != null || attached[1] != null) {
      downloadSourcesJob.scheduleDownload(project, artifact, downloadSources, downloadJavadoc);
    }
  }

  /**
   * Returns an array of {@link ArtifactKey}s. ArtifactKey[0], holds the sources {@link ArtifactKey}, if source download
   * was requested and sources are available. ArtifactKey[1], holds the javadoc {@link ArtifactKey}, if javadoc download
   * was requested, or requested sources are unavailable, and javadoc is available
   */
  ArtifactKey[] getAttachedSourcesAndJavadoc(ArtifactKey a, List<ArtifactRepository> repositories,
      boolean downloadSources, boolean downloadJavaDoc) throws CoreException {
    ArtifactKey[] result = new ArtifactKey[2];
    if(repositories != null) {
      ArtifactKey sourcesArtifact = new ArtifactKey(a.groupId(), a.artifactId(), a.version(),
          getSourcesClassifier(a.classifier()));
      ArtifactKey javadocArtifact = new ArtifactKey(a.groupId(), a.artifactId(), a.version(), CLASSIFIER_JAVADOC);
      if(downloadSources) {
        if(isUnavailable(sourcesArtifact, repositories)) {
          // 501553: fall back to requesting JavaDoc, if requested sources are missing,
          // but only if it doesn't exist locally
          if(getAttachedArtifactFile(a, CLASSIFIER_JAVADOC) == null) {
            downloadJavaDoc = true;
          }
        } else {
          result[0] = sourcesArtifact;
        }
      }
      if(downloadJavaDoc && !isUnavailable(javadocArtifact, repositories)) {
        result[1] = javadocArtifact;
      }
    }
    return result;
  }

  void attachSourcesAndJavadoc(IPackageFragmentRoot fragment, File sources, File javadoc, IProgressMonitor monitor) {
    IJavaProject javaProject = fragment.getJavaProject();

    IPath srcPath = sources != null ? IPath.fromOSString(sources.getAbsolutePath()) : null;
    String javaDocUrl = getJavaDocUrl(javadoc);

    try {
      IClasspathEntry[] cp = javaProject.getRawClasspath();
      for(int i = 0; i < cp.length; i++ ) {
        IClasspathEntry entry = cp[i];
        if(IClasspathEntry.CPE_LIBRARY == entry.getEntryKind() && entry.equals(fragment.getRawClasspathEntry())) {
          List<IClasspathAttribute> attributes = new ArrayList<>(Arrays.asList(entry.getExtraAttributes()));

          if(srcPath == null) {
            // configure javadocs if available
            if(javaDocUrl != null) {
              attributes
                  .add(JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, javaDocUrl));
            }
          }

          cp[i] = JavaCore.newLibraryEntry(entry.getPath(), srcPath, null, entry.getAccessRules(), //
              attributes.toArray(new IClasspathAttribute[attributes.size()]), //
              entry.isExported());

          break;
        }
      }

      javaProject.setRawClasspath(cp, monitor);
    } catch(CoreException e) {
      log.error(e.getMessage(), e);
    }
  }

  static class ModuleInfoDetector implements IResourceDeltaVisitor {

    private final Collection<IProject> affectedProjects;

    public ModuleInfoDetector(Collection<IProject> affectedProjects) {
      this.affectedProjects = affectedProjects;
    }

    @Override
    public boolean visit(IResourceDelta delta) {
      if(delta.getResource() instanceof IFile file) {
        if(ModuleSupport.MODULE_INFO_JAVA.equals(file.getName())) {
          affectedProjects.add(file.getProject());
        }
        return false;
      }
      return true;
    }

  }
}
