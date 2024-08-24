/*******************************************************************************
 * Copyright (c) 2021-2022 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Mickael Istria (Red Hat Inc.) - Initial implementation
 * - Hannes Wellmann - Add tests to test ConsoleLineTracker's links and automatic debugger attachment
 *******************************************************************************/

package org.eclipse.m2e.core.ui.tests;

import static java.util.Map.entry;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static org.eclipse.m2e.actions.MavenLaunchConstants.ATTR_COLOR_VALUE_ALWAYS;
import static org.eclipse.m2e.actions.MavenLaunchConstants.ATTR_COLOR_VALUE_NEVER;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.Position;
import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.internal.console.ConsoleHyperlinkPosition;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("restriction")
public class ConsoleTest extends AbstractMavenProjectTestCase {

	private static Display display;

	@BeforeClass
	public static void initialize() throws CoreException {
		display = PlatformUI.getWorkbench().getDisplay();
		if (display.getThread() == Thread.currentThread()) {
			fail("Test cannot succeed in UI-Thread. Disable 'Run in UI thread' in this tests launch configuration.");
			// The IOConsolePartitioner, that is responsible to transfer the print-out of
			// the Maven build process to the Eclipse console runs in the UI-thread. If this
			// test is running in the same thread, it blocks the UI-Thread until it
			// terminates and consequently the awaited output is not added to the
			// console-Document before this test can read it.
		}
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			project.delete(true, null);
		}
	}

	@Test
	public void testConsole_hasOutputAndHasNoMultipleSLF4Jwarnings() throws Exception {
		Path tempProjectFolder = copyTestProjectIntoWorkspace("simplePomOK");
		File pomFile = tempProjectFolder.resolve(IMavenConstants.POM_FILE_NAME).toFile();

		IDocument document = runMavenBuild(pomFile.getParent(), ILaunchManager.RUN_MODE, ATTR_COLOR_VALUE_NEVER);

		String consoleText = display.syncCall(document::get); // final document content could have changed

		assertTrue("Expecting BUILD SUCCESS in console output but got:\n" + consoleText,
				lines(consoleText).anyMatch(l -> l.equals("[INFO] BUILD SUCCESS")));
		assertTrue("Expecting no 'multiple SLF4J bindings'-Warning in console output but got:\n" + consoleText,
				lines(consoleText).noneMatch(l -> l.contains("SLF4J: Class path contains multiple SLF4J bindings")));
	}

	private static final String MAVEN_PROJECT = "simple.projectWithJUnit-5_Test";

	@Test
	public void testConsole_debuggerAttachmentAndLinkAlignmentAndBehavior_mavenProject() throws Exception {

		importMavenProjectIntoWorkspace(MAVEN_PROJECT);

		IDocument document = runMavenBuild("${project_loc:" + MAVEN_PROJECT + "}", ILaunchManager.RUN_MODE,
				ATTR_COLOR_VALUE_NEVER, entry("maven.surefire.debug", "true"));

		assertLinkTextAndOpenedEditor(1, "simple.SimpleTest", //
				TestRunnerViewPart.class, "JUnit (simple.SimpleTest)", document);

		assertLinkTextAndOpenedEditor(0, "org.eclipse.m2e.tests:" + MAVEN_PROJECT, //
				MavenPomEditor.class, MAVEN_PROJECT + "/pom.xml", document);

		assertLinkTextAndOpenedEditor(3, MAVEN_PROJECT, //
				MavenPomEditor.class, MAVEN_PROJECT + "/pom.xml", document);

		assertDebugeePrintOutAndDebuggerLaunch(document, MAVEN_PROJECT, "5005");
	}

	@Test
	public void testConsole_debuggerAttachmentAndLinkAlignmentAndBehavior_withColoredPrintout() throws Exception {

		importMavenProjectIntoWorkspace(MAVEN_PROJECT);

		IDocument document = runMavenBuild("${project_loc:" + MAVEN_PROJECT + "}", ILaunchManager.RUN_MODE,
				ATTR_COLOR_VALUE_ALWAYS, entry("maven.surefire.debug", "true"));

		assertLinkTextAndOpenedEditor(1, "simple.\u001B[1mSimpleTest", //
				TestRunnerViewPart.class, "JUnit (simple.SimpleTest)", document);

		assertLinkTextAndOpenedEditor(0, "\u001B[0;36morg.eclipse.m2e.tests:" + MAVEN_PROJECT, //
				MavenPomEditor.class, MAVEN_PROJECT + "/pom.xml", document);

		assertLinkTextAndOpenedEditor(3, "\u001B[36msimple.projectWithJUnit-5_Test", //
				MavenPomEditor.class, MAVEN_PROJECT + "/pom.xml", document);

		assertDebugeePrintOutAndDebuggerLaunch(document, MAVEN_PROJECT, "5005");
	}

	private static final String TYCHO_PROJECT = "simple-tycho";
	private static final String TYCHO_TEST_PROJECT = "simple.tests";

	@Test
	public void testConsole_debuggerAttachmentAndLinkAlignmentAndBehavior_tychoProject() throws Exception {

		Path parentParentPath = importProjectIntoWorkspace(TYCHO_PROJECT, TYCHO_TEST_PROJECT);

		IDocument document = runMavenBuild(parentParentPath.toString(), ILaunchManager.RUN_MODE, ATTR_COLOR_VALUE_NEVER,
				entry("debugPort", "8000"));

		assertLinkTextAndOpenedEditor(1, "simple.SimpleTest", //
				TestRunnerViewPart.class, "JUnit (simple.SimpleTest)", document);

		assertLinkTextAndOpenedEditor(0, "org.eclipse.m2e.tests:" + TYCHO_TEST_PROJECT, //
				ManifestEditor.class, TYCHO_TEST_PROJECT, document);

		assertLinkTextAndOpenedEditor(-2, TYCHO_TEST_PROJECT, //
				ManifestEditor.class, TYCHO_TEST_PROJECT, document);

		assertDebugeePrintOutAndDebuggerLaunch(document, TYCHO_TEST_PROJECT, "8000");
	}

	private void assertLinkTextAndOpenedEditor(int index, String expectedLinkText, Class<?> expectedPartType,
			String expectedEditorTitle, IDocument document) throws Exception {

		Position[] positions = document.getPositions(ConsoleHyperlinkPosition.HYPER_LINK_CATEGORY);
		int i = index < 0 ? positions.length + index : index;
		ConsoleHyperlinkPosition link = (ConsoleHyperlinkPosition) positions[i];

		assertEquals(expectedLinkText, document.get(link.getOffset(), link.getLength()));

		display.syncCall(() -> { // click hyper-link in console
			link.getHyperLink().linkActivated();
			return true; // syncExec does not re-throw exceptions
		});
		IWorkbenchPart activePart = display.syncCall( // get activePart in subsequent display.syncCall to await updates
				() -> PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart());
		assertEquals("Expected window part has not been opened", expectedEditorTitle, activePart.getTitle());
		assertThat("Expected window part has not been opened", activePart, is(instanceOf((Class<?>) expectedPartType)));
	}

	private static void assertDebugeePrintOutAndDebuggerLaunch(IDocument document, String debugLaunchName, String port)
			throws CoreException {
		// Check for Listening debugee print-out
		String consoleText = display.syncCall(document::get); // final document content could have changed
		assertThat(consoleText, containsString("Listening for transport dt_socket at address: " + port));

		// Check for a corresponding remote-java-debugging launch
		ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
		assertEquals(2, launches.length);
		ILaunch debugLaunch = launches[1]; // the first launch is the maven build itself
		assertEquals(IJavaLaunchConfigurationConstants.ID_REMOTE_JAVA_APPLICATION,
				debugLaunch.getLaunchConfiguration().getType().getIdentifier());
		assertEquals(debugLaunchName, debugLaunch.getLaunchConfiguration()
				.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""));
		assertEquals(ILaunchManager.DEBUG_MODE, debugLaunch.getLaunchMode());
		long startTime = System.currentTimeMillis();
		while (!debugLaunch.isTerminated() && System.currentTimeMillis() - startTime < 10000) {
			Thread.onSpinWait();
		}
		assertTrue("Debug launch " + debugLaunch.getLaunchConfiguration().getName()
				+ " is not terminated yet after waiting for 10 seconds", debugLaunch.isTerminated());
	}

	// --- common utility methods ---

	private static void importMavenProjectIntoWorkspace(String projectName) throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (!project.exists()) { // re-use project if already imported

			Path tempProjectFolder = copyTestProjectIntoWorkspace(projectName);
			File pomFile = tempProjectFolder.resolve(IMavenConstants.POM_FILE_NAME).toFile();

			List<MavenProjectInfo> projectInfos = List.of(new MavenProjectInfo("/pom.xml", pomFile, null, null));
			ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration();

			IProjectConfigurationManager pcm = MavenPlugin.getProjectConfigurationManager();
			pcm.importProjects(projectInfos, importConfiguration, null, new NullProgressMonitor());
			// build project to make it available in the ProjectRegistryManager
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		}
	}

	private static Path importProjectIntoWorkspace(String containerPath, String testProjectName) throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject(testProjectName);
		if (!project.exists()) { // re-use project if already imported

			Path tempProjectFolder = copyTestProjectIntoWorkspace(containerPath);
			Path projectPath = tempProjectFolder.resolve(testProjectName);
			IPath projectFile = IPath.fromPath(projectPath.resolve(".project"));
			IProjectDescription description = workspace.loadProjectDescription(projectFile);
			project.create(description, null);
			project.open(monitor);
			// build project to make it available in the PluginRegistryManager
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			project.build(IncrementalProjectBuilder.FULL_BUILD, null);
			return tempProjectFolder;
		}
		return Path.of(project.getLocationURI()).getParent();
	}

	private static Path copyTestProjectIntoWorkspace(String projectName) throws IOException, URISyntaxException {
		String projectPath = "/resources/projects/" + projectName;
		// import project into workspace, the super class cleans up after each test
		Path workspaceLocation = Path.of(ResourcesPlugin.getWorkspace().getRoot().getLocationURI());
		Path tempProjectFolder = workspaceLocation.resolve(projectName);
		URL pomResource = ConsoleTest.class.getResource(projectPath);
		Path sourceFolder = Path.of(FileLocator.toFileURL(pomResource).toURI());
		// Copy project resources to not pollute git repo with files generated at import
		copyFiles(sourceFolder, tempProjectFolder);
		return tempProjectFolder;
	}

	private static void copyFiles(Path source, Path target) throws IOException {
		try (Stream<Path> files = Files.walk(source).filter(Files::isRegularFile)) {
			for (Path filePath : (Iterable<Path>) files::iterator) {
				Path targetFile = target.resolve(source.relativize(filePath));
				Files.createDirectories(targetFile.getParent());
				Files.copy(filePath, targetFile, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	@SafeVarargs
	private static IDocument runMavenBuild(String pomDir, String mode, int attrColorValue,
			Entry<String, String>... properties) throws Exception {

		CompletableFuture<IConsole> consoleAfterStartSupplier = getConsoleAfterLaunchSupplier();

		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		launchManager.removeLaunches(launchManager.getLaunches()); // clear existing launches
		String configName = launchManager.generateLaunchConfigurationName("testConsole");
		var configType = launchManager.getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);
		var wc = configType.newInstance(null, configName);
		wc.setAttribute(MavenLaunchConstants.ATTR_GOALS, "clean verify");
		wc.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, pomDir);
		// The ANSI codes produced when color is enabled break the detection of certain
		// significant lines
		wc.setAttribute(MavenLaunchConstants.ATTR_COLOR, attrColorValue);
		wc.setAttribute(MavenLaunchConstants.ATTR_PROPERTIES,
				Arrays.stream(properties).map(e -> e.getKey() + "=" + e.getValue()).collect(toList()));
		wc.launch(mode, new NullProgressMonitor());

		IConsole mavenConsole = consoleAfterStartSupplier.get(30, TimeUnit.SECONDS);
		IDocument document = ((TextConsole) mavenConsole).getDocument();

		CountDownLatch finishedRead = new CountDownLatch(1);
		document.addDocumentListener(new IDocumentListener() {
			@Override
			public void documentChanged(DocumentEvent event) {
				if (isBuildFinished(event.getText())) {
					finishedRead.countDown(); // called from UI-Thread where the document updates are performed too
				}
			}

			@Override
			public void documentAboutToBeChanged(DocumentEvent event) { // ignore
			}
		});

		// First check if the full build print-out was already written. If not, wait
		// for the document-listener's signal
		String consoleText = display.syncCall(document::get);
		if (!isBuildFinished(consoleText) && !finishedRead.await(120, TimeUnit.SECONDS)) {
			fail("Build timed out.");
		}
		return document;
	}

	private static boolean isBuildFinished(String text) {
		return lines(text).anyMatch(
				l -> l.startsWith("[INFO] Finished at: ") || l.startsWith("[\u001B[1;34mINFO\u001B[m] Finished at: "));
	}

	private static final Pattern LINE_SEPARATOR = Pattern.compile("\\R");

	private static Stream<String> lines(String consoleText) {
		return LINE_SEPARATOR.splitAsStream(consoleText).map(String::strip).filter(not(String::isEmpty));
	}

	private static CompletableFuture<IConsole> getConsoleAfterLaunchSupplier() {
		IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		CompletableFuture<IConsole> container = new CompletableFuture<>();
		consoleManager.addConsoleListener(new IConsoleListener() {
			@Override
			public void consolesRemoved(IConsole[] consoles) { // No-Op
			}

			@Override
			public void consolesAdded(IConsole[] consoles) {
				container.complete(consoles[0]);
			}
		});
		return container;
	}
}
