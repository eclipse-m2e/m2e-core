/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

package org.eclipse.m2e.editor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


/**
 * @author atanasenko
 */
public class MavenPomEditorDirtyTest extends AbstractMavenProjectTestCase {

  @Test
  public void test358656_dirtyStateAfterFSModification() throws Exception {

    IProject[] projects = importProjects("projects/dirtyState", new String[] {"project/pom.xml"},
        new ResolverConfiguration());

    IProject project = projects[0];
    IFile pom = project.getFile("pom.xml");
    IEditorPart pomEditor = OpenPomAction.openEditor(new FileEditorInput(pom), pom.getName());

    // pomEditor must be inactive, unless a dialog with external
    IFile dummy = project.getFile("dummy.txt");
    IEditorPart dummyEditor = OpenPomAction.openEditor(new FileEditorInput(dummy), dummy.getName());

    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    assertSame("PomEditor active", dummyEditor, page.getActiveEditor());

    File pomFile = pom.getLocation().toFile();
    assertTrue(pomFile.exists()); // make sure we get the right one

    // replace content outside of eclipse
    assertTrue(pomFile.delete());
    File newPomFile = new File(pomFile.getParentFile(), "pom_modified.xml");
    assertTrue(newPomFile.renameTo(pomFile));

    project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());

    assertFalse("Dirty editor", pomEditor.isDirty());

  }

}
