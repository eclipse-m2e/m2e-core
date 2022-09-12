/*************************************************************************************
 * Copyright (c) 2008-2016 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat, Inc - Initial implementation.
 ************************************************************************************/

package org.eclipse.m2e.apt.internal;

import java.io.File;
import java.util.List;
import java.util.Map;


public interface AnnotationProcessorConfiguration {

  boolean isAnnotationProcessingEnabled();

  Map<String, String> getAnnotationProcessorOptions();

  List<File> getDependencies();

  File getOutputDirectory();

  File getTestOutputDirectory();

  List<String> getAnnotationProcessors();

  boolean isAddProjectDependencies();
}
