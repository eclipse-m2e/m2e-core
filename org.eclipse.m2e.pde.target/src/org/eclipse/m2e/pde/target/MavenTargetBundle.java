/*******************************************************************************
 * Copyright (c) 2018, 2021 Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *      Patrick Ziegler - Issue 743 - Occasionally, m2e is unable to read wrapped artifacts
 *******************************************************************************/
package org.eclipse.m2e.pde.target;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Properties;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.pde.core.target.TargetBundle;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;

public class MavenTargetBundle extends TargetBundle {

	private static final ILog LOGGER = Platform.getLog(MavenTargetBundle.class);
	private TargetBundle bundle;
	private IStatus status;
	private final BundleInfo bundleInfo;
	private boolean isWrapped;
	private Artifact artifact;

	@Override
	public BundleInfo getBundleInfo() {
		if (bundle == null) {
			return bundleInfo;
		}
		return bundle.getBundleInfo();
	}

	@Override
	public boolean isSourceBundle() {
		if (bundle == null) {
			return false;
		}
		return bundle.isSourceBundle();
	}

	@Override
	public BundleInfo getSourceTarget() {
		if (bundle == null) {
			return null;
		}
		return bundle.getSourceTarget();
	}

	@Override
	public boolean isFragment() {
		if (bundle == null) {
			return false;
		}
		return bundle.isFragment();
	}

	@Override
	public String getSourcePath() {
		if (bundle == null) {
			return null;
		}
		return bundle.getSourcePath();
	}

	public MavenTargetBundle(Artifact artifact, Properties bndInstructions, CacheManager cacheManager,
			MissingMetadataMode metadataMode) {
		this.artifact = artifact;
		File file = artifact.getFile();
		this.bundleInfo = new BundleInfo(artifact.getGroupId() + "." + artifact.getArtifactId(), artifact.getVersion(),
				file != null ? file.toURI() : null, -1, false);
		try {
			bundle = new TargetBundle(file);
		} catch (Exception ex) {
			if (metadataMode == MissingMetadataMode.ERROR) {
				status = new Status(Status.ERROR, MavenTargetBundle.class.getPackage().getName(),
						artifact + " is not a bundle", ex);
				LOGGER.log(status);
			} else if (metadataMode == MissingMetadataMode.GENERATE) {
				try {
					bundle = cacheManager.accessArtifactFile(artifact,
							artifactFile -> getWrappedArtifact(artifact, bndInstructions, artifactFile));
					isWrapped = true;
				} catch (Exception e) {
					// not possible then
					String message = artifact + " is not a bundle and cannot be automatically bundled as such ";
					if (e.getMessage() != null) {
						message += " (" + e.getMessage() + ")";
					}
					status = new Status(Status.ERROR, MavenTargetBundle.class.getPackage().getName(), message, e);
					LOGGER.log(status);
				}
			} else {
				status = Status.CANCEL_STATUS;
				LOGGER.log(status);
			}
		}
	}

	public Artifact getArtifact() {
		return artifact;
	}

	public static TargetBundle getWrappedArtifact(Artifact artifact, Properties bndInstructions, File wrappedFile)
			throws Exception {
		File artifactFile = artifact.getFile();
		File instructionsFile = new File(wrappedFile.getParentFile(),
				FilenameUtils.getBaseName(wrappedFile.getName()) + ".xml");
		if (CacheManager.isOutdated(wrappedFile, artifactFile)
				|| propertiesChanged(bndInstructions, instructionsFile)) {
			// The cached file may already be locked by another process (e.g. the Target Editor) and thus can't be
			// patched directly. So instead, we create a temporary file which is used instead. Since it also means the
			// original file is outdated, it has to be marked for deletion as well, once the program terminates.
			if (!wrappedFile.canWrite()) {
				// Delete the original file, once the process has released the lock
				wrappedFile.deleteOnExit();
				
				File bundleDir = wrappedFile.getParentFile();
				
				String baseName = FilenameUtils.getBaseName(wrappedFile.getName());
				String extension = FilenameUtils.getExtension(wrappedFile.getName());
				
				FilenameFilter fileFilter = new RegexFileFilter(baseName);
				int index = bundleDir.listFiles(fileFilter).length;
				
				wrappedFile = new File(wrappedFile.getParentFile(), String.format("%s-%d.%s", baseName, index, extension));
				// Delete the temporary file as well
				wrappedFile.deleteOnExit();
			}
			
			try (Jar jar = new Jar(artifactFile)) {
				Manifest originalManifest = jar.getManifest();
				try (Analyzer analyzer = new Analyzer();) {
					analyzer.setJar(jar);
					if (originalManifest != null) {
						analyzer.mergeManifest(originalManifest);
					}
					analyzer.setProperty("mvnGroupId", artifact.getGroupId());
					analyzer.setProperty("mvnArtifactId", artifact.getArtifactId());
					analyzer.setProperty("mvnVersion", artifact.getBaseVersion());
					analyzer.setProperty("mvnClassifier", artifact.getClassifier());
					analyzer.setProperty("generatedOSGiVersion",
							TargetBundles.createOSGiVersion(artifact).toString());
					analyzer.setProperties(bndInstructions);
					jar.setManifest(analyzer.calcManifest());
					jar.write(wrappedFile);
					wrappedFile.setLastModified(artifactFile.lastModified());
				}
			}
			TargetBundle targetBundle = new TargetBundle(wrappedFile);
			try (FileOutputStream os = new FileOutputStream(instructionsFile)) {
				bndInstructions.storeToXML(os, null);
			}
			return targetBundle;
		}
		try {
			return new TargetBundle(wrappedFile);
		} catch (Exception e) {
			// cached file seems invalid/stale...
			FileUtils.forceDelete(wrappedFile);
			return getWrappedArtifact(artifact, bndInstructions, wrappedFile);
		}
	}

	private static boolean propertiesChanged(Properties properties, File file) {
		Properties oldProperties = new Properties();
		if (file.exists()) {
			try {
				try (FileInputStream stream = new FileInputStream(file)) {
					oldProperties.loadFromXML(stream);
				}
				return oldProperties.equals(properties);
			} catch (IOException e) {
				// fall through and assume changed then
			}
		}
		return true;
	}

	public boolean isWrapped() {
		return isWrapped;
	}

	@Override
	public IStatus getStatus() {
		if (bundle == null) {
			if (status == null) {
				return Status.OK_STATUS;
			}
			return status;
		}
		return bundle.getStatus();
	}

	@Override
	public int hashCode() {
		return getBundleInfo().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MavenTargetBundle) {
			MavenTargetBundle other = (MavenTargetBundle) obj;
			return getBundleInfo().equals(other.getBundleInfo());
		}
		return false;
	}

}
