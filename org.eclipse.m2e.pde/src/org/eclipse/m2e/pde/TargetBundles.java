/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.pde.core.target.TargetBundle;

/**
 * represents a resolved set of {@link Artifact} -> {@link TargetBundle}
 */
class TargetBundles {
	final Map<Artifact, TargetBundle> bundles = new HashMap<>();
	final Set<Artifact> ignoredArtifacts = new HashSet<>();
}
