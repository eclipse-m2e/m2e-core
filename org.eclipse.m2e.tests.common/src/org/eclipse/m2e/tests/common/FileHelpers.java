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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

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
    BufferedInputStream in = new BufferedInputStream(new FileInputStream(src));
    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dst));

    byte[] buf = new byte[10240];
    int len;
    while((len = in.read(buf)) != -1) {
      out.write(buf, 0, len);
    }

    out.close();
    in.close();
  }

  public static void filterXmlFile(File src, File dst, Map<String, String> tokens) throws IOException {
    String text;

    Reader reader = ReaderFactory.newXmlReader(src);
    try {
      text = IOUtil.toString(reader);
    } finally {
      reader.close();
    }

    for(String token : tokens.keySet()) {
      text = text.replace(token, tokens.get(token));
    }

    dst.getParentFile().mkdirs();
    Writer writer = WriterFactory.newXmlWriter(dst);
    try {
      writer.write(text);
    } finally {
      writer.close();
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
