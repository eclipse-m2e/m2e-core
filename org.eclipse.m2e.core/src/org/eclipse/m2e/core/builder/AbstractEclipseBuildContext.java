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

package org.eclipse.m2e.core.builder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.sonatype.plexus.build.incremental.BuildContext;


/**
 * @author igor
 */
public abstract class AbstractEclipseBuildContext implements BuildContext {

  public static class Message {
    public final File file;

    public final int line;

    public final int column;

    public final String message;

    public final Throwable cause;

    Message(File file, int line, int column, String message, Throwable cause) {
      this.file = file;
      this.line = line;
      this.column = column;
      if(message == null && cause != null) {
        message = cause.getMessage();
      }
      this.message = message;
      this.cause = cause;
    }
  }

  private final Set<File> refresh = new HashSet<File>();

  protected final Map<String, Object> context;

  private String currentBuildParticipantId;

  /** Error messages by build participant id */
  private final Map<String, List<Message>> errorMessages = new LinkedHashMap<String, List<Message>>();

  /** Warning messages by build participant id */
  private final Map<String, List<Message>> warningMessages = new LinkedHashMap<String, List<Message>>();

  /** List of files to cleanup error messages for build participant id */
  private final Map<String, List<File>> removeErrors = new LinkedHashMap<String, List<File>>();

  /** List of files to cleanup warning messages for build participant id */
  private final Map<String, List<File>> removeWarnings = new LinkedHashMap<String, List<File>>();

  protected AbstractEclipseBuildContext(Map<String, Object> context) {
    this.context = context;
  }

  public void refresh(File file) {
    refresh.add(file);
  }

  public Set<File> getFiles() {
    return refresh;
  }

  public OutputStream newFileOutputStream(File file) throws IOException {
    return new ChangedFileOutputStream(file, this);
  }

  /**
   * Returns path relative to delta resource location.
   */
  protected IPath getRelativePath(File file) {
    IPath basepath = getBaseResource().getLocation();
    IPath path = Path.fromOSString(file.getAbsolutePath());

    if(!basepath.isPrefixOf(path)) {
      return null;
    }

    return path.removeFirstSegments(basepath.segmentCount());
  }

  protected IResource getResource(File file) {
    IPath relpath = getRelativePath(file);
    if (relpath == null) {
      return null;
    }
    IResource baseResource = getBaseResource();
    if (baseResource instanceof IContainer) {
      return ((IContainer) baseResource).findMember(relpath);
    }
    return null;
  }

  protected abstract IResource getBaseResource();

  public void setValue(String key, Object value) {
    context.put(key, value);
  }

  public Object getValue(String key) {
    return context.get(key);
  }

  public void addError(File file, int line, int column, String message, Throwable cause) {
    if(currentBuildParticipantId == null) {
      throw new IllegalStateException("currentBuildParticipantId cannot be null or empty");
    }
    List<Message> messages = errorMessages.get(currentBuildParticipantId);
    if(messages == null) {
      messages = new ArrayList<Message>();
      errorMessages.put(currentBuildParticipantId, messages);
    }
    messages.add(new Message(file, line, column, message, cause));
  }

  public void addWarning(File file, int line, int column, String message, Throwable cause) {
    if(currentBuildParticipantId == null) {
      throw new IllegalStateException("currentBuildParticipantId cannot be null or empty");
    }
    List<Message> messages = warningMessages.get(currentBuildParticipantId);
    if(messages == null) {
      messages = new ArrayList<Message>();
      warningMessages.put(currentBuildParticipantId, messages);
    }
    messages.add(new Message(file, line, column, message, cause));
  }

  /* (non-Javadoc)
   * @see org.sonatype.plexus.build.incremental.BuildContext#removeErrors(java.io.File)
   */
  public void removeErrors(File file) {
    if(currentBuildParticipantId == null) {
      throw new IllegalStateException("currentBuildParticipantId cannot be null or empty");
    }
    List<File> files = removeErrors.get(currentBuildParticipantId);
    if(files == null) {
      files = new ArrayList<File>();
      removeErrors.put(currentBuildParticipantId, files);
    }
    files.add(file);
  }

  /* (non-Javadoc)
   * @see org.sonatype.plexus.build.incremental.BuildContext#removeWarnings(java.io.File)
   */
  public void removeWarnings(File file) {
    if(currentBuildParticipantId == null) {
      throw new IllegalStateException("currentBuildParticipantId cannot be null or empty");
    }
    List<File> files = removeWarnings.get(currentBuildParticipantId);
    if(files == null) {
      files = new ArrayList<File>();
      removeWarnings.put(currentBuildParticipantId, files);
    }
    files.add(file);
  }

  public Map<String, List<Message>> getErrors() {
    return errorMessages;
  }

  public Map<String, List<Message>> getWarnings() {
    return warningMessages;
  }

  public Map<String, List<File>> getRemoveErrors() {
    return removeErrors;
  }

  public Map<String, List<File>> getRemoveWarnings() {
    return removeWarnings;
  }

  public boolean isUptodate(File target, File source) {
    IResource targetResource = getResource(target);
    IResource sourceResource = getResource(source);
    return targetResource != null && targetResource.isAccessible() && !hasDelta(target)
        && sourceResource != null && sourceResource.isAccessible() && !hasDelta(source)
        && targetResource.getLocalTimeStamp() >= sourceResource.getLocalTimeStamp();
  }

  public void setCurrentBuildParticipantId(String currentBuildParticipantId) {
    this.currentBuildParticipantId = currentBuildParticipantId;
  }
}
