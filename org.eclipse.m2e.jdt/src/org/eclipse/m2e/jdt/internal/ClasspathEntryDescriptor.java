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

package org.eclipse.m2e.jdt.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
import org.eclipse.m2e.jdt.IClasspathManager;


/**
 * ClasspathEntryDescriptor
 *
 * @author igor
 */
public class ClasspathEntryDescriptor implements IClasspathEntryDescriptor {

  private int entryKind;

  private IPath path;

  private boolean exported;

  private IPath outputLocation;

  private List<IAccessRule> accessRules = new ArrayList<>();

  private LinkedHashMap<String, String> attributes = new LinkedHashMap<>();

  private IPath sourceAttachmentPath;

  private IPath sourceAttachmentRootPath;

  private LinkedHashSet<IPath> inclusionPatterns;

  private LinkedHashSet<IPath> exclusionPatterns;

  private boolean combineAccessRules;

  // maven specific attributes below

  private ArtifactKey artifactKey;

  private String scope;

  private boolean optionalDependency;

  public ClasspathEntryDescriptor(int entryKind, IPath path) {
    this.entryKind = entryKind;
    this.path = path;
  }

  public ClasspathEntryDescriptor(IClasspathEntry entry) {
    setClasspathEntry(entry);
  }

  @Override
  public IClasspathEntry toClasspathEntry() {
    Map<String, String> attributes = new LinkedHashMap<>(this.attributes);

    if(artifactKey != null) {
      attributes.put(IClasspathManager.GROUP_ID_ATTRIBUTE, artifactKey.groupId());
      attributes.put(IClasspathManager.ARTIFACT_ID_ATTRIBUTE, artifactKey.artifactId());
      attributes.put(IClasspathManager.VERSION_ATTRIBUTE, artifactKey.version());
      if(artifactKey.classifier() != null) {
        attributes.put(IClasspathManager.CLASSIFIER_ATTRIBUTE, artifactKey.classifier());
      }
    }
    if(scope != null) {
      attributes.put(IClasspathManager.SCOPE_ATTRIBUTE, scope);
    }
    if(optionalDependency) {
      attributes.put(IClasspathManager.OPTIONALDEPENDENCY_ATTRIBUTE, Boolean.toString(true));
    }

    IClasspathAttribute[] attributesArray = new IClasspathAttribute[attributes.size()];
    int attributeIndex = 0;
    for(Map.Entry<String, String> attribute : attributes.entrySet()) {
      attributesArray[attributeIndex++ ] = JavaCore.newClasspathAttribute(attribute.getKey(), attribute.getValue());
    }

    IAccessRule[] accessRulesArray = accessRules.toArray(new IAccessRule[accessRules.size()]);
    return switch(entryKind) {
      case IClasspathEntry.CPE_CONTAINER -> JavaCore.newContainerEntry(path, //
          accessRulesArray, //
          attributesArray, //
          exported);
      case IClasspathEntry.CPE_LIBRARY -> JavaCore.newLibraryEntry(path, //
          sourceAttachmentPath, //
          sourceAttachmentRootPath, //
          accessRulesArray, //
          attributesArray, //
          exported);
      case IClasspathEntry.CPE_SOURCE -> JavaCore.newSourceEntry(path, //
          getInclusionPatterns(), //
          getExclusionPatterns(), //
          outputLocation, //
          attributesArray);
      case IClasspathEntry.CPE_PROJECT -> JavaCore.newProjectEntry(path, //
          accessRulesArray, //
          combineAccessRules, //
          attributesArray, //
          exported);
      case IClasspathEntry.CPE_VARIABLE -> JavaCore.newVariableEntry(path, //
          sourceAttachmentPath, //
          sourceAttachmentRootPath, //
          accessRulesArray, //
          attributesArray, //
          exported);
      default -> throw new IllegalArgumentException("Unsupported IClasspathEntry kind=" + entryKind); //$NON-NLS-1$
    };
  }

  @Override
  public String getScope() {
    return scope;
  }

  /**
   * @return true if this entry corresponds to an optional maven dependency, false otherwise
   */
  @Override
  public boolean isOptionalDependency() {
    return optionalDependency;
  }

  @Override
  public void setClasspathAttribute(String name, String value) {
    if(name == null) {
      throw new NullPointerException(); // fail fast
    }
    if(value != null) {
      attributes.put(name, value);
    } else {
      attributes.remove(name);
    }
  }

  @Override
  public Map<String, String> getClasspathAttributes() {
    return attributes;
  }

  @Override
  public String getGroupId() {
    return artifactKey != null ? artifactKey.groupId() : null;
  }

