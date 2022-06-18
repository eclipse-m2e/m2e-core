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

package org.eclipse.m2e.core.embedder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.JavaScopeDeriver;
import org.eclipse.aether.util.graph.transformer.JavaScopeSelector;
import org.eclipse.aether.util.graph.transformer.NearestVersionSelector;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;
import org.eclipse.aether.util.graph.visitor.CloningDependencyVisitor;
import org.eclipse.aether.util.graph.visitor.FilteringDependencyVisitor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import org.codehaus.plexus.PlexusContainer;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.embedder.PlexusContainerManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;


/**
 * Model manager used to read and and modify Maven models
 *
 * @author Eugene Kuleshov XXX fix circular dependency
 */
@Component(service = {MavenModelManager.class})
public class MavenModelManager {
  private static final Logger log = LoggerFactory.getLogger(MavenModelManager.class);

  @Reference
  private IMavenProjectRegistry projectManager;

  @Reference
  private PlexusContainerManager containerManager;

  @Reference
  private IMaven maven;

  public org.apache.maven.model.Model readMavenModel(InputStream reader) throws CoreException {
    return maven.readModel(reader);
  }

  /**
   * Read the model from the provided pom file (might not exits) pointer
   * 
   * @param pomFile the file pointer
   * @return a maven model, or <code>null</code> if the pointer do not point to any valid maven directory
   * @throws CoreException if the file points to a valid maven directory but the pom could not be read
   */
  public org.apache.maven.model.Model readMavenModel(File pomFile) throws CoreException {

    if(pomFile.isFile()) {
      try (FileInputStream stream = new FileInputStream(pomFile)) {
        Model model = readMavenModel(stream);
        model.setPomFile(pomFile);
        return model;
      } catch(IOException ex) {
        throw new CoreException(Status.error(ex.getMessage(), ex));
      }
    }
    //this might be a polyglot model...
    File baseDir = pomFile.isDirectory() ? pomFile : pomFile.getParentFile();
    ClassLoader ccl = Thread.currentThread().getContextClassLoader();
    try {
      PlexusContainer plexusContainer = containerManager.aquire(baseDir);
      Thread.currentThread().setContextClassLoader(plexusContainer.getContainerRealm());
      ModelProcessor modelProcessor = plexusContainer.lookup(ModelProcessor.class);
      File pom = modelProcessor.locatePom(baseDir);
      if(pom != null && pom.isFile()) {
        Model model = modelProcessor.read(pom, new HashMap<>(Map.of(ModelProcessor.SOURCE, new FileModelSource(pom))));
        model.setPomFile(pom);
        return model;
      }
    } catch(Exception ex) {
      throw new CoreException(Status.error(ex.getMessage(), ex));
    } finally {
      Thread.currentThread().setContextClassLoader(ccl);
    }
    return null;
  }

  public Optional<File> locatePom(File baseDir) {
    if(baseDir == null || !baseDir.isDirectory()) {
      return Optional.empty();
    }
    File file = new File(baseDir, IMavenConstants.POM_FILE_NAME);
    if(file.isFile()) {
      //check the obvious case first...
      return Optional.of(file);
    }
    ClassLoader ccl = Thread.currentThread().getContextClassLoader();
    try {
      PlexusContainer plexusContainer = containerManager.aquire(baseDir);
      Thread.currentThread().setContextClassLoader(plexusContainer.getContainerRealm());
      ModelProcessor modelProcessor = plexusContainer.lookup(ModelProcessor.class);
      File pom = modelProcessor.locatePom(baseDir);
      System.out.println(pom);
      if(pom != null && pom.isFile()) {
        return Optional.of(pom);
      }
    } catch(Exception ex) {
      //can't locate the pom... but don't bail to the caller...
      MavenPluginActivator.getDefault().getLog().warn("Error while locating pom for basedir " + baseDir, ex);
    } finally {
      Thread.currentThread().setContextClassLoader(ccl);
    }
    return Optional.empty();
  }

