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

package org.eclipse.m2e.core.embedder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.MavenProject;

import org.sonatype.aether.artifact.ArtifactTypeRegistry;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.collection.DependencyGraphTransformer;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.filter.ScopeDependencyFilter;
import org.sonatype.aether.util.graph.CloningDependencyVisitor;
import org.sonatype.aether.util.graph.FilteringDependencyVisitor;
import org.sonatype.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.sonatype.aether.util.graph.transformer.JavaEffectiveScopeCalculator;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.model.edit.pom.Build;
import org.eclipse.m2e.model.edit.pom.Configuration;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.Exclusion;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.Plugin;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.util.PomResourceFactoryImpl;
import org.eclipse.m2e.model.edit.pom.util.PomResourceImpl;


/**
 * Model manager used to read and and modify Maven models
 * 
 * @author Eugene Kuleshov
 * 
 * XXX fix circular dependency
 */
public class MavenModelManager {

  static final PomFactory POM_FACTORY = PomFactory.eINSTANCE;
  
  private final MavenProjectManager projectManager;
  
  private final MavenConsole console;

  private final IMaven maven;

  public MavenModelManager(IMaven maven, MavenProjectManager projectManager, MavenConsole console) {
    this.maven = maven;
    this.projectManager = projectManager;
    this.console = console;
  }

  public PomResourceImpl loadResource(IFile pomFile) throws CoreException {
    String path = pomFile.getFullPath().toOSString();
    URI uri = URI.createPlatformResourceURI(path, true);

    try {
      Resource resource = new PomResourceFactoryImpl().createResource(uri);
      resource.load(new HashMap());
      return (PomResourceImpl)resource;

    } catch(Exception ex) {
      String msg = NLS.bind(Messages.MavenModelManager_error_cannot_load, pomFile);
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
    }
  }

  public org.apache.maven.model.Model readMavenModel(InputStream reader) throws CoreException {
    return maven.readModel(reader);
  }

  public org.apache.maven.model.Model readMavenModel(File pomFile) throws CoreException {
    return maven.readModel(pomFile);
  }

  public org.apache.maven.model.Model readMavenModel(IFile pomFile) throws CoreException {
    return maven.readModel(pomFile.getLocation().toFile());
  }

  public void createMavenModel(IFile pomFile, org.apache.maven.model.Model model) throws CoreException {
    String pomFileName = pomFile.getLocation().toString();
    if(pomFile.exists()) {
      String msg = NLS.bind(Messages.MavenModelManager_error_pom_exists, pomFileName);
      console.logError(msg);
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

    } catch(RuntimeException ex) {
      String msg = NLS.bind(Messages.MavenModelManager_error_create, pomFileName, ex.toString());
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
    } catch(Exception ex) {
      String msg = NLS.bind(Messages.MavenModelManager_error_create, pomFileName, ex.toString());
      console.logError(msg);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
    }
  }

  public synchronized DependencyNode readDependencyTree(IFile file, String classpath,
      IProgressMonitor monitor) throws CoreException {
    monitor.setTaskName(Messages.MavenModelManager_monitor_reading);
    MavenProject mavenProject = readMavenProject(file, monitor);

    return readDependencyTree(mavenProject, classpath, monitor);
  }

  public synchronized DependencyNode readDependencyTree(MavenProject mavenProject,
      String classpath, IProgressMonitor monitor) throws CoreException {
    monitor.setTaskName(Messages.MavenModelManager_monitor_building);

    IMaven maven = MavenPlugin.getDefault().getMaven();
    DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(maven.createSession(
        maven.createExecutionRequest(monitor), mavenProject).getRepositorySession());

    DependencyGraphTransformer transformer = new ChainedDependencyGraphTransformer(new JavaEffectiveScopeCalculator(),
        new NearestVersionConflictResolver());
    session.setDependencyGraphTransformer(transformer);

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
        node = MavenPlugin.getDefault().getRepositorySystem().collectDependencies(session, request).getRoot();
      } catch(DependencyCollectionException ex) {
        String msg = Messages.MavenModelManager_error_read;
        MavenLogger.log(msg, ex);
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
      }

