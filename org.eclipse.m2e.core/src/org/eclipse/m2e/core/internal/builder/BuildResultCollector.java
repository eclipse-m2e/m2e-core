/*******************************************************************************
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

  private String currentParticipantId;

  public static class Message {
    public final File file;

    public final int line;

    public final int column;

    public final String message;

    public final int severity;

    public final Throwable cause;

    Message(File file, int line, int column, String message, int severity, Throwable cause) {
      this.file = file;
      this.line = line;
      this.column = column;
      if(message == null && cause != null) {
        message = cause.getMessage();
      }
      this.message = message;
      this.severity = severity;
      this.cause = cause;
    }
  }

  /** Added, changed or removed resources */
  private final Set<File> refresh = new HashSet<File>();

  /** Messages by build participant id */
  private final Map<String, List<Message>> messages = new LinkedHashMap<String, List<Message>>();

  /** List of files to cleanup messages for by build participant id */
  private final Map<String, List<File>> removeMessages = new LinkedHashMap<String, List<File>>();

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
    List<Message> messageList = messages.get(currentParticipantId);
    if(messageList == null) {
      messageList = new ArrayList<Message>();
      messages.put(currentParticipantId, messageList);
    }
    messageList.add(new Message(file, line, column, message, severity, cause));
  }

  @Override
  public void removeMessages(File file) {
    if(currentParticipantId == null) {
      throw new IllegalStateException("currentBuildParticipantId cannot be null or empty");
    }
    List<File> files = removeMessages.get(currentParticipantId);
    if(files == null) {
      files = new ArrayList<File>();
      removeMessages.put(currentParticipantId, files);
    }
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
