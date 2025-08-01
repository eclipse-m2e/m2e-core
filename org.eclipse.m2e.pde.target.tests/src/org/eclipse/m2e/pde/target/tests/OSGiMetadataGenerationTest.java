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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.Attributes;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.TargetBundle;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import aQute.bnd.osgi.Jar;

public class OSGiMetadataGenerationTest extends AbstractMavenTargetTest {

	@Test
	public void testWithEmptyMetaInfServices() throws Exception {
		ITargetLocation target = resolveMavenTarget(
				"""
						<location includeDependencyDepth="none" includeDependencyScopes="compile" includeSource="true" missingManifest="generate" type="Maven">
						    <dependencies>
						        <dependency>
						            <groupId>org.apache.qpid</groupId>
						            <artifactId>qpid-jms-discovery</artifactId>
						            <version>2.7.0</version>
						            <type>jar</type>
						        </dependency>
						    </dependencies>
						</location>
						""");
		assertStatusOk(getTargetStatus(target));
	}

	@Test
	public void testNonJarArtifactInDependencies() throws Exception {
		ITargetLocation target = resolveMavenTarget(
				"""
						    <location includeDependencyDepth="infinite" includeDependencyScopes="compile,provided,runtime" includeSource="true" label="Azure OpenAI" missingManifest="generate" type="Maven">
						        <dependencies>
						            <dependency>
						                <groupId>com.azure</groupId>
						                <artifactId>azure-ai-openai</artifactId>
						                <version>1.0.0-beta.13</version>
						                <type>jar</type>
						            </dependency>
						        </dependencies>
						    </location>
						""");
		assertStatusOk(getTargetStatus(target));
	}

	@Test
	public void testVersionRanges() throws Exception {
		ITargetLocation target = resolveMavenTarget(
				"""
						<location includeDependencyDepth="infinite" includeDependencyScopes="compile,provided,runtime" includeSource="true" label="cucumber" missingManifest="generate" type="Maven">
						    <dependencies>
						        <dependency>
						            <groupId>io.cucumber</groupId>
						            <artifactId>cucumber-java</artifactId>
						            <version>7.21.1</version>
						            <type>jar</type>
						        </dependency>
						    </dependencies>
						</location>
						        """);
		assertStatusOk(getTargetStatus(target));
	}

	@Test
	public void testBadDependencyInChain() throws Exception {
		ITargetLocation target = resolveMavenTarget("""
				<location includeDependencyScope="compile" missingManifest="generate" type="Maven">
					<dependencies>
						<dependency>
							<groupId>edu.ucar</groupId>
							<artifactId>cdm</artifactId>
							<version>4.5.5</version>
							<type>jar</type>
						</dependency>
					</dependencies>
				</location>
				""");
		assertStatusOk(getTargetStatus(target));
	}

	@Test
	public void testBadSymbolicName() throws Exception {
		ITargetLocation target = resolveMavenTarget("""
				<location includeDependencyScope="compile" missingManifest="generate" type="Maven">
				    <dependencies>
				        <dependency>
				            <groupId>javax.xml.ws</groupId>
				            <artifactId>jaxws-api</artifactId>
				            <version>2.3.1</version>
				        </dependency>
				    </dependencies>
				</location>
				""");
		assertStatusOk(getTargetStatus(target));
	}

	@Test
	public void testWithClassifierFailsToCollect() throws Exception {
		String targetXML = """
				<location includeDependencyDepth="%depth%" includeDependencyScopes="compile,provided,runtime,system" includeSource="false" missingManifest="generate" type="Maven">
					<dependencies>
						<dependency>
							<groupId>org.ehcache</groupId>
							<artifactId>ehcache</artifactId>
							<version>3.10.0</version>
							<type>jar</type>
							<classifier>jakarta</classifier>
						</dependency>
					</dependencies>
				</location>
				""";
		for (String deepth : new String[] { "none", "direct", "infinite" }) {
			ITargetLocation target = resolveMavenTarget(targetXML.replace("%depth%", deepth));
			assertStatusOk(getTargetStatus(target));
			TargetBundle[] bundles = target.getBundles();
			for (TargetBundle targetBundle : bundles) {
				URI location = targetBundle.getBundleInfo().getLocation();
				assertTrue(
						"bundle with classifier was not correctly fetched width deepth = " + deepth + " location = "
								+ location,
						location.toString().endsWith("org/ehcache/ehcache/3.10.0/ehcache-3.10.0-jakarta.jar"));
			}
		}
	}

