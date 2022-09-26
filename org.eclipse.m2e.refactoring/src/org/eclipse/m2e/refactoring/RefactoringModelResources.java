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

package org.eclipse.m2e.refactoring;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.ecore.resource.Resource;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.PropertyElement;


/**
 * This class manages all refactoring-related resources for a particular maven project
 *
 * @author Anton Kraev
 */
public class RefactoringModelResources {
  private static final ILog log = Platform.getLog(RefactoringModelResources.class);

  private static final String TMP_PROJECT_NAME = ".m2eclipse_refactoring"; //$NON-NLS-1$

  protected IFile pomFile;

  protected IFile tmpFile;

  protected ITextFileBuffer pomBuffer;

  protected ITextFileBuffer tmpBuffer;

  protected Model tmpModel;

  protected org.apache.maven.model.Model effective;

  protected ITextFileBufferManager textFileBufferManager;

  protected Map<String, PropertyInfo> properties;

  protected MavenProject project;

  protected CompoundCommand command;

  protected static IProject tmpProject;

  protected IProject getTmpProject() {
    if(tmpProject == null) {
      tmpProject = ResourcesPlugin.getWorkspace().getRoot().getProject(TMP_PROJECT_NAME);
    }
    if(!tmpProject.exists()) {
      try {
        tmpProject.create(null);
        tmpProject.open(null);
      } catch(CoreException ex) {
        log.error(ex.getMessage(), ex);
      }
    }
    return tmpProject;
  }

  public RefactoringModelResources(IMavenProjectFacade projectFacade) throws CoreException, IOException {
    textFileBufferManager = FileBuffers.getTextFileBufferManager();
    project = projectFacade.getMavenProject(null);
    effective = project.getModel();
    pomFile = projectFacade.getPom();
    pomBuffer = getBuffer(pomFile);

    //create temp file
    IProject project = getTmpProject();
    File f = File.createTempFile("pom", ".xml", project.getLocation().toFile()); //$NON-NLS-1$ //$NON-NLS-2$
    f.delete();
    tmpFile = project.getFile(f.getName());
    pomFile.copy(tmpFile.getFullPath(), true, null);

    Resource resource = AbstractPomRefactoring.loadResource(tmpFile);
    tmpModel = (Model) resource.getContents().get(0);
    tmpBuffer = getBuffer(tmpFile);
  }

  public CompoundCommand getCommand() {
    return command;
  }

  public void setCommand(CompoundCommand command) {
    this.command = command;
  }

  public IFile getPomFile() {
    return pomFile;
  }

  public IFile getTmpFile() {
    return tmpFile;
  }

  public ITextFileBuffer getPomBuffer() {
    return pomBuffer;
  }

  public ITextFileBuffer getTmpBuffer() {
    return tmpBuffer;
  }

  public Model getTmpModel() {
    return tmpModel;
  }

  public org.apache.maven.model.Model getEffective() {
    return effective;
  }

  public MavenProject getProject() {
    return project;
  }

  public Map<String, PropertyInfo> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, PropertyInfo> properties) {
    this.properties = properties;
  }

  public void releaseAllResources() throws CoreException {
    releaseBuffer(pomBuffer, pomFile);
    if(tmpFile != null && tmpFile.exists()) {
      releaseBuffer(tmpBuffer, tmpFile);
    }
    if(tmpModel != null) {
      tmpModel.eResource().unload();
    }
  }

  public static void cleanupTmpProject() throws CoreException {
    if(tmpProject.exists()) {
      tmpProject.delete(true, true, null);
    }
  }

  protected ITextFileBuffer getBuffer(IFile file) throws CoreException {
    textFileBufferManager.connect(file.getLocation(), LocationKind.NORMALIZE, null);
    return textFileBufferManager.getTextFileBuffer(file.getLocation(), LocationKind.NORMALIZE);
  }

  protected void releaseBuffer(ITextFileBuffer buffer, IFile file) throws CoreException {
    buffer.revert(null);
    textFileBufferManager.disconnect(file.getLocation(), LocationKind.NORMALIZE, null);
  }

  public String getName() {
    return pomFile.getProject().getName();
  }

  public static class PropertyInfo {
    protected PropertyElement pair;

    protected RefactoringModelResources resource;

    protected Command newValue;

    public Command getNewValue() {
      return newValue;
    }

    public void setNewValue(Command newValue) {
      this.newValue = newValue;
    }

    public PropertyElement getPair() {
      return pair;
    }

    public void setPair(PropertyElement pair) {
      this.pair = pair;
    }

    public RefactoringModelResources getResource() {
      return resource;
    }

    public void setResource(RefactoringModelResources resource) {
      this.resource = resource;
    }
  }

}
