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

import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;


/**
 * ArtifactTransferListenerAdapter
 * 
 * @author igor
 */
public class ArtifactTransferListenerAdapter extends AbstractTransferListenerAdapter implements TransferListener {

  ArtifactTransferListenerAdapter(MavenImpl maven, IProgressMonitor monitor) {
    super(maven, monitor);
  }

  public void transferInitiated(TransferEvent event) throws TransferCancelledException {
    try {
      transferInitiated(event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
    } catch(OperationCanceledException e) {
      throw new TransferCancelledException();
    }
  }

  public void transferProgressed(TransferEvent event) throws TransferCancelledException {
    long total = event.getResource().getContentLength();
    String artifactUrl = event.getResource().getRepositoryUrl() + event.getResource().getResourceName();

    try {
      transferProgress(artifactUrl, total, event.getDataBuffer().remaining());
    } catch(OperationCanceledException e) {
      throw new TransferCancelledException();
    }
  }

  public void transferStarted(TransferEvent event) {
    transferStarted(event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
  }

  public void transferCorrupted(TransferEvent event) {
  }

  public void transferSucceeded(TransferEvent event) {
    transferCompleted(event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
  }

  public void transferFailed(TransferEvent event) {
    transferCompleted(event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
  }

}
