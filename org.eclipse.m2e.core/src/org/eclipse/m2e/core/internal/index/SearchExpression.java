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
