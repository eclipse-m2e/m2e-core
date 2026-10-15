/*******************************************************************************
 * Copyright (c) 2026 Bas Gooren and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.tests.apt;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;


@SupportedAnnotationTypes("*")
public class WorkspaceProcessor extends AbstractProcessor {

  private boolean generated;

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if(!generated && !roundEnv.processingOver()) {
      generated = true;
      try {
        JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile("generated.WorkspaceGenerated");
        try(Writer writer = sourceFile.openWriter()) {
          writer.write("package generated; public final class WorkspaceGenerated {}\n");
        }
      } catch(IOException ex) {
        throw new IllegalStateException(ex);
      }
    }
    return false;
  }
}
