/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.project;

/**
 * Project import configuration bean.
 */
public class ProjectImportConfiguration {

  /** resolver configuration bean */
  private final ResolverConfiguration resolverConfiguration;

  /** the project name template */
  private String projectNameTemplate = ""; //$NON-NLS-1$

  /** Creates a new configuration. */
  public ProjectImportConfiguration(ResolverConfiguration resolverConfiguration) {
    this.resolverConfiguration = resolverConfiguration;
  }

  /** Creates a new configuration. */
  public ProjectImportConfiguration() {
    this(new ResolverConfiguration());
  }

  /** Returns the resolver configuration bean. */
  public ResolverConfiguration getResolverConfiguration() {
    return resolverConfiguration;
  }

  /** Sets the project name template. */
  public void setProjectNameTemplate(String projectNameTemplate) {
    this.projectNameTemplate = projectNameTemplate;
  }

  /** Returns the project name template. */
  public String getProjectNameTemplate() {
    return projectNameTemplate;
  }
}
