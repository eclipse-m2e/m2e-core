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

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.containsString;
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
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
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.internal.console.ConsoleHyperlinkPosition;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("restriction")
public class ConsoleTest extends AbstractMavenProjectTestCase {

	@ClassRule
	public static final TemporaryFolder TEMP_DIRECOTRY = new TemporaryFolder();
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

	@AfterClass
	public static void tearDownAfterClass() throws CoreException {
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			project.delete(true, null);
		}
	}

	@Test
	public void testConsole_hasOutputAndHasNoMultipleSLF4Jwarnings() throws Exception {
		File pomFile = getTestProjectPomFile("simplePomOK");

		IDocument document = runMavenBuild(pomFile.getParent(), ILaunchManager.RUN_MODE);

		String consoleText = display.syncCall(document::get); // final document content could have changed

		assertTrue("Expecting BUILD SUCCESS in console output but got:\n" + consoleText,
				lines(consoleText).anyMatch(l -> l.equals("[INFO] BUILD SUCCESS")));
		assertTrue("Expecting no 'multiple SLF4J bindings'-Warning in console output but got:\n" + consoleText,
				lines(consoleText).noneMatch(l -> l.contains("SLF4J: Class path contains multiple SLF4J bindings")));
	}

	@Test
	public void testConsole_testReportLinkAlignmentAndClickability() throws Exception {

		importMavenProjectIntoWorkspace("simpleProjectWithJUnit5Test");

		IDocument document = runMavenBuild("${project_loc:simpleProjectWithJUnit5Test}", ILaunchManager.RUN_MODE);

		Position[] positions = document.getPositions(ConsoleHyperlinkPosition.HYPER_LINK_CATEGORY);
		assertEquals(1, positions.length);
		ConsoleHyperlinkPosition linkPosition = (ConsoleHyperlinkPosition) positions[0];

		String linkText = document.get(linkPosition.getOffset(), linkPosition.getLength());
		assertEquals("Invalid aligmnent of Test-Report link text", "simpleProjectWithJUnit5Test.SimpleTest", linkText);

		display.syncCall(() -> { // click hyper-link in console
			linkPosition.getHyperLink().linkActivated();
			return true; // syncExec does not re-throw exceptions
		});
		String activePartTitle = display.syncCall( // get titel in subsequent display.syncCall to await updates
				() -> PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getTitle());
		assertThat("Click Test-Report link should open JUnit-view", activePartTitle,
				containsString("simpleProjectWithJUnit5Test.SimpleTest"));
	}

	@Test
	public void testConsole_automaticDebuggerAttachment() throws Exception {

		importMavenProjectIntoWorkspace("simpleProjectWithJUnit5Test");

		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		launchManager.removeLaunches(launchManager.getLaunches());

		Map<String, Map<String, String>> launchConfigs = Map.of(//
				ILaunchManager.RUN_MODE, Map.of("maven.surefire.debug", "true"), // explicit debug mode
				ILaunchManager.DEBUG_MODE, Map.of()); // automatic debug mode of surefire plugin

		for (Entry<String, Map<String, String>> entry : launchConfigs.entrySet()) {
			String launchMode = entry.getKey();
			@SuppressWarnings("unchecked")
			Entry<String, String>[] args = entry.getValue().entrySet().toArray(Entry[]::new);

			IDocument document = runMavenBuild("${project_loc:simpleProjectWithJUnit5Test}", launchMode, args);

			// Check for Listening debugee print-out
			String consoleText = display.syncCall(document::get); // final document content could have changed
			assertThat(consoleText, containsString("Listening for transport dt_socket at address: 5005"));

			// Check for a corresponding remote-java-debugging launch
			ILaunch[] launches = launchManager.getLaunches();
			assertEquals(2, launches.length, 2);
			ILaunch debugLaunch = launches[1]; // the first launch is the maven build itself
			assertEquals(IJavaLaunchConfigurationConstants.ID_REMOTE_JAVA_APPLICATION,
					debugLaunch.getLaunchConfiguration().getType().getIdentifier());
			assertEquals("simpleProjectWithJUnit5Test", debugLaunch.getLaunchConfiguration()
					.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""));
			assertEquals("debug", debugLaunch.getLaunchMode());
			assertTrue(debugLaunch.isTerminated());
		}
	}

	private static File getTestProjectPomFile(String testProjectName) throws URISyntaxException, IOException {
		String projectPath = "/resources/projects/" + testProjectName;
		Path tempProjectFolder = TEMP_DIRECOTRY.newFolder(testProjectName).toPath();
		URL pomResource = ConsoleTest.class.getResource(projectPath);
		Path sourceFolder = Path.of(FileLocator.toFileURL(pomResource).toURI());
		// Copy project resources to not pollute git repo with files generated at import
		copyFiles(sourceFolder, tempProjectFolder);
		return tempProjectFolder.resolve(IMavenConstants.POM_FILE_NAME).toFile();
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

	private static void importMavenProjectIntoWorkspace(String testProjectName)
			throws CoreException, URISyntaxException, IOException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(testProjectName);
		if (!project.exists()) { // re-use project if already imported
			File pomFile = getTestProjectPomFile(testProjectName);

			List<MavenProjectInfo> projectInfos = List.of(new MavenProjectInfo("/pom.xml", pomFile, null, null));
			ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration();

			IProjectConfigurationManager pcm = MavenPlugin.getProjectConfigurationManager();
			pcm.importProjects(projectInfos, importConfiguration, null, new NullProgressMonitor());
			// build project to make it available in the ProjectRegistryManager
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		}
	}

	@SafeVarargs
	private static IDocument runMavenBuild(String pomDir, String mode, Entry<String, String>... properties)
			throws Exception {

		CompletableFuture<IConsole> consoleAfterStartSupplier = getConsoleAfterLaunchSupplier();

		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		String configName = launchManager.generateLaunchConfigurationName("testConsole");
		var configType = launchManager.getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);
		var wc = configType.newInstance(null, configName);
		wc.setAttribute(MavenLaunchConstants.ATTR_GOALS, "clean verify");
		wc.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, pomDir);
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
		if (!isBuildFinished(consoleText) && !finishedRead.await(30, TimeUnit.SECONDS)) {
			fail("Build timed out.");
		}
		return document;
	}

	private static boolean isBuildFinished(String text) {
		return lines(text).anyMatch(l -> l.startsWith("[INFO] Finished at"));
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
