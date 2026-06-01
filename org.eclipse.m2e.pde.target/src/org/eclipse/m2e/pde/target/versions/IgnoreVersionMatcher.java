/********************************************************************************
 * Copyright (c) 2026 Patrick Ziegler and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *	 Patrick Ziegler - initial API and implementation
 ********************************************************************************/
package org.eclipse.m2e.pde.target.versions;

import java.util.List;
import java.util.function.Predicate;

import org.codehaus.mojo.versions.api.IgnoreVersionHelper;
import org.codehaus.mojo.versions.model.IgnoreVersion;
import org.eclipse.aether.version.Version;

/**
 * This matcher is used to filter out all undesirable versions when updating a
 * Maven artifact. The {@link #test(Version)} method returns {@code true}, if
 * the provided version should be ignored based on the ignored versions this
 * matcher was initialized with.
 */
public final class IgnoreVersionMatcher implements Predicate<Version> {
	private final List<IgnoreVersion> ignoredVersions;

	/**
	 * Creates a new matcher ignoring the specified versions.
	 * 
	 * @param ignoredVersions The versions to ignore.
	 */
	public IgnoreVersionMatcher(List<IgnoreVersion> ignoredVersions) {
		this.ignoredVersions = ignoredVersions;
	}

	@Override
	public boolean test(Version version) {
		for (IgnoreVersion ignoredVersion : ignoredVersions) {
			if (IgnoreVersionHelper.isVersionIgnored(version, ignoredVersion)) {
				return true;
			}
		}
		return false;
	}
}