/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.core.internal.launch;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.CoreException;


/**
 * {@link IMavenLauncher} allows to trigger a maven run
 */
public interface IMavenLauncher {

  CompletableFuture<?> runMaven(File basedir, String goals, Properties properties, boolean interactive)
      throws CoreException;

}