  public org.apache.maven.model.Model readMavenModel(IFile pomFile) throws CoreException {
    try (InputStream is = pomFile.getContents()) {
      Model model = maven.readModel(is);
      IPath location = pomFile.getLocation();
      if(location != null) {
        model.setPomFile(location.toFile());
      }
      return model;
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, null, ex));
    }
  }

  public void createMavenModel(IFile pomFile, org.apache.maven.model.Model model) throws CoreException {
    String pomFileName = pomFile.getLocation().toString();
    if(pomFile.exists()) {
      String msg = NLS.bind(Messages.MavenModelManager_error_pom_exists, pomFileName);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, null));
    }

    try {
      ByteArrayOutputStream buf = new ByteArrayOutputStream();

      maven.writeModel(model, buf);

      // XXX MNGECLIPSE-495
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setNamespaceAware(false);
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

      Document document = documentBuilder.parse(new ByteArrayInputStream(buf.toByteArray()));
      Element documentElement = document.getDocumentElement();

      NamedNodeMap attributes = documentElement.getAttributes();

      if(attributes == null || attributes.getNamedItem("xmlns") == null) { //$NON-NLS-1$
        Attr attr = document.createAttribute("xmlns"); //$NON-NLS-1$
        attr.setTextContent("http://maven.apache.org/POM/4.0.0"); //$NON-NLS-1$
        documentElement.setAttributeNode(attr);
      }

      if(attributes == null || attributes.getNamedItem("xmlns:xsi") == null) { //$NON-NLS-1$
        Attr attr = document.createAttribute("xmlns:xsi"); //$NON-NLS-1$
        attr.setTextContent("http://www.w3.org/2001/XMLSchema-instance"); //$NON-NLS-1$
        documentElement.setAttributeNode(attr);
      }

      if(attributes == null || attributes.getNamedItem("xsi:schemaLocation") == null) { //$NON-NLS-1$
        Attr attr = document.createAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation"); //$NON-NLS-1$ //$NON-NLS-2$
        attr.setTextContent("http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"); //$NON-NLS-1$
        documentElement.setAttributeNode(attr);
      }

      TransformerFactory transfac = TransformerFactory.newInstance();
      Transformer trans = transfac.newTransformer();
      trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); //$NON-NLS-1$

      buf.reset();
      trans.transform(new DOMSource(document), new StreamResult(buf));

      pomFile.create(new ByteArrayInputStream(buf.toByteArray()), true, new NullProgressMonitor());

    } catch(Exception ex) {
      String msg = NLS.bind(Messages.MavenModelManager_error_create, pomFileName, ex.toString());
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
    }
  }

  public synchronized DependencyNode readDependencyTree(IMavenProjectFacade context, final MavenProject mavenProject,
      final String scope, IProgressMonitor monitor) throws CoreException {
    monitor.setTaskName(Messages.MavenModelManager_monitor_building);

    ICallable<DependencyNode> callable = (context1, monitor1) -> readDependencyTree(context1.getRepositorySession(), mavenProject, scope);

    return (context != null) ? projectManager.execute(context, callable, monitor) : maven.execute(callable, monitor);
  }

  DependencyNode readDependencyTree(RepositorySystemSession repositorySession, MavenProject mavenProject, String scope)
      throws CoreException {
    DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(repositorySession);

    //
    // Taken from MavenRepositorySystemSession.newSession()
    //
    ConflictResolver transformer = new ConflictResolver(new NearestVersionSelector(), new JavaScopeSelector(),
        new SimpleOptionalitySelector(), new JavaScopeDeriver());
    session.setDependencyGraphTransformer(transformer);
    session.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, Boolean.toString(true));
    session.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);

    ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(maven.getProjectRealm(mavenProject));

      ArtifactTypeRegistry stereotypes = session.getArtifactTypeRegistry();

      CollectRequest request = new CollectRequest();
      request.setRequestContext("project"); //$NON-NLS-1$
      request.setRepositories(mavenProject.getRemoteProjectRepositories());

      for(org.apache.maven.model.Dependency dependency : mavenProject.getDependencies()) {
        request.addDependency(RepositoryUtils.toDependency(dependency, stereotypes));
      }

      DependencyManagement depMngt = mavenProject.getDependencyManagement();
      if(depMngt != null) {
        for(org.apache.maven.model.Dependency dependency : depMngt.getDependencies()) {
          request.addManagedDependency(RepositoryUtils.toDependency(dependency, stereotypes));
        }
      }

      DependencyNode node;
      try {
        node = MavenPluginActivator.getDefault().getRepositorySystem().collectDependencies(session, request).getRoot();
      } catch(DependencyCollectionException ex) {
        String msg = Messages.MavenModelManager_error_read;
        log.error(msg, ex);
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
      }

      Collection<String> scopes = new HashSet<>();
      Collections.addAll(scopes, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_COMPILE, Artifact.SCOPE_PROVIDED,
          Artifact.SCOPE_RUNTIME, Artifact.SCOPE_TEST);
      if(Artifact.SCOPE_COMPILE.equals(scope)) {
        scopes.remove(Artifact.SCOPE_COMPILE);
        scopes.remove(Artifact.SCOPE_SYSTEM);
        scopes.remove(Artifact.SCOPE_PROVIDED);
      } else if(Artifact.SCOPE_RUNTIME.equals(scope)) {
        scopes.remove(Artifact.SCOPE_COMPILE);
        scopes.remove(Artifact.SCOPE_RUNTIME);
      } else if(Artifact.SCOPE_COMPILE_PLUS_RUNTIME.equals(scope)) {
        scopes.remove(Artifact.SCOPE_COMPILE);
        scopes.remove(Artifact.SCOPE_SYSTEM);
        scopes.remove(Artifact.SCOPE_PROVIDED);
        scopes.remove(Artifact.SCOPE_RUNTIME);
      } else {
        scopes.clear();
      }

      CloningDependencyVisitor cloner = new CloningDependencyVisitor();
      node.accept(new FilteringDependencyVisitor(cloner, new ScopeDependencyFilter(null, scopes)));
      node = cloner.getRootNode();

      return node;
    } finally {
      Thread.currentThread().setContextClassLoader(oldClassLoader);
    }
  }

  public MavenProject readMavenProject(IFile file, IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade projectFacade = projectManager.create(file, true, monitor);
    return projectFacade.getMavenProject(monitor);
  }

}
