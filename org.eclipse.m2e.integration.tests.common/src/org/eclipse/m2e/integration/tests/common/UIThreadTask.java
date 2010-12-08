/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.integration.tests.common;

import org.eclipse.swt.widgets.Display;

/**
 * Executes a task on the UI thread. Task can return an object to caller, and
 * any exceptions which occur will be re-thrown in the calling thread.
 */
public abstract class UIThreadTask implements Runnable {
	private Object result = null;

	private Exception exception = null;

	final public void run() {
		try {
			result = runEx();
		} catch (Exception ex) {
			exception = ex;
		}
	}

	public Exception getException() {
		return exception;
	}

	public Object getResult() {
		return result;
	}

	public abstract Object runEx() throws Exception;

	public static Object executeOnEventQueue(UIThreadTask task)
			throws Exception {
		if (Display.getDefault().getThread() == Thread.currentThread()) {
			task.run();
		} else {
			Display.getDefault().syncExec(task);
		}
		if (task.getException() != null) {
			throw task.getException();
		}
		return task.getResult();
	}
}
