/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.search.util;

/**
 * @author Lukas Krecan
 */
public class SearchException extends RuntimeException {

  private static final long serialVersionUID = 6909305234190388928L;

  public SearchException(String message, Throwable cause) {
    super(message, cause);
  }

  public SearchException(String message) {
    super(message);
  }

}
