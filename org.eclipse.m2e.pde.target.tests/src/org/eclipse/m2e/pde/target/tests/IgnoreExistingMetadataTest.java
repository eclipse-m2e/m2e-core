/*******************************************************************************
 * Copyright (c) 2004-2026 Vector Informatik GmbH
 * All rights reserved.
 * www.vector.com
 *******************************************************************************/
package org.eclipse.m2e.pde.target.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.jar.Attributes;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.m2e.pde.target.MissingMetadataMode;
import org.eclipse.m2e.pde.target.shared.DependencyDepth;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.TargetBundle;
import org.junit.Test;
import org.osgi.framework.Constants;

public class IgnoreExistingMetadataTest extends AbstractMavenTargetTest {

	@Test
	public void testExistingBundle_MissingManifestGenerate_IgnoreExistingMetadataFalse_WithInstructions_MissingMetadataModeGenerate_ExpectOriginalManifest()
			throws Exception {
		ITargetLocation target = resolveMavenTarget(getTargetXMLForDependencyWithExistingManifest(DependencyDepth.NONE,
				false, false, MissingMetadataMode.GENERATE));
		assertOriginalManifest(target);
	}

	private String getTargetXMLForDependencyWithExistingManifest(DependencyDepth dependencyDepth, boolean includeSource,
			boolean ignoreExistingMetadata, MissingMetadataMode missingMetadataMode, String instructions) {
		return String.format(
				"""
						<location includeDependencyDepth="%s" includeDependencyScopes="compile" includeSource="%s" ignoreExistingMetadata="%s" missingManifest="%s" type="Maven">
						    <dependencies>
						        <dependency>
						            <groupId>org.slf4j</groupId>
						            <artifactId>slf4j-api</artifactId>
						            <version>2.0.7</version>
						            <type>jar</type>
						        </dependency>
						    </dependencies>
						    <instructions><![CDATA[
						        %s
						    ]]></instructions>
						</location>
						""",
				dependencyDepth, includeSource, ignoreExistingMetadata, missingMetadataMode, instructions);
	}

	private void assertOriginalManifest(ITargetLocation target) throws IOException {
		assertStatusOk(getTargetStatus(target));
		TargetBundle[] bundles = target.getBundles();
		assertEquals(1, bundles.length);

		Attributes attributes = getManifestMainAttributes(bundles[0]);

		assertEquals("slf4j.api", attributes.getValue(Constants.BUNDLE_SYMBOLICNAME));
		assertEquals("2.0.7", attributes.getValue(Constants.BUNDLE_VERSION));
	}

	@Test
	public void testExistingBundle_MissingManifestGenerate_IgnoreExistingMetadataFalse_WithInstructions_MissingMetadataModeError_ExpectOriginalManifest()
			throws Exception {
		ITargetLocation target = resolveMavenTarget(getTargetXMLForDependencyWithExistingManifest(DependencyDepth.NONE,
				false, false, MissingMetadataMode.ERROR));
		assertOriginalManifest(target);
	}

	@Test
	public void testExistingBundle_MissingManifestGenerate_IgnoreExistingMetadataFalse_WithInstructions_MissingMetadataModeIgnore_ExpectOriginalManifest()
			throws Exception {
		ITargetLocation target = resolveMavenTarget(getTargetXMLForDependencyWithExistingManifest(DependencyDepth.NONE,
				false, false, MissingMetadataMode.IGNORE));
		assertOriginalManifest(target);
	}

	private String getTargetXMLForDependencyWithExistingManifest(DependencyDepth dependencyDepth, boolean includeSource,
			boolean ignoreExistingMetadata, MissingMetadataMode missingMetadataMode) {
		return getTargetXMLForDependencyWithExistingManifest(dependencyDepth, includeSource, ignoreExistingMetadata,
				missingMetadataMode, getInstructionsForCustomBSN("custom.slf4j.api"));
	}

	private String getInstructionsForCustomBSN(String customBSN) {
		return String.format("""
				Bundle-SymbolicName: %s
				""", customBSN);
	}

	@Test
	public void testExistingBundle_MissingManifestGenerate_IgnoreExistingMetadataTrue_WithInstructions_MissingMetadataModeGenerate_ExpectOverriddenManifest()
			throws Exception {
		ITargetLocation target = resolveMavenTarget(getTargetXMLForDependencyWithExistingManifest(DependencyDepth.NONE,
				false, true, MissingMetadataMode.GENERATE));
		assertOverriddenManifest(target);
	}

	private void assertOverriddenManifest(ITargetLocation target) throws IOException {
		assertStatusOk(getTargetStatus(target));
		TargetBundle[] bundles = target.getBundles();
		assertEquals(1, bundles.length);

		Attributes attributes = getManifestMainAttributes(bundles[0]);

		assertEquals("custom.slf4j.api", attributes.getValue(Constants.BUNDLE_SYMBOLICNAME));
		assertEquals("2.0.7", attributes.getValue(Constants.BUNDLE_VERSION));
	}

	@Test
	public void testExistingBundle_MissingManifestGenerate_IgnoreExistingMetadataTrue_WithInstructions_MissingMetadataModeError_ExpectOverriddenManifest()
			throws Exception {
		ITargetLocation target = resolveMavenTarget(getTargetXMLForDependencyWithExistingManifest(DependencyDepth.NONE,
				false, true, MissingMetadataMode.GENERATE));
		assertOverriddenManifest(target);
	}

