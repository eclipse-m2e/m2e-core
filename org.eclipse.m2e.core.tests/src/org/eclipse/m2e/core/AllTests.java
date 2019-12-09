/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Mickael Istria (Red Hat Inc.) - Initial implementation
 *******************************************************************************/

package org.eclipse.m2e.core;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import org.eclipse.m2e.core.internal.project.registry.MemoryConsumptionTest;
import org.eclipse.m2e.core.internal.project.registry.RegistryTest;


@RunWith(Suite.class)
@SuiteClasses({MavenBugsTest.class, RegistryTest.class, MemoryConsumptionTest.class})
public class AllTests {

}
