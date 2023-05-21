/*******************************************************************************
 * Copyright (c) 2023, 2023 Hannes Wellmann and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target.tests;

import static org.eclipse.osgi.util.ManifestElement.parseHeader;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.Attributes;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.TargetBundle;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class OSGiMetadataGenerationTest extends AbstractMavenTargetTest {

	@Test
	public void testNonOSGiArtifact_missingArtifactError() throws CoreException {
		ITargetDefinition target = resolveMavenTarget("""
				<location includeDependencyDepth="none" includeSource="true" missingManifest="error" type="Maven">
					<dependencies>
						<dependency>
							<groupId>com.google.errorprone</groupId>
							<artifactId>error_prone_annotations</artifactId>
							<version>2.18.0</version>
							<type>jar</type>
						</dependency>
					</dependencies>
				</location>
				""");
		IStatus targetStatus = target.getStatus();
		assertEquals(IStatus.ERROR, targetStatus.getSeverity());

		assertEquals(1, targetStatus.getChildren().length);
		String notABundleErrorMessage = "com.google.errorprone:error_prone_annotations:jar:2.18.0 is not a bundle";
		assertEquals(notABundleErrorMessage, targetStatus.getChildren()[0].getMessage());

		assertArrayEquals(EMPTY, target.getAllFeatures());
		TargetBundle[] allBundles = target.getAllBundles();
		assertEquals(1, allBundles.length);
		IStatus status = allBundles[0].getStatus();
		assertEquals(IStatus.ERROR, status.getSeverity());
		assertEquals(notABundleErrorMessage, status.getMessage());
	}

	@Test
	public void testNonOSGiArtifact_missingArtifactIgnore() throws CoreException {
		ITargetDefinition target = resolveMavenTarget("""
				<location includeDependencyDepth="none" includeSource="true" missingManifest="ignore" type="Maven">
					<dependencies>
						<dependency>
							<groupId>com.google.errorprone</groupId>
							<artifactId>error_prone_annotations</artifactId>
							<version>2.18.0</version>
							<type>jar</type>
						</dependency>
					</dependencies>
				</location>
				""");
		assertTrue(target.getStatus().isOK());
		assertArrayEquals(EMPTY, target.getAllFeatures());
		assertArrayEquals(EMPTY, target.getAllBundles());
	}

	@Test
	public void testNonOSGiArtifact_missingArtifactGenerate_defaultInstructions() throws Exception {
		ITargetDefinition target = resolveMavenTarget("""
				<location includeDependencyDepth="none" includeSource="true" missingManifest="generate" type="Maven">
					<dependencies>
						<dependency>
							<groupId>com.google.errorprone</groupId>
							<artifactId>error_prone_annotations</artifactId>
							<version>2.18.0</version>
							<type>jar</type>
						</dependency>
					</dependencies>
				</location>
				""");
		assertTrue(target.getStatus().isOK());
		assertArrayEquals(EMPTY, target.getAllFeatures());
		ExpectedBundle expectedBundle = generatedBundle("wrapped.com.google.errorprone.error_prone_annotations",
				"2.18.0", "com.google.errorprone:error_prone_annotations");
		assertTargetBundles(target, withSourceBundles(List.of(expectedBundle)));

		// Validate generated metadata
		Attributes attributes = getManifestMainAttributes(getGeneratedBundle(target));
		assertEquals("wrapped.com.google.errorprone.error_prone_annotations",
				attributes.getValue(Constants.BUNDLE_SYMBOLICNAME));
		assertEquals("Bundle derived from maven artifact com.google.errorprone:error_prone_annotations:2.18.0",
				attributes.getValue(Constants.BUNDLE_NAME));
		assertEqualManifestHeaders(Constants.IMPORT_PACKAGE, attributes,
				"javax.lang.model.element;resolution:=optional");
		assertEqualManifestHeaders(Constants.EXPORT_PACKAGE, attributes,
				"com.google.errorprone.annotations;version=\"2.18.0\";uses:=\"javax.lang.model.element\"",
				"com.google.errorprone.annotations.concurrent;version=\"2.18.0\"");
		assertNull(attributes.getValue(Constants.REQUIRE_BUNDLE));
		assertEquals("*", attributes.getValue(Constants.DYNAMICIMPORT_PACKAGE));

		Attributes sourceAttributes = getManifestMainAttributes(getGeneratedSourceBundle(target));
		assertEquals("wrapped.com.google.errorprone.error_prone_annotations.source",
				sourceAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME));
		assertEquals("Source Bundle for wrapped.com.google.errorprone.error_prone_annotations:2.18.0",
				sourceAttributes.getValue(Constants.BUNDLE_NAME));
		assertEqualManifestHeaders("Eclipse-SourceBundle", sourceAttributes,
				"wrapped.com.google.errorprone.error_prone_annotations;version=\"2.18.0\";roots:=\".\"");
		assertNull(sourceAttributes.getValue(Constants.IMPORT_PACKAGE));
		assertNull(sourceAttributes.getValue(Constants.EXPORT_PACKAGE));
		assertNull(sourceAttributes.getValue(Constants.REQUIRE_BUNDLE));
		assertNull(sourceAttributes.getValue(Constants.DYNAMICIMPORT_PACKAGE));
	}

	@Test
	public void testNonOSGiArtifact_missingArtifactGenerate_customInstructions() throws Exception {
		ITargetDefinition target = resolveMavenTarget(
				"""
						<location includeDependencyDepth="none" includeDependencyScopes="" includeSource="true" missingManifest="generate" type="Maven">
							<dependencies>
								<dependency>
									<groupId>com.google.errorprone</groupId>
									<artifactId>error_prone_annotations</artifactId>
									<version>2.18.0</version>
									<type>jar</type>
								</dependency>
							</dependencies>
							<instructions><![CDATA[
								Bundle-Name:           Bundle in Test from artifact ${mvnGroupId}:${mvnArtifactId}:${mvnVersion}:${mvnClassifier}
								version:               ${version_cleanup;${mvnVersion}}
								Bundle-SymbolicName:   m2e.custom.test.wrapped.${mvnArtifactId}
								Bundle-Version:        ${version}
								Import-Package:        *
								Export-Package:        *;version="${version}";-noimport:=true
							]]></instructions>
						</location>
						""");
		assertTrue(target.getStatus().isOK());
		assertArrayEquals(EMPTY, target.getAllFeatures());
		ExpectedBundle expectedBundle = generatedBundle("m2e.custom.test.wrapped.error_prone_annotations", "2.18.0",
				"com.google.errorprone:error_prone_annotations");
		assertTargetBundles(target, withSourceBundles(List.of(expectedBundle)));

		// Validate generated metadata
		Attributes attributes = getManifestMainAttributes(getGeneratedBundle(target));
		assertEquals("m2e.custom.test.wrapped.error_prone_annotations",
				attributes.getValue(Constants.BUNDLE_SYMBOLICNAME));
		assertEquals("Bundle in Test from artifact com.google.errorprone:error_prone_annotations:2.18.0:",
				attributes.getValue(Constants.BUNDLE_NAME));
		assertEqualManifestHeaders(Constants.IMPORT_PACKAGE, attributes, "javax.lang.model.element");
		assertEqualManifestHeaders(Constants.EXPORT_PACKAGE, attributes,
				"com.google.errorprone.annotations;version=\"2.18.0\";uses:=\"javax.lang.model.element\"",
				"com.google.errorprone.annotations.concurrent;version=\"2.18.0\"");
		assertNull(attributes.getValue(Constants.REQUIRE_BUNDLE));
		assertNull(attributes.getValue(Constants.DYNAMICIMPORT_PACKAGE));

		Attributes sourceAttributes = getManifestMainAttributes(getGeneratedSourceBundle(target));
		assertEquals("m2e.custom.test.wrapped.error_prone_annotations.source",
				sourceAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME));
		assertEquals("Source Bundle for m2e.custom.test.wrapped.error_prone_annotations:2.18.0",
				sourceAttributes.getValue(Constants.BUNDLE_NAME));
		assertEqualManifestHeaders("Eclipse-SourceBundle", sourceAttributes,
				"m2e.custom.test.wrapped.error_prone_annotations;version=\"2.18.0\";roots:=\".\"");
		assertNull(sourceAttributes.getValue(Constants.IMPORT_PACKAGE));
		assertNull(sourceAttributes.getValue(Constants.EXPORT_PACKAGE));
		assertNull(sourceAttributes.getValue(Constants.REQUIRE_BUNDLE));
		assertNull(sourceAttributes.getValue(Constants.DYNAMICIMPORT_PACKAGE));

	}

	private static TargetBundle getGeneratedBundle(ITargetDefinition target) {
		return Arrays.stream(target.getBundles()).filter(b -> !b.isSourceBundle()).findFirst().orElseThrow();
	}

	private static TargetBundle getGeneratedSourceBundle(ITargetDefinition target) {
		return Arrays.stream(target.getBundles()).filter(TargetBundle::isSourceBundle).findFirst().orElseThrow();
	}

	private static void assertEqualManifestHeaders(String header, Attributes mainManifestAttributes,
			String... expectedHeaderValues) throws BundleException {
		ManifestElement[] expected = parseHeader(header, String.join(",", expectedHeaderValues));
		ManifestElement[] actual = parseHeader(Constants.EXPORT_PACKAGE, mainManifestAttributes.getValue(header));
		Function<ManifestElement[], String[]> toString = a -> Arrays.stream(a).map(ManifestElement::toString)
				.toArray(String[]::new);
		assertEquals(Set.of(toString.apply(expected)), Set.of(toString.apply(actual))); // order is irrelevant
	}

}