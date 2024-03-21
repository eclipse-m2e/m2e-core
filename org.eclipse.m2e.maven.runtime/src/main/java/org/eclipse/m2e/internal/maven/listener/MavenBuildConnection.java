/********************************************************************************
 * Copyright (c) 2022, 2024 Hannes Wellmann and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Hannes Wellmann - initial API and implementation
 *   Christoph Läubrich - factor out into dedicated component
 ********************************************************************************/
package org.eclipse.m2e.internal.maven.listener;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MavenBuildConnection {
	private final ServerSocketChannel server;
	private final AtomicBoolean closed = new AtomicBoolean(false);
	private MavenBuildConnectionListener listener;

	MavenBuildConnection(ServerSocketChannel server, MavenBuildConnectionListener listener) {
		this.server = server;
		this.listener = listener;
	}

	public boolean isCompleted() {
		return closed.get();
	}

	public void close() {
		if (closed.compareAndSet(false, true)) {
			listener.onClose();
			// Close the server to ensure the reader-thread does not wait forever for a
			// connection from the Maven-process in case something went wrong during
			// launching or while setting up the connection.
			try {
				server.close();
			} catch (IOException e) {
			}
		}
	}
}