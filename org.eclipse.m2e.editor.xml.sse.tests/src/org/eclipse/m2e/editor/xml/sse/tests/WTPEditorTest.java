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
package org.eclipse.m2e.editor.xml.sse.tests;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.MultiPageEditor;
import org.eclipse.ui.tests.harness.util.DisplayHelper;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.junit.After;
import org.junit.Test;

public class WTPEditorTest {

	private IWorkbenchPage page;
	private IProject project;

	@After
	public void closeAndDeleteAll() throws CoreException {
		page.closeAllEditors(false);
		project.delete(true, null);
	}

	@Test
	public void testXSDCompletion() throws Exception {
		IPreferenceStore preferenceStore = M2EUIPluginActivator.getDefault().getPreferenceStore();
		preferenceStore.setValue(MavenPreferenceConstants.P_DEFAULT_POM_EDITOR_PAGE, true);
		page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		project = ResourcesPlugin.getWorkspace().getRoot().getProject("test" + System.currentTimeMillis());
		project.create(null);
		project.open(null);
		IFile pomFile = project.getFile("pom.xml");
		pomFile.create(getClass().getResourceAsStream("pom.xml"), true, null);
		ITextEditor editorPart = (ITextEditor) ((MavenPomEditor)IDE.openEditor(page, pomFile, MavenPomEditor.EDITOR_ID)).getActiveEditor();
		editorPart.getSelectionProvider().setSelection(new TextSelection(359, 0));
		Set<Shell> beforeShells = new HashSet<Shell>(Arrays.asList(Display.getDefault().getShells()));
		assertTrue(DisplayHelper.waitForCondition(editorPart.getSite().getShell().getDisplay(), 5000, () -> 
			openCompletionTable(editorPart, beforeShells)
				.map(Table::getItems)
				.flatMap(items -> Arrays.stream(items).map(TableItem::getText).filter("groupId"::equals).findAny())
				.isPresent()
		));
	}

	private Optional<Table> openCompletionTable(ITextEditor editorPart, Set<Shell> beforeShells) {
		editorPart.getAction(ITextEditorActionConstants.CONTENT_ASSIST).run();
		Set<Shell> afterShells = new HashSet<Shell>(Arrays.asList(Display.getDefault().getShells()));
		afterShells.removeAll(beforeShells);
		afterShells.removeIf(shell -> !shell.isVisible());
		Shell completionShell = afterShells.iterator().next();
		Optional<Table> table = Arrays.stream(completionShell.getChildren())
			.filter(Table.class::isInstance)
			.map(Table.class::cast)
			.findAny();
		return table;
	}

}
