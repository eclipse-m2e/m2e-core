/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.embedder;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.SessionData;
import org.eclipse.aether.transfer.TransferListener;


/**
 * FilterRepositorySystemSession implementation that allows setting of some/relevant session attributes.
 * 
 * @since 1.4
 */
class FilterRepositorySystemSession extends org.eclipse.aether.AbstractForwardingRepositorySystemSession {

  private final String updatePolicy;

  private final DefaultRepositorySystemSession session;

  public FilterRepositorySystemSession(DefaultRepositorySystemSession session, String updatePolicy) {
    this.session = session;
    this.updatePolicy = updatePolicy;
  }

  public String getUpdatePolicy() {
    return updatePolicy != null ? updatePolicy : super.getUpdatePolicy();
  }

  public TransferListener setTransferListener(TransferListener transferListener) {
    DefaultRepositorySystemSession session = getSession();
    TransferListener origTransferListener = session.getTransferListener();
    session.setTransferListener(transferListener);
    return origTransferListener;
  }

  public SessionData setData(SessionData data) {
    DefaultRepositorySystemSession session = getSession();
    SessionData origSessionData = session.getData();
    session.setData(data);
    return origSessionData;
  }

  protected DefaultRepositorySystemSession getSession() {
    return this.session;
  }
}
