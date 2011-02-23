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

package org.eclipse.m2e.core.internal.lifecyclemapping.discovery;

/**
 * Identifies Maven project elements that can have lifecycle mapping configuration. Currently, these are project
 * packaging types and maven plugin executions. Implementations must provide #hashCode() and #equals(Object) methods.
 */
public interface ILifecycleMappingElementKey {

}
