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

package org.eclipse.m2e.core.ui.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;


/**
 * @author Eugene Kuleshov
 */
public class MavenVersionDecorator implements ILabelDecorator {

  private final Map<ILabelProviderListener, IMavenProjectChangedListener> listeners = new HashMap<>();

  @Override
  public Image decorateImage(Image image, Object element) {
    return null;
  }

  @Override
  public String decorateText(String text, Object element) {
    if(element instanceof IResource) {
      IResource resource = (IResource) element;
      IProject project = resource.getProject();
      if(project != null) {
        IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
        IMavenProjectFacade facade = projectManager.create(project, new NullProgressMonitor());
        if(facade != null) {
          ArtifactKey mavenProject = facade.getArtifactKey();
          if(mavenProject != null) {
            String name = resource.getName();
            int start = text.indexOf(name);
            if(start > -1) {
              int n = text.indexOf(' ', start + name.length());
              if(n > -1) {
                return text.substring(0, n) + "  " + mavenProject.getVersion() + text.substring(n); //$NON-NLS-1$
              }
            }
            return text + "  " + mavenProject.getVersion(); //$NON-NLS-1$
          }
        }
      }
    }
    return null;
  }

  @Override
  public boolean isLabelProperty(Object element, String property) {
    return false;
  }

  @Override
  public void addListener(final ILabelProviderListener listener) {
    IMavenProjectChangedListener projectChangeListener = (events, monitor) -> {
      ArrayList<IResource> pomList = new ArrayList<>();
      for(MavenProjectChangedEvent event : events) {
        // pomList.add(events[i].getSource());
        if(event != null && event.getMavenProject() != null) {
          IFile pom = event.getMavenProject().getPom();
          pomList.add(pom);
          if(pom.getParent().getType() == IResource.PROJECT) {
            pomList.add(pom.getParent());
          }
        }
      }
      listener.labelProviderChanged(new LabelProviderChangedEvent(MavenVersionDecorator.this, pomList.toArray()));
    };

    listeners.put(listener, projectChangeListener);

    IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
    projectManager.addMavenProjectChangedListener(projectChangeListener);
  }

  @Override
  public void removeListener(ILabelProviderListener listener) {
    IMavenProjectChangedListener projectChangeListener = listeners.get(listener);
    if(projectChangeListener != null) {
      IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
      projectManager.removeMavenProjectChangedListener(projectChangeListener);
    }
  }

  @Override
  public void dispose() {
    // TODO remove all listeners
  }

}
