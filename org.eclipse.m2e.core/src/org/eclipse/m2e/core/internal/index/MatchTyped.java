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
 * MatchTyped is a interface that describes the wanted match type to be used.
 * 
 * @author cstamas
 */
public interface MatchTyped {

  public enum MatchType {
    /** Exact match wanted */
    EXACT,
    /** Partial match wanted, like prefix, contains, etc. */
    PARTIAL;
  };

  MatchType getMatchType();

}
