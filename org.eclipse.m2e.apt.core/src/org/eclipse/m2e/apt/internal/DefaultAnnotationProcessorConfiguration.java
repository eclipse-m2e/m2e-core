/*************************************************************************************
 * Copyright (c) 2008-2016 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - Initial implementation.
 ************************************************************************************/

package org.eclipse.m2e.apt.internal;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class DefaultAnnotationProcessorConfiguration implements AnnotationProcessorConfiguration {

  private boolean isAnnotationProcessingEnabled = false;

  private boolean isAddProjectDependencies = true;

  private File outputDirectory = null;

  private File testOutputDirectory = null;

  private Map<String, String> annotationProcessorOptions;

  private List<String> annotationProcessors;

  private List<File> dependencies;

  @Override
  public File getOutputDirectory() {
    return outputDirectory;
  }

  public void setOutputDirectory(File generatedOutputDirectory) {
    this.outputDirectory = generatedOutputDirectory;
  }

  @Override
  public boolean isAnnotationProcessingEnabled() {
    return isAnnotationProcessingEnabled;
  }

  public void setAnnotationProcessingEnabled(boolean enabled) {
    this.isAnnotationProcessingEnabled = enabled;
  }

  @Override
  public Map<String, String> getAnnotationProcessorOptions() {
    if(annotationProcessorOptions == null) {
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(annotationProcessorOptions);
  }

  public void setAnnotationProcessorOptions(Map<String, String> annotationProcessorOptions) {
    this.annotationProcessorOptions = annotationProcessorOptions;
  }

  @Override
  public List<File> getDependencies() {
    if(dependencies == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(dependencies);
  }

  public void setDependencies(List<File> dependencies) {
    this.dependencies = dependencies;
  }

  @Override
  public List<String> getAnnotationProcessors() {
    if(annotationProcessors == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(annotationProcessors);
  }

  public void setAnnotationProcessors(List<String> annotationProcessors) {
    this.annotationProcessors = annotationProcessors;
  }

  @Override
  public File getTestOutputDirectory() {
    return this.testOutputDirectory;
  }

  public void setTestOutputDirectory(File testOutputDirectory) {
    this.testOutputDirectory = testOutputDirectory;
  }

  public void setAddProjectDependencies(boolean addProjectDependencies) {
    this.isAddProjectDependencies = addProjectDependencies;
  }

  @Override
  public boolean isAddProjectDependencies() {
    return this.isAddProjectDependencies;
  }
}
