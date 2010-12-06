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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
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
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.DeltaProcessingState;
import org.eclipse.jdt.internal.core.JavaElementDelta;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.index.IndexManager;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.jdt.internal.ClasspathDescriptor;
import org.eclipse.m2e.jdt.internal.DefaultClasspathManagerDelegate;
import org.eclipse.m2e.jdt.internal.MavenClasspathContainer;
import org.eclipse.m2e.jdt.internal.MavenClasspathContainerSaveHelper;
import org.eclipse.m2e.jdt.internal.Messages;

/**
 * This class is responsible for mapping Maven classpath to JDT and back.
 * 
 * @deprecated this classes is internal implementation and should be replaced with IClasspathManager before 1.0
 */
@SuppressWarnings("restriction")
public class BuildPathManager implements IMavenProjectChangedListener, IResourceChangeListener {

  // container settings
  public static final String CONTAINER_ID = "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER"; //$NON-NLS-1$
  
  // entry attributes
  public static final String GROUP_ID_ATTRIBUTE = "maven.groupId"; //$NON-NLS-1$

  public static final String ARTIFACT_ID_ATTRIBUTE = "maven.artifactId"; //$NON-NLS-1$

  public static final String VERSION_ATTRIBUTE = "maven.version"; //$NON-NLS-1$

  public static final String CLASSIFIER_ATTRIBUTE = "maven.classifier"; //$NON-NLS-1$

  public static final String SCOPE_ATTRIBUTE = "maven.scope"; //$NON-NLS-1$

  // local repository variable
  public static final String M2_REPO = "M2_REPO"; //$NON-NLS-1$

  private static final String PROPERTY_SRC_ROOT = ".srcRoot"; //$NON-NLS-1$

  private static final String PROPERTY_SRC_PATH = ".srcPath"; //$NON-NLS-1$

  private static final String PROPERTY_JAVADOC_URL = ".javadoc"; //$NON-NLS-1$

  static final String CLASSIFIER_SOURCES = "sources"; //$NON-NLS-1$

  static final String CLASSIFIER_JAVADOC = "javadoc"; //$NON-NLS-1$

  static final String CLASSIFIER_TESTS = "tests"; //$NON-NLS-1$

  static final String CLASSIFIER_TESTSOURCES = "test-sources"; //$NON-NLS-1$

  public static final int CLASSPATH_TEST = 0;

  public static final int CLASSPATH_RUNTIME = 1;

  // test is the widest possible scope, and this is what we need by default
  public static final int CLASSPATH_DEFAULT = CLASSPATH_TEST;

