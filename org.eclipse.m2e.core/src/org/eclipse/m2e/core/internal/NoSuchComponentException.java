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

package org.eclipse.m2e.core.internal;

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
