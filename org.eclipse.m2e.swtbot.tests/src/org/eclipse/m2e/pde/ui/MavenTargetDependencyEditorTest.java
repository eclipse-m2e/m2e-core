/*******************************************************************************
 * Copyright (c) 2024 Patrick Ziegler and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Patrick Ziegler - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.m2e.pde.target.MavenTargetLocation;
import org.eclipse.m2e.pde.target.MavenTargetLocationFactory;
import org.eclipse.m2e.pde.ui.target.editor.MavenTargetLocationWizard;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.utils.TableCollection;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@code MavenTargetDependencyEditor} using SWTBot.
 */
public class MavenTargetDependencyEditorTest {
	private static final String MAVEN_LOCATION_XML = """
		<location includeDependencyDepth="none" includeDependencyScopes="compile" includeSource="true" label="Maven Central" missingManifest="generate" type="Maven">
			<dependencies>
				<dependency>
					<groupId>com.fasterxml.jackson.core</groupId>
					<artifactId>jackson-annotations</artifactId>
					<version>2.14.1</version>
					<type>jar</type>
				</dependency>
				<dependency>
					<groupId>com.fasterxml.jackson.core</groupId>
					<artifactId>jackson-core</artifactId>
					<version>2.14.1</version>
					<type>jar</type>
				</dependency>
				<dependency>
					<groupId>com.fasterxml.jackson.core</groupId>
					<artifactId>jackson-databind</artifactId>
					<version>2.14.1</version>
					<type>jar</type>
				</dependency>
				<dependency>
					<groupId>com.github.ben-manes.caffeine</groupId>
					<artifactId>caffeine</artifactId>
					<version>3.1.2</version>
					<type>jar</type>
				</dependency>
				<dependency>
					<groupId>com.squareup.okhttp3</groupId>
					<artifactId>okhttp</artifactId>
					<version>4.10.0</version>
					<type>jar</type>
				</dependency>
				<dependency>
					<groupId>com.squareup.okio</groupId>
					<artifactId>okio-jvm</artifactId>
					<version>3.2.0</version>
					<type>jar</type>
				</dependency>
				<dependency>
					<groupId>jakarta.activation</groupId>
					<artifactId>jakarta.activation-api</artifactId>
					<version>1.2.2</version>
					<type>jar</type>
				</dependency>
				<dependency>
					<groupId>jakarta.annotation</groupId>
					<artifactId>jakarta.annotation-api</artifactId>
					<version>1.3.5</version>
					<type>jar</type>
				</dependency>
				<dependency>
					<groupId>jakarta.inject</groupId>
					<artifactId>jakarta.inject-api</artifactId>
					<version>1.0.5</version>
					<type>jar</type>
				</dependency>
				<dependency>
					<groupId>jakarta.ws.rs</groupId>
					<artifactId>jakarta.ws.rs-api</artifactId>
					<version>2.1.6</version>
					<type>jar</type>
				</dependency>
				<dependency>
					<groupId>jakarta.xml.bind</groupId>
					<artifactId>jakarta.xml.bind-api</artifactId>
					<version>2.3.3</version>
					<type>jar</type>
				</dependency>
				<dependency>
					<groupId>org.eclipse.jdt</groupId>
					<artifactId>org.eclipse.jdt.annotation</artifactId>
					<version>2.2.700</version>
					<type>jar</type>
				</dependency>
				<dependency>
					<groupId>org.jetbrains.kotlin</groupId>
					<artifactId>kotlin-stdlib-common</artifactId>
					<version>1.7.22</version>
					<type>jar</type>
				</dependency>
				<dependency>
					<groupId>org.jetbrains.kotlin</groupId>
					<artifactId>kotlin-stdlib-jdk7</artifactId>
					<version>1.7.22</version>
					<type>jar</type>
				</dependency>
				<dependency>
					<groupId>org.jetbrains.kotlin</groupId>
					<artifactId>kotlin-stdlib-jdk8</artifactId>
					<version>1.7.22</version>
					<type>jar</type>
				</dependency>
				<dependency>
					<groupId>org.jetbrains.kotlin</groupId>
					<artifactId>kotlin-stdlib</artifactId>
					<version>1.7.22</version>
					<type>jar</type>
				</dependency>
				<dependency>
					<groupId>org.slf4j</groupId>
					<artifactId>jcl-over-slf4j</artifactId>
					<version>2.0.6</version>
					<type>jar</type>
				</dependency>
				<dependency>
					<groupId>org.slf4j</groupId>
					<artifactId>slf4j-api</artifactId>
					<version>2.0.6</version>
					<type>jar</type>
				</dependency>
				<dependency>
					<groupId>org.slf4j</groupId>
					<artifactId>slf4j-simple</artifactId>
					<version>2.0.6</version>
					<type>jar</type>
				</dependency>
			</dependencies>
		</location>
		""";
	private WizardDialog wizardDialog;
	private IWorkbench workbench;
	private SWTBot robot;
	
