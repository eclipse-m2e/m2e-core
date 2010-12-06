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

package org.eclipse.m2e.core.core;

import java.util.EventListener;

/**
 * A console listener is notified of output to the Maven console.
 * 
 * @author Benjamin Bentmann
 */
public interface IMavenConsoleListener extends EventListener {

  void loggingMessage(String msg);

  void loggingError(String msg);

}