	@Test
	public void testExistingBundle_MissingManifestGenerate_IgnoreExistingMetadataTrue_WithInstructions_MissingMetadataModeIgnore_ExpectOverriddenManifest()
			throws Exception {
		ITargetLocation target = resolveMavenTarget(getTargetXMLForDependencyWithExistingManifest(DependencyDepth.NONE,
				false, true, MissingMetadataMode.GENERATE));
		assertOverriddenManifest(target);
	}

	@Test
	public void testExistingBundle_MissingManifestGenerate_IgnoreExistingMetadataTrue_DependencyDepthDirect_ExpectError()
			throws Exception {
		ITargetLocation target = resolveMavenTarget(getTargetXMLForDependencyWithExistingManifest(
				DependencyDepth.DIRECT, false, true, MissingMetadataMode.GENERATE));
		IStatus status = getTargetStatus(target);
		assertErrorStatusAndMessage(status, "The dependency depth must be none!");
	}

	@Test
	public void testExistingBundle_MissingManifestGenerate_IgnoreExistingMetadataTrue_DependencyDepthInfinite_ExpectError()
			throws Exception {
		ITargetLocation target = resolveMavenTarget(getTargetXMLForDependencyWithExistingManifest(
				DependencyDepth.INFINITE, false, true, MissingMetadataMode.GENERATE));
		IStatus status = getTargetStatus(target);
		assertErrorStatusAndMessage(status, "The dependency depth must be none!");
	}

	@Test
	public void testExistingBundle_MissingManifestGenerate_IgnoreExistingMetadataTrue_noBSNInInstructions_ExpectError()
			throws Exception {
		ITargetLocation target = resolveMavenTarget(getTargetXMLForDependencyWithExistingManifest(DependencyDepth.NONE,
				false, true, MissingMetadataMode.GENERATE, "Bundle-Version: 2.0.0"));
		IStatus status = getTargetStatus(target);
		assertErrorStatusAndMessage(status,
				"The symbolic name in the bnd instructions must be defined and differ from the original one.");
	}

	@Test
	public void testExistingBundle_MissingManifestGenerate_IgnoreExistingMetadataTrue_WithSameBSN_ExpectError()
			throws Exception {
		ITargetLocation target = resolveMavenTarget(getTargetXMLForDependencyWithExistingManifest(DependencyDepth.NONE,
				false, true, MissingMetadataMode.GENERATE, getInstructionsForCustomBSN("slf4j.api")));
		IStatus status = getTargetStatus(target);
		assertErrorStatusAndMessage(status,
				"The symbolic name in the bnd instructions must be defined and differ from the original one.");
	}

	private void assertErrorStatusAndMessage(IStatus status, String errorReason) {
		assertEquals(IStatus.ERROR, status.getSeverity());
		IStatus[] statusChildren = status.getChildren();
		assertEquals(1, statusChildren.length);
		String errorMessage = statusChildren[0].getMessage();
		assertTrue("Unexpected error message: " + errorMessage,
				errorMessage.equals("org.slf4j:slf4j-api:jar:2.0.7 cannot be overridden: " + errorReason));
	}

	@Test
	public void testNonBundle_IgnoreExistingMetadataTrue_ExpectError() throws Exception {
		ITargetLocation target = resolveMavenTarget(//
				"""
						<location includeDependencyDepth="none" includeDependencyScopes="compile" includeSource="false" ignoreExistingMetadata="true" missingManifest="error" type="Maven">
						    <dependencies>
						        <dependency>
						            <groupId>com.google.errorprone</groupId>
						            <artifactId>error_prone_annotations</artifactId>
						            <version>2.18.0</version>
						            <type>jar</type>
						        </dependency>
						    </dependencies>
						    <instructions><![CDATA[
						        Bundle-SymbolicName: custom.errorprone
						        Bundle-Version: 1.0.0
						    ]]></instructions>
						</location>
						""");
		IStatus status = getTargetStatus(target);
		assertEquals(IStatus.ERROR, status.getSeverity());

		IStatus[] statusChildren = status.getChildren();
		assertEquals(1, statusChildren.length);

		Throwable exception = statusChildren[0].getException();
		assertTrue(exception instanceof CoreException);
		assertTrue(exception.getMessage().contains("Error reading manifest"));
	}

	@Test
	public void testExistingBundle_IgnoreExistingMetadataTrue_IncludeSourceTrue_ExpectOverriddenSourceBundle()
			throws Exception {
		ITargetLocation target = resolveMavenTarget(getTargetXMLForDependencyWithExistingManifest(DependencyDepth.NONE,
				true, true, MissingMetadataMode.GENERATE));
		assertStatusOk(getTargetStatus(target));
		TargetBundle[] bundles = target.getBundles();
		assertEquals(2, bundles.length);

		Optional<TargetBundle> mainBundle = Arrays.stream(bundles).filter(b -> !b.isSourceBundle()).findFirst();
		assertTrue(mainBundle.isPresent());

		Attributes mainBundleAttributes = getManifestMainAttributes(mainBundle.get());

		assertEquals("custom.slf4j.api", mainBundleAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME));
		assertEquals("2.0.7", mainBundleAttributes.getValue(Constants.BUNDLE_VERSION));

		Optional<TargetBundle> sourceBundle = Arrays.stream(bundles).filter(b -> b.isSourceBundle()).findFirst();
		assertTrue(sourceBundle.isPresent());

		Attributes sourceAttributes = getManifestMainAttributes(sourceBundle.get());
		assertEquals("custom.slf4j.api.source", sourceAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME));
		String sourceBundleHeader = sourceAttributes.getValue("Eclipse-SourceBundle");
		assertTrue("Header Eclipse-SourceBundle has wrong value: " + sourceBundleHeader,
				sourceBundleHeader.startsWith("custom.slf4j.api;version="));
	}

}