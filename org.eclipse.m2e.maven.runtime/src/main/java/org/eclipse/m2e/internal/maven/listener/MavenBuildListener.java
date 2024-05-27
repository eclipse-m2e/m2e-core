/********************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 ********************************************************************************/
package org.eclipse.m2e.internal.maven.listener;

/**
 * Interface to implement when wanted to be informed about remote maven events
 */
public interface MavenBuildListener extends AutoCloseable {

	void projectStarted(MavenProjectBuildData data);

	void onTestEvent(MavenTestEvent mavenTestEvent);

	@Override
	void close();

}
