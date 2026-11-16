/*******************************************************************************
 * Copyright (c) 2026 Aleksandar Kurtakov and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Aleksandar Kurtakov - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.markers;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputLocationTracker;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3ReaderEx;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.preferences.ProblemSeverity;


/**
 * Finds the pom.xml problems m2e reports as editor hints (missing schema, groupId/version duplicating the parent ones,
 * versions overriding managed ones). The pom is read with Maven's location tracking reader, the reported locations
 * (just after the start tag of an element) are widened to the whole element using the text of the pom.
 */
final class PomHints {

  private static final String XSI_SCHEMA_LOCATION = "xsi:schemaLocation"; //$NON-NLS-1$

  private static final String SUPER_POM_PREFIX = "org.apache.maven:maven-model-builder:"; //$NON-NLS-1$

  private static final String SUPER_POM_SUFFIX = ":super-pom"; //$NON-NLS-1$

  /**
   * A problem found in the pom.
   *
   * @param editorHint the {@link IMavenConstants#MARKER_ATTR_EDITOR_HINT} of the marker
   * @param line the 1-based line of the element
   * @param start the offset of the element start
   * @param end the offset after the element end
   * @param groupId the groupId of the dependency or plugin overriding a managed version, otherwise {@code null}
   * @param artifactId the artifactId of the dependency or plugin overriding a managed version, otherwise {@code null}
   * @param profile the profile containing the dependency or plugin overriding a managed version, or {@code null}
   * @param managed the managed dependency or plugin whose version is overridden, otherwise {@code null}
   */
  record Hint(String editorHint, String message, int severity, int line, int start, int end, String groupId,
      String artifactId, String profile, InputLocationTracker managed) {
  }

  private record Candidate(InputLocationTracker element, String key, String groupId, String artifactId,
      String version, String profile) {
  }

  private final IDocument document;

  private final String text;

  private final Model model;

  private final List<Hint> hints = new ArrayList<>();

  private PomHints(IDocument document, Model model) {
    this.document = document;
    this.text = document.get();
    this.model = model;
  }

  /**
   * Reads the pom.xml in the document, returns {@code null} if it can not be read.
   */
  static Model read(IDocument document) {
    try {
      return new MavenXpp3ReaderEx().read(new StringReader(document.get()), false, new InputSource());
    } catch(IOException | XmlPullParserException ex) {
      return null;
    }
  }

  /**
   * Reports a pom.xml whose project element does not declare the schema location.
   */
  static List<Hint> checkForSchema(IDocument document, Model model) {
    PomHints pomHints = new PomHints(document, model);
    pomHints.checkForSchema();
    return pomHints.hints;
  }

  /**
   * Reports a groupId or version duplicating the parent one with the given severities, and dependencies or plugins
   * overriding versions managed in the given project, if not {@code null}.
   */
  static List<Hint> checkVarious(IDocument document, Model model, MavenProject mavenProject,
      ProblemSeverity parentGroupIdSeverity, ProblemSeverity parentVersionSeverity,
      ProblemSeverity managedVersionSeverity) {
    PomHints pomHints = new PomHints(document, model);
    pomHints.checkParentMatchingGroupIdVersion(parentGroupIdSeverity, parentVersionSeverity);
    if(mavenProject != null && !ProblemSeverity.ignore.equals(managedVersionSeverity)) {
      pomHints.checkManagedDependencies(mavenProject, managedVersionSeverity);
      pomHints.checkManagedPlugins(mavenProject, managedVersionSeverity);
    }
    return pomHints.hints;
  }

  private void checkForSchema() {
    int tagEnd = offset(model.getLocation("")); //$NON-NLS-1$
    if(tagEnd < 0) {
      return;
    }
    int tagStart = text.lastIndexOf('<', tagEnd - 1);
    if(tagStart >= 0 && text.substring(tagStart, tagEnd).lastIndexOf(XSI_SCHEMA_LOCATION) == -1) {
      add(IMavenConstants.EDITOR_HINT_MISSING_SCHEMA, Messages.MavenMarkerManager_error_noschema,
          IMarker.SEVERITY_WARNING, tagStart, tagEnd);
    }
  }

  private void checkParentMatchingGroupIdVersion(ProblemSeverity groupIdSeverity, ProblemSeverity versionSeverity) {
    Parent parent = model.getParent();
    if(parent == null) {
      return;
    }
    if(!ProblemSeverity.ignore.equals(groupIdSeverity) && isSet(parent.getGroupId())
        && parent.getGroupId().equals(model.getGroupId())) {
      IRegion element = element(model, "groupId"); //$NON-NLS-1$
      if(element != null) {
        add(IMavenConstants.EDITOR_HINT_PARENT_GROUP_ID, Messages.MavenMarkerManager_duplicate_groupid,
            groupIdSeverity.getSeverity(), element.getOffset(), element.getOffset() + element.getLength());
      }
    }
    if(!ProblemSeverity.ignore.equals(versionSeverity) && isSet(parent.getVersion())
        && parent.getVersion().equals(model.getVersion())) {
      IRegion element = element(model, "version"); //$NON-NLS-1$
      if(element != null) {
        add(IMavenConstants.EDITOR_HINT_PARENT_VERSION, Messages.MavenMarkerManager_duplicate_version,
            versionSeverity.getSeverity(), element.getOffset(), element.getOffset() + element.getLength());
      }
    }
  }

