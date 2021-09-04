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

package org.eclipse.m2e.core.internal.builder.plexusbuildapi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.sonatype.plexus.build.incremental.BuildContext;


/**
 * Writes to the file only if content of the file is different. TODO. Current implementation defers actual writing to
 * the output file until invocation of {@link #close()} method. This results in missed/ignored IOExceptions in some
 * cases. First, {@link #flush()} method does not actually flush buffer to the disk. Second, any problems writing to the
 * file will be reported as IOException thrown by {@link #close()}, which are generally ignored.
 */
public class ChangedFileOutputStream extends OutputStream {

  private final File file;

  private final BuildContext buildContext;

  private final OutputStream os;

  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

  public ChangedFileOutputStream(File file) throws FileNotFoundException {
    this(file, null);
  }

  public ChangedFileOutputStream(File file, BuildContext buildContext) throws FileNotFoundException {
    this.file = file;
    this.buildContext = buildContext;
    this.os = new BufferedOutputStream(new FileOutputStream(file));
  }

  @Override
  public void write(int b) {
    buffer.write(b);
  }

  @Override
  public void write(byte[] b, int off, int len) {
    buffer.write(b, off, len);
  }

  @Override
  public void close() throws IOException {
    try (os) {
      writeIfNewOrChanged();
    }
  }

  protected void writeIfNewOrChanged() throws IOException {
    byte[] bytes = buffer.toByteArray();

    boolean needToWrite = false;

    // XXX harden
    if(file.exists()) {
      try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(file))) {
        for(byte element : bytes) {
          if(element != is.read()) {
            needToWrite = true;
            break;
          }
        }
      }
    } else {
      // file does not exist
      needToWrite = true;
    }

    if(needToWrite) {
      if(buildContext != null) {
        buildContext.refresh(file);
      }

      os.write(bytes);
    }
  }
}
