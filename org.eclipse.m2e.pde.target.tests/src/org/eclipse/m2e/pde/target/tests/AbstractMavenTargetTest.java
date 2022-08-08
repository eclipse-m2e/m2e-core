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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.pde.target.MavenTargetLocationFactory;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;

public abstract class AbstractMavenTargetTest {
	static final String SOURCE_BUNDLE_SUFFIX = ".source";
	static final TargetBundle[] EMPTY = {};

	static ITargetDefinition resolveMavenTarget(String targetXML) throws CoreException {
		@SuppressWarnings("restriction")
		ITargetPlatformService s = org.eclipse.pde.internal.core.PDECore.getDefault()
				.acquireService(ITargetPlatformService.class);
		ITargetDefinition target = s.newTarget();

		setMavenTargetLocationAndResolver(target, targetXML);
		return target;
	}

	static void setMavenTargetLocationAndResolver(ITargetDefinition target, String targetXML) throws CoreException {
		ITargetLocation targetLocation = new MavenTargetLocationFactory().getTargetLocation("Maven", targetXML);
		target.setTargetLocations(new ITargetLocation[] { targetLocation });
		target.resolve(null);
	}

	// --- common assertion utilities ---

	interface ExpectedUnit {

		String id();

		boolean isSourceBundle();

		boolean isOriginal();

		ArtifactKey key();
	}

	private static <U extends ExpectedUnit, T> Map<U, T> assertTargetContent(List<U> expectedUnits, T[] allUnit,
			BiPredicate<U, T> matcher, Function<T, URI> getLocation, Predicate<T> isSourceUnit,
			Function<T, String> getSourceTarget, Function<T, String> toString) {

		Map<U, T> units = new HashMap<>();
		List<T> allElements = new ArrayList<>(Arrays.asList(allUnit));
		for (U expectedUnit : expectedUnits) {
			List<T> matchingUnits = allElements.stream().filter(u -> matcher.test(expectedUnit, u)).toList();

			if (matchingUnits.isEmpty()) {
				fail("Expected unit is missing: " + expectedUnit);
			} else if (matchingUnits.size() == 1) {
				T targetUnit = matchingUnits.get(0);
				allElements.remove(targetUnit);

				assertEquals("Unexpected 'original' state of " + targetUnit, expectedUnit.isOriginal(),
						isOriginalArtifact(expectedUnit, targetUnit, getLocation));
				assertEquals("Unexpected 'isSource' state of " + targetUnit, expectedUnit.isSourceBundle(),
						isSourceUnit.test(targetUnit));
				if (expectedUnit.isSourceBundle()) {
					String expectedSourceTarget = expectedUnit.id().substring(0,
							expectedUnit.id().length() - SOURCE_BUNDLE_SUFFIX.length());
					assertEquals("Source target id", expectedSourceTarget, getSourceTarget.apply(targetUnit));
				} else {
					assertNull(getSourceTarget.apply(targetUnit));
				}
				units.put(expectedUnit, targetUnit);
			} else {
				fail("Expected bundle contaiend multiple times:" + expectedUnit);
			}
		}
		if (!allElements.isEmpty()) {
			String unepxectedBundlesList = allElements.stream().map(u -> "  " + toString.apply(u))
					.collect(Collectors.joining("\n"));
			fail("Encoutnered the following unexpected bundles:" + unepxectedBundlesList);
		}
		return units;
	}

	private static <T> boolean isOriginalArtifact(ExpectedUnit expectedUnit, T unit, Function<T, URI> getLocation) {
		ArtifactKey key = expectedUnit.key();
		if (key == null) {
			return false;
		}
		URI location = getLocation.apply(unit);
		String expectedPathSuffix = "/" + String.join("/", ".m2", "repository", key.groupId().replace('.', '/'),
				key.artifactId(), key.version(), key.artifactId() + "-" + key.version() + ".jar");
		return location.toASCIIString().endsWith(expectedPathSuffix);
	}

	// --- assertion utilities for Bundles in target ---

	static record ExpectedBundle(String bsn, String version, boolean isSourceBundle, boolean isOriginal,
			ArtifactKey key) implements ExpectedUnit {

		@Override
		public String id() {
			return bsn();
		}

		@Override
		public String toString() {
			return bsn + ":" + version;
		}
	}

	static ExpectedBundle originalOSGiBundle(String bsn, String version, String groupArtifact) {
		return originalOSGiBundle(bsn, version, groupArtifact, version);
	}

	static ExpectedBundle originalOSGiBundle(String bsn, String version, String groupArtifact, String mavenVersion) {
		return new ExpectedBundle(bsn, version, false, true,
				ArtifactKey.fromPortableString(groupArtifact + ":" + mavenVersion + "::"));
	}

