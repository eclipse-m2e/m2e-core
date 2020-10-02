/*******************************************************************************
 * Copyright (c) 2018, 2020 Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde;

import java.io.File;
import java.util.jar.Manifest;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.pde.core.target.TargetBundle;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;

public class MavenTargetBundle extends TargetBundle {

	private TargetBundle bundle;
	private IStatus status;
	private BundleInfo bundleInfo;
	private boolean isWrapped;

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

	public MavenTargetBundle(Artifact artifact, MissingMetadataMode metadataMode) {
		File file = artifact.getFile();
		this.bundleInfo = new BundleInfo(artifact.getGroupId() + "." + artifact.getArtifactId(), artifact.getVersion(),
				file != null ? file.toURI() : null, -1, false);
		try {
			bundle = new TargetBundle(file);
		} catch (Exception ex) {
			if (metadataMode == MissingMetadataMode.ERROR) {
				status = new Status(Status.ERROR, MavenTargetBundle.class.getPackage().getName(),
						artifact + " is not a bundle", ex);
			} else if (metadataMode == MissingMetadataMode.GENERATE) {
				try {
					bundle = getWrappedArtifact(artifact, bundleInfo);
					isWrapped = true;
				} catch (Exception e) {
					// not possible then
					String message = artifact + " is not a bundle and cannot be automatically bundled as such ";
					if (e.getMessage() != null) {
						message += " (" + e.getMessage() + ")";
					}
					status = new Status(Status.ERROR, MavenTargetBundle.class.getPackage().getName(), message, e);
				}
			} else {
				status = Status.CANCEL_STATUS;
			}
		}
	}

	public static TargetBundle getWrappedArtifact(Artifact artifact, BundleInfo bundleInfo) throws Exception {
		synchronized (artifact) {
			File file = artifact.getFile();
			String name = file.getName();
			File wrappedFile = new File(file.getParentFile(), name + ".wrapped");
			if (!wrappedFile.exists() || file.lastModified() > wrappedFile.lastModified()) {
				try (Jar jar = new Jar(file)) {
					Manifest originalManifest = jar.getManifest();
					try (Analyzer analyzer = new Analyzer();) {
						analyzer.setJar(jar);
						if (originalManifest != null) {
							analyzer.mergeManifest(originalManifest);
						}
						analyzer.setProperty(Analyzer.IMPORT_PACKAGE, "*;resolution:=optional");
						analyzer.setProperty(Analyzer.EXPORT_PACKAGE, "*;-noimport:=true");
						analyzer.setProperty(Analyzer.BUNDLE_SYMBOLICNAME, createSymbolicName(artifact));
						analyzer.setProperty(Analyzer.BUNDLE_NAME, "Derived from " + artifact.getGroupId() + ":"
								+ artifact.getArtifactId() + ":" + artifact.getVersion());
						analyzer.setBundleVersion(createBundleVersion(artifact));
						jar.setManifest(analyzer.calcManifest());
						jar.write(wrappedFile);
					}
				}
				return new TargetBundle(wrappedFile);
			}
			try {
				return new TargetBundle(wrappedFile);
			} catch (Exception e) {
				// cached file seems invalid/stale...
				file.delete();
				return getWrappedArtifact(artifact, bundleInfo);
			}
		}
	}

	public static String createBundleVersion(Artifact artifact) {
		String version = artifact.getVersion();
		if (version == null || version.isEmpty()) {
			return "0";
		}
		return version.replaceAll("[^a-zA-Z0-9\\.]", ".").replaceAll("\\.\\.+", ".");
	}

	public static String createSymbolicName(Artifact artifact) {

		return "wrapped." + artifact.getGroupId() + "." + artifact.getArtifactId();
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