	@Test
	@Ignore("see https://gitlab.eclipse.org/eclipsefdn/helpdesk/-/issues/5987")
	public void testSourceWithSignature() throws Exception {
		ITargetLocation target = resolveMavenTarget(
				"""
						<location includeDependencyDepth="none" includeDependencyScopes="compile" label="LemMinX" includeSource="true" missingManifest="error" type="Maven">
						    <dependencies>
						        <dependency>
						            <groupId>org.eclipse.lemminx</groupId>
						            <artifactId>org.eclipse.lemminx</artifactId>
						            <version>0.29.0</version>
						            <type>jar</type>
						        </dependency>
						    </dependencies>
						    <repositories>
						        <repository>
						            <id>lemminx-releases</id>
						            <url>https://repo.eclipse.org/content/repositories/lemminx-releases/</url>
						        </repository>
						    </repositories>
						</location>
						""");
		assertStatusOk(getTargetStatus(target));
		TargetBundle[] allBundles = target.getBundles();
		boolean sourcesFound = false;
		for (TargetBundle targetBundle : allBundles) {
			if (targetBundle.isSourceBundle()) {
				sourcesFound = true;
				assertValidSignature(targetBundle);
			}
		}
		assertTrue("No source bundle generated!", sourcesFound);
	}

	@Test
	public void testArtifactWithSignature() throws Exception {
		ITargetLocation target = resolveMavenTarget(
				"""
						<location includeDependencyDepth="none" includeDependencyScopes="compile" label="verapdf" includeSource="true" missingManifest="generate" type="Maven">
						    <dependencies>
						        <dependency>
						            <groupId>org.verapdf</groupId>
						            <artifactId>core</artifactId>
						            <version>1.26.5</version>
						        </dependency>
						    </dependencies>
						</location>
						""");
		assertStatusOk(getTargetStatus(target));
		TargetBundle[] allBundles = target.getBundles();
		for (TargetBundle targetBundle : allBundles) {
			assertValidSignature(targetBundle);
		}
		assertTrue("No bundle generated!", allBundles.length > 0);
	}

	@Test
	public void testBadDependencyDirect() throws Exception {
		ITargetLocation target = resolveMavenTarget("""
				<location missingManifest="generate" type="Maven">
					<dependencies>
						<dependency>
							  <groupId>com.ibm.icu</groupId>
							  <artifactId>icu4j</artifactId>
							  <version>2.6.1</version>
							<type>jar</type>
						</dependency>
					</dependencies>
				</location>
				""");
		IStatus targetStatus = getTargetStatus(target);
		assertEquals(String.valueOf(targetStatus), IStatus.ERROR, targetStatus.getSeverity());
	}

	@Test
	public void testMissingOptionalDependency() throws Exception {
		ITargetLocation target = resolveMavenTarget("""
				<location includeDependencyDepth="none" includeDependencyScopes="compile" missingManifest="generate" type="Maven">
					<dependencies>
						<dependency>
							<groupId>net.sf.saxon</groupId>
							<artifactId>Saxon-HE</artifactId>
							<version>10.9</version>
							<type>jar</type>
						</dependency>
					</dependencies>
				</location>
				""");
		assertStatusOk(getTargetStatus(target));
	}

