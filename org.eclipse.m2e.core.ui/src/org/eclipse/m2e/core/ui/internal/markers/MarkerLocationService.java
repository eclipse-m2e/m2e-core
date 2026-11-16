/*******************************************************************************
 * Copyright (c) 2008-2026 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Rob Newton - added warning preferences page for disabling warnings
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.markers;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputLocationTracker;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.markers.IEditorMarkerService;
import org.eclipse.m2e.core.internal.markers.IMarkerLocationService;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.internal.preferences.ProblemSeverity;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.util.XmlUtils;


/**
 * a service impl used by the core module to improve marker locations and addition of our own markers
 *
 * @author mkleint
 * @since 1.16
 */
@SuppressWarnings("restriction")
public class MarkerLocationService implements IMarkerLocationService, IEditorMarkerService {
  private static final Logger log = LoggerFactory.getLogger(MarkerLocationService.class);

  private static final String OFFSET = "offset"; //$NON-NLS-1$

  public static final String ATTR_MANAGED_VERSION_LOCATION = "managedVersionLocation"; //$NON-NLS-1$

  @Override
  public void findLocationForMarker(final IMarker marker) {
    IFile resource = (IFile) marker.getResource();
    ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
    boolean connected = false;
    try {
      Integer lineNumber = (Integer) marker.getAttribute(IMarker.LINE_NUMBER);
      if(lineNumber == null) {
        return;
      }
      Integer columnStart = (Integer) marker.getAttribute(IMavenConstants.MARKER_COLUMN_START);
      if(columnStart == null) {
        return;
      }
      Integer columnEnd = (Integer) marker.getAttribute(IMavenConstants.MARKER_COLUMN_END);
      if(columnEnd == null) {
        return;
      }

      bufferManager.connect(resource.getFullPath(), LocationKind.IFILE, null);
      connected = true;
      ITextFileBuffer buffer = bufferManager.getTextFileBuffer(resource.getFullPath(), LocationKind.IFILE);
      if(buffer == null) {
        log.debug("Document is not a text file: " + resource);
        return;
      }
      IDocument document = buffer.getDocument();
      int charStart = document.getLineOffset(lineNumber - 1) + columnStart - 1;
      marker.setAttribute(IMarker.CHAR_START, charStart);
      int charEnd;
      if(columnEnd > columnStart) {
        charEnd = document.getLineOffset(lineNumber - 1) + columnEnd;
      } else {
        IRegion line = document.getLineInformation(lineNumber - 1);
        charEnd = line.getOffset() + line.getLength();
      }
      marker.setAttribute(IMarker.CHAR_END, charEnd);
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      if(connected) {
        try {
          bufferManager.disconnect(resource.getFullPath(), LocationKind.IFILE, null);
        } catch(CoreException e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }

  @Override
  public void addEditorHintMarkers(IMavenMarkerManager markerManager, IFile pom, MavenProject mavenProject,
      String type) {
    ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
    boolean connected = false;
    try {
      bufferManager.connect(pom.getFullPath(), LocationKind.IFILE, null);
      connected = true;
      ITextFileBuffer buffer = bufferManager.getTextFileBuffer(pom.getFullPath(), LocationKind.IFILE);
      if(buffer == null) {
        return;
      }
      // copy the content, so all checks see the same text even if the pom is being edited
      IDocument document = new Document(buffer.getDocument().get());
      Model model = PomHints.read(document);
      if(model == null) {
        return;
      }
      for(PomHints.Hint hint : PomHints.checkForSchema(document, model)) {
        IMarker marker = addMarker(markerManager, pom, type, hint);
        //the quick fix in the marker view needs to know the offset, since it doesn't have access to the
        //editor/source viewer
        if(marker != null) {
          marker.setAttribute(OFFSET, hint.start());
        }
      }
      for(PomHints.Hint hint : PomHints.checkVarious(document, model, mavenProject,
          getSeverity(MavenPreferenceConstants.P_DUP_OF_PARENT_GROUPID_PB),
          getSeverity(MavenPreferenceConstants.P_DUP_OF_PARENT_VERSION_PB),
          getSeverity(MavenPreferenceConstants.P_OVERRIDING_MANAGED_VERSION_PB))) {
        IMarker marker = addMarker(markerManager, pom, type, hint);
        if(marker != null && hint.artifactId() != null) {
          //add these attributes to easily and deterministically find the declaration in question
          marker.setAttribute("groupId", hint.groupId()); //$NON-NLS-1$
          marker.setAttribute("artifactId", hint.artifactId()); //$NON-NLS-1$
          setManagedVersionAttributes(marker, mavenProject, hint.managed());
          if(hint.profile() != null) {
            marker.setAttribute("profile", hint.profile()); //$NON-NLS-1$
          }
        }
      }
    } catch(Exception ex) {
      log.error("Error checking for warnings", ex); //$NON-NLS-1$
    } finally {
      if(connected) {
        try {
          bufferManager.disconnect(pom.getFullPath(), LocationKind.IFILE, null);
        } catch(CoreException e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }

  private static IMarker addMarker(IMavenMarkerManager markerManager, IFile pom, String type, PomHints.Hint hint)
      throws CoreException {
    IMarker marker = markerManager.addMarker(pom, type, hint.message(), hint.line(), hint.severity());
    if(marker != null) {
      marker.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, hint.editorHint());
      marker.setAttribute(IMarker.CHAR_START, hint.start());
      marker.setAttribute(IMarker.CHAR_END, hint.end());
      marker.setAttribute("problemType", "pomhint"); //only important in case we enable the generic xml quick fixes //$NON-NLS-1$ //$NON-NLS-2$
    }
    return marker;
  }

  private static void setManagedVersionAttributes(IMarker mark, MavenProject mavenproject,
      InputLocationTracker dependencyOrPlugin) throws CoreException {
    InputLocation loc = dependencyOrPlugin == null ? null : dependencyOrPlugin.getLocation("version");
    File file = loc == null ? null : XmlUtils.fileForInputLocation(loc, mavenproject);

    if(file != null) {
      mark.setAttribute(ATTR_MANAGED_VERSION_LOCATION, file.toURI().toString());
      int lineNumber = loc != null ? loc.getLineNumber() : -1;
      if(lineNumber > 0) {
        mark.setAttribute("managedVersionLine", lineNumber);
      }
      int columnNumber = loc != null ? loc.getColumnNumber() : -1;
      if(columnNumber > 0) {
        mark.setAttribute("managedVersionColumn", columnNumber);
      }
    }
  }

  private static ProblemSeverity getSeverity(String preference) {
    return ProblemSeverity.get(M2EUIPluginActivator.getDefault().getPreferenceStore().getString(preference));
  }

}
