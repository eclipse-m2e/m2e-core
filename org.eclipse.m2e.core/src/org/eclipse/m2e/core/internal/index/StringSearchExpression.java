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
 * StringSearchExpression is a SearchExpression that has String value.
 *
 * @author cstamas
 */
public class StringSearchExpression implements SearchExpression {

  private final String expression;

  public StringSearchExpression(String expression) {
    if(expression == null || expression.trim().length() == 0) {
      throw new RuntimeException("The expression cannot be empty!");
    }
    this.expression = expression;
  }

  @Override
  public String getStringValue() {
    return expression;
  }

}
