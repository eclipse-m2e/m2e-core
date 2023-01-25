/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.editor.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.tests.harness.util.DisplayHelper;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.Test;

public class M2EEditorTest extends AbstractMavenProjectTestCase {

	@Test
	public void testEffectivePomRendersWithoutException() throws CoreException, IOException {
		try (InputStream in = new ByteArrayInputStream("""
				<project>
				  <modelVersion>4.0.0</modelVersion>
				  <groupId>com.mycompany.app</groupId>
				  <artifactId>my-app</artifactId>
				  <version>1</version>
				</project>""".getBytes())) {
			IProject project = createProject("basic", in);
			MavenPomEditor editor = (MavenPomEditor) IDE.openEditor(
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
					new FileEditorInput(project.getFile("pom.xml")), MavenPomEditor.EDITOR_ID);
			boolean[] done = { false };
			Job.getJobManager().addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					if (event.getJob().getClass().getSimpleName().contains("LoadEffective")) {
						done[0] = true;
					}
				}
			});
			List<IStatus> allStatus = new ArrayList<>();
			Platform.getLog(Platform.getBundle("org.eclipse.text"))
					.addLogListener((status, plugin) -> allStatus.add(status));
			editor.loadEffectivePOM();
			ITextEditor effectivePomEditor = editor.getEffectivePomSourcePage();
			assertTrue(DisplayHelper.waitForCondition(Display.getDefault(), 3000, () -> done[0]));
			assertEquals(List.of(), allStatus);
			IDocument document = effectivePomEditor.getDocumentProvider()
					.getDocument(effectivePomEditor.getEditorInput());
			String docText = document.get();
			assertTrue(docText.contains("my-app"));
			assertFalse(docText.contains("Loading"));
		}
	}

	@Test
	public void testOpenPomEditor() throws Exception {
		IProject project = null;
		try (InputStream stream = getClass().getResourceAsStream("pom.xml")) {
			project = createProject("basic", stream);
			IProjectDescription desc = project.getDescription();
			desc.setNatureIds(new String[] { IMavenConstants.NATURE_ID });
			project.setDescription(desc, monitor);
			refreshMavenProject(project);
		}
		waitForJobsToComplete();
		MavenPomEditor editor = (MavenPomEditor) new OpenPomAction().openPomEditor("org.apache.commons",
				"commons-math3", "3.2", MavenPlugin.getMavenProjectRegistry().getProject(project).getMavenProject(),
				new NullProgressMonitor());
		assertNotEquals(-1, editor.getActivePage());
	}

	@Test
	public void testDeleteResourceClosesEditor() throws Exception {
		IProject project = null;
		try (InputStream stream = getClass().getResourceAsStream("pom.xml")) {
			project = createProject("basic", stream);
		}
		waitForJobsToComplete();
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		assertEquals(0, page.getEditorReferences().length);
		IEditorPart editor = IDE.openEditor(page, project.getFile("pom.xml"));
		assertTrue(editor instanceof MavenPomEditor);
		assertEquals(1, page.getEditorReferences().length);
		project.getFile("pom.xml").delete(true, null);
		assertTrue(DisplayHelper.waitForCondition(Display.getDefault(), 5000,
				() -> page.getEditorReferences().length == 0));
	}
}
