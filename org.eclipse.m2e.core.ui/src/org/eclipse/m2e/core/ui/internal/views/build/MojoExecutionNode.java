/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.views.build;

import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


class MojoExecutionNode extends ContainerNode {

  private final MojoExecutionKey mojoExecutionKey;

  private int buildCount;

  public MojoExecutionNode(MojoExecutionKey mojoExecutionKey) {
    super(mojoExecutionKey.toString());
    this.mojoExecutionKey = mojoExecutionKey;
  }

  public MojoExecutionKey getMojoExecutionKey() {
    return mojoExecutionKey;
  }

  @Override
  public synchronized int getBuildCount() {
    return buildCount;
  }

  public void setBuildCount(int buildCount) {
    this.buildCount = buildCount;
  }
}
