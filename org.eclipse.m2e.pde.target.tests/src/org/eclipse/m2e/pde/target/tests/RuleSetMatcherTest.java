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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.pde.target.versions.IgnoreVersionMatcher;
import org.eclipse.m2e.pde.target.versions.RuleSetMatcher;
import org.junit.Before;
import org.junit.Test;

public class RuleSetMatcherTest {
	private RuleSetMatcher ruleSetMatcher;
	private VersionScheme versionScheme;

	@Before
	public void setUp() throws CoreException {
		versionScheme = new GenericVersionScheme();
		ruleSetMatcher = RuleSetMatcher.getMatcherFromXMLString("""
				<ruleset xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="https://www.mojohaus.org/VERSIONS/RULE/3.0.0 https://www.mojohaus.org/versions/versions-model/xsd/rule-3.0.0.xsd" xmlns="https://www.mojohaus.org/VERSIONS/RULE/3.0.0">
					<ignoreVersions>
						<ignoreVersion type="exact">1.0.0</ignoreVersion>
						<ignoreVersion type="regex">.*-alpha</ignoreVersion>
					</ignoreVersions>
					<rules>
						<rule groupId="org.json" artifactId="json">
							<ignoreVersions>
								<ignoreVersion type="range">[20250107,20251224)</ignoreVersion>
							</ignoreVersions>
						</rule>
					</rules>
				</ruleset>
				""");
	}
	
	@Test
	public void test_invalidXML() {
		CoreException exception = assertThrows(CoreException.class,
				() -> RuleSetMatcher.getMatcherFromXMLString("<test></test>"));
		Throwable cause = exception.getCause();
		assertNotNull(cause);
		assertEquals("ParseError at [row,col]:[1,7]\nMessage: Expected root element 'ruleset' but found 'test'",
				cause.getMessage());
	}

	@Test
	public void test_matchWithRule() throws CoreException {
		IgnoreVersionMatcher matcher = ruleSetMatcher.getIgnoreVersionMatcher("org.json", "json");
		assertTrue(matcher.test(getVersion("1.0.0")));
		assertTrue(matcher.test(getVersion("2.0.0-alpha")));
		assertTrue(matcher.test(getVersion("20250107")));
		assertTrue(matcher.test(getVersion("20250615")));
		assertFalse(matcher.test(getVersion("2.0.0")));
		assertFalse(matcher.test(getVersion("20240101")));
		assertFalse(matcher.test(getVersion("20250106")));
		assertFalse(matcher.test(getVersion("20251224")));
		assertFalse(matcher.test(getVersion("20260101")));
	}
	
	@Test
	public void test_matchWithGlobalRule() throws CoreException {
		IgnoreVersionMatcher matcher = ruleSetMatcher.getIgnoreVersionMatcher("org.eclipse.platform",
				"org.eclipse.core.runtime");
		assertTrue(matcher.test(getVersion("1.0.0")));
		assertTrue(matcher.test(getVersion("2.0.0-alpha")));
		assertFalse(matcher.test(getVersion("20250107")));
		assertFalse(matcher.test(getVersion("20250615")));
		assertFalse(matcher.test(getVersion("2.0.0")));
		assertFalse(matcher.test(getVersion("20240101")));
		assertFalse(matcher.test(getVersion("20250106")));
		assertFalse(matcher.test(getVersion("20251224")));
		assertFalse(matcher.test(getVersion("20260101")));
	}

	private Version getVersion(String version) throws CoreException {
		try {
			return versionScheme.parseVersion(version);
		} catch (InvalidVersionSpecificationException e) {
			throw new CoreException(Status.error(e.getMessage(), e));
		}
	}
}
