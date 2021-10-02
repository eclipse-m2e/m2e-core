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
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.tests.harness.util.DisplayHelper;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.Test;

public class M2EEditorTest extends AbstractMavenProjectTestCase {

	@Test
	public void testEffectivePomRendersWithoutException() throws CoreException, IOException {
		try (InputStream in = new ByteArrayInputStream(("<project>\n"
				+ "  <modelVersion>4.0.0</modelVersion>\n"
				+ "  <groupId>com.mycompany.app</groupId>\n"
				+ "  <artifactId>my-app</artifactId>\n"
				+ "  <version>1</version>\n"
				+ "</project>").getBytes())) {
			IProject project = createProject("basic", in);
			MavenPomEditor editor = (MavenPomEditor) IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), new FileEditorInput(project.getFile("pom.xml")), MavenPomEditor.EDITOR_ID);
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
			Platform.getLog(Platform.getBundle("org.eclipse.text")).addLogListener((status, plugin) -> allStatus.add(status));
			editor.loadEffectivePOM();
			ITextEditor effectivePomEditor = editor.getEffectivePomSourcePage();
			assertTrue(DisplayHelper.waitForCondition(Display.getDefault(), 3000, () -> done[0]));
			assertEquals(List.of(), allStatus);
			IDocument document = effectivePomEditor.getDocumentProvider().getDocument(effectivePomEditor.getEditorInput());
			assertTrue(document.get().contains("my-app"));
			assertFalse(document.get().contains("Loading"));
		}
	}
}
