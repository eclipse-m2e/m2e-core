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
package org.eclipse.m2e.pde.target.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.VersionScheme;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.pde.target.versions.IgnoreVersionMatcher;
import org.eclipse.m2e.pde.target.versions.RuleSetMatcher;
import org.junit.Before;
import org.junit.Test;

public class IgnoreVersionMatcherTest {
	private VersionScheme versionScheme;

	@Before
	public void setUp() {
		versionScheme = new GenericVersionScheme();
	}

	@Test
	public void test_matchWithIgnoredVersions() throws Exception {
		IgnoreVersionMatcher matcher = getIgnoreVersionMatcher("org.eclipse.platform", "org.eclipse.core.runtime", """
				<ruleset xmlns="https://www.mojohaus.org/VERSIONS/RULE/3.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
						 xsi:schemaLocation="https://www.mojohaus.org/VERSIONS/RULE/3.0.0 https://www.mojohaus.org/versions/versions-model/xsd/rule-3.0.0.xsd">
					<rules>
						<rule groupId="org.eclipse.platform" artifactId="org.eclipse.core.runtime">
							<ignoreVersions>
								<ignoreVersion type="range">[3.34.0, 4.0.0)</ignoreVersion>
							</ignoreVersions>
						</rule>
					</rules>
				</ruleset>
				""");
		assertFalse(matcher.test(versionScheme.parseVersion("3.33.0")));
		assertFalse(matcher.test(versionScheme.parseVersion("3.33.100")));
		assertFalse(matcher.test(versionScheme.parseVersion("3.33")));
		assertFalse(matcher.test(versionScheme.parseVersion("3")));
		assertTrue(matcher.test(versionScheme.parseVersion("3.34")));
		assertTrue(matcher.test(versionScheme.parseVersion("3.34.0")));
	}

	@Test
	public void test_matchWithoutIgnoredVersions() throws Exception {
		IgnoreVersionMatcher matcher = getIgnoreVersionMatcher("org.eclipse.platform", "org.eclipse.core.runtime", "");
		assertFalse(matcher.test(versionScheme.parseVersion("3.33.0")));
		assertFalse(matcher.test(versionScheme.parseVersion("3.33.100")));
		assertFalse(matcher.test(versionScheme.parseVersion("3.33")));
		assertFalse(matcher.test(versionScheme.parseVersion("3")));
		assertFalse(matcher.test(versionScheme.parseVersion("3.34")));
		assertFalse(matcher.test(versionScheme.parseVersion("3.34.0")));

	}

	private static IgnoreVersionMatcher getIgnoreVersionMatcher(String groupId, String artifactId, String xmlRuleSet)
			throws CoreException {
		RuleSetMatcher ruleSetMatcher = RuleSetMatcher.getMatcherFromXMLString(xmlRuleSet);
		return ruleSetMatcher.getIgnoreVersionMatcher(groupId, artifactId);
	}
}