	static ExpectedBundle generatedBundle(String bsn, String version, String groupArtifact) {
		return new ExpectedBundle(bsn, version, false, false,
				ArtifactKey.fromPortableString(groupArtifact + ":" + version + "::"));
	}

	static List<ExpectedBundle> withSourceBundles(List<ExpectedBundle> mainBundles) {
		return mainBundles.stream().<ExpectedBundle>mapMulti((unit, downStream) -> {
			downStream.accept(unit);
			String sourceId = unit.bsn + SOURCE_BUNDLE_SUFFIX;
			ExpectedBundle sourceUnit = new ExpectedBundle(sourceId, unit.version, true, false, unit.key);
			downStream.accept(sourceUnit);
		}).toList();
	}

	static Attributes getManifestMainAttributes(TargetBundle targetBundle) throws IOException {
		BundleInfo bundleInfo = targetBundle.getBundleInfo();
		File file = URIUtil.toFile(bundleInfo.getLocation());
		try (var jar = new JarFile(file)) {
			return jar.getManifest().getMainAttributes();
		}
	}

	static void assertTargetBundles(ITargetDefinition target, List<ExpectedBundle> expectedUnits) {
		assertTargetContent(expectedUnits, target.getAllBundles(), //
				(expectedBundle, bundle) -> {
					BundleInfo info = bundle.getBundleInfo();
					return expectedBundle.bsn().equals(info.getSymbolicName())
							&& expectedBundle.version.equals(info.getVersion());
				}, //
				tb -> tb.getBundleInfo().getLocation(), //
				tb -> tb.isSourceBundle(),
				tb -> tb.getSourceTarget() != null ? tb.getSourceTarget().getSymbolicName() : null,
				tb -> tb.getBundleInfo().getSymbolicName() + ":" + tb.getBundleInfo().getVersion());
	}

	// --- assertion utilities for Features in a target ---

	static record ExpectedFeature(String id, String version, boolean isSourceBundle, boolean isOriginal,
			ArtifactKey key, List<NameVersionDescriptor> containedPlugins) implements ExpectedUnit {

		@Override
		public String toString() {
			return id + ":" + version;
		}
	}

	static ExpectedFeature originalFeature(String id, String version, String groupArtifact,
			List<NameVersionDescriptor> containedPlugins) {
		ArtifactKey key = ArtifactKey.fromPortableString(groupArtifact + ":" + version + "::");
		return new ExpectedFeature(id, version, false, true, key, containedPlugins);
	}

	static ExpectedFeature generatedFeature(String id, String version, List<NameVersionDescriptor> containedPlugins) {
		return new ExpectedFeature(id, version, false, false, null, containedPlugins);
	}

	static NameVersionDescriptor featurePlugin(String bsn, String version) {
		return new NameVersionDescriptor(bsn, version);
	}

	static List<ExpectedFeature> withSourceFeatures(List<ExpectedFeature> mainFeatures) {
		return mainFeatures.stream().<ExpectedFeature>mapMulti((feature, downStream) -> {
			downStream.accept(feature);
			String sourceId = feature.id() + SOURCE_BUNDLE_SUFFIX;
			List<NameVersionDescriptor> sourcePlugins = feature.containedPlugins.stream()
					.map(d -> featurePlugin(d.getId() + SOURCE_BUNDLE_SUFFIX, d.getVersion())).toList();
			ExpectedFeature sourceUnit = new ExpectedFeature(sourceId, feature.version, true, false, feature.key,
					sourcePlugins);
			downStream.accept(sourceUnit);
		}).toList();
	}

	static Map<ExpectedFeature, TargetFeature> assertTargetFeatures(ITargetDefinition target,
			List<ExpectedFeature> expectedFeatures) {
		var encounteredFeatures = assertTargetContent(expectedFeatures, target.getAllFeatures(), //
				(expectedFeature, feature) -> expectedFeature.id().equals(feature.getId())
						&& expectedFeature.version.equals(feature.getVersion()), //
				f -> Path.of(f.getLocation()).toUri(), //
				f -> isSourceFeature(f), //
				f -> isSourceFeature(f) ? f.getId().substring(0, f.getId().length() - SOURCE_BUNDLE_SUFFIX.length())
						: null, //
				f -> f.getId() + ":" + f.getVersion());
		encounteredFeatures.forEach((expectedFeature, feature) -> {
			assertEquals(Set.copyOf(expectedFeature.containedPlugins()), Set.of(feature.getPlugins()));
		});
		return encounteredFeatures;
	}

	private static boolean isSourceFeature(TargetFeature f) {
		return f.getId().endsWith(SOURCE_BUNDLE_SUFFIX)
				&& Arrays.stream(f.getPlugins()).allMatch(d -> d.getId().endsWith(SOURCE_BUNDLE_SUFFIX));
	}
}
