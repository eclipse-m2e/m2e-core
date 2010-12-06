/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.index;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactScanningListener;
import org.apache.maven.index.ScanningResult;
import org.apache.maven.index.context.IndexingContext;

import org.eclipse.m2e.core.core.MavenConsole;

class ArtifactScanningMonitor implements ArtifactScanningListener {

  private static final long THRESHOLD = 1 * 1000L;

  //private final IndexInfo indexInfo;

  private final IProgressMonitor monitor;

  private final MavenConsole console;
  
  private long timestamp = System.currentTimeMillis();

  private File repositoryDir;

  ArtifactScanningMonitor(File repositoryDir, IProgressMonitor monitor, MavenConsole console) {
    //this.indexInfo = indexInfo;
    this.repositoryDir = repositoryDir;
    this.monitor = monitor;
    this.console = console;
  }

  public void scanningStarted(IndexingContext ctx) {
  }

  public void scanningFinished(IndexingContext ctx, ScanningResult result) {
  }

  public void artifactDiscovered(ArtifactContext ac) {
    long current = System.currentTimeMillis();
    if((current - timestamp) > THRESHOLD) {
      // String id = info.groupId + ":" + info.artifactId + ":" + info.version;
      String id = ac.getPom().getAbsolutePath().substring(
          this.repositoryDir.getAbsolutePath().length());
      this.monitor.setTaskName(id);
      this.timestamp = current;
    }
  }

  public void artifactError(ArtifactContext ac, Exception e) {
    String id = ac.getPom().getAbsolutePath().substring(repositoryDir.getAbsolutePath().length());
    console.logError(id + " " + e.getMessage()); //$NON-NLS-1$
  }
}