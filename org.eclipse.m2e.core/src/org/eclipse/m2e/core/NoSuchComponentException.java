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

package org.eclipse.m2e.core;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * NoSuchComponentException
 *
 * @author igor
 */
public class NoSuchComponentException extends IllegalArgumentException {

  private static final long serialVersionUID = 9184391358528175461L;

  public NoSuchComponentException(ComponentLookupException ex) {
    super(ex);
  }

}
