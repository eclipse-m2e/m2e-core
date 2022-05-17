/*******************************************************************************
 * Copyright (c) 2008-2022 Sonatype, Inc.
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

package org.eclipse.m2e.core.embedder;

import org.eclipse.core.runtime.CoreException;

import org.apache.maven.settings.Settings;


/**
 * If one is interested in changes to the global settings one might register this interface as an OSGi service to be
 * notified
 */
public interface ISettingsChangeListener {

  void settingsChanged(Settings settings) throws CoreException;
}
