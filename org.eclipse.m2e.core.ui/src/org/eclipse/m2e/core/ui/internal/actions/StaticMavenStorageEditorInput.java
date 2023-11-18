/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.core.ui.internal.actions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;


/**
 * Storage editor input implementation for Maven poms, such as effective pom
 */
public class StaticMavenStorageEditorInput implements IStorageEditorInput {

  private final String name;

  private final String path;

  private final String tooltip;

  private final byte[] content;

  public StaticMavenStorageEditorInput(String name, String tooltip, String path, byte[] content) {
    this.name = name;
    this.path = path;
    this.tooltip = tooltip;
    this.content = content;
  }

  // IStorageEditorInput

  @Override
  public boolean exists() {
    return true;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public String getToolTipText() {
    return this.tooltip;
  }

  @Override
  public IStorage getStorage() {
    return new StaticContentStorage(name, path, content);
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return null;
  }

  @Override
  public IPersistableElement getPersistable() {
    return null;
  }

  @Override
  public <T> T getAdapter(Class<T> adapter) {
    return null;
  }

  public IPath getPath() {
    return path == null ? null : IPath.fromOSString(path);
  }

}

/**
 * Implementation of IStorage that only serve static/immutable content
 */
class StaticContentStorage implements IStorage {
  private final String name;

  private final String path;

  private final byte[] content;

  public StaticContentStorage(String name, String path, byte[] content) {
    this.name = name;
    this.path = path;
    this.content = content;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public IPath getFullPath() {
    return path == null ? null : IPath.fromOSString(path);
  }

  @Override
  public InputStream getContents() {
    return new ByteArrayInputStream(content);
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public <T> T getAdapter(Class<T> adapter) {
    return null;
  }
}
