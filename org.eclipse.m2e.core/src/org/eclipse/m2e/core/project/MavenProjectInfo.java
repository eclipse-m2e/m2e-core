/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.project;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.maven.model.Model;


/**
 * @author Eugene Kuleshov
 */
public class MavenProjectInfo {
  private static final Logger log = LoggerFactory.getLogger(MavenProjectInfo.class);

  /**
   * Project basedir must NOT be renamed on filesystem.
   */
  public static final int RENAME_NO = 0;

  /**
   * Project basedir MUST be ranamed to match workspace project name.
   */
  public static final int RENAME_REQUIRED = 2;

  private final String label;

  private File pomFile;

  private Model model;

  private final MavenProjectInfo parent;

  /**
   * Map of MavenProjectInfo
   */
  private final Map<String, MavenProjectInfo> projects = new LinkedHashMap<>();

  private final Set<String> profiles = new HashSet<>();

  private int basedirRename = RENAME_NO;

  public MavenProjectInfo(String label, File pomFile, Model model, MavenProjectInfo parent) {
    this.label = label;
    this.pomFile = pomFile;
    this.model = model;
    this.parent = parent;
  }

  public void setPomFile(File pomFile) {
    File oldDir = this.pomFile.getParentFile();
    File newDir = pomFile.getParentFile();

    for(MavenProjectInfo projectInfo : projects.values()) {
      File childPom = projectInfo.getPomFile();
      if(isSubDir(oldDir, childPom.getParentFile())) {
        String oldPath = oldDir.getAbsolutePath();
        String path = childPom.getAbsolutePath().substring(oldPath.length());
        projectInfo.setPomFile(new File(newDir, path));
      }
    }

    this.pomFile = pomFile;
  }

  /** @deprecated use set/get BasedirRename */
  @Deprecated
  public void setNeedsRename(boolean needsRename) {
    setBasedirRename(needsRename ? RENAME_REQUIRED : RENAME_NO);
  }

  /** @deprecated use set/get BasedirRenamePolicy */
  @Deprecated
  public boolean isNeedsRename() {
    return getBasedirRename() == RENAME_REQUIRED;
  }

  /**
   * See {@link #RENAME_NO}, {@link #RENAME_REQUIRED}
   */
  public void setBasedirRename(int basedirRename) {
    this.basedirRename = basedirRename;
  }

  /**
   * See {@link #RENAME_NO}, {@link #RENAME_REQUIRED}
   */
  public int getBasedirRename() {
    return basedirRename;
  }

  private boolean isSubDir(File parentDir, File subDir) {
    if(parentDir.equals(subDir)) {
      return true;
    }

    if(subDir.getParentFile() != null) {
      return isSubDir(parentDir, subDir.getParentFile());
    }

    return false;
  }

  public void add(MavenProjectInfo info) {
    String key;
    try {
      if(info.getPomFile() == null) {
        // Is this possible?
        key = info.getLabel();
      } else {
        key = info.getPomFile().getCanonicalPath();
      }
    } catch(IOException ex) {
      throw new RuntimeException(ex);
    }
    MavenProjectInfo i = projects.get(key);
    if(i == null) {
      projects.put(key, info);
    } else {
      log.error("Project info " + this + " already has a child project info with key '" + key + "'"); //$NON-NLS-3$
      for(String string : info.getProfiles()) {
        i.addProfile(string);
      }
    }
  }

  public void addProfile(String profileId) {
    if(profileId != null) {
      this.profiles.add(profileId);
    }
  }

  public void addProfiles(Collection<String> profiles) {
    this.profiles.addAll(profiles);
  }

  public String getLabel() {
    return this.label;
  }

  public File getPomFile() {
    return this.pomFile;
  }

  public Model getModel() {
    return this.model;
  }

  public void setModel(Model model) {
    this.model = model;
  }

  public Collection<MavenProjectInfo> getProjects() {
    return this.projects.values();
  }

  public MavenProjectInfo getParent() {
    return this.parent;
  }

  public Set<String> getProfiles() {
    return this.profiles;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj instanceof MavenProjectInfo) {
      MavenProjectInfo info = (MavenProjectInfo) obj;
      if(pomFile == null) {
        return info.getPomFile() == null;
      }
      return pomFile.equals(info.getPomFile());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return pomFile == null ? 0 : pomFile.hashCode();
  }

  @Override
  public String toString() {
    return "'" + label + "'" + (pomFile == null ? "" : " " + pomFile.getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
  }
}
