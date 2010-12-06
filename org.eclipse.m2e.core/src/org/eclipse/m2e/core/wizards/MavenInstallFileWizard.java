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

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.progress.IProgressConstants;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.actions.OpenMavenConsoleAction;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.Messages;


/**
 * Wizard to install artifacts into the local Maven repository.
 * 
 * @author Guillaume Sauthier
 * @author Mike Haller
 * @author Eugene Kuleshov
 * @since 0.9.7
 */
public class MavenInstallFileWizard extends Wizard implements IImportWizard {

  private IFile selectedFile;
  
  private IFile pomFile;
  
  private MavenInstallFileArtifactWizardPage artifactPage;

  private MavenInstallFileRepositoryWizardPage repositoryPage;

  public MavenInstallFileWizard() {
    setNeedsProgressMonitor(true);
    setWindowTitle(Messages.MavenInstallFileWizard_title);
  }

  public void addPages() {
    artifactPage = new MavenInstallFileArtifactWizardPage(selectedFile);
    addPage(artifactPage);
    
    // repositoryPage = new MavenInstallFileRepositoryWizardPage(pomFile);
    // addPage(repositoryPage);
  }
  
  public boolean performFinish() {
    final Properties properties = new Properties();
    
    // Mandatory Properties for install:install-file
    properties.setProperty("file", artifactPage.getArtifactFileName()); //$NON-NLS-1$
    
    properties.setProperty("groupId", artifactPage.getGroupId()); //$NON-NLS-1$
    properties.setProperty("artifactId", artifactPage.getArtifactId()); //$NON-NLS-1$
    properties.setProperty("version", artifactPage.getVersion()); //$NON-NLS-1$
    properties.setProperty("packaging", artifactPage.getPackaging()); //$NON-NLS-1$

    if(artifactPage.getClassifier().length()>0) {
      properties.setProperty("classifier", artifactPage.getClassifier()); //$NON-NLS-1$
    }

    if(artifactPage.getPomFileName().length()>0) {
      properties.setProperty("pomFile", artifactPage.getPomFileName()); //$NON-NLS-1$
    }
    if(artifactPage.isGeneratePom()) {
      properties.setProperty("generatePom", "true"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    if(artifactPage.isCreateChecksum()) {
      properties.setProperty("createChecksum", "true"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    new Job(Messages.MavenInstallFileWizard_job) {
      protected IStatus run(IProgressMonitor monitor) {
        setProperty(IProgressConstants.ACTION_PROPERTY, new OpenMavenConsoleAction());
        MavenPlugin plugin = MavenPlugin.getDefault();
        try {
          // Run the install:install-file goal
          IMaven maven = MavenPlugin.getDefault().getMaven();
          MavenExecutionRequest request = maven.createExecutionRequest(monitor);
          request.setGoals(Arrays.asList("install:install-file")); //$NON-NLS-1$
          request.setUserProperties(properties);
          MavenExecutionResult executionResult = maven.execute(request, monitor);
          
          List<Throwable> exceptions = executionResult.getExceptions();
          if(!exceptions.isEmpty()) {
            for(Throwable exception : exceptions) {
              String msg = Messages.MavenInstallFileWizard_error;
              plugin.getConsole().logError(msg + "; " + exception.toString());  //$NON-NLS-1$
              MavenLogger.log(msg, exception);
            }
          }
          
          // TODO update index for local maven repository
          
        } catch (CoreException ex) {
          MavenLogger.log(ex);
          plugin.getConsole().logError("Failed to install artifact");
        }
        return Status.OK_STATUS;
      }
    }.schedule();
    
    return true;
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    Object element = selection.getFirstElement();
    if(element instanceof IFile) {
      selectedFile = (IFile) element;
      setPomFile(selectedFile.getProject());
    } else if(element instanceof IProject) {
      setPomFile((IProject) element);
    }
  }

  private void setPomFile(IProject project) {
    if(project.isAccessible()) {
      IFile pomFile = project.getFile(IMavenConstants.POM_FILE_NAME);
      if(pomFile!=null && pomFile.isAccessible()) {
        this.pomFile = pomFile; 
      }
    }
  }

}
