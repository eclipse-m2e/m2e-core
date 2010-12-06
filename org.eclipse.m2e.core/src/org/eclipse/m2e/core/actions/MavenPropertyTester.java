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

package org.eclipse.m2e.core.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.m2e.core.core.IMavenConstants;

/**
 * Helper IPropertyTester implementation to check if receiver can be launched with Maven.
 * E.g. it is pom.xml file of folder or project that has pom.xml. 
 *
 * @author Eugene Kuleshov
 */
public class MavenPropertyTester extends PropertyTester {

  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    IAdaptable adaptable = (IAdaptable) receiver;
    
    IProject projectAdapter = (IProject) adaptable.getAdapter(IProject.class);
    if(projectAdapter!=null) {
      return projectAdapter.getFile(IMavenConstants.POM_FILE_NAME).exists();
    }
    
    IFolder folderAdapter = (IFolder) adaptable.getAdapter(IFolder.class);
    if(folderAdapter!=null) {
      return folderAdapter.getFile(IMavenConstants.POM_FILE_NAME).exists();
    }

    IFile fileAdapter = (IFile) adaptable.getAdapter(IFile.class);
    if(fileAdapter!=null) {
      return fileAdapter.exists() && IMavenConstants.POM_FILE_NAME.equals(fileAdapter.getName());
    }
    
    return false;
  }

}

