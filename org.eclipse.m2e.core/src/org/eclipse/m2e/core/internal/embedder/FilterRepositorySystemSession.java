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
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.SessionData;
import org.eclipse.aether.transfer.TransferListener;


/**
 * A forwarding {@link RepositorySystemSession} implementation that allows overriding specific session attributes.
 * <p>
 * This class extends {@link org.eclipse.aether.AbstractForwardingRepositorySystemSession} to provide a filtered view
 * of a repository system session, allowing selective modification of session properties such as update policy,
 * transfer listener, and session data while delegating all other operations to the wrapped session.
 * </p>
 * <p>
 * For mutable properties like {@link TransferListener} and {@link SessionData}, this class attempts to forward
 * modifications to the underlying session if it is a {@link DefaultRepositorySystemSession}. If forwarding fails or
 * the underlying session is immutable, the values are stored locally and returned by the corresponding getter methods.
 * </p>
 */
class FilterRepositorySystemSession extends org.eclipse.aether.AbstractForwardingRepositorySystemSession {

  /**
   * The custom update policy to override the session's default update policy, or {@code null} to use the session's
   * policy.
   */
  private final String updatePolicy;

  /**
   * The underlying repository system session that this instance wraps and delegates to.
   */
  private final RepositorySystemSession session;

  /**
   * The custom transfer listener, or {@code null} if the session's transfer listener should be used.
   */
  private TransferListener transferListener;

  /**
   * The custom session data, or {@code null} if the session's data should be used.
   */
  private SessionData data;

  /**
   * Creates a new filtering repository system session.
   *
   * @param session the underlying repository system session to wrap
   * @param updatePolicy the custom update policy to use, or {@code null} to use the session's default policy
   */
  FilterRepositorySystemSession(RepositorySystemSession session, String updatePolicy) {
    this.session = session;
    this.updatePolicy = updatePolicy;
  }

  /**
   * Returns the update policy for this session.
   *
   * @return the custom update policy if set, otherwise the policy from the underlying session
   */
  @Override
  public String getUpdatePolicy() {
    return updatePolicy != null ? updatePolicy : super.getUpdatePolicy();
  }

  /**
   * Returns the transfer listener for this session.
   *
   * @return the custom transfer listener if set, otherwise the listener from the underlying session
   */
  @Override
  public TransferListener getTransferListener() {
    if(this.transferListener == null) {
      // No custom value stored, use the listener from the underlying session
      return session.getTransferListener();
    }
    // Return the custom listener that could not be forwarded to the underlying session
    return transferListener;
  }

  /**
   * Returns the session data for this session.
   *
   * @return the custom session data if set, otherwise the data from the underlying session
   */
  @Override
  public SessionData getData() {
    if(this.data == null) {
      // No custom value stored, use the data from the underlying session
      return session.getData();
    }
    // Return the custom data that could not be forwarded to the underlying session
    return this.data;
  }

  /**
   * Sets the transfer listener for this session.
   * <p>
   * This method attempts to forward the transfer listener to the underlying session if it is a
   * {@link DefaultRepositorySystemSession}. If the underlying session is immutable or the forwarding fails, the
   * listener is stored locally and will be returned by {@link #getTransferListener()}.
   * </p>
   *
   * @param transferListener the transfer listener to set
   * @return the previous transfer listener
   */
  TransferListener setTransferListener(TransferListener transferListener) {
    TransferListener origTransferListener = getTransferListener();
    if(session instanceof DefaultRepositorySystemSession def) {
      // Attempt to forward to the underlying session if mutable
      def.setTransferListener(transferListener);
    }
    if(transferListener == session.getTransferListener()) {
      // Forwarding was successful or the same value already exists in the session, clear local override
      this.transferListener = null;
    } else {
      // Forwarding failed or session is immutable, store locally to return from getter
      this.transferListener = transferListener;
    }
    return origTransferListener;
  }

  /**
   * Sets the session data for this session.
   * <p>
   * This method attempts to forward the session data to the underlying session if it is a
   * {@link DefaultRepositorySystemSession}. If the underlying session is immutable or the forwarding fails, the data
   * is stored locally and will be returned by {@link #getData()}.
   * </p>
   *
   * @param data the session data to set
   * @return the previous session data
   */
  SessionData setData(SessionData data) {
    SessionData origSessionData = getData();
    if(session instanceof DefaultRepositorySystemSession def) {
      // Attempt to forward to the underlying session if mutable
      def.setData(data);
    }
    if(data == session.getData()) {
      // Forwarding was successful or the same value already exists in the session, clear local override
      this.data = null;
    } else {
      // Forwarding failed or session is immutable, store locally to return from getter
      this.data = data;
    }
    return origSessionData;
  }

  /**
   * Returns the underlying repository system session that this instance wraps.
   *
   * @return the wrapped repository system session
   */
  @Override
  protected RepositorySystemSession getSession() {
    return this.session;
  }
}
