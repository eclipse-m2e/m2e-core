/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.core.embedder;

import org.apache.maven.execution.MavenExecutionRequest;


/**
 * IMavenExecutionRequestInitializer
 *
 * @author karypid
 */
public interface IMavenExecutionContextInitializer {

  void initializeExecutionRequest(IMavenExecutionContext context, MavenExecutionRequest request);

}
