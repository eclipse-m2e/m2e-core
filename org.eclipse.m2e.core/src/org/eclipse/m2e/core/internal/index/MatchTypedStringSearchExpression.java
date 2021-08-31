/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core.internal.index;

/**
 * MatchTypedStringSearchExpression
 *
 * @author cstamas
 */
public class MatchTypedStringSearchExpression extends StringSearchExpression implements MatchTyped {

  private final MatchType matchType;

  public MatchTypedStringSearchExpression(final String expression, final MatchType matchType) {
    super(expression);
    this.matchType = matchType;
  }

  @Override
  public MatchType getMatchType() {
    return matchType;
  }

}
