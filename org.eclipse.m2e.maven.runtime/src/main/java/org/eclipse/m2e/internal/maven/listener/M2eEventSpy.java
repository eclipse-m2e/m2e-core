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
 *   Christoph Läubrich - factored out
 ********************************************************************************/

package org.eclipse.m2e.internal.maven.listener;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionEvent.Type;

import com.google.inject.Inject;

/**
 * This {@link EventSpy} listens to certain events within a Maven build JVM and
 * sends certain data (e.g. about projects being built) to the JVM of the
 * Eclipse IDE that launched the Maven build JVM.
 * 
 * @author Hannes Wellmann
 *
 */
@Named("m2e")
@Singleton
public class M2eEventSpy implements EventSpy {

	private M2EMavenBuildDataBridge bridge;
	static final String PROJECT_START_EVENT = "PSE#";

	@Inject
	public M2eEventSpy(M2EMavenBuildDataBridge bridge) {
		this.bridge = bridge;
	}

	@Override
	public void init(Context context) throws Exception {

	}

	@Override
	public void onEvent(Object event) throws Exception {
		if (!bridge.isActive()) {
			return;
		}
		if (event instanceof ExecutionEvent) {
			ExecutionEvent executionEvent = (ExecutionEvent) event;
			if (executionEvent.getType() == Type.ProjectStarted) {
				String message = M2eEventSpy.PROJECT_START_EVENT
						+ MavenProjectBuildData.serializeProjectData(executionEvent.getProject());
				bridge.sendMessage(message);
			}
		}
	}

	@Override
	public void close() throws Exception {

	}

}