  public static final ArtifactFilter SCOPE_FILTER_RUNTIME = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);

  public static final ArtifactFilter SCOPE_FILTER_TEST = new ScopeArtifactFilter(Artifact.SCOPE_TEST);

  final MavenConsole console;

  final MavenProjectManager projectManager;

  final IMavenConfiguration mavenConfiguration;

  final IndexManager indexManager;

  final BundleContext bundleContext;

  final IMaven maven;

  final File stateLocationDir;

  private String jdtVersion;

  private final DownloadSourcesJob downloadSourcesJob;

  private final DefaultClasspathManagerDelegate defaultDelegate;

  public BuildPathManager(MavenConsole console, MavenProjectManager projectManager, IndexManager indexManager,
      BundleContext bundleContext, File stateLocationDir) {
    this.console = console;
    this.projectManager = projectManager;
    this.indexManager = indexManager;
    this.mavenConfiguration = MavenPlugin.getDefault().getMavenConfiguration();
    this.bundleContext = bundleContext;
    this.stateLocationDir = stateLocationDir;
    this.maven = MavenPlugin.getDefault().getMaven();
    this.downloadSourcesJob = new DownloadSourcesJob(this);
    this.defaultDelegate = new DefaultClasspathManagerDelegate();
  }

  public static boolean isMaven2ClasspathContainer(IPath containerPath) {
    return containerPath != null && containerPath.segmentCount() > 0
        && CONTAINER_ID.equals(containerPath.segment(0));
  }

  public static IClasspathEntry getDefaultContainerEntry() {
    return JavaCore.newContainerEntry(new Path(CONTAINER_ID));
  }

  public static IClasspathEntry getMavenContainerEntry(IJavaProject javaProject) {
    if(javaProject != null) {
      try {
        for(IClasspathEntry entry : javaProject.getRawClasspath()) {
          if(isMaven2ClasspathContainer(entry.getPath())) {
            return entry;
          }
        }
      } catch(JavaModelException ex) {
        return null;
      }
    }
    return null;
  }

  /**
     XXX In Eclipse 3.3, changes to resolved classpath are not announced by JDT Core
     and PackageExplorer does not properly refresh when we update Maven
     classpath container.
     As a temporary workaround, send F_CLASSPATH_CHANGED notifications
     to all PackageExplorerContentProvider instances listening to
     java ElementChangedEvent. 
     Note that even with this hack, build clean is sometimes necessary to
     reconcile PackageExplorer with actual classpath
     See https://bugs.eclipse.org/bugs/show_bug.cgi?id=154071
   */
  private void forcePackageExplorerRefresh(IJavaProject javaProject) {
    if(getJDTVersion().startsWith("3.3")) { //$NON-NLS-1$
      DeltaProcessingState state = JavaModelManager.getJavaModelManager().deltaState;
      synchronized(state) {
        for(IElementChangedListener listener : state.elementChangedListeners) {
          if(listener instanceof PackageExplorerContentProvider) {
            JavaElementDelta delta = new JavaElementDelta(javaProject);
            delta.changed(IJavaElementDelta.F_CLASSPATH_CHANGED);
            listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));
          }
        }
      }
    }
  }

  // XXX should inject version instead of looking it up 
  private synchronized String getJDTVersion() {
    if(jdtVersion == null) {
      Bundle[] bundles = bundleContext.getBundles();
      for(int i = 0; i < bundles.length; i++ ) {
        if(JavaCore.PLUGIN_ID.equals(bundles[i].getSymbolicName())) {
          jdtVersion = (String) bundles[i].getHeaders().get(Constants.BUNDLE_VERSION);
          break;
        }
      }
    }
    return jdtVersion;
  }

  public static IClasspathContainer getMaven2ClasspathContainer(IJavaProject project) throws JavaModelException {
    IClasspathEntry[] entries = project.getRawClasspath();
    for(int i = 0; i < entries.length; i++ ) {
      IClasspathEntry entry = entries[i];
      if(entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER && isMaven2ClasspathContainer(entry.getPath())) {
        return JavaCore.getClasspathContainer(entry.getPath(), project);
      }
    }
    return null;
  }

  public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    Set<IProject> projects = new HashSet<IProject>();
    monitor.setTaskName(Messages.BuildPathManager_monitor_setting_cp);
    for(int i = 0; i < events.length; i++ ) {
      MavenProjectChangedEvent event = events[i];
      IFile pom = event.getSource();
      IProject project = pom.getProject();
      if(project.isAccessible() && projects.add(project)) {
        updateClasspath(project, monitor);
      }
    }
  }

  public void updateClasspath(IProject project, IProgressMonitor monitor) {
    IJavaProject javaProject = JavaCore.create(project);
    if(javaProject != null) {
      try {
        IClasspathEntry containerEntry = getMavenContainerEntry(javaProject);
        IPath path = containerEntry != null ? containerEntry.getPath() : new Path(CONTAINER_ID);
        IClasspathEntry[] classpath = getClasspath(project, monitor);
        IClasspathContainer container = new MavenClasspathContainer(path, classpath);
        JavaCore.setClasspathContainer(container.getPath(), new IJavaProject[] {javaProject},
            new IClasspathContainer[] {container}, monitor);
        forcePackageExplorerRefresh(javaProject);
        saveContainerState(project, container);
      } catch(CoreException ex) {
        MavenLogger.log(ex);
      }
    }
  }

  private void saveContainerState(IProject project, IClasspathContainer container) {
    File containerStateFile = getContainerStateFile(project);
    FileOutputStream is = null;
    try {
      is = new FileOutputStream(containerStateFile);
      new MavenClasspathContainerSaveHelper().writeContainer(container, is);
    } catch(IOException ex) {
      MavenLogger.log("Can't save classpath container state for " + project.getName(), ex); //$NON-NLS-1$
    } finally {
      if(is != null) {
        try {
          is.close();
        } catch(IOException ex) {
          MavenLogger.log("Can't close output stream for " + containerStateFile.getAbsolutePath(), ex); //$NON-NLS-1$
        }
      }
    }
  }

  public IClasspathContainer getSavedContainer(IProject project) throws CoreException {
    File containerStateFile = getContainerStateFile(project);
    if(!containerStateFile.exists()) {
      return null;
    }

    FileInputStream is = null;
    try {
      is = new FileInputStream(containerStateFile);
      return new MavenClasspathContainerSaveHelper().readContainer(is);
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, MavenJdtPlugin.PLUGIN_ID, -1, //
          "Can't read classpath container state for " + project.getName(), ex));
    } catch(ClassNotFoundException ex) {
      throw new CoreException(new Status(IStatus.ERROR, MavenJdtPlugin.PLUGIN_ID, -1, //
          "Can't read classpath container state for " + project.getName(), ex));
    } finally {
      if(is != null) {
        try {
          is.close();
        } catch(IOException ex) {
          MavenLogger.log("Can't close output stream for " + containerStateFile.getAbsolutePath(), ex); //$NON-NLS-1$
        }
      }
    }
  }

  private IClasspathEntry[] getClasspath(IMavenProjectFacade projectFacade, final int kind,
      final Properties sourceAttachment, boolean uniquePaths, final IProgressMonitor monitor) throws CoreException {

    IJavaProject javaProject = JavaCore.create(projectFacade.getProject());

    final ClasspathDescriptor classpath = new ClasspathDescriptor(javaProject);

    getDelegate(projectFacade, monitor).populateClasspath(classpath, projectFacade, kind, monitor);

    configureAttchedSourcesAndJavadoc(projectFacade, sourceAttachment, classpath, monitor);

    IClasspathEntry[] entries = classpath.getEntries();

    if(uniquePaths) {
      Map<IPath, IClasspathEntry> paths = new LinkedHashMap<IPath, IClasspathEntry>();
      for(IClasspathEntry entry : entries) {
        if(!paths.containsKey(entry.getPath())) {
          paths.put(entry.getPath(), entry);
        }
      }
      return paths.values().toArray(new IClasspathEntry[paths.size()]);
    }

    return entries;
  }

  private IClasspathManagerDelegate getDelegate(IMavenProjectFacade projectFacade, IProgressMonitor monitor)
      throws CoreException {
    ILifecycleMapping lifecycleMapping = projectFacade.getLifecycleMapping(monitor);
    if(lifecycleMapping instanceof IClasspathManagerDelegate) {
      return (IClasspathManagerDelegate) lifecycleMapping;
    }
    return defaultDelegate;
  }

  private void configureAttchedSourcesAndJavadoc(IMavenProjectFacade facade, Properties sourceAttachment,
      ClasspathDescriptor classpath, IProgressMonitor monitor) throws CoreException {
    for(IClasspathEntryDescriptor desc : classpath.getEntryDescriptors()) {
      if(IClasspathEntry.CPE_LIBRARY == desc.getEntryKind() && desc.getSourceAttachmentPath() == null) {
        ArtifactKey a = desc.getArtifactKey();
        String key = desc.getPath().toPortableString();

        IPath srcPath = desc.getSourceAttachmentPath();
        IPath srcRoot = desc.getSourceAttachmentRootPath();
        if(srcPath == null && sourceAttachment != null && sourceAttachment.containsKey(key + PROPERTY_SRC_PATH)) {
          srcPath = Path.fromPortableString((String) sourceAttachment.get(key + PROPERTY_SRC_PATH));
          if(sourceAttachment.containsKey(key + PROPERTY_SRC_ROOT)) {
            srcRoot = Path.fromPortableString((String) sourceAttachment.get(key + PROPERTY_SRC_ROOT));
          }
        }
        if(srcPath == null && a != null) {
          srcPath = getSourcePath(a);
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
        if (aKey != null) { // maybe we should try to find artifactKey little harder here?
          boolean downloadSources = desc.getSourceAttachmentPath() == null && srcPath == null
              && mavenConfiguration.isDownloadSources();
          boolean downloadJavaDoc = desc.getJavadocUrl() == null && javaDocUrl == null
              && mavenConfiguration.isDownloadJavaDoc();
  
          scheduleDownload(facade.getProject(), facade.getMavenProject(), aKey, downloadSources, downloadJavaDoc);
        }
      }
    }
  }

  private boolean isUnavailable(ArtifactKey a, List<ArtifactRepository> repositories) throws CoreException {
    return maven.isUnavailable(a.getGroupId(), a.getArtifactId(), a.getVersion(), "jar" /*type*/, a.getClassifier(), repositories); //$NON-NLS-1$
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

  public IClasspathEntry[] getClasspath(IProject project, int scope, boolean uniquePaths, IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade facade = projectManager.create(project, monitor);
    if(facade == null) {
      return new IClasspathEntry[0];
    }
    try {
      Properties props = new Properties();
      File file = getSourceAttachmentPropertiesFile(project);
      if(file.canRead()) {
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        try {
          props.load(is);
        } finally {
          is.close();
        }
      }
      return getClasspath(facade, scope, props, uniquePaths, monitor);
    } catch(IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, MavenJdtPlugin.PLUGIN_ID, -1, //
          "Can't save classpath container changes", e));
    }
  }

  public IClasspathEntry[] getClasspath(IProject project, IProgressMonitor monitor) throws CoreException {
    return getClasspath(project, CLASSPATH_DEFAULT, monitor);
  }

  /**
   * Downloads artifact sources using background job.
   * 
   * If path is null, downloads sources for all classpath entries of the project,
   * otherwise downloads sources for the first classpath entry with the
   * given path.
   */
