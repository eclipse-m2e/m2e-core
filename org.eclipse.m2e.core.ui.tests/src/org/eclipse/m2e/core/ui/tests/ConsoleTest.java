/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Mickael Istria (Red Hat Inc.) - Initial implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.tests;

import static java.util.function.Predicate.not;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.TextConsole;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("restriction")
public class ConsoleTest extends AbstractMavenProjectTestCase {

	@BeforeClass
	public static void initialize() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (display.getThread() == Thread.currentThread()) {
			fail("Test cannot succeed in UI-Thread. Disable 'Run in UI thread' in this tests launch configuration.");
			// The IOConsolePartitioner, that is responsible to transfer the print-out of
			// the Maven build process to the Eclipse console runs in the UI-thread. If this
			// test is running in the same thread, it blocks the UI-Thread until it
			// terminates and consequently the awaited output is not added to the
			// console-Document before this test can read it.
		}
	}

	private static final String TEST_POM = "/resources/projects/simplePomOK/pom.xml";

	@Test
	public void testConsoleHasOutputAndHasNoMultipleSLF4Jwarnings() throws Exception {
		File pomFile = new File(FileLocator.toFileURL(ConsoleTest.class.getResource(TEST_POM)).toURI());

		CompletableFuture<IConsole> consoleAfterStartSupplier = getConsoleAfterLaunchSupplier();

		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		String configName = launchManager.generateLaunchConfigurationName("testConsole");
		var configType = launchManager.getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);
		var wc = configType.newInstance(null, configName);
		wc.setAttribute(DebugPlugin.ATTR_WORKING_DIRECTORY, pomFile.getParent());
		wc.setAttribute(MavenLaunchConstants.ATTR_GOALS, "verify");
		wc.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, pomFile.getParent());
		wc.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());

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
		Display display = PlatformUI.getWorkbench().getDisplay();
		String consoleText = display.syncCall(document::get);
		if (!isBuildFinished(consoleText) && !finishedRead.await(30, TimeUnit.SECONDS)) {
			fail("Build timed out.");
		}
		consoleText = display.syncCall(document::get); // final document content could have changed

		assertTrue("Expecting BUILD SUCCESS in console output but got:\n" + consoleText,
				lines(consoleText).anyMatch(l -> l.equals("[INFO] BUILD SUCCESS")));
		assertTrue("Expecting no 'multiple SLF4J bindings'-Warning in console output but got:\n" + consoleText,
				lines(consoleText).noneMatch(l -> l.contains("SLF4J: Class path contains multiple SLF4J bindings")));
	}

	private static boolean isBuildFinished(String text) {
		return lines(text).anyMatch(l -> l.startsWith("[INFO] Finished at"));
	}

	private static final Pattern LINE_SEPARATOR = Pattern.compile("\\R");

	private static Stream<String> lines(String consoleText) {
		return LINE_SEPARATOR.splitAsStream(consoleText).map(String::strip).filter(not(String::isEmpty));
	}

	private CompletableFuture<IConsole> getConsoleAfterLaunchSupplier() {
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
