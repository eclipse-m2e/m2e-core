/*************************************************************************************
 * Copyright (c) 2008-2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Red Hat, Inc - Initial implementation.
 ************************************************************************************/

package org.jboss.tools.maven.apt.internal;

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

}