//  public void downloadSources(IProject project, IPath path) throws CoreException {
//    downloadSourcesJob.scheduleDownload(project, path, findArtifacts(project, path), true, false);
//  }

  /**
   * Downloads artifact JavaDocs using background job.
   * 
   * If path is null, downloads sources for all classpath entries of the project,
   * otherwise downloads sources for the first classpath entry with the
   * given path.
   */
//  public void downloadJavaDoc(IProject project, IPath path) throws CoreException {
//    downloadSourcesJob.scheduleDownload(project, path, findArtifacts(project, path), false, true);
//  }

  private Set<ArtifactKey> findArtifacts(IProject project, IPath path) throws CoreException {
    ArrayList<IClasspathEntry> entries = findClasspathEntries(project, path);

    Set<ArtifactKey> artifacts = new LinkedHashSet<ArtifactKey>();

    for(IClasspathEntry entry : entries) {
      ArtifactKey artifact = findArtifactByArtifactKey(entry);

      if(artifact == null) {
        artifact = findArtifactInIndex(project, entry);
        if(artifact == null) {
          // console.logError("Can't find artifact for " + entry.getPath());
        } else {
          // console.logMessage("Found indexed artifact " + artifact + " for " + entry.getPath());
          artifacts.add(artifact);
        }
      } else {
        // console.logMessage("Found artifact " + artifact + " for " + entry.getPath());
        artifacts.add(artifact);
      }
    }

    return artifacts;
  }

  public ArtifactKey findArtifact(IProject project, IPath path) throws CoreException {
    if(path != null) {
      Set<ArtifactKey> artifacts = findArtifacts(project, path);
      // it is not possible to have more than one classpath entry with the same path
      if(artifacts.size() > 0) {
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
    for(int j = 0; j < attributes.length; j++ ) {
      if(GROUP_ID_ATTRIBUTE.equals(attributes[j].getName())) {
        groupId = attributes[j].getValue();
      } else if(ARTIFACT_ID_ATTRIBUTE.equals(attributes[j].getName())) {
        artifactId = attributes[j].getValue();
      } else if(VERSION_ATTRIBUTE.equals(attributes[j].getName())) {
        version = attributes[j].getValue();
      } else if(CLASSIFIER_ATTRIBUTE.equals(attributes[j].getName())) {
        classifier = attributes[j].getValue();
      }
    }

    if(groupId != null && artifactId != null && version != null) {
      return new ArtifactKey(groupId, artifactId, version, classifier);
    }
    return null;
  }

  private ArtifactKey findArtifactInIndex(IProject project, IClasspathEntry entry) throws CoreException {
    IFile jarFile = project.getWorkspace().getRoot().getFile(entry.getPath());
    File file = jarFile==null || jarFile.getLocation()==null ? entry.getPath().toFile() : jarFile.getLocation().toFile();

    IndexedArtifactFile iaf = indexManager.getIndex(project).identify(file);
    if(iaf != null) {
      return new ArtifactKey(iaf.group, iaf.artifact, iaf.version, iaf.classifier);
    }

    return null;
  }

  // TODO should it be just one entry?
  private ArrayList<IClasspathEntry> findClasspathEntries(IProject project, IPath path) throws JavaModelException {
    ArrayList<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();

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
    for(int i = 0; i < entries.length; i++ ) {
      IClasspathEntry entry = entries[i];
      if(IClasspathEntry.CPE_LIBRARY == entry.getEntryKind()) {
        String path = entry.getPath().toPortableString();
        if(entry.getSourceAttachmentPath() != null) {
          props.put(path + PROPERTY_SRC_PATH, entry.getSourceAttachmentPath().toPortableString());
        }
        if(entry.getSourceAttachmentRootPath() != null) {
          props.put(path + PROPERTY_SRC_ROOT, entry.getSourceAttachmentRootPath().toPortableString());
        }
        String javadocUrl = getJavadocLocation(entry);
        if(javadocUrl != null) {
          props.put(path + PROPERTY_JAVADOC_URL, javadocUrl);
        }
      }
    }

    // eliminate all "standard" source/javadoc attachement we get from local repo
    entries = getClasspath(facade, CLASSPATH_DEFAULT, null, true, monitor);
    for(int i = 0; i < entries.length; i++ ) {
      IClasspathEntry entry = entries[i];
      if(IClasspathEntry.CPE_LIBRARY == entry.getEntryKind()) {
        String path = entry.getPath().toPortableString();
        String value = (String) props.get(path + PROPERTY_SRC_PATH);
        if (value != null && entry.getSourceAttachmentPath() != null && value.equals(entry.getSourceAttachmentPath().toPortableString())) {
          props.remove(path + PROPERTY_SRC_PATH);
        }
        value = (String) props.get(path + PROPERTY_SRC_ROOT);
        if (value != null && entry.getSourceAttachmentRootPath() != null && value.equals(entry.getSourceAttachmentRootPath().toPortableString())) {
          props.remove(path + PROPERTY_SRC_ROOT);
        }
      }
    }

    // persist custom source/javadoc attachement info
    File file = getSourceAttachmentPropertiesFile(project.getProject());
    try {
      OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
      try {
        props.store(os, null);
      } finally {
        os.close();
      }
    } catch(IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, MavenJdtPlugin.PLUGIN_ID, -1, "Can't save classpath container changes", e));
    }

    // update classpath container. suboptimal as this will re-calculate classpath
    updateClasspath(project.getProject(), monitor);
  }

  /** public for unit tests only */
  public String getJavadocLocation(IClasspathEntry entry) {
    IClasspathAttribute[] attributes = entry.getExtraAttributes();
    for(int j = 0; j < attributes.length; j++ ) {
      IClasspathAttribute attribute = attributes[j];
      if(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME.equals(attribute.getName())) {
        return attribute.getValue();
      }
    }
    return null;
  }

  /** public for unit tests only */
  public File getSourceAttachmentPropertiesFile(IProject project) {
    return new File(stateLocationDir, project.getName() + ".sources"); //$NON-NLS-1$
  }

  /** public for unit tests only */
  public File getContainerStateFile(IProject project) {
    return new File(stateLocationDir, project.getName() + ".container"); //$NON-NLS-1$
  }

  public void resourceChanged(IResourceChangeEvent event) {
    int type = event.getType();
    if(IResourceChangeEvent.PRE_DELETE == type) {
      // remove custom source and javadoc configuration
      File attachmentProperties = getSourceAttachmentPropertiesFile((IProject) event.getResource());
      if(attachmentProperties.exists() && !attachmentProperties.delete()) {
        MavenLogger.log("Can't delete " + attachmentProperties.getAbsolutePath(), null); //$NON-NLS-1$
      }

      // remove classpath container state
      File containerState = getContainerStateFile((IProject) event.getResource());
      if(containerState.exists() && !containerState.delete()) {
        MavenLogger.log("Can't delete " + containerState.getAbsolutePath(), null); //$NON-NLS-1$
      }
    }
  }

  public boolean setupVariables() {
    boolean changed = false;
    try {
      File localRepositoryDir = new File(maven.getLocalRepository().getBasedir());
      IPath oldPath = JavaCore.getClasspathVariable(M2_REPO);
      IPath newPath = new Path(localRepositoryDir.getAbsolutePath());
      JavaCore.setClasspathVariable(M2_REPO, //
          newPath, //
          new NullProgressMonitor());
      changed = !newPath.equals(oldPath);
    } catch(CoreException ex) {
      MavenLogger.log(ex);
      changed = false;
    }
    return changed;
  }

  public boolean variablesAreInUse() {
    try {
      IJavaModel model = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
      IJavaProject[] projects = model.getJavaProjects();
      for(int i = 0; i < projects.length; i++ ) {
        IClasspathEntry[] entries = projects[i].getRawClasspath();
        for(int k = 0; k < entries.length; k++ ) {
          IClasspathEntry curr = entries[k];
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
    File file = getAttachedArtifactFile(a, getSourcesClassifier(a.getClassifier()));

    if(file != null) {
      return Path.fromOSString(file.getAbsolutePath());
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
      String relPath = maven.getArtifactPath(localRepository, a.getGroupId(), a.getArtifactId(), a.getVersion(),
          "jar", classifier); //$NON-NLS-1$
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
        URL fileUrl = file.toURL();
        return "jar:" + fileUrl.toExternalForm() + "!/" + getJavaDocPathInArchive(file); //$NON-NLS-1$ //$NON-NLS-2$
      }
    } catch(MalformedURLException ex) {
      // fall through
    }

    return null;
  }

  private static String getJavaDocPathInArchive(File file) {
    ZipFile jarFile = null;
    try {
      jarFile = new ZipFile(file);
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
    } finally {
      try {
        if(jarFile != null)
          jarFile.close();
      } catch(IOException ex) {
        //
      }
    }

    return ""; //$NON-NLS-1$
  }

  /**
   * this is for unit tests only!
   */
  public Job getDownloadSourcesJob() {
    return downloadSourcesJob;
  }

  public void scheduleDownload(IPackageFragmentRoot fragment, boolean downloadSources, boolean downloadJavadoc) {
    ArtifactKey artifact = (ArtifactKey) fragment.getAdapter(ArtifactKey.class);

    if(artifact == null) {
      // we don't know anything about this JAR/ZIP
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
      MavenLogger.log("Could not schedule sources/javadoc download", e); //$NON-NLS-1$
    }

  }

  public void scheduleDownload(final IProject project, final boolean downloadSources, final boolean downloadJavadoc) {
    try {
      if(project != null && project.isAccessible() && project.hasNature(IMavenConstants.NATURE_ID)) {
        IMavenProjectFacade facade = projectManager.getProject(project);
        MavenProject mavenProject = facade != null ? facade.getMavenProject() : null;
        if(mavenProject != null) {
          for(Artifact artifact : mavenProject.getArtifacts()) {
            ArtifactKey artifactKey = new ArtifactKey(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(), artifact.getClassifier());
            scheduleDownload(project, mavenProject, artifactKey, downloadSources, downloadJavadoc);
          }
        } else {
          // project is not in the cache, push all processing to the background job
          downloadSourcesJob.scheduleDownload(project, null, downloadSources, downloadJavadoc);
        }
      }
    } catch(CoreException e) {
      MavenLogger.log("Could not schedule sources/javadoc download", e); //$NON-NLS-1$
    }
  }

  private void scheduleDownload(IProject project, MavenProject mavenProject, ArtifactKey artifact, boolean downloadSources, boolean downloadJavadoc) throws CoreException {
    ArtifactKey[] attached = getAttachedSourcesAndJavadoc(artifact, mavenProject.getRemoteArtifactRepositories(), downloadSources, downloadJavadoc);

    if(attached[0] != null || attached[1] != null) {
      downloadSourcesJob.scheduleDownload(project, artifact, downloadSources, downloadJavadoc);
    }
  }

  ArtifactKey[] getAttachedSourcesAndJavadoc(ArtifactKey a, List<ArtifactRepository> repositories, boolean downloadSources, boolean downloadJavaDoc) throws CoreException {
    ArtifactKey sourcesArtifact = new ArtifactKey(a.getGroupId(), a.getArtifactId(), a.getVersion(), getSourcesClassifier(a.getClassifier()));
    ArtifactKey javadocArtifact = new ArtifactKey(a.getGroupId(), a.getArtifactId(), a.getVersion(), CLASSIFIER_JAVADOC);

    if(repositories != null) {
      downloadSources &= !isUnavailable(sourcesArtifact, repositories);
      downloadJavaDoc &= !isUnavailable(javadocArtifact, repositories);
    }

    ArtifactKey[] result = new ArtifactKey[2];

    if(downloadSources) {
      result[0] = sourcesArtifact;
    }

    if(downloadJavaDoc) {
      result[1] = javadocArtifact;
    }

    return result;
  }

  void attachSourcesAndJavadoc(IPackageFragmentRoot fragment, File sources, File javadoc, IProgressMonitor monitor) {
    IJavaProject javaProject = fragment.getJavaProject();

    IPath srcPath = sources != null ? Path.fromOSString(sources.getAbsolutePath()) : null;
    String javaDocUrl = getJavaDocUrl(javadoc);

    try {
      IClasspathEntry[] cp = javaProject.getRawClasspath();
      for(int i = 0; i < cp.length; i++ ) {
        IClasspathEntry entry = cp[i];
        if(IClasspathEntry.CPE_LIBRARY == entry.getEntryKind() && entry.equals(fragment.getRawClasspathEntry())) {
          List<IClasspathAttribute> attributes = new ArrayList<IClasspathAttribute>(Arrays.asList(entry.getExtraAttributes()));

          if(srcPath == null) {
            // configure javadocs if available
            if(javaDocUrl != null) {
              attributes.add(JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
                  javaDocUrl));
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
      MavenLogger.log(e);
    }
  }
}
