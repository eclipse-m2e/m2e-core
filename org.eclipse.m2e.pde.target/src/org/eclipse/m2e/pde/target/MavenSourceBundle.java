/*******************************************************************************
 * Copyright (c) 2020, 2023 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.pde.core.target.TargetBundle;
import org.osgi.framework.Constants;

public class MavenSourceBundle extends TargetBundle {

	@SuppressWarnings("restriction")
	public static final String ECLIPSE_SOURCE_BUNDLE_HEADER = org.eclipse.pde.internal.core.ICoreConstants.ECLIPSE_SOURCE_BUNDLE;

	public MavenSourceBundle(BundleInfo sourceTarget, Artifact artifact, CacheManager cacheManager) throws Exception {
		this.fSourceTarget = sourceTarget;
		fInfo.setSymbolicName(sourceTarget.getSymbolicName() + ".source");
		fInfo.setVersion(sourceTarget.getVersion());
		Manifest manifest;
		File sourceFile = artifact.getFile();
		try (JarFile jar = new JarFile(sourceFile)) {
			manifest = Objects.requireNonNullElseGet(jar.getManifest(), Manifest::new);
		}
		if (isValidSourceManifest(manifest)) {
			fInfo.setLocation(sourceFile.toURI());
		} else {
			File generatedSourceBundle = cacheManager.accessArtifactFile(artifact, file -> {
				if (CacheManager.isOutdated(file, sourceFile)) {
					addSourceBundleMetadata(manifest, sourceTarget);
					transferJarEntries(sourceFile, manifest, file);
				}
				return file;
			});
			fInfo.setLocation(generatedSourceBundle.toURI());
		}
	}

	private void addSourceBundleMetadata(Manifest manifest, BundleInfo bundle) {
		Attributes attr = manifest.getMainAttributes();
		if (attr.isEmpty()) {
			attr.put(Name.MANIFEST_VERSION, "1.0");
		}
		attr.putValue(ECLIPSE_SOURCE_BUNDLE_HEADER,
				bundle.getSymbolicName() + ";version=\"" + bundle.getVersion() + "\";roots:=\".\"");
		attr.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
		attr.putValue(Constants.BUNDLE_NAME,
				"Source Bundle for " + bundle.getSymbolicName() + ":" + bundle.getVersion());
		attr.putValue(Constants.BUNDLE_SYMBOLICNAME, fInfo.getSymbolicName());
		attr.putValue(Constants.BUNDLE_VERSION, fInfo.getVersion());
	}

	private void transferJarEntries(File source, Manifest manifest, File target) throws IOException {
		try (var output = new JarOutputStream(new FileOutputStream(target), manifest);
				var input = new JarInputStream(new FileInputStream(source));) {
			for (JarEntry entry; (entry = input.getNextJarEntry()) != null;) {
				if (!JarFile.MANIFEST_NAME.equals(entry.getName())) {
					output.putNextEntry(new ZipEntry(entry.getName()));
					input.transferTo(output);
				}
			}
		}
	}

	private static boolean isValidSourceManifest(Manifest manifest) {
		return manifest != null && manifest.getMainAttributes().getValue(ECLIPSE_SOURCE_BUNDLE_HEADER) != null;
	}

}
