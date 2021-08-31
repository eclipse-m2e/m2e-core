/*******************************************************************************
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Takari, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.launch;

/**
 * @since 1.5
 */
public class DefaultWorkspaceRuntime extends MavenWorkspaceRuntime {

  public DefaultWorkspaceRuntime() {
    super(MavenRuntimeManagerImpl.WORKSPACE);
  }

  @Override
  public boolean isEditable() {
    return false;
  }

}
