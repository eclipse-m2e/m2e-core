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
 *   Patrick Ziegler - initial API and implementation
 ********************************************************************************/
package org.eclipse.m2e.core.ui.internal.preferences.ruleset;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionRange;
import org.eclipse.aether.version.VersionScheme;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;

import org.eclipse.m2e.core.ui.internal.preferences.ruleset.model.IgnoreVersion;

public class IgnoreVersionMatcher implements Predicate<Version> {
  private static final ILog LOG = Platform.getLog(IgnoreVersionMatcher.class);
  private final List<IgnoreVersion> ignoredVersions;

  private final VersionScheme versionScheme;

  public IgnoreVersionMatcher(List<IgnoreVersion> ignoredVersions) {
    this.ignoredVersions = ignoredVersions;
    this.versionScheme = new GenericVersionScheme();
  }

  @Override
  public boolean test(Version version) {
    for(IgnoreVersion ignoredVersion : ignoredVersions) {
      try {
        switch(ignoredVersion.getType()) {
          case RuleSetParser.TYPE_EXACT:
            Version otherVersion = versionScheme.parseVersion(ignoredVersion.getValue());
            return Objects.equals(otherVersion, version);
          case RuleSetParser.TYPE_RANGE:
            VersionRange versionRange = versionScheme.parseVersionRange(ignoredVersion.getValue());
            return versionRange.containsVersion(version);
          case RuleSetParser.TYPE_REGEX:
            Pattern pattern = Pattern.compile(ignoredVersion.getValue());
            return pattern.matcher(version.toString()).matches();
          default:
            return false;
        }
      } catch(InvalidVersionSpecificationException e) {
        LOG.error(e.getMessage(), e);
      }
    }
    return false;
  }
}