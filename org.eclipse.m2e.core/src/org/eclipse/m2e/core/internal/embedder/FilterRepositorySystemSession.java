/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

  @Override
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

  @Override
  protected DefaultRepositorySystemSession getSession() {
    return this.session;
  }
}
