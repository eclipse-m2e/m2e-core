package org.jboss.tools.maven.apt.internal;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/*************************************************************************************
 * Copyright (c) 2008-2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Red Hat, Inc. - Initial implementation.
 ************************************************************************************/
public class DefaultAnnotationProcessorConfiguration implements AnnotationProcessorConfiguration {

  private boolean isAnnotationProcessingEnabled = false;
  
  private File outputDirectory = null;

  private File testOutputDirectory = null;
  
  private Map<String, String> annotationProcessorOptions;
  
  private List<String> annotationProcessors;

  private List<File> dependencies;
  
  public File getOutputDirectory() {
    return outputDirectory;
  }
  
  public void setOutputDirectory(File generatedOutputDirectory) {
    this.outputDirectory = generatedOutputDirectory;
  }
  
  public boolean isAnnotationProcessingEnabled() {
    return isAnnotationProcessingEnabled;
  }
  
  public void setAnnotationProcessingEnabled(boolean enabled) {
    this.isAnnotationProcessingEnabled = enabled;
  }
  
  
  public Map<String, String> getAnnotationProcessorOptions() {
    if (annotationProcessorOptions == null) {
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(annotationProcessorOptions);
  }

  public void setAnnotationProcessorOptions(Map<String, String> annotationProcessorOptions) {
    this.annotationProcessorOptions = annotationProcessorOptions;
  }

  public List<File> getDependencies() {
    if (dependencies == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(dependencies);
  }

  public void setDependencies(List<File> dependencies) {
    this.dependencies = dependencies;
  }
  
  public List<String> getAnnotationProcessors() {
    if (annotationProcessors == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(annotationProcessors);
  }

  public void setAnnotationProcessors(List<String> annotationProcessors) {
    this.annotationProcessors = annotationProcessors;
  }

  public File getTestOutputDirectory() {
    return this.testOutputDirectory;
  }

  public void setTestOutputDirectory(File testOutputDirectory) {
    this.testOutputDirectory = testOutputDirectory;
  }

}
