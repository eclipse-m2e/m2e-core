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

package org.eclipse.m2e.editor.composites;

import java.util.Collection;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.MavenEditorPlugin;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.Exclusion;
import org.eclipse.m2e.model.edit.pom.Extension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;


/**
 * Label provider for Dependency, Exclusion and Extension elements
 * 
 * @author Eugene Kuleshov
 */
public class DependencyLabelProvider extends LabelProvider implements IColorProvider {

  private MavenPomEditor pomEditor;

  private boolean showGroupId = false;

  public void setPomEditor(MavenPomEditor pomEditor) {
    this.pomEditor = pomEditor;
  }
  
  public void setShowGroupId(boolean showGroupId) {
    this.showGroupId = showGroupId;
  }

  // IColorProvider
  
  public Color getForeground(Object element) {
    if (element instanceof org.apache.maven.model.Dependency) {
      //mkleint: let's just assume all maven Dependency instances are inherited
      return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
    }
    return null;
  }
  
  public Color getBackground(Object element) {
    return null;
  }

  // LabelProvider
  
  @Override
  public String getText(Object element) {
    if(element instanceof Dependency) {
      Dependency dependency = (Dependency) element;
      return getText(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), //
          dependency.getClassifier(), dependency.getType(), dependency.getScope());
    } else if (element instanceof org.apache.maven.model.Dependency) {
      org.apache.maven.model.Dependency dependency = (org.apache.maven.model.Dependency) element;
      return getText(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(),
          dependency.getClassifier(), dependency.getType(), dependency.getScope());
    } else if(element instanceof Exclusion) {
      Exclusion exclusion = (Exclusion) element;
      return getText(exclusion.getGroupId(), exclusion.getArtifactId(), null, null, null, null);
    } else if(element instanceof Extension) {
      Extension extension = (Extension) element;
      return getText(extension.getGroupId(), extension.getArtifactId(), extension.getVersion(), null, null, null);
    }
    return super.getText(element);
  }

  @Override
  public Image getImage(Object element) {
    if(element instanceof Dependency) {
      Dependency dependency = (Dependency) element;
      return getImage(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
    } else if (element instanceof org.apache.maven.model.Dependency) {
      //mkleint: all MavenDependency instances are inherited
      return MavenEditorImages.IMG_INHERITED;
    }else if(element instanceof Exclusion) {
      Exclusion exclusion = (Exclusion) element;
      return getImage(exclusion.getGroupId(), exclusion.getArtifactId(), null);
    } else if(element instanceof Extension) {
      Extension extension = (Extension) element;
      return getImage(extension.getGroupId(), extension.getArtifactId(), extension.getVersion());
    }

    return null;
  }

  private Image getImage(String groupId, String artifactId, String version) {
    // XXX need to resolve actual dependencies (i.e. inheritance, dependency management or properties)
    // XXX need to handle version ranges
    
    if((version == null || version.indexOf("${") > -1) && pomEditor != null) { //$NON-NLS-1$
        MavenProject mavenProject = pomEditor.getMavenProject();
        if(mavenProject != null) {
          Artifact artifact = mavenProject.getArtifactMap().get(groupId + ":" + artifactId); //$NON-NLS-1$
          if(artifact!=null) {
            version = artifact.getVersion();
          }
          if(version==null || version.indexOf("${") > -1) { //$NON-NLS-1$
            Collection<Artifact> artifacts = mavenProject.getManagedVersionMap().values();
            for(Artifact a : artifacts) {
              if(a.getGroupId().equals(groupId) && a.getArtifactId().equals(artifactId)) {
                version = a.getVersion();
                break;
              }
            }
          }
        }
    }
    
    if(groupId != null && artifactId != null && version != null) {
      MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
      IMavenProjectFacade projectFacade = projectManager.getMavenProject(groupId, artifactId, version);
      if(projectFacade != null) {
        return MavenEditorImages.IMG_PROJECT;
      }
    } 
    return MavenEditorImages.IMG_JAR;
  }

  private String getText(String groupId, String artifactId, String version, String classifier, String type, String scope) {
    StringBuilder sb = new StringBuilder();

    if(showGroupId) {
      sb.append(isEmpty(groupId) ? "?" : groupId).append(" : "); //$NON-NLS-1$ //$NON-NLS-2$
    }
    sb.append(isEmpty(artifactId) ? "?" : artifactId); //$NON-NLS-1$

    if(!isEmpty(version)) {
      sb.append(" : ").append(version); //$NON-NLS-1$
    }

    if(!isEmpty(classifier)) {
      sb.append(" : ").append(classifier); //$NON-NLS-1$
    }

    if(!isEmpty(type)) {
      sb.append(" : ").append(type); //$NON-NLS-1$
    }

    if(!isEmpty(scope)) {
      sb.append(" [").append(scope).append(']'); //$NON-NLS-1$
    }

    return sb.toString();
  }

  private boolean isEmpty(String s) {
    return s == null || s.trim().length() == 0;
  }

}