      Collection<String> scopes = new HashSet<String>();
      Collections.addAll(scopes, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_COMPILE, Artifact.SCOPE_PROVIDED,
          Artifact.SCOPE_RUNTIME, Artifact.SCOPE_TEST);
      if(Artifact.SCOPE_COMPILE.equals(classpath)) {
        scopes.remove(Artifact.SCOPE_COMPILE);
        scopes.remove(Artifact.SCOPE_SYSTEM);
        scopes.remove(Artifact.SCOPE_PROVIDED);
      } else if(Artifact.SCOPE_RUNTIME.equals(classpath)) {
        scopes.remove(Artifact.SCOPE_COMPILE);
        scopes.remove(Artifact.SCOPE_RUNTIME);
      } else if(Artifact.SCOPE_COMPILE_PLUS_RUNTIME.equals(classpath)) {
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
    MavenProject mavenProject = projectFacade.getMavenProject(monitor);
    return mavenProject;
  }

//  public ProjectDocument readProjectDocument(IFile pomFile) throws CoreException {
//    String name = pomFile.getProject().getName() + "/" + pomFile.getProjectRelativePath();
//    try {
//      return ProjectDocument.Factory.parse(pomFile.getLocation().toFile(), getXmlOptions());
//    } catch(XmlException ex) {
//      String msg = "Unable to parse " + name;
//      console.logError(msg + "; " + ex.toString());
//      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, msg, ex));
//    } catch(IOException ex) {
//      String msg = "Unable to read " + name;
//      console.logError(msg + "; " + ex.toString());
//      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, msg, ex));
//    }
//  }
//
//  public ProjectDocument readProjectDocument(File pom) throws CoreException {
//    try {
//      return ProjectDocument.Factory.parse(pom, getXmlOptions());
//    } catch(XmlException ex) {
//      String msg = "Unable to parse " + pom.getAbsolutePath();
//      console.logError(msg + "; " + ex.toString());
//      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, msg, ex));
//    } catch(IOException ex) {
//      String msg = "Unable to read " + pom.getAbsolutePath();
//      console.logError(msg + "; " + ex.toString());
//      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, msg, ex));
//    }
//  }

  public void updateProject(IFile pomFile, ProjectUpdater updater) {
    File pom = pomFile.getLocation().toFile();
    PomResourceImpl resource = null;
    try {
      resource = loadResource(pomFile);
      updater.update(resource.getModel());
      resource.save(Collections.EMPTY_MAP);
    } catch(Exception ex) {
      String msg = "Unable to update " + pom;
      console.logError(msg + "; " + ex.getMessage()); //$NON-NLS-1$
      MavenLogger.log(msg, ex);
    } finally {
      if (resource != null) {
        resource.unload();
      }
    }
  }

  public void addDependency(IFile pomFile, org.apache.maven.model.Dependency dependency) {
    updateProject(pomFile, new DependencyAdder(dependency));
  }

  public void addModule(IFile pomFile, final String moduleName) {
    updateProject(pomFile, new ModuleAdder(moduleName));
  }

//  /**
//   * Project updater for adding Maven namespace declaration
//   */
//  public static class NamespaceAdder extends ProjectUpdater {
//
//    public void update(Model model) {
//      DocumentRoot documentRoot = PomFactory.eINSTANCE.createDocumentRoot();
//      EMap<String, String> prefixMap = documentRoot.getXMLNSPrefixMap();
//      EMap<String, String> schemaLocation = documentRoot.getXSISchemaLocation();
//
//      // xmlns="http://maven.apache.org/POM/4.0.0" 
//      // xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
//      // xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"      
//      
////      XmlCursor cursor = project.newCursor();
////      cursor.toNextToken();
////      if(!cursor.toFirstAttribute()) {
////        cursor.toNextToken();
////      }
////
////      String uri = ProjectDocument.type.getDocumentElementName().getNamespaceURI();
////      cursor.insertNamespace("", uri);
////      cursor.insertNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
////      cursor.insertAttributeWithValue( //
////          new QName("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation", "xsi"), uri
////              + " http://maven.apache.org/xsd/maven-4.0.0.xsd");
//    }
//
//  }

  /**
   * Project updater for adding dependencies
   */
  public static class DependencyAdder extends ProjectUpdater {

    private final org.apache.maven.model.Dependency dependency;
    private final List<Dependency> modelDependencies;

    public DependencyAdder(org.apache.maven.model.Dependency dependency) {
      this.dependency = dependency;
      modelDependencies = null;
    }
    
    public DependencyAdder(List<Dependency> dependencies) {
      this.modelDependencies = dependencies;
      dependency = null;
      
    }

