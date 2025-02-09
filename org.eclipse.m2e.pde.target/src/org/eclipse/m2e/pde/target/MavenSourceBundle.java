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
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.m2e.pde.target.shared.MavenBundleWrapper;
import org.eclipse.pde.core.target.TargetBundle;

public class MavenSourceBundle extends TargetBundle {

	public MavenSourceBundle(BundleInfo sourceTarget, Artifact artifact, CacheManager cacheManager) throws Exception {
		this.fSourceTarget = sourceTarget;
		String symbolicName = sourceTarget.getSymbolicName();
		String version = sourceTarget.getVersion();
		fInfo.setSymbolicName(MavenBundleWrapper.getSourceBundleName(symbolicName));
		fInfo.setVersion(version);
		Manifest manifest;
		File sourceFile = artifact.getFile();
		try (JarFile jar = new JarFile(sourceFile)) {
			manifest = Objects.requireNonNullElseGet(jar.getManifest(), Manifest::new);
		}
		if (MavenBundleWrapper.isValidSourceManifest(manifest)) {
			fInfo.setLocation(sourceFile.toURI());
		} else {
			File generatedSourceBundle = cacheManager.accessArtifactFile(artifact, file -> {
				if (CacheManager.isOutdated(file, sourceFile)) {
					MavenBundleWrapper.addSourceBundleMetadata(manifest, symbolicName, version);
					MavenBundleWrapper.transferJarEntries(sourceFile, manifest, file);
				}
				return file;
			});
			fInfo.setLocation(generatedSourceBundle.toURI());
		}
	}


}