	@Before
	public void setUp() throws Exception {
		if (Display.getCurrent() != null) {
			fail("""
					SWTBot test needs to run in a non-UI thread.
					Make sure that "Run in UI thread" is unchecked in your launch configuration or that useUIThread is set to false in the pom.xml
					""");
		}
		
		workbench = PlatformUI.getWorkbench();
		wizardDialog = workbench.getDisplay().syncCall(() -> {
			Shell shell = new Shell(workbench.getActiveWorkbenchWindow().getShell());
			MavenTargetLocation location = new MavenTargetLocationFactory().getTargetLocation("maven", MAVEN_LOCATION_XML);
			MavenTargetLocationWizard wizard = new MavenTargetLocationWizard(location);
			WizardDialog wizardDialog = new WizardDialog(shell, wizard);
			wizardDialog.setBlockOnOpen(false);
			wizardDialog.open();
			return wizardDialog;
		});
		robot = new SWTBot().shell("Maven Artifact Target Entry - Maven Central").bot();
	}

	@After
	public void tearDown() throws Exception {
		if (wizardDialog != null) {
			workbench.getDisplay().syncExec(wizardDialog::close);
		}
	}
	
	private void readAndDispatch() {
		Display display = workbench.getDisplay();
		display.syncExec(display::readAndDispatch);
	}

	/**
	 * Checks whether the initial "enablement" state of all buttons in the Maven
	 * dependency editor is set correctly.
	 */
	@Test
	public void testInitialButtonState() throws Exception {
		assertTrue(robot.button("Add").isEnabled());
		assertTrue(robot.button("Remove").isEnabled());
		assertTrue(robot.button("Update").isEnabled());
		assertFalse(robot.button("Undo").isEnabled());
		assertFalse(robot.button("Redo").isEnabled());
	}

	/**
	 * Checks whether new Maven coordinates can be added and edited.
	 */
	@Test
	public void testAddMavenLocation() throws Exception {
		workbench.getDisplay().syncExec(() -> {
			Clipboard clipboard = new Clipboard(workbench.getDisplay());
			clipboard.setContents(new Object[] {
					"""
					<dependency\\>
					"""
			}, new Transfer[] { TextTransfer.getInstance() });
			clipboard.dispose();
		});
		robot.button("Add").click();

		SWTBotTable table = robot.table();
		assertEquals(table.cell(19, 0), "<required>");
		assertEquals(table.cell(19, 1), "<required>");
		assertEquals(table.cell(19, 2), "<required>");
		assertEquals(table.cell(19, 3), "");
		assertEquals(table.cell(19, 4), "jar");

		assertFalse("Expected \"Update\" button to be disabled", robot.button("Update").isEnabled());
		assertFalse("Expected \"Finish\" button to be disabled", robot.button("Finish").isEnabled());
		// There is no elegant way to select the cell editor, but we
		// know that it will be the first text widget in the dialog.
		table.click(19, 0);
		robot.text(0).setText("org.apache.commons");
		table.click(19, 1);
		robot.text(0).setText("commons-lang3");
		table.click(19, 2);
		robot.text(0).setText("3.12.0");
		table.click(19, 3); // Close cell editor

		assertEquals(table.cell(19, 0), "org.apache.commons");
		assertEquals(table.cell(19, 1), "commons-lang3");
		assertEquals(table.cell(19, 2), "3.12.0");
		assertEquals(table.cell(19, 3), "");
		assertEquals(table.cell(19, 4), "jar");

		assertTrue("Expected \"Update\" button to be enabled", robot.button("Update").isEnabled());
		assertTrue("Expected \"Finished\" button to be enabled", robot.button("Finish").isEnabled());
	}

