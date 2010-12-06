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

package org.eclipse.m2e.core.internal.project.registry;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;

/**
 * ILifecycleMapping2
 *
 * @author igor
 */
public interface ILifecycleMapping2 extends ILifecycleMapping {
  public AbstractMavenDependencyResolver getDependencyResolver(IProgressMonitor monitor);
}
