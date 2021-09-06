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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


class ProjectNode extends ContainerNode {

  private final Map<MojoExecutionKey, MojoExecutionNode> executions = new HashMap<>();

  private final AtomicInteger buildCount = new AtomicInteger();

  public ProjectNode(String name) {
    super(name);
  }

  public int incrementBuildCount() {
    return buildCount.incrementAndGet();
  }

  @Override
  public int getBuildCount() {
    return buildCount.get();
  }

  public synchronized MojoExecutionNode getMojoExecutionNode(MojoExecutionKey mojoExecutionKey) {
    MojoExecutionNode node = executions.get(mojoExecutionKey);
    if(node == null) {
      node = new MojoExecutionNode(mojoExecutionKey);
      executions.put(mojoExecutionKey, node);
    }
    return node;
  }

  public synchronized Collection<MojoExecutionNode> getMojoExecutions() {
    return new ArrayList<>(executions.values());
  }
}
