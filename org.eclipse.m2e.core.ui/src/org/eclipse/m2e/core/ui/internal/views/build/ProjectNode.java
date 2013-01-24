/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

  private final Map<MojoExecutionKey, MojoExecutionNode> executions = new HashMap<MojoExecutionKey, MojoExecutionNode>();

  private final AtomicInteger buildCount = new AtomicInteger();

  public ProjectNode(String name) {
    super(name);
  }

  public int incrementBuildCount() {
    return buildCount.incrementAndGet();
  }

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
    return new ArrayList<MojoExecutionNode>(executions.values());
  }
}
