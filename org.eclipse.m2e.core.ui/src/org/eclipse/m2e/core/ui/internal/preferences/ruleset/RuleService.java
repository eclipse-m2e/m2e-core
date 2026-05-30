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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.eclipse.m2e.core.ui.internal.preferences.ruleset.model.IgnoreVersion;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.model.Rule;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.model.RuleSet;


/**
 * Service providing access to rules and ignore-version handling.
 * <p>
 * The service is constructed with a {@link RuleSet} and offers methods to find the best-fitting {@link Rule} for a
 * given artifact and to collect the ignored versions for an artifact.
 * </p>
 * <p>
 * This class is a modified version of {@code org.codehaus.mojo.versions.rule.RuleService}.
 * </p>
 */
public class RuleService {
  private final Map<String, Rule> bestFitRuleCache = new ConcurrentHashMap<>();
  private final RuleSet ruleSet;

  public RuleService(RuleSet ruleSet) {
    this.ruleSet = ruleSet;
  }

  /**
   * Find the rule, if any, which best fits the artifact details given.
   *
   * @param groupId Group id of the artifact
   * @param artifactId Artifact id of the artifact
   * @return rule which best describes the given artifact, or {@code null} if none
   */
  public Rule getBestFitRule(String groupId, String artifactId) {
    String groupArtifactId = groupId + ':' + artifactId;
    if(bestFitRuleCache.containsKey(groupArtifactId)) {
      return bestFitRuleCache.get(groupArtifactId);
    }

    Rule bestFit = null;
    final List<Rule> rules = getRules();
    int bestGroupIdScore = Integer.MAX_VALUE;
    int bestArtifactIdScore = Integer.MAX_VALUE;
    boolean exactGroupId = false;
    boolean exactArtifactId = false;
    for(Rule rule : rules) {
      int groupIdScore = RegexUtils.getWildcardScore(rule.getGroupId());
      if(groupIdScore > bestGroupIdScore) {
        continue;
      }
      boolean exactMatch = exactMatch(rule.getGroupId(), groupId);
      boolean match = exactMatch || match(rule.getGroupId(), groupId);
      if(!match || (exactGroupId && !exactMatch)) {
        continue;
      }
      if(bestGroupIdScore > groupIdScore) {
        bestArtifactIdScore = Integer.MAX_VALUE;
        exactArtifactId = false;
      }
      bestGroupIdScore = groupIdScore;
      if(exactMatch && !exactGroupId) {
        exactGroupId = true;
        bestArtifactIdScore = Integer.MAX_VALUE;
        exactArtifactId = false;
      }
      int artifactIdScore = RegexUtils.getWildcardScore(rule.getArtifactId());
      if(artifactIdScore > bestArtifactIdScore) {
        continue;
      }
      exactMatch = exactMatch(rule.getArtifactId(), artifactId);
      match = exactMatch || match(rule.getArtifactId(), artifactId);
      if(!match || (exactArtifactId && !exactMatch)) {
        continue;
      }
      bestArtifactIdScore = artifactIdScore;
      if(exactMatch && !exactArtifactId) {
        exactArtifactId = true;
      }
      bestFit = rule;
    }

    if(bestFit != null) {
      bestFitRuleCache.put(groupArtifactId, bestFit);
    }
    return bestFit;
  }

  /**
   * Returns a list of versions which should not be considered when looking for updates.
   *
   * @param groupId groupId of the artifact to evaluate
   * @param artifactId artifactId of the artifact to evaluate
   * @return list of ignored versions (never {@code null})
   */
  public List<IgnoreVersion> getIgnoredVersions(String groupId, String artifactId) {
    List<IgnoreVersion> ignoredVersions = new ArrayList<>(getIgnoredVersions());

    Rule bestFitRule = getBestFitRule(groupId, artifactId);
    if(bestFitRule != null) {
      ignoredVersions.addAll(getIgnoredVersions(bestFitRule));
    }

    return Collections.unmodifiableList(ignoredVersions);
  }

  private List<Rule> getRules() {
    RuleSet.Rules rules = ruleSet.getRules();
    if(rules == null) {
      return Collections.emptyList();
    }
    return rules.getRule();
  }

  private List<IgnoreVersion> getIgnoredVersions() {
    RuleSet.IgnoreVersions ignoreVersions = ruleSet.getIgnoreVersions();
    if(ignoreVersions == null) {
      return Collections.emptyList();
    }
    return ignoreVersions.getIgnoreVersion();
  }

  private static List<IgnoreVersion> getIgnoredVersions(Rule rule) {
    Rule.IgnoreVersions ignoreVersions = rule.getIgnoreVersions();
    if(ignoreVersions == null) {
      return Collections.emptyList();
    }
    return ignoreVersions.getIgnoreVersion();
  }

  static boolean exactMatch(String wildcardRule, String value) {
    Pattern p = Pattern.compile(RegexUtils.convertWildcardsToRegex(wildcardRule, true));
    return p.matcher(value).matches();
  }

  static boolean match(String wildcardRule, String value) {
    Pattern p = Pattern.compile(RegexUtils.convertWildcardsToRegex(wildcardRule, false));
    return p.matcher(value).matches();
  }
}
