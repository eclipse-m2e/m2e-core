/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.index;

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

  public String getStringValue() {
    return expression;
  }

}