  private void checkManagedDependencies(MavenProject mavenProject, ProblemSeverity severity) {
    List<Candidate> candidates = new ArrayList<>();
    addDependencies(candidates, model.getDependencies(), null);
    //we should also consider <dependencies> section in the profiles, but profile are optional and so is their
    // dependencyManagement section.. that makes handling our markers more complex.
    // see MavenProject.getInjectedProfileIds() for a list of currently active profiles in effective pom
    for(Profile profile : activeProfiles(mavenProject)) {
      addDependencies(candidates, profile.getDependencies(), profile.getId());
    }

    Map<String, InputLocationTracker> managed = new HashMap<>();
    DependencyManagement dm = mavenProject.getDependencyManagement();
    if(dm != null) {
      for(Dependency dep : dm.getDependencies()) {
        if(dep.getVersion() != null) { //#335366
          //355882 use dep.getManagementKey() to prevent false positives
          //when type or classifier doesn't match
          managed.put(dep.getManagementKey(), dep);
        }
      }
    }
    addManagedVersionHints(candidates, managed, IMavenConstants.EDITOR_HINT_MANAGED_DEPENDENCY_OVERRIDE, severity);
  }

  private static void addDependencies(List<Candidate> candidates, List<Dependency> dependencies, String profile) {
    for(Dependency dep : dependencies) {
      if(isSet(dep.getGroupId()) && isSet(dep.getArtifactId()) && isSet(dep.getVersion())) {
        candidates.add(new Candidate(dep, dep.getManagementKey(), dep.getGroupId(), dep.getArtifactId(),
            dep.getVersion(), profile));
      }
    }
  }

  private void checkManagedPlugins(MavenProject mavenProject, ProblemSeverity severity) {
    List<Candidate> candidates = new ArrayList<>();
    addPlugins(candidates, model.getBuild(), null);
    //we should also consider <plugins> section in the profiles, but profile are optional and so is their
    // pluginManagement section.. that makes handling our markers more complex.
    // see MavenProject.getInjectedProfileIds() for a list of currently active profiles in effective pom
    for(Profile profile : activeProfiles(mavenProject)) {
      addPlugins(candidates, profile.getBuild(), profile.getId());
    }

    Map<String, InputLocationTracker> managed = new HashMap<>();
    PluginManagement pm = mavenProject.getPluginManagement();
    if(pm != null) {
      for(Plugin plg : pm.getPlugins()) {
        InputLocation loc = plg.getLocation("version"); //$NON-NLS-1$
        //#350203 skip plugins defined in the superpom
        String modelID = loc == null || loc.getSource() == null ? null : loc.getSource().getModelId();
        if(loc != null
            && (modelID == null || !(modelID.startsWith(SUPER_POM_PREFIX) && modelID.endsWith(SUPER_POM_SUFFIX)))) {
          managed.put(plg.getKey(), plg);
        }
      }
    }
    addManagedVersionHints(candidates, managed, IMavenConstants.EDITOR_HINT_MANAGED_PLUGIN_OVERRIDE, severity);
  }

  private static void addPlugins(List<Candidate> candidates, BuildBase build, String profile) {
    if(build == null) {
      return;
    }
    for(Plugin plugin : build.getPlugins()) {
      if(isSet(plugin.getArtifactId()) && isSet(plugin.getVersion())) {
        candidates.add(new Candidate(plugin, plugin.getKey(), plugin.getGroupId(), plugin.getArtifactId(),
            plugin.getVersion(), profile));
      }
    }
  }

  private List<Profile> activeProfiles(MavenProject mavenProject) {
    String currentProjectKey = mavenProject.getGroupId() + ":" + mavenProject.getArtifactId() + ":" //$NON-NLS-1$//$NON-NLS-2$
        + mavenProject.getVersion();
    List<String> activeProfileIds = mavenProject.getInjectedProfileIds().get(currentProjectKey);
    if(activeProfileIds == null) {
      return Collections.emptyList();
    }
    return model.getProfiles().stream().filter(p -> p.getId() != null && activeProfileIds.contains(p.getId()))
        .toList();
  }

  private void addManagedVersionHints(List<Candidate> candidates, Map<String, InputLocationTracker> managed,
      String editorHint, ProblemSeverity severity) {
    for(Candidate candidate : candidates) {
      if(!managed.containsKey(candidate.key())) {
        continue;
      }
      IRegion version = element(candidate.element(), "version"); //$NON-NLS-1$
      if(version == null || isIgnored(version, IMavenConstants.MARKER_IGNORE_MANAGED)) {
        continue;
      }
      InputLocationTracker managedElement = managed.get(candidate.key());
      String managedVersion = managedElement instanceof Dependency dep ? dep.getVersion()
          : managedElement instanceof Plugin plugin ? plugin.getVersion() : null;
      String msg = candidate.version().equals(managedVersion) ? Messages.MavenMarkerManager_redundant_managed_title
          : Messages.MavenMarkerManager_managed_title;
      hints.add(new Hint(editorHint, NLS.bind(msg, managedVersion, candidate.artifactId()), severity.getSeverity(),
          line(version.getOffset()), version.getOffset(), version.getOffset() + version.getLength(),
          candidate.groupId(), candidate.artifactId(), candidate.profile(), managedElement));
    }
  }

