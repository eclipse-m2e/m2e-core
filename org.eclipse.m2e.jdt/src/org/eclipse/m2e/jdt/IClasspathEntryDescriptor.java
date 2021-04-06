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

package org.eclipse.m2e.jdt;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathEntry;

import org.eclipse.m2e.core.embedder.ArtifactKey;


/**
 * Mutable version of IClasspathEntry with additional Maven specific attributes.
 *
 * @author igor
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IClasspathEntryDescriptor {

  // classpath entry getters and setters (open a bug if you need any of the missing getters/setters)

  IPath getPath();

  void setPath(IPath path);

  int getEntryKind();

  void setEntryKind(int entryKind);

  void setSourceAttachment(IPath srcPath, IPath srcRoot);

  void setJavadocUrl(String javaDocUrl);

  IPath getSourceAttachmentPath();

  IPath getSourceAttachmentRootPath();

  String getJavadocUrl();

  void setOutputLocation(IPath outputLocation);

  void addInclusionPattern(IPath pattern);

  void removeInclusionPattern(IPath pattern);

  void setInclusionPatterns(IPath[] inclusionPatterns);

  IPath[] getInclusionPatterns();

  void addExclusionPattern(IPath pattern);

  void removeExclusionPattern(IPath pattern);

  void setExclusionPatterns(IPath[] exclusionPatterns);

  IPath[] getExclusionPatterns();

  void setExported(boolean exported);

  boolean isExported();

  IPath getOutputLocation();

  void setClasspathAttribute(String name, String value);

  Map<String, String> getClasspathAttributes();

  void addAccessRule(IAccessRule rule);

  List<IAccessRule> getAccessRules();

  void setCombineAccessRules(boolean combineAccessRules);

  boolean combineAccessRules();

  // maven-specific getters and setters

  /**
   * Short for getArtifactKey().getGroupId(), with appropriate null check
   */
  String getGroupId();

  /**
   * Short for getArtifactKey().getArtifactId(), with appropriate null check
   */
  String getArtifactId();

  ArtifactKey getArtifactKey();

  void setArtifactKey(ArtifactKey artifactKey);

  /**
   * @return true if this entry corresponds to an optional maven dependency, false otherwise
   */
  boolean isOptionalDependency();

  void setOptionalDependency(boolean optional);

  String getScope();

  void setScope(String scope);

  //

  /**
   * Create IClasspathEntry with information collected in this descriptor
   */
  IClasspathEntry toClasspathEntry();

  /**
   * Returns <code>true</code> if this classpath entry was derived from pom.xml and <code>false</code> otherwise. <br/>
   * Stale derived entries are automatically removed when workspace project configuration is synchronized with pom.xml.
   *
   * @see #setPomDerived(boolean)
   * @since 1.1
   */
  boolean isPomDerived();

  /**
   * Marks classpath entry as derived from pom.xml (<code>true</code>) or not (<code>false</code>).
   * <p>
   * Not-derived (or custom) entries are preserved during project configuration update, while derived entries are
   * automatically removed whenever their corresponding pom.xml configuration is changed or removed.
   * <p>
   * All new classpath entries are marked as derived by default, however value of this flag is preserved when entry
   * descriptor is read from .classpath file. The intend is to make sure that custom classpath entries are not removed
   * automatically. Clients of IClasspathDescriptor API who prefer to manage cleanup of stale class classpath entries
   * explicitly may set derived flag to <code>false</code>.
   * <p>
   * Although not enforced, derived flag only applies to project 'raw' classpath entries. The flag is silently ignored
   * for classpath container entries.
   *
   * @since 1.1
   */
  void setPomDerived(boolean derived);
}
