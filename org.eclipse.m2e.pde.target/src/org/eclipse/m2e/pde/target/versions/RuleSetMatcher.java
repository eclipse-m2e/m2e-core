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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.codehaus.mojo.versions.model.IgnoreVersion;
import org.codehaus.mojo.versions.model.RuleSet;
import org.codehaus.mojo.versions.model.io.stax.RuleStaxReader;
import org.codehaus.mojo.versions.rule.RuleService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * This matcher contains all rules defined in a rule-set. Instances of this
 * class can either be created via {@link #getMatcherFromPreferences()} or
 * {@link #getMatcherFromXMLString(String)}. The matcher for individual
 * artifacts is created via {@link #getIgnoreVersionMatcher(String, String)}.
 */
public final class RuleSetMatcher {
	/**
	 * Path to the XML-encoded rule set.
	 * 
	 * @since 2.7.800
	 * @see <a href=
	 *      "https://www.mojohaus.org/versions/versions-model/rule.html">Rule</a>
	 */
	public static final String P_MAVEN_VERSION_RULESET_FILEPATH = "versionRuleSetPath";

	private static final Bundle BUNDLE = FrameworkUtil.getBundle(RuleSetMatcher.class);
	private static final IEclipsePreferences PREFERENCES = InstanceScope.INSTANCE.getNode(BUNDLE.getSymbolicName());
	private final RuleSet ruleSet;

	private RuleSetMatcher(RuleSet ruleSet) {
		this.ruleSet = ruleSet;
	}

	/**
	 * Returns a new rule-set matcher based on the XML file that is specified in the
	 * preferences. Returns an empty matcher if no file is set.
	 * 
	 * @return A new matcher instance.
	 * @throws CoreException If the files doesn't exist or is not a valid rule-set.
	 */
	public static RuleSetMatcher getMatcherFromPreferences() throws CoreException {
		String ruleSetFilePath = PREFERENCES.get(P_MAVEN_VERSION_RULESET_FILEPATH, "");

		if (ruleSetFilePath.isEmpty()) {
			RuleSet ruleSet = new RuleSet();
			return new RuleSetMatcher(ruleSet);
		}
		return getMatcherFromPath(Path.of(ruleSetFilePath));
	}

	/**
	 * 
	 * Returns a new rule-set matcher based on the given path. Returns an empty
	 * matcher if the path doesn't point to a file.
	 * 
	 * @param ruleSetPath The absolute path to the rule-set file.
	 * @return A new matcher instance.
	 * @throws CoreException If the path doesn't describe a rule-set.
	 */
	public static RuleSetMatcher getMatcherFromPath(Path ruleSetPath) throws CoreException {
		if (!Files.isRegularFile(ruleSetPath)) {
			RuleSet ruleSet = new RuleSet();
			return new RuleSetMatcher(ruleSet);
		}
		try {
			String xmlRuleSet = Files.readString(ruleSetPath);
			return getMatcherFromXMLString(xmlRuleSet);
		} catch (IOException e) {
			throw new CoreException(Status.error(e.getMessage(), e));
		}
	}

	/**
	 * 
	 * Returns a new rule-set matcher based on the input string. Returns an empty
	 * matcher if the string is empty.
	 * 
	 * @param xmlRuleSet The XML-encoded rule-set.
	 * @return A new matcher instance.
	 * @throws CoreException If the string doesn't describe a rule-set.
	 */
	public static RuleSetMatcher getMatcherFromXMLString(String xmlRuleSet) throws CoreException {
		if (xmlRuleSet.isEmpty()) {
			RuleSet ruleSet = new RuleSet();
			return new RuleSetMatcher(ruleSet);
		}
		try (Reader reader = new StringReader(xmlRuleSet)) {
			RuleSet ruleSet = new RuleStaxReader().read(reader, true);
			return new RuleSetMatcher(ruleSet);
		} catch (IOException | XMLStreamException e) {
			throw new CoreException(Status.error(e.getMessage(), e));
		}
	}

	/**
	 * Returns the underlying rule-set used by this matcher. Should not be modified!
	 * 
	 * @return The rule-set used by this matcher.
	 */
	public RuleSet getRuleSet() {
		return ruleSet;
	}

	/**
	 * Returns a checker for the given artifact. This matcher includes both the
	 * "global" rules, as well as the best-fitting rule for the given artifact.
	 * 
	 * @param groupId    The Maven group-id for the artifact to match.
	 * @param artifactId The Maven artifact-id for the artifact to match.
	 * @return A version matcher for the given artifact.
	 */
	public IgnoreVersionMatcher getIgnoreVersionMatcher(String groupId, String artifactId) {
		RuleService ruleService = new RuleService(null, ruleSet);
		List<IgnoreVersion> ignoreVersions = ruleService.getIgnoredVersions(groupId, artifactId);
		return new IgnoreVersionMatcher(ignoreVersions);
	}
}
