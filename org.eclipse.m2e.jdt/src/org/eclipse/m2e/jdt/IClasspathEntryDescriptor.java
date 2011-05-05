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

package org.eclipse.m2e.jdt;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
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

  public void setClasspathAttribute(String name, String value);

  public IPath getPath();

  public int getEntryKind();

  public void setSourceAttachment(IPath srcPath, IPath srcRoot);

  public void setJavadocUrl(String javaDocUrl);

  public IPath getSourceAttachmentPath();

  public IPath getSourceAttachmentRootPath();

  public String getJavadocUrl();

  public void setOutputLocation(IPath outputLocation);

  public void setInclusionPatterns(IPath[] inclusionPatterns);

  public void addInclusionPattern(IPath pattern);
  
  public void setExclusionPatterns(IPath[] exclusionPatterns);

  public IPath[] getInclusionPatterns();

  public void addExclusionPattern(IPath pattern);

  public IPath[] getExclusionPatterns();

  public void setExported(boolean exported);

  public boolean isExported();

  public IPath getOutputLocation();

  public Map<String, String> getClasspathAttributes();

  public List<IAccessRule> getAccessRules();

  public void setCombineAccessRules(boolean combineAccessRules);

  public boolean combineAccessRules();


  // maven-specific getters and setters

  /**
   * Short for getArtifactKey().getGroupId(), with appropriate null check
   */
  public String getGroupId();

  public void addAccessRule(IAccessRule rule);

  /**
   * Short for getArtifactKey().getArtifactId(), with appropriate null check
   */
  public String getArtifactId();

  public ArtifactKey getArtifactKey();

  public void setArtifactKey(ArtifactKey artifactKey);

  /**
   * @return true if this entry corresponds to an optional maven dependency, false otherwise
   */
  public boolean isOptionalDependency();

  public void setOptionalDependency(boolean optional);

  public String getScope();

  public void setScope(String scope);

  //

  /**
   * Create IClasspathEntry with information collected in this descriptor
   */
  public IClasspathEntry toClasspathEntry();

  // deprecated methods, to be removed before 1.0

  /**
   * @deprecated use individual setter methods instead
   */
  public void setClasspathEntry(IClasspathEntry entry);

  /**
   * @deprecated use {@link #setClasspathAttribute(String, String)} instead
   */
  public void addClasspathAttribute(IClasspathAttribute attribute);

  /**
   * @deprecated use {@link #toClasspathEntry()} instead
   */
  public IClasspathEntry getClasspathEntry();

}