	@Test
	public void testNonOSGiArtifact_missingArtifactError() throws Exception {
		ITargetLocation target = resolveMavenTarget("""
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
		IStatus targetStatus = getTargetStatus(target);
		assertEquals(String.valueOf(targetStatus), IStatus.ERROR, targetStatus.getSeverity());

		assertEquals(1, targetStatus.getChildren().length);
		String notABundleErrorMessage = "com.google.errorprone:error_prone_annotations:jar:2.18.0 is not a bundle";
		assertEquals(notABundleErrorMessage, targetStatus.getChildren()[0].getMessage());

		assertArrayEquals(EMPTY, target.getFeatures());
		TargetBundle[] allBundles = target.getBundles();
		assertEquals(1, allBundles.length);
		IStatus status = allBundles[0].getStatus();
		assertEquals(IStatus.ERROR, status.getSeverity());
		assertEquals(notABundleErrorMessage, status.getMessage());
	}

	@Test
	public void testNonOSGiArtifact_missingArtifactIgnore() throws Exception {
		ITargetLocation target = resolveMavenTarget("""
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
		assertStatusOk(getTargetStatus(target));
		assertArrayEquals(EMPTY, target.getFeatures());
		assertArrayEquals(EMPTY, target.getBundles());
	}

	@Test
	public void testNonOSGiArtifact_missingArtifactGenerate_defaultInstructions() throws Exception {
		ITargetLocation target = resolveMavenTarget("""
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
		assertStatusOk(getTargetStatus(target));
		assertArrayEquals(EMPTY, target.getFeatures());
		ExpectedBundle expectedBundle = generatedBundle("wrapped.com.google.errorprone.error_prone_annotations",
				"2.18.0", "com.google.errorprone:error_prone_annotations");
		assertTargetBundles(target, withSourceBundles(List.of(expectedBundle)));

		// Validate generated metadata
		Attributes attributes = getManifestMainAttributes(getGeneratedBundle(target));
		assertEquals("wrapped.com.google.errorprone.error_prone_annotations",
				attributes.getValue(Constants.BUNDLE_SYMBOLICNAME));
		assertEquals("Bundle derived from maven artifact com.google.errorprone:error_prone_annotations:2.18.0",
				attributes.getValue(Constants.BUNDLE_NAME));
		assertEqualManifestHeaders(Constants.IMPORT_PACKAGE, attributes, "java.lang;resolution:=optional",
				"java.lang.annotation;resolution:=optional", "javax.lang.model.element;resolution:=optional");
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
	public void testConditionalPackage() throws Exception {
		ITargetLocation target = resolveMavenTarget(
				"""
						<location includeDependencyDepth="none" includeDependencyScopes="compile" includeSource="false" label="Maven-archetype" missingManifest="generate" type="Maven">
						<dependencies>
							<dependency>
								<groupId>org.apache.maven.archetype</groupId>
								<artifactId>archetype-common</artifactId>
								<version>3.3.1</version>
								<type>jar</type>
							</dependency>
							</dependencies>
							<instructions><![CDATA[
								Bundle-Name:           Bundle in Test from artifact ${mvnGroupId}:${mvnArtifactId}:${mvnVersion}:${mvnClassifier}
								version:               ${version_cleanup;${mvnVersion}}
								Bundle-SymbolicName:   m2e.custom.test.wrapped.${mvnArtifactId}
								Bundle-Version:        ${version}
								Import-Package:        *
								Export-Package:        org.apache.maven.archetype;version="${version}";-noimport:=true
								-conditionalpackage:   org.apache.commons.*
							]]></instructions>
						</location>
						""");
		assertStatusOk(getTargetStatus(target));
		TargetBundle[] bundles = target.getBundles();
		for (TargetBundle bundle : bundles) {
			BundleInfo bundleInfo = bundle.getBundleInfo();
			File file = new File(bundleInfo.getLocation());
			try (Jar jar = new Jar(file)) {
				Set<String> resources = jar.getResources().keySet();
				assertTrue("Conditional package org.apache.commons.* not included.",
						resources.stream().anyMatch(s -> s.startsWith("org/apache/commons/")));
			}
			;
		}
	}

	@Test
	public void testNonOSGiArtifact_missingArtifactGenerate_customInstructions() throws Exception {
		ITargetLocation target = resolveMavenTarget(
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
		assertStatusOk(getTargetStatus(target));
		assertArrayEquals(EMPTY, target.getFeatures());
		ExpectedBundle expectedBundle = generatedBundle("m2e.custom.test.wrapped.error_prone_annotations", "2.18.0",
				"com.google.errorprone:error_prone_annotations");
		assertTargetBundles(target, withSourceBundles(List.of(expectedBundle)));

		// Validate generated metadata
		Attributes attributes = getManifestMainAttributes(getGeneratedBundle(target));
		assertEquals("m2e.custom.test.wrapped.error_prone_annotations",
				attributes.getValue(Constants.BUNDLE_SYMBOLICNAME));
		assertEquals("Bundle in Test from artifact com.google.errorprone:error_prone_annotations:2.18.0:",
				attributes.getValue(Constants.BUNDLE_NAME));
		assertEqualManifestHeaders(Constants.IMPORT_PACKAGE, attributes, "java.lang", "java.lang.annotation",
				"javax.lang.model.element");
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

	@Test
	public void testNonOSGiArtifact_missingArtifactGenerate_changedCustomInstructions() throws Exception {
		String targetXML = """
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
						Bundle-SymbolicName:   %s
						Bundle-Version:        ${version}
						Import-Package:        *
						Export-Package:        *;version="${version}";-noimport:=true
					]]></instructions>
				</location>
				""";
		ITargetLocation target = resolveMavenTarget(targetXML.formatted("m2e.wrapped.${mvnArtifactId}"));
		assertStatusOk(getTargetStatus(target));
		assertArrayEquals(EMPTY, target.getFeatures());
		assertEquals(2, target.getBundles().length);
		assertEquals("m2e.wrapped.error_prone_annotations",
				getGeneratedBundle(target).getBundleInfo().getSymbolicName());
		assertEquals("m2e.wrapped.error_prone_annotations.source",
				getGeneratedSourceBundle(target).getBundleInfo().getSymbolicName());

		target = resolveMavenTarget(targetXML.formatted("others.wrapped.${mvnArtifactId}"));
		assertStatusOk(getTargetStatus(target));
		assertArrayEquals(EMPTY, target.getFeatures());
		assertEquals(2, target.getBundles().length);
		assertEquals("others.wrapped.error_prone_annotations",
				getGeneratedBundle(target).getBundleInfo().getSymbolicName());
		assertEquals("others.wrapped.error_prone_annotations.source",
				getGeneratedSourceBundle(target).getBundleInfo().getSymbolicName());
	}

	@Test
	public void testNonOSGiArtifact_missingArtifactGenerate_hasVersions() throws Exception {
		ITargetLocation target = resolveMavenTarget(
				"""
				<location includeDependencyDepth="infinite" includeDependencyScopes="compile,provided,runtime" missingManifest="generate" type="Maven">
					<dependencies>
						<dependency>
							<groupId>org.apache.lucene</groupId>
							<artifactId>lucene-analysis-common</artifactId>
							<version>9.5.0</version>
							<type>jar</type>
						</dependency>
					</dependencies>
				</location>
				""");
		assertStatusOk(getTargetStatus(target));
		Optional<TargetBundle> luceneAnalysisCommon = Arrays.stream(target.getBundles()).filter(
				tb -> tb.getBundleInfo().getSymbolicName().equals("wrapped.org.apache.lucene.lucene-analysis-common"))
				.findFirst();
		assertTrue("lucene-analysis-common bundle not found in target state", luceneAnalysisCommon.isPresent());
		Attributes manifest = getManifestMainAttributes(luceneAnalysisCommon.get());
		ManifestElement[] importHeader = parseHeader(Constants.IMPORT_PACKAGE,
				manifest.getValue(Constants.IMPORT_PACKAGE));
		for (ManifestElement element : importHeader) {
			String value = element.getValue();
			if (value.startsWith("org.apache.lucene.")) {
				String attribute = element.getAttribute(Constants.VERSION_ATTRIBUTE);
				assertNotNull("Package " + value + " has no version attribute: " + element, attribute);
				VersionRange versionRange = VersionRange.valueOf(attribute);
				assertEquals("Unexpected version range " + versionRange + " on package " + value + ": " + element, 0,
						versionRange.getLeft().compareTo(Version.valueOf("9.5.0")));
			}
		}
	}

	private static TargetBundle getGeneratedBundle(ITargetLocation target) {
		return Arrays.stream(target.getBundles()).filter(b -> !b.isSourceBundle()).findFirst().orElseThrow();
	}

	private static TargetBundle getGeneratedSourceBundle(ITargetLocation target) {
		return Arrays.stream(target.getBundles()).filter(TargetBundle::isSourceBundle).findFirst().orElseThrow();
	}

	private static void assertEqualManifestHeaders(String header, Attributes mainManifestAttributes,
			String... expectedHeaderValues) throws BundleException {
		ManifestElement[] expected = parseHeader(header, String.join(",", expectedHeaderValues));
		ManifestElement[] actual = parseHeader(header, mainManifestAttributes.getValue(header));
		Function<ManifestElement[], String[]> toString = a -> Arrays.stream(a).map(ManifestElement::toString)
				.toArray(String[]::new);
		assertEquals(Set.of(toString.apply(expected)), Set.of(toString.apply(actual))); // order is irrelevant
	}

}