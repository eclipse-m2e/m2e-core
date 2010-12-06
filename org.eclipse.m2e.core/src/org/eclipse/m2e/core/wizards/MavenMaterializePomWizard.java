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

package org.eclipse.m2e.core.wizards;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import org.apache.maven.model.Dependency;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.actions.SelectionUtil;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.project.MavenProjectPomScanner;
import org.eclipse.m2e.core.project.MavenProjectScmInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;


/**
 * A wizard used to import projects for Maven artifacts 
 * 
 * @author Eugene Kuleshov
 */
public class MavenMaterializePomWizard extends Wizard implements IImportWizard, INewWizard {

  ProjectImportConfiguration importConfiguration;
  
  MavenDependenciesWizardPage selectionPage;
  
  MavenProjectWizardLocationPage locationPage;
  
  Button checkOutAllButton;
  
  Button useDeveloperConnectionButton;
  
  // TODO replace with ArtifactKey
  private Dependency[] dependencies;

  private IStructuredSelection selection;


  public MavenMaterializePomWizard() {
    importConfiguration = new ProjectImportConfiguration();
    setNeedsProgressMonitor(true);
    setWindowTitle(Messages.MavenMaterializePomWizard_title);
  }

  public void setDependencies(Dependency[] dependencies) {
    this.dependencies = dependencies;
  }
  
  public Dependency[] getDependencies() {
    return dependencies;
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.selection = selection;
    
    importConfiguration.setWorkingSet(SelectionUtil.getSelectedWorkingSet(selection));
    
    ArrayList<Dependency> dependencies = new ArrayList<Dependency>();

    for(Iterator<?> it = selection.iterator(); it.hasNext();) {
      Object element = it.next();
      ArtifactKey artifactKey = SelectionUtil.getType(element, ArtifactKey.class);
      if(artifactKey!=null) {
        Dependency d = new Dependency();
        d.setGroupId(artifactKey.getGroupId());
        d.setArtifactId(artifactKey.getArtifactId());
        d.setVersion(artifactKey.getVersion());
        d.setClassifier(artifactKey.getClassifier());
        dependencies.add(d);
      }
    }
    
    setDependencies(dependencies.toArray(new Dependency[dependencies.size()]));
  }

  public void addPages() {
    selectionPage = new MavenDependenciesWizardPage(importConfiguration, //
        Messages.MavenMaterializePomWizard_dialog_title, //
        Messages.MavenMaterializePomWizard_dialog_message) {
      protected void createAdvancedSettings(Composite composite, GridData gridData) {
        checkOutAllButton = new Button(composite, SWT.CHECK);
        checkOutAllButton.setText(Messages.MavenMaterializePomWizard_btnCheckout);
        checkOutAllButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));
        
        useDeveloperConnectionButton = new Button(composite, SWT.CHECK);
        useDeveloperConnectionButton.setText(Messages.MavenMaterializePomWizard_btnDev);
        useDeveloperConnectionButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));
        
        super.createAdvancedSettings(composite, gridData);
      }
    };
    selectionPage.setDependencies(dependencies);
    
    locationPage = new MavenProjectWizardLocationPage(importConfiguration, //
        Messages.MavenMaterializePomWizard_location_title, 
        Messages.MavenMaterializePomWizard_location_message);
    locationPage.setLocationPath(SelectionUtil.getSelectedLocation(selection));
    
    addPage(selectionPage);
    addPage(locationPage);
  }
  
  public boolean canFinish() {
    return super.canFinish();
  }

  public boolean performFinish() {
    if(!canFinish()) {
      return false;
    }

    final Dependency[] dependencies = selectionPage.getDependencies();
    
    final boolean checkoutAllProjects = checkOutAllButton.getSelection();
    final boolean developer = useDeveloperConnectionButton.getSelection();
    
    MavenProjectCheckoutJob job = new MavenProjectCheckoutJob(importConfiguration, checkoutAllProjects) {
      protected List<MavenProjectScmInfo> getProjects(IProgressMonitor monitor) throws InterruptedException {
        MavenPlugin plugin = MavenPlugin.getDefault();
        MavenProjectPomScanner<MavenProjectScmInfo> scanner = new MavenProjectPomScanner<MavenProjectScmInfo>(developer, dependencies, //
            plugin.getMavenModelManager(), //
            plugin.getConsole());
        scanner.run(monitor);
        // XXX handle errors/warnings
        
        return scanner.getProjects();
      }
    };
    
    if(!locationPage.isInWorkspace()) {
      job.setLocation(locationPage.getLocationPath().toFile());
    }
    
    job.schedule();

    return true;
  }
  
//  public Scm[] getScms(IProgressMonitor monitor) {
//    ArrayList scms = new ArrayList();
//    
//    MavenPlugin plugin = MavenPlugin.getDefault();
//    MavenEmbedderManager embedderManager = plugin.getMavenEmbedderManager();
//    IndexManager indexManager = plugin.getMavenRepositoryIndexManager();
//    MavenConsole console = plugin.getConsole();
//        
//    MavenEmbedder embedder = embedderManager.getWorkspaceEmbedder();
//
//    for(int i = 0; i < dependencies.length; i++ ) {
//      try {
//        Dependency d = dependencies[i];
//        
//        Artifact artifact = embedder.createArtifact(d.getGroupId(), //
//            d.getArtifactId(), d.getVersion(), null, "pom");
//        
//        List remoteRepositories = indexManager.getArtifactRepositories(null, null);
//        
//        embedder.resolve(artifact, remoteRepositories, embedder.getLocalRepository());
//        
//        File file = artifact.getFile();
//        if(file != null) {
//          MavenProject project = embedder.readProject(file);
//          
//          Scm scm = project.getScm();
//          if(scm == null) {
//            String msg = project.getId() + " doesn't specify SCM info";
//            console.logError(msg);
//            continue;
//          }
//          
//          String connection = scm.getConnection();
//          String devConnection = scm.getDeveloperConnection();
//          String tag = scm.getTag();
//          String url = scm.getUrl();
//
//          console.logMessage(project.getArtifactId());
//          console.logMessage("Connection: " + connection);
//          console.logMessage("       dev: " + devConnection);
//          console.logMessage("       url: " + url);
//          console.logMessage("       tag: " + tag);
//          
//          if(connection==null) {
//            if(devConnection==null) {
//              String msg = project.getId() + " doesn't specify SCM connection";
//              console.logError(msg);
//              continue;
//            }
//            scm.setConnection(devConnection);
//          }
//
//          // connection: scm:svn:https://svn.apache.org/repos/asf/incubator/wicket/branches/wicket-1.2.x/wicket
//          //        dev: scm:svn:https://svn.apache.org/repos/asf/incubator/wicket/branches/wicket-1.2.x/wicket
//          //        url: http://svn.apache.org/viewvc/incubator/wicket/branches/wicket-1.2.x/wicket
//          //        tag: HEAD  
//
//          // TODO add an option to select all modules/projects and optimize scan 
//          
//          scms.add(scm);
//          
////          if(!connection.startsWith(SCM_SVN_PROTOCOL)) {
////            String msg = project.getId() + " SCM type is not supported " + connection;
////            console.logError(msg);
////            addError(new Exception(msg));
////          } else {
////            String svnUrl = connection.trim().substring(SCM_SVN_PROTOCOL.length());
////          }
//        }
//
//      } catch(Exception ex) {
//        console.logError(ex.getMessage());
//      }
//    }
//    
//    return (Scm[]) scms.toArray(new Scm[scms.size()]);
//  }

}
