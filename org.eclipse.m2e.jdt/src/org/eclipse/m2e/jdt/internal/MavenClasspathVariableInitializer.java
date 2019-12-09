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

package org.eclipse.m2e.jdt.internal;

import org.eclipse.jdt.core.ClasspathVariableInitializer;

import org.eclipse.m2e.jdt.MavenJdtPlugin;


/**
 * Maven classpath variable initializer is used to handle M2_REPO variable.
 * 
 * @author Eugene Kuleshov
 */
public class MavenClasspathVariableInitializer extends ClasspathVariableInitializer {

  public MavenClasspathVariableInitializer() {
  }

  public void initialize(String variable) {
    ((BuildPathManager) MavenJdtPlugin.getDefault().getBuildpathManager()).setupVariables();
  }

}
