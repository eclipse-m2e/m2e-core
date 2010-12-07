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

package org.eclipse.m2e.editor.xml;

import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Node;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.osgi.util.NLS;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.editor.xml.PomHyperlinkDetector.ExpressionRegion;
import org.eclipse.m2e.editor.xml.PomHyperlinkDetector.ManagedArtifactRegion;
import org.eclipse.m2e.editor.xml.internal.Messages;
import org.eclipse.m2e.editor.xml.internal.XmlUtils;

public class PomTextHover implements ITextHover {

  public PomTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
  }
  
  public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
    if (hoverRegion instanceof ExpressionRegion) {
      ExpressionRegion region = (ExpressionRegion) hoverRegion;
      IMavenProjectFacade mvnproject = MavenPlugin.getDefault().getMavenProjectManager().getProject(region.project);
      if (mvnproject != null) {
        MavenProject mavprj = mvnproject.getMavenProject();
        if (mavprj != null) {
          String value = PomTemplateContext.simpleInterpolate(region.project, "${" + region.property + "}"); //$NON-NLS-1$ //$NON-NLS-2$
          String loc = null;
          Model mdl = mavprj.getModel();
          if (mdl.getProperties() != null && mdl.getProperties().containsKey(region.property)) {
            if (mdl.getLocation("properties") != null) {
              InputLocation location = mdl.getLocation("properties").getLocation(region.property); //$NON-NLS-1$
              if (location != null) {
                //MNGECLIPSE-2539 apparently you can have an InputLocation with null input source.
                // check!
                InputSource source = location.getSource();
                if (source != null) {
                  loc = source.getModelId();
                }
              }
            }
          }
          String ret = NLS.bind(Messages.PomTextHover_eval1, 
              value, loc != null ? NLS.bind(Messages.PomTextHover_eval2, loc) : ""); //$NON-NLS-2$ //$NON-NLS-1$
          return ret;
        }
      }
    } else if (hoverRegion instanceof ManagedArtifactRegion) {
      ManagedArtifactRegion region = (ManagedArtifactRegion) hoverRegion;
      IMavenProjectFacade mvnproject = MavenPlugin.getDefault().getMavenProjectManager().getProject(region.project);
      if (mvnproject != null) {
        MavenProject mavprj = mvnproject.getMavenProject();
        if (mavprj != null) {
          String version = null;
          if (region.isDependency) {
            DependencyManagement dm = mavprj.getDependencyManagement();
            if (dm != null) {
              List<Dependency> list = dm.getDependencies();
              String id = region.groupId + ":" + region.artifactId + ":"; //$NON-NLS-1$ //$NON-NLS-2$
              if (list != null) {
                for (Dependency dep : list) {
                  if (dep.getManagementKey().startsWith(id)) {
                    version = dep.getVersion();
                  }
                }
              }
            }
          }
          
          if (region.isPlugin) {
            PluginManagement pm = mavprj.getPluginManagement();
            if (pm != null) {
              List<Plugin> list = pm.getPlugins();
              String id = Plugin.constructKey(region.groupId, region.artifactId);
              if (list != null) {
                for (Plugin plg : list) {
                  if (id.equals(plg.getKey())) {
                    version = plg.getVersion();
                  }
                }
              }
              
            }
          }
          StringBuffer ret = new StringBuffer();
          ret.append("<html>"); //$NON-NLS-1$
          if (version != null) {
            ret.append(NLS.bind(Messages.PomTextHover_managed_version, version));
          } else {
            ret.append(Messages.PomTextHover_managed_version_missing);
          }
          ret.append("<br>"); //$NON-NLS-1$
          InputLocation openLocation = PomHyperlinkDetector.findLocationForManagedArtifact(region, mavprj);
          if (openLocation != null) {
            //MNGECLIPSE-2539 apparently you can have an InputLocation with null input source.
            // check!
            InputSource source = openLocation.getSource();
            if (source != null) {
              ret.append(NLS.bind(Messages.PomTextHover_managed_location, source.getModelId()));
            }
          } else {
            ret.append(Messages.PomTextHover_managed_location_missing);
          }
          ret.append("</html>"); //$NON-NLS-1$
          return ret.toString();
        }
      }
    }
    
    return null;
  }

  public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
    IDocument document = textViewer.getDocument();
    if(document == null) {
      return null;
    }

    Node current = XmlUtils.getCurrentNode(document, offset);
    if (current != null) {
      ExpressionRegion region = PomHyperlinkDetector.findExpressionRegion(current, textViewer, offset);
      if (region != null) {
        return region;
      }
      ManagedArtifactRegion manReg = PomHyperlinkDetector.findManagedArtifactRegion(current, textViewer, offset);
      if (manReg != null) {
        return manReg;
      }
    }
    return null;
  }
  


}