	/**
	 * Checks whether adding Maven dependencies from the clipboard is supported.
	 */
	@Test
	public void testAddMavenLocationWithClipboard() throws Exception {
		workbench.getDisplay().syncExec(() -> {
			Clipboard clipboard = new Clipboard(workbench.getDisplay());
			clipboard.setContents(new Object[] {
					"""
					<dependency>
						<groupId>org.eclipse.platform</groupId>
						<artifactId>org.eclipse.core.runtime</artifactId>
						<version>3.26.100</version>
					</dependency>"
					"""
			}, new Transfer[] { TextTransfer.getInstance() });
			clipboard.dispose();
		});
		robot.button("Add").click();

		SWTBotTable table = robot.table();
		assertEquals(table.cell(19, 0), "org.eclipse.platform");
		assertEquals(table.cell(19, 1), "org.eclipse.core.runtime");
		assertEquals(table.cell(19, 2), "3.26.100");
		assertEquals(table.cell(19, 3), "");
		assertEquals(table.cell(19, 4), "jar");
	}

	/**
	 * Tests whether the cells can be edited directly without first having to select
	 * the row (only possible with SWT.FULL_SELECTION).
	 */
	@Test
	public void testEditCellsDirectly() throws Exception {
		SWTBotTable table = robot.table();
		table.click(0, 0);
		robot.text("com.fasterxml.jackson.core").setText("a");
		table.click(1, 1);
		robot.text("jackson-core").setText("b");
		table.click(2, 2);
		robot.text("2.14.1").setText("c");
		table.click(0, 0); // Close cell editor

		assertEquals(table.cell(0, 0), "a");
		assertEquals(table.cell(1, 1), "b");
		assertEquals(table.cell(2, 2), "c");
	}

	/**
	 * Tests whether the version of one or more dependencies can be updated.
	 */
	@Test
	public void testUpdateMavenArtifactVersion() throws Exception {
		SWTBotTable table = robot.table();
		// Update single artifact
		assertEquals(table.cell(12, 1), "kotlin-stdlib-common");
		assertEquals(table.cell(12, 2), "1.7.22");

		table.select(12);
		robot.button("Update").click();
		readAndDispatch();

		assertEquals(table.cell(12, 1), "kotlin-stdlib-common");
		assertNotEquals(table.cell(12, 2), "1.7.22");

		// Update multiple artifacts
		assertEquals(table.cell(13, 1), "kotlin-stdlib-jdk7");
		assertEquals(table.cell(13, 2), "1.7.22");
		assertEquals(table.cell(15, 1), "kotlin-stdlib");
		assertEquals(table.cell(15, 2), "1.7.22");

		table.select(13, 15);
		robot.button("Update").click();
		readAndDispatch();

		assertEquals(table.cell(13, 1), "kotlin-stdlib-jdk7");
		assertNotEquals(table.cell(13, 2), "1.7.22");
		assertEquals(table.cell(15, 1), "kotlin-stdlib");
		assertNotEquals(table.cell(15, 2), "1.7.22");
	}

