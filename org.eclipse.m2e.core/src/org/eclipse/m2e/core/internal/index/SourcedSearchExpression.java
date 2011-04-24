/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.internal.index;

/**
 * SourcedSearchExpression is a search expression usually "sourced" from some programmatic source, and we already know
 * it is complete, exact value that we want to search for. Indexer will try to match exactly the provided string value,
 * no more no less.
 * 
 * @author cstamas
 */
public class SourcedSearchExpression extends MatchTypedStringSearchExpression {

  public SourcedSearchExpression(String expression) {
    super(expression, MatchType.EXACT);
  }
}
