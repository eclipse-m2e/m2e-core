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

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.repository.Repository;

/**
 * @author Eugene Kuleshov
 */
final class WagonTransferListenerAdapter extends AbstractTransferListenerAdapter implements TransferListener {

  WagonTransferListenerAdapter(MavenImpl maven, IProgressMonitor monitor) {
    super(maven, monitor);
  }

  public void transferInitiated(TransferEvent e) {
    // System.err.println( "init "+e.getWagon().getRepository()+"/"+e.getResource().getName());
    transferInitiated((String) null);
  }

  public void transferStarted(TransferEvent e) {
    StringBuilder sb = new StringBuilder();
    if(e.getWagon() != null && e.getWagon().getRepository() != null) {
      Wagon wagon = e.getWagon();
      Repository repository = wagon.getRepository();
      String repositoryId = repository.getId();
      sb.append(repositoryId).append(" : "); //$NON-NLS-1$
    }
    sb.append(e.getResource().getName());
    transferStarted(sb.toString());
  }

  public void transferProgress(TransferEvent e, byte[] buffer, int length) {
    long total = e.getResource().getContentLength();
    String artifactUrl = e.getWagon().getRepository() + "/" + e.getResource().getName(); //$NON-NLS-1$

    transferProgress(artifactUrl, total, length);
  }

  public void transferCompleted(TransferEvent e) {
    String artifactUrl = e.getWagon().getRepository() + "/" + e.getResource().getName(); //$NON-NLS-1$
    transferCompleted(artifactUrl);
  }

  public void transferError(TransferEvent e) {
    transferError(e.getWagon().getRepository() + "/" + e.getResource().getName(), e.getException()); //$NON-NLS-1$
  }

  public void debug(String message) {
    // System.err.println( "debug "+message);
  }

}
