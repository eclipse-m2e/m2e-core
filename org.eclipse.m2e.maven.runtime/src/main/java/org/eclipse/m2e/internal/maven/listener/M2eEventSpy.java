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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionEvent.Type;

@Singleton
@Named("m2e")
public class M2eEventSpy implements EventSpy {

	private M2EMavenBuildDataBridge bridge;

	@Inject
	public M2eEventSpy(M2EMavenBuildDataBridge bridge) {
		this.bridge = bridge;
	}

	@Override
	public void init(Context context) throws Exception {

	}

	@Override
	public void onEvent(Object event) throws Exception {
		if (bridge.isActive()) {
			if (event instanceof ExecutionEvent) {
				ExecutionEvent executionEvent = (ExecutionEvent) event;
				if (executionEvent.getType() == Type.ProjectStarted) {
					String message = MavenProjectBuildData.serializeProjectData(((ExecutionEvent) event).getProject());
					bridge.sendMessage(message);
				}
			}
		}
	}

	@Override
	public void close() throws Exception {

	}

}
