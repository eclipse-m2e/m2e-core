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
package org.eclipse.m2e.core.ui.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.model.IgnoreVersion;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.model.Rule;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.model.RuleSet;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.RuleSetParser;
import org.junit.Before;
import org.junit.Test;

public class RuleSetParserTest {
	private RuleSet ruleSet;

	@Before
	public void setUp() {
		ruleSet = new RuleSet();
		{
			RuleSet.IgnoreVersions ignoreVersions = new RuleSet.IgnoreVersions();
			{
				IgnoreVersion ignoreVersion = new IgnoreVersion();
				ignoreVersion.setType(RuleSetParser.TYPE_EXACT);
				ignoreVersion.setValue("1.0.0");
				ignoreVersions.getIgnoreVersion().add(ignoreVersion);
			}
			{
				IgnoreVersion ignoreVersion = new IgnoreVersion();
				ignoreVersion.setType(RuleSetParser.TYPE_REGEX);
				ignoreVersion.setValue(".*-alpha");
				ignoreVersions.getIgnoreVersion().add(ignoreVersion);
			}
			ruleSet.setIgnoreVersions(ignoreVersions);
		}

		{
			RuleSet.Rules rules = new RuleSet.Rules();
			{
				Rule rule = new Rule();
				rule.setGroupId("org.json");
				rule.setArtifactId("json");
				{
					Rule.IgnoreVersions ignoreVersions = new Rule.IgnoreVersions();
					IgnoreVersion ignoreVersion = new IgnoreVersion();
					ignoreVersion.setType(RuleSetParser.TYPE_RANGE);
					ignoreVersion.setValue("[20250107,20251224)");
					ignoreVersions.getIgnoreVersion().add(ignoreVersion);
					rule.setIgnoreVersions(ignoreVersions);
				}
				rules.getRule().add(rule);
			}
			ruleSet.setRules(rules);
		}
	}
	
	@Test
	public void testStringToRuleSetInvalid() {
		CoreException exception = assertThrows(CoreException.class, () -> RuleSetParser.fromXMLString("<test></test>"));
		Throwable cause = exception.getCause();
		assertNotNull(cause);
		assertEquals("cvc-elt.1.a: Cannot find the declaration of element 'test'.", cause.getMessage());
	}

	@Test
	public void testStringToRuleSet1() throws CoreException {
		ruleSet = RuleSetParser.fromXMLString("""
				<ruleset xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="https://www.mojohaus.org/VERSIONS/RULE/3.0.0 https://www.mojohaus.org/versions/versions-model/xsd/rule-3.0.0.xsd" xmlns="https://www.mojohaus.org/VERSIONS/RULE/3.0.0">
					<ignoreVersions>
						<ignoreVersion type="exact">1.0.0</ignoreVersion>
						<ignoreVersion type="regex">.*-alpha</ignoreVersion>
					</ignoreVersions>
					<rules>
						<rule artifactId="json" groupId="org.json">
							<ignoreVersions>
								<ignoreVersion type="range">[20250107,20251224)</ignoreVersion>
							</ignoreVersions>
						</rule>
					</rules>
				</ruleset>
				""");
		assertNotNull(ruleSet);

		{
			RuleSet.IgnoreVersions ignoreVersions = ruleSet.getIgnoreVersions();
			assertNotNull(ignoreVersions);
			assertEquals(2, ignoreVersions.getIgnoreVersion().size());

			IgnoreVersion ignoreVersion0 = ignoreVersions.getIgnoreVersion().get(0);
			assertEquals(RuleSetParser.TYPE_EXACT, ignoreVersion0.getType());
			assertEquals("1.0.0", ignoreVersion0.getValue());

			IgnoreVersion ignoreVersion1 = ignoreVersions.getIgnoreVersion().get(1);
			assertEquals(RuleSetParser.TYPE_REGEX, ignoreVersion1.getType());
			assertEquals(".*-alpha", ignoreVersion1.getValue());
		}

		{
			RuleSet.Rules rules = ruleSet.getRules();
			assertNotNull(rules);
			assertEquals(1, rules.getRule().size());

			Rule rule = rules.getRule().get(0);
			assertEquals("org.json", rule.getGroupId());
			assertEquals("json", rule.getArtifactId());

			Rule.IgnoreVersions ignoreVersions = rule.getIgnoreVersions();
			assertNotNull(ignoreVersions);
			assertEquals(1, ignoreVersions.getIgnoreVersion().size());

			IgnoreVersion ignoreVersion = ignoreVersions.getIgnoreVersion().get(0);
			assertEquals(RuleSetParser.TYPE_RANGE, ignoreVersion.getType());
			assertEquals("[20250107,20251224)", ignoreVersion.getValue());
		}
	}
	
