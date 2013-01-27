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

import org.sonatype.aether.SessionData;
import org.sonatype.aether.transfer.TransferListener;
import org.sonatype.aether.util.DefaultRepositorySystemSession;


/**
 * FilterRepositorySystemSession implementation that allows setting of some/relevant session attributes.
 * 
 * @since 1.4
 */
class FilterRepositorySystemSession extends org.sonatype.aether.util.FilterRepositorySystemSession {

  private final String updatePolicy;

  public FilterRepositorySystemSession(DefaultRepositorySystemSession session, String updatePolicy) {
    super(session);
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

  private DefaultRepositorySystemSession getSession() {
    return (DefaultRepositorySystemSession) this.session;
  }
}
