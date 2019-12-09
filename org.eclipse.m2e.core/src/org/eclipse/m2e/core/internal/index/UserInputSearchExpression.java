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
 * UserInputSearchExpression is a search expression usually coming from user input (like some UI dialogue, element or
 * CLI). It will be normalized and tokenized and then a search will happen against it. Search expressions of this type
 * will always provide "broader" results, since it defaults to prefix searches.
 * 
 * @author cstamas
 */
public class UserInputSearchExpression extends MatchTypedStringSearchExpression {

  public UserInputSearchExpression(String expression) {
    super(expression, MatchType.PARTIAL);
  }
}
