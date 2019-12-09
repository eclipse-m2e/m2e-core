/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecyclemapping.discovery;

/**
 * Identifies Maven project elements that can have lifecycle mapping configuration. Currently, these are project
 * packaging types and maven plugin executions. Implementations must provide #hashCode() and #equals(Object) methods.
 * Instances of the same element referenced from different projects are expected to be equal.
 */
public interface ILifecycleMappingElement {

  public ILifecycleMappingRequirement getLifecycleMappingRequirement();

}