  /**
   * Checks whether a comment containing the given string starts on the line the element ends on, after the element.
   * Comment-like text in CDATA sections and processing instructions is skipped.
   */
  private boolean isIgnored(IRegion element, String ignoreString) {
    try {
      int from = element.getOffset() + element.getLength();
      IRegion line = document.getLineInformationOfOffset(from);
      int to = line.getOffset() + line.getLength();
      for(int offset = text.indexOf('<', from, to); offset >= 0; offset = text.indexOf('<', offset, to)) {
        if(text.startsWith("<!--", offset)) { //$NON-NLS-1$
          int commentEnd = text.indexOf("-->", offset + 4); //$NON-NLS-1$
          if(commentEnd < 0) {
            return false;
          }
          if(text.substring(offset + 4, commentEnd).contains(ignoreString)) {
            return true;
          }
          offset = commentEnd + 3;
        } else if(text.startsWith("<![CDATA[", offset)) { //$NON-NLS-1$
          offset = endOf("]]>", offset + 9); //$NON-NLS-1$
        } else if(text.startsWith("<?", offset)) { //$NON-NLS-1$
          offset = endOf("?>", offset + 2); //$NON-NLS-1$
        } else {
          offset++ ;
        }
        if(offset < 0 || offset >= to) {
          return false;
        }
      }
    } catch(BadLocationException ex) {
      // the offset comes from the document
    }
    return false;
  }

  /**
   * Returns the region of the given child element, from the start of its start tag to the end of its end tag, or
   * {@code null} if it is not in the pom text or empty.
   */
  private IRegion element(InputLocationTracker parent, String name) {
    int contentStart = offset(parent.getLocation(name));
    if(contentStart < 2 || text.charAt(contentStart - 1) != '>' || text.charAt(contentStart - 2) == '/') {
      return null;
    }
    int start = text.lastIndexOf('<', contentStart - 1);
    int endTag = findEndTag(name, contentStart);
    int end = endTag < 0 ? -1 : text.indexOf('>', endTag);
    if(start < 0 || end < 0) {
      return null;
    }
    return new Region(start, end + 1 - start);
  }

  /**
   * Returns the offset of the end tag of the element with the given name and content starting at the given offset,
   * skipping comments, CDATA sections and processing instructions, or -1.
   */
  private int findEndTag(String name, int contentStart) {
    String endTag = "</" + name; //$NON-NLS-1$
    int offset = text.indexOf('<', contentStart);
    while(offset >= 0) {
      int skipTo;
      if(text.startsWith("<!--", offset)) { //$NON-NLS-1$
        skipTo = endOf("-->", offset + 4); //$NON-NLS-1$
      } else if(text.startsWith("<![CDATA[", offset)) { //$NON-NLS-1$
        skipTo = endOf("]]>", offset + 9); //$NON-NLS-1$
      } else if(text.startsWith("<?", offset)) { //$NON-NLS-1$
        skipTo = endOf("?>", offset + 2); //$NON-NLS-1$
      } else if(text.startsWith(endTag, offset) && offset + endTag.length() < text.length()
          && (text.charAt(offset + endTag.length()) == '>'
              || Character.isWhitespace(text.charAt(offset + endTag.length())))) {
        return offset;
      } else {
        skipTo = offset + 1;
      }
      if(skipTo < 0) {
        return -1;
      }
      offset = text.indexOf('<', skipTo);
    }
    return -1;
  }

  private int endOf(String delimiter, int from) {
    int index = text.indexOf(delimiter, from);
    return index < 0 ? -1 : index + delimiter.length();
  }

  /**
   * Returns the offset of the given location in the document, or -1.
   */
  private int offset(InputLocation location) {
    if(location == null || location.getLineNumber() < 1 || location.getColumnNumber() < 1) {
      return -1;
    }
    try {
      int offset = document.getLineOffset(location.getLineNumber() - 1) + location.getColumnNumber() - 1;
      return offset <= text.length() ? offset : -1;
    } catch(BadLocationException ex) {
      return -1;
    }
  }

  private int line(int offset) {
    try {
      return document.getLineOfOffset(offset) + 1;
    } catch(BadLocationException ex) {
      return -1;
    }
  }

  private void add(String editorHint, String message, int severity, int start, int end) {
    hints.add(new Hint(editorHint, message, severity, line(start), start, end, null, null, null, null));
  }

  private static boolean isSet(String value) {
    return value != null && !value.isEmpty();
  }
}
