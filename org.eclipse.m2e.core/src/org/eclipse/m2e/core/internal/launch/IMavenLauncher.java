/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.launch;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.CoreException;


/**
 * {@link IMavenLauncher} allows to trigger a maven run
 */
public interface IMavenLauncher {

  CompletableFuture<?> runMaven(File basedir, String goals, Map<String, String> properties, boolean interactive)
      throws CoreException;

}
