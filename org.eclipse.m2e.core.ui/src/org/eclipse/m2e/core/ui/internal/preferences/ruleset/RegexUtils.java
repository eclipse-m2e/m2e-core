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

import static java.util.Optional.ofNullable;


/**
 * Utility methods to help with regex manipulation.
 * <p>
 * This class is a modified version of {@code org.codehaus.mojo.versions.utils.RegexUtils}.
 * </p>
 */
public final class RegexUtils {
    /**
     * The end of a regex literal sequence.
     */
    public static final String REGEX_QUOTE_END = "\\E"; //$NON-NLS-1$

    /**
     * The start of a regex literal sequence.
     */
    public static final String REGEX_QUOTE_START = "\\Q"; //$NON-NLS-1$

    /**
     * Escape the escapes.
     */
    public static final String REGEX_QUOTE_END_ESCAPED = REGEX_QUOTE_END + '\\' + REGEX_QUOTE_END + REGEX_QUOTE_START;

    private RegexUtils() {
      throw new IllegalAccessError("Utility classes should never be instantiated"); //$NON-NLS-1$
    }

    /**
     * Takes a string and returns the regex that will match that string exactly.
     *
     * @param s The string to match.
     * @return The regex that will match the string exactly.
     */
    public static String quote(String s) {
        int i = s.indexOf(REGEX_QUOTE_END);
        if (i == -1) {
            // we're safe as nobody has a crazy \E in the string
            return REGEX_QUOTE_START + s + REGEX_QUOTE_END;
        }

        // damn there's at least one \E in the string
        StringBuilder sb = new StringBuilder(s.length() + 32);
        // each escape-escape takes 10 chars...
        // hope there's less than 4 of them

        sb.append(REGEX_QUOTE_START);
        int pos = 0;
        do {
            // we are safe from pos to i
            sb.append(s, pos, i);
            // now escape-escape
            sb.append(REGEX_QUOTE_END_ESCAPED);
            // move the working start
            pos = i + REGEX_QUOTE_END.length();
            i = s.indexOf(REGEX_QUOTE_END, pos);
        } while (i != -1);

        sb.append(s.substring(pos));
        sb.append(REGEX_QUOTE_END);

        return sb.toString();
    }

    /**
     * Calculates a score for a wildcard rule. The score is calculated as follows:
     * <ul>
     * <li>each {@code ?} character adds 1 to the score</li>
     * <li>each {@code *} character adds 1000 to the score</li>
     * </ul>
     * Thus rules with fewer wildcards will have a lower score, and rules with more specific wildcards (i.e. {@code ?})
     * will have a lower score than those with less specific wildcards (i.e. {@code *}).
     *
     * @param wildcardRule the wildcard rule, may be {@code null}
     * @return the score
     */
    public static int getWildcardScore(String wildcardRule) {
        int score = 0;
        if (wildcardRule != null) {
            for (char c : wildcardRule.toCharArray()) {
                if (c == '?') {
                    score++;
                } else if (c == '*') {
                    score += 1000;
                }
            }
        }
        return score;
    }

    /**
     * Converts a wildcard rule to a regex rule.
     *
     * @param wildcardRule the wildcard rule, may be {@code null}
     * @param exactMatch <code>true</code> results in an regex that will match the entire string, while
     *            <code>false</code> will match the start of the string.
     * @return The regex rule.
     */
    public static String convertWildcardsToRegex(String wildcardRule, boolean exactMatch) {
        StringBuilder regex = new StringBuilder();
        final int wildcardLength = ofNullable(wildcardRule).map(String::length).orElse(0);
        for (int index = 0, nextIndex = 0; index < wildcardLength; index = nextIndex + 1) {
            final int nextQ = wildcardRule.indexOf('?', index);
            final int nextS = wildcardRule.indexOf('*', index);
            if (nextQ == -1 && nextS == -1) {
                regex.append(quote(wildcardRule.substring(index)));
                break;
            }
            if (nextQ == -1) {
                nextIndex = nextS;
            } else if (nextS == -1) {
                nextIndex = nextQ;
            } else {
                nextIndex = Math.min(nextQ, nextS);
            }
            if (index < nextIndex) {
                // we have some characters to match
                regex.append(quote(wildcardRule.substring(index, nextIndex)));
            }
            char c = wildcardRule.charAt(nextIndex);
            if (c == '?') {
                regex.append('.');
            } else {
              regex.append(".*"); //$NON-NLS-1$
            }
        }
        if (!exactMatch) {
          regex.append(".*"); //$NON-NLS-1$
        }
        return regex.toString();
    }
}