	@Test
	public void testStringToRuleSet2() throws CoreException {
		ruleSet = RuleSetParser.fromXMLString("""
				<ruleset xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="https://www.mojohaus.org/VERSIONS/RULE/3.0.0 https://www.mojohaus.org/versions/versions-model/xsd/rule-3.0.0.xsd" xmlns="https://www.mojohaus.org/VERSIONS/RULE/3.0.0">
					<rules>
						<rule artifactId="json" groupId="org.json">
							<ignoreVersions>
								<ignoreVersion type="range">[20250107,20251224)</ignoreVersion>
							</ignoreVersions>
						</rule>
					</rules>
				</ruleset>
				""");

		assertNotNull(ruleSet);
		assertNull(ruleSet.getIgnoreVersions());

		RuleSet.Rules rules = ruleSet.getRules();
		assertNotNull(rules);
		assertEquals(1, rules.getRule().size());

		Rule rule = rules.getRule().get(0);
		assertEquals("org.json", rule.getGroupId());
		assertEquals("json", rule.getArtifactId());

		Rule.IgnoreVersions ignoreVersions = rule.getIgnoreVersions();
		assertNotNull(ignoreVersions);
		assertEquals(1, ignoreVersions.getIgnoreVersion().size());

		IgnoreVersion ignoreVersion = ignoreVersions.getIgnoreVersion().get(0);
		assertEquals(RuleSetParser.TYPE_RANGE, ignoreVersion.getType());
		assertEquals("[20250107,20251224)", ignoreVersion.getValue());
	}

	@Test
	public void testStringToRuleSet3() throws CoreException {
		ruleSet = RuleSetParser.fromXMLString("""
				<ruleset xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="https://www.mojohaus.org/VERSIONS/RULE/3.0.0 https://www.mojohaus.org/versions/versions-model/xsd/rule-3.0.0.xsd" xmlns="https://www.mojohaus.org/VERSIONS/RULE/3.0.0">
					<ignoreVersions>
						<ignoreVersion type="exact">1.0.0</ignoreVersion>
						<ignoreVersion type="regex">.*-alpha</ignoreVersion>
					</ignoreVersions>
				</ruleset>
				""");
		assertNotNull(ruleSet);
		assertNull(ruleSet.getRules());

		RuleSet.IgnoreVersions ignoreVersions = ruleSet.getIgnoreVersions();
		assertNotNull(ignoreVersions);
		assertEquals(2, ignoreVersions.getIgnoreVersion().size());

		IgnoreVersion ignoreVersion0 = ignoreVersions.getIgnoreVersion().get(0);
		assertEquals(RuleSetParser.TYPE_EXACT, ignoreVersion0.getType());
		assertEquals("1.0.0", ignoreVersion0.getValue());

		IgnoreVersion ignoreVersion1 = ignoreVersions.getIgnoreVersion().get(1);
		assertEquals(RuleSetParser.TYPE_REGEX, ignoreVersion1.getType());
		assertEquals(".*-alpha", ignoreVersion1.getValue());
	}

	@Test
	public void testRuleSetToString1() throws CoreException {
		assertEquals("""
				<ruleset xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="https://www.mojohaus.org/VERSIONS/RULE/3.0.0 https://www.mojohaus.org/versions/versions-model/xsd/rule-3.0.0.xsd" xmlns="https://www.mojohaus.org/VERSIONS/RULE/3.0.0">
				    <ignoreVersions>
				        <ignoreVersion type="exact">1.0.0</ignoreVersion>
				        <ignoreVersion type="regex">.*-alpha</ignoreVersion>
				    </ignoreVersions>
				    <rules>
				        <rule artifactId="json" groupId="org.json">
				            <ignoreVersions>
				                <ignoreVersion type="range">[20250107,20251224)</ignoreVersion>
				            </ignoreVersions>
				        </rule>
				    </rules>
				</ruleset>
				""", RuleSetParser.toXMLString(ruleSet));
	}

	@Test
	public void testRuleSetToString2() throws CoreException {
		ruleSet.setRules(null);
		assertEquals("""
				<ruleset xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="https://www.mojohaus.org/VERSIONS/RULE/3.0.0 https://www.mojohaus.org/versions/versions-model/xsd/rule-3.0.0.xsd" xmlns="https://www.mojohaus.org/VERSIONS/RULE/3.0.0">
				    <ignoreVersions>
				        <ignoreVersion type="exact">1.0.0</ignoreVersion>
				        <ignoreVersion type="regex">.*-alpha</ignoreVersion>
				    </ignoreVersions>
				</ruleset>
				""", RuleSetParser.toXMLString(ruleSet));
	}

	@Test
	public void testRuleSetToString3() throws CoreException {
		ruleSet.setIgnoreVersions(null);
		assertEquals("""
				<ruleset xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="https://www.mojohaus.org/VERSIONS/RULE/3.0.0 https://www.mojohaus.org/versions/versions-model/xsd/rule-3.0.0.xsd" xmlns="https://www.mojohaus.org/VERSIONS/RULE/3.0.0">
				    <rules>
				        <rule artifactId="json" groupId="org.json">
				            <ignoreVersions>
				                <ignoreVersion type="range">[20250107,20251224)</ignoreVersion>
				            </ignoreVersions>
				        </rule>
				    </rules>
				</ruleset>
				""", RuleSetParser.toXMLString(ruleSet));
	}
}
