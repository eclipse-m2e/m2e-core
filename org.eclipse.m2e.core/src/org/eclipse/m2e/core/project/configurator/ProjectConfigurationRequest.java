/*******************************************************************************
 * Copyright (c) 2008-2022 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Hannes Wellmann - Convert to record
 *******************************************************************************/

package org.eclipse.m2e.core.project.configurator;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.project.IMavenProjectFacade;


public record ProjectConfigurationRequest(IMavenProjectFacade mavenProjectFacade, MavenProject mavenProject) {

}
