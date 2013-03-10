/*************************************************************************************
 * Copyright (c) 2008-2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 *     Karl M. Davis (Knowledge Computing Corp.) - initial implementation
 ************************************************************************************/
package org.jboss.tools.maven.apt.internal.compiler;

import org.jboss.tools.maven.apt.internal.AbstractAptProjectConfigurator;
import org.jboss.tools.maven.apt.internal.AptConfiguratorDelegate;
import org.jboss.tools.maven.apt.internal.NoOpDelegate;
import org.jboss.tools.maven.apt.preferences.AnnotationProcessingMode;

import org.eclipse.core.runtime.Assert;

public class MavenCompilerAptProjectConfigurator extends AbstractAptProjectConfigurator {
  
  protected AptConfiguratorDelegate getDelegate(AnnotationProcessingMode mode) {
    Assert.isNotNull(mode, "AnnotationProcessingMode can not be null");
    switch(mode) {
      case jdt_apt:
        return new MavenCompilerJdtAptDelegate(markerManager);
      case maven_execution:
        return new MavenCompilerExecutionDelegate(markerManager);
      case disabled:
      default:
    }
    return new NoOpDelegate();
  }
  
}
