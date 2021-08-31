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

package org.eclipse.m2e.core.internal.index.nexus;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactScanningListener;
import org.apache.maven.index.ScanningResult;
import org.apache.maven.index.context.IndexingContext;


class ArtifactScanningMonitor implements ArtifactScanningListener {
  private static final Logger log = LoggerFactory.getLogger(ArtifactScanningMonitor.class);

  private static final long THRESHOLD = 1 * 1000L;

  //private final IndexInfo indexInfo;

  private final IProgressMonitor monitor;

  private long timestamp = System.currentTimeMillis();

  private final File repositoryDir;

  ArtifactScanningMonitor(File repositoryDir, IProgressMonitor monitor) {
    //this.indexInfo = indexInfo;
    this.repositoryDir = repositoryDir;
    this.monitor = monitor;
  }

  @Override
  public void scanningStarted(IndexingContext ctx) {
  }

  @Override
  public void scanningFinished(IndexingContext ctx, ScanningResult result) {
  }

  @Override
  public void artifactDiscovered(ArtifactContext ac) {
    long current = System.currentTimeMillis();
    if((current - timestamp) > THRESHOLD) {
      // String id = info.groupId + ":" + info.artifactId + ":" + info.version;
      String id = ac.getPom().getAbsolutePath().substring(this.repositoryDir.getAbsolutePath().length());
      this.monitor.setTaskName(id);
      this.timestamp = current;
    }
  }

  @Override
  public void artifactError(ArtifactContext ac, Exception e) {
    String id = ac.getPom().getAbsolutePath().substring(repositoryDir.getAbsolutePath().length());
    log.error(id + " " + e.getMessage()); //$NON-NLS-1$
  }
}
