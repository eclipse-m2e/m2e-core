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

package org.eclipse.m2e.editor.pom;

/**
 * Value provider for retrieving and creating holder element values
 *
 * @author Eugene Kuleshov
 */
public abstract class ValueProvider<T> {

  public abstract T getValue();

  public boolean isEmpty() {
    return false;
  }

}
