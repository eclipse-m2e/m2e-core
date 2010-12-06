/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.index;

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

  public MatchType getMatchType() {
    return matchType;
  }

}