  private void setClasspathEntry(IClasspathEntry entry) {
    this.entryKind = entry.getEntryKind();
    this.path = entry.getPath();
    this.exported = entry.isExported();
    this.outputLocation = entry.getOutputLocation();

    this.accessRules = new ArrayList<>();
    Collections.addAll(this.accessRules, entry.getAccessRules());

    this.attributes = new LinkedHashMap<>();
    for(IClasspathAttribute attribute : entry.getExtraAttributes()) {
      attributes.put(attribute.getName(), attribute.getValue());
    }

    this.sourceAttachmentPath = entry.getSourceAttachmentPath();
    this.sourceAttachmentRootPath = entry.getSourceAttachmentRootPath();
    setInclusionPatterns(entry.getInclusionPatterns());
    setExclusionPatterns(entry.getExclusionPatterns());
    this.combineAccessRules = entry.combineAccessRules();

    String groupId = attributes.get(IClasspathManager.GROUP_ID_ATTRIBUTE);
    String artifactId = attributes.get(IClasspathManager.ARTIFACT_ID_ATTRIBUTE);
    String version = attributes.get(IClasspathManager.VERSION_ATTRIBUTE);
    String classifier = attributes.get(IClasspathManager.CLASSIFIER_ATTRIBUTE);
    if(groupId != null && artifactId != null && version != null) {
      this.artifactKey = new ArtifactKey(groupId, artifactId, version, classifier);
    }
  }

  @Override
  public String getArtifactId() {
    return artifactKey != null ? artifactKey.artifactId() : null;
  }

  @Override
  public IPath getPath() {
    return path;
  }

  @Override
  public void setPath(IPath path) {
    if(path == null) {
      throw new NullPointerException();
    }
    this.path = path;
  }

  @Override
  public int getEntryKind() {
    return entryKind;
  }

  @Override
  public void setEntryKind(int entryKind) {
    this.entryKind = entryKind;
  }

  @Override
  public ArtifactKey getArtifactKey() {
    return artifactKey;
  }

  @Override
  public void setArtifactKey(ArtifactKey artifactKey) {
    this.artifactKey = artifactKey;
  }

  @Override
  public void setSourceAttachment(IPath srcPath, IPath srcRoot) {
    this.sourceAttachmentPath = srcPath;
    this.sourceAttachmentRootPath = srcRoot;
  }

  @Override
  public void setJavadocUrl(String javaDocUrl) {
    setClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, javaDocUrl);
  }

  @Override
  public IPath getSourceAttachmentPath() {
    return sourceAttachmentPath;
  }

  @Override
  public IPath getSourceAttachmentRootPath() {
    return sourceAttachmentRootPath;
  }

  @Override
  public String getJavadocUrl() {
    return attributes.get(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME);
  }

  @Override
  public void setScope(String scope) {
    this.scope = scope;
  }

  @Override
  public void setOptionalDependency(boolean optional) {
    this.optionalDependency = optional;
  }

  @Override
  public void addAccessRule(IAccessRule rule) {
    this.accessRules.add(rule);
  }

  @Override
  public List<IAccessRule> getAccessRules() {
    return accessRules;
  }

  @Override
  public void setOutputLocation(IPath outputLocation) {
    this.outputLocation = outputLocation;
  }

  @Override
  public IPath getOutputLocation() {
    return outputLocation;
  }

  @Override
  public void setInclusionPatterns(IPath[] inclusionPatterns) {
    if(inclusionPatterns != null) {
      this.inclusionPatterns = new LinkedHashSet<>(Arrays.asList(inclusionPatterns));
    } else {
      this.inclusionPatterns = null;
    }
  }

  @Override
  public void addInclusionPattern(IPath pattern) {
    if(inclusionPatterns == null) {
      inclusionPatterns = new LinkedHashSet<>();
    }
    inclusionPatterns.add(pattern);
  }

  @Override
  public void removeInclusionPattern(IPath pattern) {
    if(inclusionPatterns != null) {
      inclusionPatterns.remove(pattern);
    }
  }

  @Override
  public IPath[] getInclusionPatterns() {
    return inclusionPatterns != null ? inclusionPatterns.toArray(new IPath[inclusionPatterns.size()]) : null;
  }

  @Override
  public void setExclusionPatterns(IPath[] exclusionPatterns) {
    if(exclusionPatterns != null) {
      this.exclusionPatterns = new LinkedHashSet<>(Arrays.asList(exclusionPatterns));
    } else {
      this.exclusionPatterns = null;
    }
  }

  @Override
  public void addExclusionPattern(IPath pattern) {
    if(exclusionPatterns == null) {
      exclusionPatterns = new LinkedHashSet<>();
    }
    exclusionPatterns.add(pattern);
  }

  @Override
  public void removeExclusionPattern(IPath pattern) {
    if(exclusionPatterns != null) {
      exclusionPatterns.remove(pattern);
    }
  }

  @Override
  public IPath[] getExclusionPatterns() {
    return exclusionPatterns != null ? exclusionPatterns.toArray(new IPath[exclusionPatterns.size()]) : null;
  }

  @Override
  public void setExported(boolean exported) {
    this.exported = exported;
  }

  @Override
  public boolean isExported() {
    return exported;
  }

  @Override
  public void setCombineAccessRules(boolean combineAccessRules) {
    this.combineAccessRules = combineAccessRules;
  }

  @Override
  public boolean combineAccessRules() {
    return combineAccessRules;
  }

  @Override
  public boolean isPomDerived() {
    return Boolean.parseBoolean(attributes.get(IClasspathManager.POMDERIVED_ATTRIBUTE));
  }

  @Override
  public void setPomDerived(boolean derived) {
    if(derived) {
      attributes.put(IClasspathManager.POMDERIVED_ATTRIBUTE, Boolean.toString(true));
    } else {
      attributes.remove(IClasspathManager.POMDERIVED_ATTRIBUTE);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "@" + System.identityHashCode(this) + "{path=" + path + "}";
  }
}