    public void update(org.eclipse.m2e.model.edit.pom.Model model) {
      if (modelDependencies != null) {
        model.getDependencies().addAll(modelDependencies);
        return;
      }
      Dependency dependency = POM_FACTORY.createDependency();
      
      dependency.setGroupId(this.dependency.getGroupId());
      dependency.setArtifactId(this.dependency.getArtifactId());
      
      if(this.dependency.getVersion()!=null) {
        dependency.setVersion(this.dependency.getVersion());
      }
      
      if(this.dependency.getClassifier() != null) {
        dependency.setClassifier(this.dependency.getClassifier());
      }
      
      if(this.dependency.getType() != null //
          && !"jar".equals(this.dependency.getType()) // //$NON-NLS-1$
          && !"null".equals(this.dependency.getType())) { // guard against MNGECLIPSE-622 //$NON-NLS-1$
        dependency.setType(this.dependency.getType());
      }
      
      if(this.dependency.getScope() != null && !"compile".equals(this.dependency.getScope())) { //$NON-NLS-1$
        dependency.setScope(this.dependency.getScope());
      }
      
      if(this.dependency.getSystemPath() != null) {
        dependency.setSystemPath(this.dependency.getSystemPath());
      }
      
      if(this.dependency.isOptional()) {
        dependency.setOptional("true"); //$NON-NLS-1$
      }

      if(!this.dependency.getExclusions().isEmpty()) {

        Iterator<org.apache.maven.model.Exclusion> it = this.dependency.getExclusions().iterator();
        while(it.hasNext()) {
          org.apache.maven.model.Exclusion e = it.next();
          Exclusion exclusion = POM_FACTORY.createExclusion();
          exclusion.setGroupId(e.getGroupId());
          exclusion.setArtifactId(e.getArtifactId());
          dependency.getExclusions().add(exclusion);
        }
      }
      
      // search for dependency with same GAC and remove if found
      Iterator<Dependency> it = model.getDependencies().iterator();
      boolean mergeScope = false;
      String oldScope = Artifact.SCOPE_COMPILE;
      while (it.hasNext()) {
        Dependency dep = it.next();
        if (dep.getGroupId().equals(dependency.getGroupId()) && 
            dep.getArtifactId().equals(dependency.getArtifactId()) &&
            compareNulls(dep.getClassifier(), dependency.getClassifier())) {
          oldScope = dep.getScope();
          it.remove();
          mergeScope = true;
        }
      }
      
      if (mergeScope) {
        // merge scopes
        if (oldScope == null) {
          oldScope = Artifact.SCOPE_COMPILE;
        }
        
        String newScope = this.dependency.getScope();
        if (newScope == null) {
          newScope = Artifact.SCOPE_COMPILE;
        }
        
        if (!oldScope.equals(newScope)) {
          boolean systemScope = false;
          boolean providedScope = false;
          boolean compileScope = false;
          boolean runtimeScope = false;
          boolean testScope = false;
  
          // test old scope
          if ( Artifact.SCOPE_COMPILE.equals( oldScope ) ) {
            systemScope = true;
            providedScope = true;
            compileScope = true;
            runtimeScope = false;
            testScope = false;
          } else if ( Artifact.SCOPE_RUNTIME.equals( oldScope ) ) {
            systemScope = false;
            providedScope = false;
            compileScope = true;
            runtimeScope = true;
            testScope = false;
          } else if ( Artifact.SCOPE_TEST.equals( oldScope ) ) {
            systemScope = true;
            providedScope = true;
            compileScope = true;
            runtimeScope = true;
            testScope = true;
          }

          // merge with new one
          if ( Artifact.SCOPE_COMPILE.equals( newScope ) ) {
            systemScope = systemScope || true;
            providedScope = providedScope || true;
            compileScope = compileScope || true;
            runtimeScope = runtimeScope || false;
            testScope = testScope || false;
          } else if ( Artifact.SCOPE_RUNTIME.equals( newScope ) ) {
            systemScope = systemScope || false;
            providedScope = providedScope || false;
            compileScope = compileScope || true;
            runtimeScope = runtimeScope || true;
            testScope = testScope || false;
          } else if ( Artifact.SCOPE_TEST.equals( newScope ) ) {
            systemScope = systemScope || true;
            providedScope = providedScope || true;
            compileScope = compileScope || true;
            runtimeScope = runtimeScope || true;
            testScope = testScope || true;
          }
          
          if (testScope) {
            newScope = Artifact.SCOPE_TEST;
          } else if (runtimeScope) {
            newScope = Artifact.SCOPE_RUNTIME;
          } else if (compileScope) {
            newScope = Artifact.SCOPE_COMPILE;
          } else {
            // unchanged
          }

          dependency.setScope(newScope);
        }
      }
      
      model.getDependencies().add(dependency);
    }

    @SuppressWarnings("null")
    private boolean compareNulls(String s1, String s2) {
      if (s1 == null && s2 == null) {
        return true;
      }
      if ((s1 == null && s2 != null) || (s2 == null && s1 != null)) {
        return false;
      }
      return s1.equals(s2);   
    }
  }
  

  /**
   * Project updater for adding modules
   */
  public static class ModuleAdder extends ProjectUpdater {

    private final String moduleName;

    public ModuleAdder(String moduleName) {
      this.moduleName = moduleName;
    }

    public void update(Model model) {
      model.getModules().add(moduleName);
    }
  }

  /**
   * Project updater for adding plugins
   */
  public static class PluginAdder extends ProjectUpdater {

    private final String groupId;
    private final String artifactId;
    private final String version;

    public PluginAdder(String groupId, String artifactId, String version) {
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.version = version;
    }

    public void update(Model model) {
      Build build = model.getBuild();
      if(build==null) {
        build = POM_FACTORY.createBuild();
        model.setBuild(build);
      }

      Plugin plugin = POM_FACTORY.createPlugin();
      
      if(!"org.apache.maven.plugins".equals(this.groupId)) { //$NON-NLS-1$
        plugin.setGroupId(this.groupId);
      }
      
      plugin.setArtifactId(this.artifactId);

      if(this.version != null) {
        plugin.setVersion(this.version);
      }

      Configuration configuration = POM_FACTORY.createConfiguration();
      plugin.setConfiguration(configuration);
      
      build.getPlugins().add(plugin);
    }
  }

}
