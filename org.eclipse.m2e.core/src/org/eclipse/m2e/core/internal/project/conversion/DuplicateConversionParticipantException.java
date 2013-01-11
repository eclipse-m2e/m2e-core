/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project.conversion;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;


/**
 * Duplicate Conversion participant exception thrown when sorting conversion participants.
 * 
 * @author Fred Bricon
 */
public class DuplicateConversionParticipantException extends CoreException {

  private static final long serialVersionUID = 6964958800119046412L;

  public DuplicateConversionParticipantException(IStatus status) {
    super(status);
  }

}
