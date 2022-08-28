/*******************************************************************************
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Takari, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @since 1.6
 */
class BuildResultCollector implements IIncrementalBuildFramework.BuildResultCollector {

  public static record Message(File file, int line, int column, String message, int severity, Throwable cause) {
    public Message {
      if(message == null && cause != null) {
        message = cause.getMessage();
      }
    }
  }

  private String currentParticipantId;

  /** Added, changed or removed resources */
  private final Set<File> refresh = new HashSet<>();

  /** Messages by build participant id */
  private final Map<String, List<Message>> messages = new LinkedHashMap<>();

  /** List of files to cleanup messages for by build participant id */
  private final Map<String, List<File>> removeMessages = new LinkedHashMap<>();

  @Override
  public Set<File> getFiles() {
    return refresh;
  }

  @Override
  public void refresh(File file) {
    refresh.add(file);
  }

  @Override
  public void addMessage(File file, int line, int column, String message, int severity, Throwable cause) {
    if(currentParticipantId == null) {
      throw new IllegalStateException("currentBuildParticipantId cannot be null or empty");
    }
    List<Message> messageList = messages.computeIfAbsent(currentParticipantId, i -> new ArrayList<>());
    messageList.add(new Message(file, line, column, message, severity, cause));
  }

  @Override
  public void removeMessages(File file) {
    if(currentParticipantId == null) {
      throw new IllegalStateException("currentBuildParticipantId cannot be null or empty");
    }
    List<File> files = removeMessages.computeIfAbsent(currentParticipantId, i -> new ArrayList<>());
    files.add(file);
  }

  public Map<String, List<Message>> getMessages() {
    return messages;
  }

  public Map<String, List<File>> getRemoveMessages() {
    return removeMessages;
  }

  public void setParticipantId(String participantId) {
    this.currentParticipantId = participantId;
  }

}
