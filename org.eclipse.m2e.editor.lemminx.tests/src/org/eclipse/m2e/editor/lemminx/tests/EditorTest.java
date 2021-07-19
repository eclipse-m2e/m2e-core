/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.editor.lemminx.tests;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.tests.harness.util.DisplayHelper;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class EditorTest {

	private IWorkbenchPage page;
	private IProject project;

	@After
	public void closeAndDeleteAll() throws CoreException {
		page.closeAllEditors(false);
		project.delete(true, null);
	}

	@Test
	public void testGenericEditorHasMavenExtensionEnabled() throws Exception {
		page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		project = ResourcesPlugin.getWorkspace().getRoot().getProject("test" + System.currentTimeMillis());
		project.create(null);
		project.open(null);
		IFile pomFile = project.getFile("pom.xml");
		pomFile.create(getClass().getResourceAsStream("pom.xml"), true, null);
		ITextEditor editorPart = (ITextEditor)IDE.openEditor(page, pomFile, "org.eclipse.ui.genericeditor.GenericEditor");
		Display display = page.getWorkbenchWindow().getShell().getDisplay();
		assertTrue("Missing diagnostic report", DisplayHelper.waitForCondition(display, 10000, () ->
			{
				try {
					return Arrays.stream(pomFile.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO)).anyMatch(marker ->
						marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR &&
						marker.getAttribute(IMarker.MESSAGE, "").contains("artifactId")
					);
				} catch (CoreException e) {
					return false;
				}
			}
		));
		int offset = editorPart.getDocumentProvider().getDocument(editorPart.getEditorInput()).get().indexOf("</scope>");
		Set<Shell> beforeShells = Arrays.stream(display.getShells()).filter(Shell::isVisible).collect(Collectors.toSet());
		editorPart.getSelectionProvider().setSelection(new TextSelection(offset, 0));
		editorPart.getAction(ITextEditorActionConstants.CONTENT_ASSIST).run();
		assertTrue("Missing completion proposals", DisplayHelper.waitForCondition(display, 3000, () -> {
			Set<Shell> afterShells = Arrays.stream(display.getShells()).filter(Shell::isVisible).collect(Collectors.toSet());
			afterShells.removeAll(beforeShells);
			return afterShells.stream()
				.flatMap(shell -> Arrays.stream(shell.getChildren()))
				.filter(Table.class::isInstance)
				.map(Table.class::cast)
				.findFirst()
				.map(table -> Boolean.valueOf(Arrays.stream(table.getItems()).map(TableItem::getText).anyMatch("compile"::equals)))
				.orElse(Boolean.FALSE).booleanValue();
		}));
	}

	@Test
	public void testEditorOpenOnSourcePage() throws CoreException {
		IPreferenceStore preferenceStore = M2EUIPluginActivator.getDefault().getPreferenceStore();
		preferenceStore.setValue(MavenPreferenceConstants.P_DEFAULT_POM_EDITOR_PAGE, true);
		page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		project = ResourcesPlugin.getWorkspace().getRoot().getProject("test" + System.currentTimeMillis());
		project.create(null);
		project.open(null);
		IFile pomFile = project.getFile("pom.xml");
		pomFile.create(getClass().getResourceAsStream("pom.xml"), true, null);
		MavenPomEditor editor = (MavenPomEditor)page.openEditor(new FileEditorInput(pomFile), MavenPomEditor.EDITOR_ID);
		Assert.assertNotNull(editor.getSourcePage());
		Assert.assertEquals(editor.getSourcePage(), editor.getActiveEditor());
	}
}
