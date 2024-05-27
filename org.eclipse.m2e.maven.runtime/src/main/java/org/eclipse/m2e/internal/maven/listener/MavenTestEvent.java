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

import java.nio.file.Path;

import org.apache.maven.execution.ExecutionEvent;

public class MavenTestEvent {


	private Path reportDirectory;
	private ExecutionEvent.Type type;

	public MavenTestEvent(ExecutionEvent.Type type, Path reportDirectory) {
		this.type = type;
		this.reportDirectory = reportDirectory;
	}

	public Path getReportDirectory() {
		return reportDirectory;
	}

	public ExecutionEvent.Type getType() {
		return type;
	}

	@Override
	public String toString() {
		return "MavenTestEvent [type=" + type + ", reportDirectory=" + reportDirectory + "]";
	}

}
