/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.search.util;

import org.eclipse.m2e.core.internal.index.SearchExpression;
import org.eclipse.m2e.core.internal.index.SourcedSearchExpression;


/**
 * Packaging representation.
 * 
 * @author Lukas Krecan
 */
public enum Packaging {
  ALL(null), PLUGIN("maven-plugin"), // //$NON-NLS-1$
  POM("pom"); //$NON-NLS-1$

  private final String text;

  private Packaging(String text) {
    this.text = text;
  }

  /**
   * Text representation of the packaging.
   */
  public String getText() {
    return text;
  }

  public SearchExpression toSearchExpression() {
    if(ALL.equals(this)) {
      return null;
    }

    return new SourcedSearchExpression(getText());
  }
}
