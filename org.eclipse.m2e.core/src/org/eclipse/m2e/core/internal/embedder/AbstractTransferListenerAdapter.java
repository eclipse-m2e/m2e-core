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

package org.eclipse.m2e.core.internal.embedder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.wagon.WagonConstants;

import org.eclipse.m2e.core.internal.Messages;


/**
 * AbstractTransferListenerAdapter
 * 
 * @author igor
 */
abstract class AbstractTransferListenerAdapter {
  private static final Logger log = LoggerFactory.getLogger(AbstractTransferListenerAdapter.class);

  protected final MavenImpl maven;

  protected final IProgressMonitor monitor;

  //The same TransferListener monitors parallel downloads
  protected Map<String, Long> progressMap = new ConcurrentHashMap<String, Long>();

  private static final String[] units = {Messages.AbstractTransferListenerAdapter_byte,
      Messages.AbstractTransferListenerAdapter_kb, Messages.AbstractTransferListenerAdapter_mb};

  protected AbstractTransferListenerAdapter(MavenImpl maven, IProgressMonitor monitor) {
    this.maven = maven;
    this.monitor = monitor == null ? new NullProgressMonitor() : monitor;
  }

  protected void formatBytes(long n, StringBuilder sb) {
    int i = 0;
    while(n >= 1024 && ++i < units.length)
      n >>= 10;

    sb.append(n);
    sb.append(units[i]);
  }

  protected void transferInitiated(String artifactUrl) {
    if(monitor.isCanceled()) {
      throw new OperationCanceledException(Messages.AbstractTransferListenerAdapter_cancelled);
    }

    if(artifactUrl != null) {
      monitor.subTask(artifactUrl);
    }
  }

  protected void transferStarted(String artifactUrl) {
    log.info(NLS.bind("Downloading {0}", artifactUrl));
    // monitor.beginTask("0% "+e.getWagon().getRepository()+"/"+e.getResource().getName(), IProgressMonitor.UNKNOWN);
    monitor.subTask(NLS.bind(Messages.AbstractTransferListenerAdapter_4, artifactUrl));
  }

  protected void transferProgress(String artifactUrl, long total, int length) throws OperationCanceledException {
    if(monitor.isCanceled()) {
      throw new OperationCanceledException(Messages.AbstractTransferListenerAdapter_cancelled);
    }

    Long downloadProgress = progressMap.get(artifactUrl);
    long complete = downloadProgress == null ? 0L : downloadProgress.longValue();
    complete += length;
    progressMap.put(artifactUrl, complete);

    StringBuilder sb = new StringBuilder();

    formatBytes(complete, sb);
    if(total != WagonConstants.UNKNOWN_LENGTH) {
      sb.append('/');
      formatBytes(total, sb);
      if(total > 0) {
        sb.append(" (");
        sb.append(100l * complete / total);
        sb.append("%)");
      }
    }
    sb.append(' ');

    monitor.subTask(sb.toString() + artifactUrl);
  }

  protected void transferCompleted(String artifactUrl) {
    log.info(NLS.bind("Downloaded {0}", artifactUrl));

    // monitor.subTask("100% "+e.getWagon().getRepository()+"/"+e.getResource().getName());
    monitor.subTask(""); //$NON-NLS-1$
    progressMap.remove(artifactUrl);
  }

  protected void transferError(String artifactUrl, Exception exception) {
    log.error(NLS.bind("Unable to download {0} : {1}", artifactUrl, exception));
    monitor.subTask(NLS.bind(Messages.AbstractTransferListenerAdapter_subtask, artifactUrl));
    progressMap.remove(artifactUrl);
  }

}
