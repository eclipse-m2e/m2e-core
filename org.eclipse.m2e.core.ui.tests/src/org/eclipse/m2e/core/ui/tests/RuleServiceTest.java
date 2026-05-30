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

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.model.IgnoreVersion;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.model.Rule;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.model.RuleSet;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.RuleService;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.RuleSetParser;
import org.junit.Test;

public class RuleServiceTest {

	/**
	 * A rule without wildcards overrides a rule with wildcards.
	 */
	@Test
	public void test_getBestFitRule_artifactId_wildcard() throws CoreException {
		RuleService ruleService = getRuleService("""
				<ruleset xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="https://www.mojohaus.org/VERSIONS/RULE/3.0.0 https://www.mojohaus.org/versions/versions-model/xsd/rule-3.0.0.xsd" xmlns="https://www.mojohaus.org/VERSIONS/RULE/3.0.0">
				<rules>
					<rule groupId="org.eclipse.platform" artifactId="org.eclipse.core.runtime"/>
					<rule groupId="org.eclipse.platform" artifactId="org.eclipse.core.?"/>
					<rule groupId="org.eclipse.platform" artifactId="org.eclipse.core.*"/>
				</rules>
			</ruleset>
			""");
		Rule rule = ruleService.getBestFitRule("org.eclipse.platform", "org.eclipse.core.runtime");
		assertEquals(rule.getGroupId(), "org.eclipse.platform");
		assertEquals(rule.getArtifactId(), "org.eclipse.core.runtime");
	}

	/**
	 * A rule with ? wildcards will override a rule with * wildcards.
	 */
	@Test
	public void test_getBestFitRule_artifactId_question_wildcard() throws CoreException {
		RuleService ruleService = getRuleService("""
				<ruleset xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="https://www.mojohaus.org/VERSIONS/RULE/3.0.0 https://www.mojohaus.org/versions/versions-model/xsd/rule-3.0.0.xsd" xmlns="https://www.mojohaus.org/VERSIONS/RULE/3.0.0">
				<rules>
					<rule groupId="org.eclipse.platform" artifactId="org.eclipse.core.?"/>
					<rule groupId="org.eclipse.platform" artifactId="org.eclipse.core.*"/>
				</rules>
			</ruleset>
			""");
		Rule rule = ruleService.getBestFitRule("org.eclipse.platform", "org.eclipse.core.runtime");
		assertEquals(rule.getGroupId(), "org.eclipse.platform");
		assertEquals(rule.getArtifactId(), "org.eclipse.core.?");
	}

	/**
	 * A rule applies to all child groupIds unless overridden by a subsequent rule.
	 */
	@Test
	public void test_getBestFitRule_groupId_childGroupId() throws CoreException {
		RuleService ruleService = getRuleService("""
				<ruleset xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="https://www.mojohaus.org/VERSIONS/RULE/3.0.0 https://www.mojohaus.org/versions/versions-model/xsd/rule-3.0.0.xsd" xmlns="https://www.mojohaus.org/VERSIONS/RULE/3.0.0">
				<rules>
					<rule groupId="org" artifactId="json"/>
					<rule groupId="org.json" artifactId="json"/>
				</rules>
			</ruleset>
			""");
		Rule rule1 = ruleService.getBestFitRule("org.json", "json");
		assertEquals(rule1.getGroupId(), "org.json");
		assertEquals(rule1.getArtifactId(), "json");

		Rule rule2 = ruleService.getBestFitRule("org.notjson", "json");
		assertEquals(rule2.getGroupId(), "org");
		assertEquals(rule2.getArtifactId(), "json");
	}

	/**
	 * A rule without wildcards overrides a rule with wildcards.
	 */
	@Test
	public void test_getBestFitRule_groupId_wildcard() throws CoreException {
		RuleService ruleService = getRuleService("""
				<ruleset xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="https://www.mojohaus.org/VERSIONS/RULE/3.0.0 https://www.mojohaus.org/versions/versions-model/xsd/rule-3.0.0.xsd" xmlns="https://www.mojohaus.org/VERSIONS/RULE/3.0.0">
				<rules>
					<rule groupId="org.json" artifactId="json"/>
					<rule groupId="org.*" artifactId="json"/>
					<rule groupId="org.?" artifactId="json"/>
				</rules>
			</ruleset>
			""");
		Rule rule = ruleService.getBestFitRule("org.json", "json");
		assertEquals(rule.getGroupId(), "org.json");
		assertEquals(rule.getArtifactId(), "json");
	}

	/**
	 * A rule with ? wildcards will override a rule with * wildcards.
	 */
	@Test
	public void test_getBestFitRule_groupId_question_wildcard() throws CoreException {
		RuleService ruleService = getRuleService("""
				<ruleset xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="https://www.mojohaus.org/VERSIONS/RULE/3.0.0 https://www.mojohaus.org/versions/versions-model/xsd/rule-3.0.0.xsd" xmlns="https://www.mojohaus.org/VERSIONS/RULE/3.0.0">
				<rules>
					<rule groupId="org.?" artifactId="json"/>
					<rule groupId="org.*" artifactId="json"/>
				</rules>
			</ruleset>
			""");
		Rule rule = ruleService.getBestFitRule("org.json", "json");
		assertEquals(rule.getGroupId(), "org.?");
		assertEquals(rule.getArtifactId(), "json");
	}
	
	@Test
	public void test_getIgnoredRules() throws CoreException {
		RuleService ruleService = getRuleService("""
				<ruleset xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="https://www.mojohaus.org/VERSIONS/RULE/3.0.0 https://www.mojohaus.org/versions/versions-model/xsd/rule-3.0.0.xsd" xmlns="https://www.mojohaus.org/VERSIONS/RULE/3.0.0">
				<rules>
					<rule groupId="org.eclipse.platform" artifactId="org.eclipse.core.runtime">
						<ignoreVersions>
							<ignoreVersion type="exact">2.0.0</ignoreVersion>
						</ignoreVersions>
					</rule>
				</rules>
				<ignoreVersions>
					<ignoreVersion type="exact">1.0.0</ignoreVersion>
				</ignoreVersions>
			</ruleset>
			""");
		List<IgnoreVersion> ignoredVersions = ruleService.getIgnoredVersions("org.eclipse.platform", "org.eclipse.core.runtime");
		assertEquals(2, ignoredVersions.size());
		assertEquals("1.0.0", ignoredVersions.get(0).getValue());
		assertEquals("2.0.0", ignoredVersions.get(1).getValue());
	}

	private static RuleService getRuleService(String xmlRuleSet) throws CoreException {
		RuleSet ruleSet = RuleSetParser.fromXMLString(xmlRuleSet);
		return new RuleService(ruleSet);
	}
}
