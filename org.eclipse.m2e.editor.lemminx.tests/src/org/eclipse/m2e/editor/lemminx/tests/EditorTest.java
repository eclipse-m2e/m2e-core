/*******************************************************************************
 * Copyright (c) 2020, 2023 Red Hat Inc. and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.lsp4e.operations.completion.LSContentAssistProcessor;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.tests.harness.util.DisplayHelper;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.wildwebdeveloper.xml.internal.Activator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("restriction")
public class EditorTest extends AbstractMavenProjectTestCase {

	private static final String GENERIC_EDITOR = "org.eclipse.ui.genericeditor.GenericEditor";

	private static final String XML_PREFERENCES_DOWNLOAD_EXTERNAL_RESOURCES = "org.eclipse.wildwebdeveloper.xml.downloadExternalResources.enabled";

	private static final long WAIT_TIMEOUT = 15000;

	private IWorkbenchPage page;
	private IProject project;

	@Before
	public void setPage() {
		Activator.getDefault().getPreferenceStore().setValue(XML_PREFERENCES_DOWNLOAD_EXTERNAL_RESOURCES, true);
		page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}

	@After
	public void closeAndDeleteAll() throws CoreException {
		page.closeAllEditors(false);
		if (project != null) {
			project.delete(true, null);
		}
	}

	@Test
	public void testGenericEditorHasMavenExtensionEnabled() throws Exception {
		project = createMavenProject("test" + System.currentTimeMillis(), "pom.xml");
		IFile pomFile = project.getFile("pom.xml");

		ITextEditor editorPart = (ITextEditor) IDE.openEditor(page, pomFile, GENERIC_EDITOR);
		Display display = page.getWorkbenchWindow().getShell().getDisplay();
		assertTrue("Missing diagnostic report", DisplayHelper.waitForCondition(display, WAIT_TIMEOUT, () -> {
			try {
				project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
				return Arrays.stream(pomFile.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE))
						.anyMatch(marker -> marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR
								&& marker.getAttribute(IMarker.MESSAGE, "").contains("artifactId"));
			} catch (CoreException e) {
				return false;
			}
		}));
		int offset = editorPart.getDocumentProvider().getDocument(editorPart.getEditorInput()).get()
				.indexOf("</scope>");
		editorPart.getSelectionProvider().setSelection(new TextSelection(offset, 0));
		editorPart.getAction(ITextEditorActionConstants.CONTENT_ASSIST).run();
		LSContentAssistProcessor contentAssistProcessor = new LSContentAssistProcessor();
		ICompletionProposal[] proposals = contentAssistProcessor
				.computeCompletionProposals(getSourceViewer(editorPart), offset);
		assertTrue("Missing completion proposals",
				Arrays.stream(proposals).map(ICompletionProposal::getDisplayString).anyMatch("compile"::equals));
	}

	@Test
	public void testEditorOpenOnSourcePage() throws Exception {
		IPreferenceStore preferenceStore = M2EUIPluginActivator.getDefault().getPreferenceStore();
		preferenceStore.setValue(MavenPreferenceConstants.P_DEFAULT_POM_EDITOR_PAGE, true);

		project = createMavenProject("test" + System.currentTimeMillis(), "pom.xml");
		IFile pomFile = project.getFile("pom.xml");

		MavenPomEditor editor = (MavenPomEditor) IDE.openEditor(page, pomFile, MavenPomEditor.EDITOR_ID);
		Assert.assertNotNull(editor.getSourcePage());
		Assert.assertEquals(editor.getSourcePage(), editor.getActiveEditor());
	}

	@Test
	public void testOpenChildThenParentResolvesParent() throws Exception {
		createMavenProject("parent", "pom-parent.xml");
		IProject child = createMavenProject("child", "pom-child.xml");
		IFile pomFile = child.getFile("pom.xml");
		IDE.openEditor(page, pomFile, GENERIC_EDITOR);
		Display display = page.getWorkbenchWindow().getShell().getDisplay();
		assertTrue("Expected marker not published", DisplayHelper.waitForCondition(display, WAIT_TIMEOUT, () -> {
			try {
				IMarker[] markers = pomFile.findMarkers("org.eclipse.lsp4e.diagnostic", false, IResource.DEPTH_ZERO);
				if (markers.length == 0) {
					return false;
				}
				return markers[0].getAttribute(IMarker.MESSAGE, "").contains("packaging");
			} catch (CoreException e) {
				return false;
			}
		}));
	}

	private IProject createMavenProject(String projectName, String pomFileName) throws CoreException, IOException {
		try (InputStream pomContent = getClass().getResourceAsStream(pomFileName)) {
			return createProject(projectName, pomContent);
		}
	}

	private static ISourceViewer getSourceViewer(ITextEditor editor) {
		try {
			Method method = AbstractTextEditor.class.getDeclaredMethod("getSourceViewer");
			method.setAccessible(true);
			return (ISourceViewer) method.invoke(editor);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
