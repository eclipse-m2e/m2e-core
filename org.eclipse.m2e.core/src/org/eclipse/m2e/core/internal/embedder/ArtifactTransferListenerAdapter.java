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

import org.eclipse.core.runtime.IProgressMonitor;

import org.sonatype.aether.transfer.TransferEvent;
import org.sonatype.aether.transfer.TransferListener;

import org.eclipse.m2e.core.core.MavenConsole;

/**
 * ArtifactTransferListenerAdapter
 * 
 * @author igor
 */
public class ArtifactTransferListenerAdapter extends AbstractTransferListenerAdapter implements
    TransferListener {

  ArtifactTransferListenerAdapter(MavenImpl maven, IProgressMonitor monitor, MavenConsole console) {
    super(maven, monitor, console);
  }

  public void transferInitiated(TransferEvent event) {
    transferInitiated(event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
  }

  public void transferProgressed(TransferEvent event) {
    long total = event.getResource().getContentLength();
    String artifactUrl = event.getResource().getRepositoryUrl() + event.getResource().getResourceName();

    transferProgress(artifactUrl, total, event.getDataBuffer().remaining());
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
