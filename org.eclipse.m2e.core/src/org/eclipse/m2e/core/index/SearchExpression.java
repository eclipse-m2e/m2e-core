/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.index;

/**
 * SearchExpression is a wrapper interface for expressions representable as plain strings to be used within searches.
 * 
 * @author cstamas
 */
public interface SearchExpression {

  /**
   * Returns the expression value as plain java String.
   * 
   * @return
   */
  String getStringValue();

}