	/**
	 * Tests whether one or more artifacts can be deleted and whether the selection
	 * is updated correctly.
	 */
	@Test
	public void testRemoveArtifacts() throws Exception {
		// Removing multiple elements clears the selection
		SWTBotTable table = robot.table();
		assertEquals(table.cell(0, 1), "jackson-annotations");
		assertEquals(table.cell(1, 1), "jackson-core");
		assertEquals(table.cell(2, 1), "jackson-databind");
		table.select(0, 1, 2);

		robot.button("Remove").click();
		assertEquals(table.cell(0, 1), "caffeine");
		assertEquals(table.selectionCount(), 0);

		// Removing a single element selects the next element
		assertEquals(table.cell(3, 1), "jakarta.activation-api");
		table.select(3);

		robot.button("Remove").click();
		assertEquals(table.cell(3, 1), "jakarta.annotation-api");
		assertEquals(table.selectionCount(), 1);
		assertEquals(table.selection().get(0, 1), "jakarta.annotation-api");
	}

	/**
	 * Tests whether changes to the artifacts can be tracked via undo/redo.
	 */
	@Test
	public void testUndoRedo() throws Exception {
		SWTBotTable table = robot.table();
		assertEquals(table.cell(6, 1), "jakarta.activation-api");

		// Tests undo/redo on removal
		table.select(6);
		robot.button("Remove").click();
		assertEquals(table.cell(6, 1), "jakarta.annotation-api");

		robot.button("Undo").click();
		assertEquals(table.cell(6, 1), "jakarta.activation-api");

		robot.button("Redo").click();
		assertEquals(table.cell(6, 1), "jakarta.annotation-api");

		// Tests undo/redo on editing
		table.click(6, 1);
		robot.text("jakarta.annotation-api").setText("foo");
		table.click(6, 2); // Close cell editor
		assertEquals(table.cell(6, 1), "foo");

		robot.button("Undo").click();
		assertEquals(table.cell(6, 1), "jakarta.annotation-api");

		robot.button("Redo").click();
		assertEquals(table.cell(6, 1), "foo");
	}

	/**
	 * Tests whether artifacts are correctly sorted by the selected columns and
	 * whether items are inserted at the correct location.
	 */
	@Test
	public void testColumnSorting() throws Exception {
		SWTBotTable table = robot.table();

		// Sort by Version
		table.header("Version").click();
		assertEquals(table.cell(0, 1), "jakarta.inject-api");

		// Sort by Artifact Id
		table.header("Artifact Id").click();
		assertEquals(table.cell(0, 1), "caffeine");

		// Sort by Group Id
		table.header("Group Id").click();
		assertEquals(table.cell(0, 1), "jackson-annotations");

		workbench.getDisplay().syncExec(() -> {
			Clipboard clipboard = new Clipboard(workbench.getDisplay());
			clipboard.setContents(new Object[] {
					"""
					<dependency>
						<groupId>com.fasterxml.jackson.core</groupId>
						<artifactId>jackson-databind</artifactId>
						<version>2.14.0</version>
					</dependency>"
					"""
			}, new Transfer[] { TextTransfer.getInstance() });
			clipboard.dispose();
		});
		robot.button("Add").click();
		assertEquals(table.cell(2, 1), "jackson-databind");
	}

	/**
	 * Tests whether items are preserved when selecting more than one element.
	 */
	@Test
	public void testMultiSelection() throws Exception {
		SWTBotTable table = robot.table();

		// Forward selection
		table.select(0, 1, 2, 3);
		TableCollection selection = table.selection();
		assertEquals(selection.rowCount(), 4);
		assertEquals(selection.get(0, 1), "jackson-annotations");
		assertEquals(selection.get(1, 1), "jackson-core");
		assertEquals(selection.get(2, 1), "jackson-databind");
		assertEquals(selection.get(3, 1), "caffeine");

		// Backward selection
		table.select(16, 15, 14, 13);
		selection = table.selection();
		assertEquals(selection.rowCount(), 4);
		assertEquals(selection.get(0, 1), "kotlin-stdlib-jdk7");
		assertEquals(selection.get(1, 1), "kotlin-stdlib-jdk8");
		assertEquals(selection.get(2, 1), "kotlin-stdlib");
		assertEquals(selection.get(3, 1), "jcl-over-slf4j");
	}
}
