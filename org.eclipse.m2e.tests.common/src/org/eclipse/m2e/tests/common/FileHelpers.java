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

package org.eclipse.m2e.tests.common;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;


public class FileHelpers {

  public static void copyDir(File src, File dst) throws IOException {
    copyDir(src, dst, pathname -> !".svn".equals(pathname.getName()));
  }

  public static void copyDir(File src, File dst, FileFilter filter) throws IOException {
    copyDir(src, dst, filter, true);
  }

  private static void copyDir(File src, File dst, FileFilter filter, boolean deleteDst) throws IOException {
    if(!src.isDirectory()) {
      throw new IllegalArgumentException("Not a directory:" + src.getAbsolutePath());
    }
    if(deleteDst) {
      FileUtils.deleteDirectory(dst);
    }
    dst.mkdirs();
    File[] files = src.listFiles(filter);
    if(files != null) {
      for(File file : files) {
        if(file.canRead()) {
          File dstChild = new File(dst, file.getName());
          if(file.isDirectory()) {
            copyDir(file, dstChild, filter, false);
          } else {
            copyFile(file, dstChild);
          }
        }
      }
    }
  }

  private static void copyFile(File src, File dst) throws IOException {
    Files.copy(src.toPath(), dst.toPath());
  }

  public static void filterXmlFile(File src, File dst, Map<String, String> tokens) throws IOException {
    String text;

    try (Reader reader = ReaderFactory.newXmlReader(src)) {
      text = IOUtil.toString(reader);
    }

    for(Entry<String, String> entry : tokens.entrySet()) {
      text = text.replace(entry.getKey(), entry.getValue());
    }

    dst.getParentFile().mkdirs();
    try (Writer writer = WriterFactory.newXmlWriter(dst)) {
      writer.write(text);
    }
  }

  public static boolean deleteDirectory(File directory) {
    try {
      FileUtils.deleteDirectory(directory);
      return true;
    } catch(IOException e) {
      return false;
    }
  }

}
