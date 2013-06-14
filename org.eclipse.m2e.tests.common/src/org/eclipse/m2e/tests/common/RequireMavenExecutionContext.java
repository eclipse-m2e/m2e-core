/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.common;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * Indicates a test requires active MavenExecutionContext. Execution of tests annotated with @RequireMavenExecutionContext
 * will be automatically wrapped in IMaven.execute(...).
 * 
 * @since 1.5
 */
@Target({TYPE, METHOD})
@Retention(RUNTIME)
public @interface RequireMavenExecutionContext {
  /**
   * If @RequireMavenExecutionContext is enabled for a test suite, {@code @RequireMavenExecutionContext(require=false)}
   * can be used to disable @RequireMavenExecutionContext for individual test methods of the suite.
   */
  boolean require() default true;
}
