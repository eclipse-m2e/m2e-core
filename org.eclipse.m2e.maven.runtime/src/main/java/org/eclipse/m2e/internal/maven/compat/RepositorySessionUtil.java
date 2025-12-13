/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.maven.compat;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.SessionData;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.transfer.TransferListener;

/**
 * Compatibility layer for RepositorySystemSession to handle Maven 3 and Maven 4 differences.
 * This class provides methods to work with session configuration without direct dependencies
 * on DefaultRepositorySystemSession.
 */
public class RepositorySessionUtil {

	/**
	 * Creates a new mutable session based on the given session.
	 * 
	 * @param session the base session to copy from
	 * @return a new mutable session
	 */
	public static RepositorySystemSession newSession(RepositorySystemSession session) {
		if (session instanceof DefaultRepositorySystemSession) {
			return new DefaultRepositorySystemSession(session);
		}
		throw new IllegalArgumentException("Unsupported session type: " + session.getClass().getName());
	}

	/**
	 * Sets the dependency graph transformer on the session.
	 * 
	 * @param session the session to modify
	 * @param transformer the transformer to set
	 */
	public static void setDependencyGraphTransformer(RepositorySystemSession session,
			DependencyGraphTransformer transformer) {
		if (session instanceof DefaultRepositorySystemSession) {
			((DefaultRepositorySystemSession) session).setDependencyGraphTransformer(transformer);
			return;
		}
		throw new IllegalArgumentException("Unsupported session type: " + session.getClass().getName());
	}

	/**
	 * Sets a configuration property on the session.
	 * 
	 * @param session the session to modify
	 * @param key the configuration key
	 * @param value the configuration value
	 */
	public static void setConfigProperty(RepositorySystemSession session, String key, Object value) {
		if (session instanceof DefaultRepositorySystemSession) {
			((DefaultRepositorySystemSession) session).setConfigProperty(key, value);
			return;
		}
		throw new IllegalArgumentException("Unsupported session type: " + session.getClass().getName());
	}

	/**
	 * Sets the transfer listener on the session.
	 * 
	 * @param session the session to modify
	 * @param transferListener the transfer listener to set
	 * @return the previous transfer listener
	 */
	public static TransferListener setTransferListener(RepositorySystemSession session,
			TransferListener transferListener) {
		if (session instanceof DefaultRepositorySystemSession) {
			DefaultRepositorySystemSession defaultSession = (DefaultRepositorySystemSession) session;
			TransferListener previous = defaultSession.getTransferListener();
			defaultSession.setTransferListener(transferListener);
			return previous;
		}
		throw new IllegalArgumentException("Unsupported session type: " + session.getClass().getName());
	}

	/**
	 * Sets the session data on the session.
	 * 
	 * @param session the session to modify
	 * @param data the session data to set
	 * @return the previous session data
	 */
	public static SessionData setData(RepositorySystemSession session, SessionData data) {
		if (session instanceof DefaultRepositorySystemSession) {
			DefaultRepositorySystemSession defaultSession = (DefaultRepositorySystemSession) session;
			SessionData previous = defaultSession.getData();
			defaultSession.setData(data);
			return previous;
		}
		throw new IllegalArgumentException("Unsupported session type: " + session.getClass().getName());
	}
}
