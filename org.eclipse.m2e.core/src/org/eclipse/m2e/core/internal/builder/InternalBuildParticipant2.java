/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.internal.builder;

import java.util.Collections;
import java.util.Map;

import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;


public abstract class InternalBuildParticipant2 extends AbstractBuildParticipant {

  private Map<String, String> args = Collections.emptyMap();

  void setArgs(Map<String, String> args) {
    this.args = args;
  }

  protected Map<String, String> getArgs() {
    return args;
  }
}
