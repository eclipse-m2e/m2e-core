/*******************************************************************************
 * Copyright (c) 2026 Aleksandar Kurtakov and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Aleksandar Kurtakov - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.markers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.junit.Test;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.preferences.ProblemSeverity;

public class PomHintsTest {

	private static final String SCHEMA = " xmlns=\"http://maven.apache.org/POM/4.0.0\"" //
			+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" //
			+ " xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\"";

	@Test
	public void testMissingSchema() {
		String pom = """
				<?xml version="1.0" encoding="UTF-8"?>
				<!-- <project> in a comment -->
				<project xmlns="http://maven.apache.org/POM/4.0.0">
					<modelVersion>4.0.0</modelVersion>
				</project>
				""";
		List<PomHints.Hint> hints = checkForSchema(pom);
		assertEquals(1, hints.size());
		PomHints.Hint hint = hints.get(0);
		assertEquals(IMavenConstants.EDITOR_HINT_MISSING_SCHEMA, hint.editorHint());
		assertEquals(IMarker.SEVERITY_WARNING, hint.severity());
		assertRegion(pom, "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">", hint);
		assertEquals(3, hint.line());
	}

	@Test
	public void testSchemaPresent() {
		String pom = "<project" + SCHEMA + ">\n\t<modelVersion>4.0.0</modelVersion>\n</project>\n";
		assertTrue(checkForSchema(pom).isEmpty());
	}

	@Test
	public void testDuplicateParentGroupIdAndVersion() {
		String pom = "<project" + SCHEMA + """
				>
					<parent>
						<groupId>org.example</groupId>
						<artifactId>parent</artifactId>
						<version>1.0.0</version>
					</parent>
					<groupId>org.example</groupId>
					<artifactId>child</artifactId>
					<version>1.0.0</version>
				</project>
				""";
		List<PomHints.Hint> hints = checkVarious(pom, null);
		assertEquals(2, hints.size());
		assertEquals(IMavenConstants.EDITOR_HINT_PARENT_GROUP_ID, hints.get(0).editorHint());
		assertEquals(IMarker.SEVERITY_WARNING, hints.get(0).severity());
		assertRegion(pom, "<groupId>org.example</groupId>", pom.indexOf("</parent>"), hints.get(0));
		assertEquals(7, hints.get(0).line());
		assertEquals(IMavenConstants.EDITOR_HINT_PARENT_VERSION, hints.get(1).editorHint());
		assertRegion(pom, "<version>1.0.0</version>", pom.indexOf("</parent>"), hints.get(1));
		assertEquals(9, hints.get(1).line());

		assertTrue(PomHints.checkVarious(document(pom), read(pom), null, ProblemSeverity.ignore,
				ProblemSeverity.ignore, ProblemSeverity.warning).isEmpty());
	}

	@Test
	public void testDifferentParentGroupIdAndVersion() {
		String pom = """
				<project>
					<parent>
						<groupId>org.example</groupId>
						<artifactId>parent</artifactId>
						<version>1.0.0</version>
					</parent>
					<groupId>org.example.child</groupId>
					<artifactId>child</artifactId>
					<version>2.0.0</version>
				</project>
				""";
		assertTrue(checkVarious(pom, null).isEmpty());
	}

	@Test
	public void testOverridingManagedDependencyVersion() {
		String pom = """
				<project>
					<groupId>org.example</groupId>
					<artifactId>child</artifactId>
					<version>1.0.0</version>
					<dependencies>
						<dependency>
							<groupId>org.example</groupId>
							<artifactId>overriding</artifactId>
							<version >2.0</version>
						</dependency>
						<dependency>
							<groupId>org.example</groupId>
							<artifactId>duplicating</artifactId>
							<version>1.0</version>
						</dependency>
						<dependency>
							<groupId>org.example</groupId>
							<artifactId>ignored</artifactId>
							<version>2.0</version><!--$NO-MVN-MAN-VER$-->
						</dependency>
						<dependency>
							<groupId>org.example</groupId>
							<artifactId>overriding</artifactId>
							<classifier>tests</classifier>
							<version>2.0</version>
						</dependency>
						<dependency>
							<groupId>org.example</groupId>
							<artifactId>unmanaged</artifactId>
							<version>2.0</version>
						</dependency>
						<dependency>
							<groupId>org.example</groupId>
							<artifactId>noversion</artifactId>
						</dependency>
					</dependencies>
				</project>
				""";
		MavenProject project = project(pom);
		DependencyManagement dm = new DependencyManagement();
		Dependency overriding = managedDependency("overriding");
		dm.addDependency(overriding);
		Dependency duplicating = managedDependency("duplicating");
		dm.addDependency(duplicating);
		dm.addDependency(managedDependency("ignored"));
		dm.addDependency(managedDependency("noversion"));
		project.getModel().setDependencyManagement(dm);

		List<PomHints.Hint> hints = checkVarious(pom, project);
		assertEquals(2, hints.size());

		PomHints.Hint hint = hints.get(0);
		assertEquals(IMavenConstants.EDITOR_HINT_MANAGED_DEPENDENCY_OVERRIDE, hint.editorHint());
		assertEquals("Overriding managed version 1.0 for overriding", hint.message());
		assertRegion(pom, "<version >2.0</version>", hint);
		assertEquals(9, hint.line());
		assertEquals("org.example", hint.groupId());
		assertEquals("overriding", hint.artifactId());
		assertNull(hint.profile());
		assertSame(overriding, hint.managed());

		hint = hints.get(1);
		assertEquals("Duplicating managed version 1.0 for duplicating", hint.message());
		assertRegion(pom, "<version>1.0</version>", hint);
		assertSame(duplicating, hint.managed());

		assertTrue(PomHints.checkVarious(document(pom), read(pom), project, ProblemSeverity.warning,
				ProblemSeverity.warning, ProblemSeverity.ignore).isEmpty());
	}

	@Test
	public void testIgnoredMultilineManagedDependencyVersion() {
		String pom = """
				<project>
					<groupId>org.example</groupId>
					<artifactId>child</artifactId>
					<version>1.0.0</version>
					<dependencies>
						<dependency>
							<groupId>org.example</groupId>
							<artifactId>ignored</artifactId>
							<version>
								2.0
							</version><!--$NO-MVN-MAN-VER$-->
						</dependency>
						<dependency>
							<groupId>org.example</groupId>
							<artifactId>overriding</artifactId>
							<version>
								2.0
							</version>
						</dependency>
					</dependencies>
				</project>
				""";
		MavenProject project = project(pom);
		DependencyManagement dm = new DependencyManagement();
		dm.addDependency(managedDependency("ignored"));
		dm.addDependency(managedDependency("overriding"));
		project.getModel().setDependencyManagement(dm);

		List<PomHints.Hint> hints = checkVarious(pom, project);
		assertEquals(1, hints.size());
		assertEquals("overriding", hints.get(0).artifactId());
		assertRegion(pom, "<version>\n\t\t\t\t2.0\n\t\t\t</version>", pom.indexOf("overriding"), hints.get(0));
	}

	@Test
	public void testIgnoreMarkerInCdataOrProcessingInstruction() {
		String pom = """
				<project>
					<groupId>org.example</groupId>
					<artifactId>child</artifactId>
					<version>1.0.0</version>
					<build>
						<plugins>
							<plugin>
								<groupId>org.example</groupId>
								<artifactId>cdata</artifactId>
								<version>2.0</version><configuration><text><![CDATA[<!--$NO-MVN-MAN-VER$-->]]></text></configuration>
							</plugin>
							<plugin>
								<groupId>org.example</groupId>
								<artifactId>pi</artifactId>
								<version>2.0</version><?pi <!--$NO-MVN-MAN-VER$-->?>
							</plugin>
							<plugin>
								<groupId>org.example</groupId>
								<artifactId>ignored</artifactId>
								<version>2.0</version><?pi ?><![CDATA[]]><!--$NO-MVN-MAN-VER$-->
							</plugin>
						</plugins>
					</build>
				</project>
				""";
		MavenProject project = project(pom);
		PluginManagement pm = new PluginManagement();
		pm.addPlugin(managedPlugin("org.example", "cdata", "pom"));
		pm.addPlugin(managedPlugin("org.example", "pi", "pom"));
		pm.addPlugin(managedPlugin("org.example", "ignored", "pom"));
		project.getModel().setBuild(new org.apache.maven.model.Build());
		project.getModel().getBuild().setPluginManagement(pm);

		List<PomHints.Hint> hints = checkVarious(pom, project);
		assertEquals(List.of("cdata", "pi"), hints.stream().map(PomHints.Hint::artifactId).toList());
	}

	@Test
	public void testEndTagInComment() {
		String pom = """
				<project>
					<parent>
						<groupId>org.example</groupId>
						<artifactId>parent</artifactId>
						<version>1.0.0</version>
					</parent>
					<artifactId>child</artifactId>
					<version><!-- </version> --><?pi </version>?>1.0.0</version>
				</project>
				""";
		List<PomHints.Hint> hints = checkVarious(pom, null);
		assertEquals(1, hints.size());
		assertEquals(IMavenConstants.EDITOR_HINT_PARENT_VERSION, hints.get(0).editorHint());
		assertRegion(pom, "<version><!-- </version> --><?pi </version>?>1.0.0</version>", hints.get(0));
	}

	@Test
	public void testOverridingManagedPluginVersionInActiveProfile() {
		String pom = """
				<project>
					<groupId>org.example</groupId>
					<artifactId>child</artifactId>
					<version>1.0.0</version>
					<build>
						<plugins>
							<plugin>
								<artifactId>maven-compiler-plugin</artifactId>
								<version>3.0</version>
							</plugin>
						</plugins>
					</build>
					<profiles>
						<profile>
							<id>active</id>
							<build>
								<plugins>
									<plugin>
										<groupId>org.example</groupId>
										<artifactId>example-plugin</artifactId>
										<version>3.0</version>
									</plugin>
								</plugins>
							</build>
						</profile>
						<profile>
							<id>inactive</id>
							<build>
								<plugins>
									<plugin>
										<groupId>org.example</groupId>
										<artifactId>example-plugin</artifactId>
										<version>4.0</version>
									</plugin>
								</plugins>
							</build>
						</profile>
					</profiles>
				</project>
				""";
		MavenProject project = project(pom);
		project.setInjectedProfileIds("org.example:child:1.0.0", List.of("active"));
		PluginManagement pm = new PluginManagement();
		Plugin compiler = managedPlugin("org.apache.maven.plugins", "maven-compiler-plugin", "pom");
		pm.addPlugin(compiler);
		Plugin example = managedPlugin("org.example", "example-plugin", "pom");
		pm.addPlugin(example);
		project.getModel().setBuild(new org.apache.maven.model.Build());
		project.getModel().getBuild().setPluginManagement(pm);

		List<PomHints.Hint> hints = checkVarious(pom, project);
		assertEquals(2, hints.size());

		PomHints.Hint hint = hints.get(0);
		assertEquals(IMavenConstants.EDITOR_HINT_MANAGED_PLUGIN_OVERRIDE, hint.editorHint());
		assertEquals("Overriding managed version 1.0 for maven-compiler-plugin", hint.message());
		assertEquals("org.apache.maven.plugins", hint.groupId());
		assertNull(hint.profile());
		assertSame(compiler, hint.managed());
		assertRegion(pom, "<version>3.0</version>", hint);

		hint = hints.get(1);
		assertEquals("org.example", hint.groupId());
		assertEquals("example-plugin", hint.artifactId());
		assertEquals("active", hint.profile());
		assertSame(example, hint.managed());
		assertRegion(pom, "<version>3.0</version>", pom.indexOf("<profiles>"), hint);
	}

	@Test
	public void testManagedPluginFromSuperPomIsIgnored() {
		String pom = """
				<project>
					<groupId>org.example</groupId>
					<artifactId>child</artifactId>
					<version>1.0.0</version>
					<build>
						<plugins>
							<plugin>
								<artifactId>maven-clean-plugin</artifactId>
								<version>3.0</version>
							</plugin>
						</plugins>
					</build>
				</project>
				""";
		MavenProject project = project(pom);
		PluginManagement pm = new PluginManagement();
		pm.addPlugin(managedPlugin("org.apache.maven.plugins", "maven-clean-plugin",
				"org.apache.maven:maven-model-builder:3.9.16:super-pom"));
		project.getModel().setBuild(new org.apache.maven.model.Build());
		project.getModel().getBuild().setPluginManagement(pm);
		assertTrue(checkVarious(pom, project).isEmpty());
	}

	@Test
	public void testWindowsLineEndingsAndTabs() {
		String pom = "<project>\r\n\t<parent>\r\n\t\t<groupId>g</groupId>\r\n\t\t<artifactId>p</artifactId>\r\n"
				+ "\t\t<version>1</version>\r\n\t</parent>\r\n\t<groupId>g</groupId>\r\n</project>\r\n";
		List<PomHints.Hint> hints = checkVarious(pom, null);
		assertEquals(1, hints.size());
		assertRegion(pom, "<groupId>g</groupId>", pom.indexOf("</parent>"), hints.get(0));
		assertEquals(7, hints.get(0).line());
	}

	@Test
	public void testUnreadablePom() {
		assertNull(read("<project><groupId>g</groupId>"));
		assertNotNull(read("<project/>"));
	}

	private static void assertRegion(String pom, String element, PomHints.Hint hint) {
		assertRegion(pom, element, 0, hint);
	}

	private static void assertRegion(String pom, String element, int from, PomHints.Hint hint) {
		int start = pom.indexOf(element, from);
		assertTrue(element, start >= 0);
		assertEquals(element, pom.substring(hint.start(), hint.end()));
		assertEquals(start, hint.start());
	}

	private static List<PomHints.Hint> checkForSchema(String pom) {
		return PomHints.checkForSchema(document(pom), read(pom));
	}

	private static List<PomHints.Hint> checkVarious(String pom, MavenProject project) {
		return PomHints.checkVarious(document(pom), read(pom), project, ProblemSeverity.warning,
				ProblemSeverity.warning, ProblemSeverity.warning);
	}

	private static Model read(String pom) {
		return PomHints.read(document(pom));
	}

	private static IDocument document(String pom) {
		return new Document(pom);
	}

	private static MavenProject project(String pom) {
		Model model = read(pom);
		assertNotNull(model);
		return new MavenProject(model.clone());
	}

	private static Dependency managedDependency(String artifactId) {
		Dependency dependency = new Dependency();
		dependency.setGroupId("org.example");
		dependency.setArtifactId(artifactId);
		dependency.setVersion("1.0");
		return dependency;
	}

	private static Plugin managedPlugin(String groupId, String artifactId, String modelId) {
		Plugin plugin = new Plugin();
		plugin.setGroupId(groupId);
		plugin.setArtifactId(artifactId);
		plugin.setVersion("1.0");
		InputSource source = new InputSource();
		source.setModelId(modelId);
		plugin.setLocation("version", new InputLocation(1, 1, source));
		return plugin;
	}
}
