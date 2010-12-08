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

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Node;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.osgi.util.NLS;

import org.eclipse.m2e.editor.xml.PomHyperlinkDetector.ExpressionRegion;
import org.eclipse.m2e.editor.xml.PomHyperlinkDetector.ManagedArtifactRegion;
import org.eclipse.m2e.editor.xml.internal.Messages;
import org.eclipse.m2e.editor.xml.internal.NodeOperation;
import org.eclipse.m2e.editor.xml.internal.XmlUtils;

public class PomTextHover implements ITextHover {

  public PomTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
  }
  
  public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
    if (hoverRegion instanceof ExpressionRegion) {
      ExpressionRegion region = (ExpressionRegion) hoverRegion;
      MavenProject mavprj = region.project;
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
    } else if (hoverRegion instanceof ManagedArtifactRegion) {
      ManagedArtifactRegion region = (ManagedArtifactRegion) hoverRegion;
      MavenProject mavprj = region.project;
      if (mavprj != null) {
        String version = null;
        if (region.isDependency) {
          version = PomTemplateContext.searchDM(mavprj, region.groupId, region.artifactId);
        }
        if (region.isPlugin) {
          version = PomTemplateContext.searchPM(mavprj, region.groupId, region.artifactId);
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
    
    return null;
  }

  public IRegion getHoverRegion(final ITextViewer textViewer, final int offset) {
    IDocument document = textViewer.getDocument();
    if(document == null) {
      return null;
    }
    final IRegion[] toRet = new IRegion[1];
    XmlUtils.performOnCurrentElement(document, offset, new NodeOperation<Node>() {
      public void process(Node node) {
        ExpressionRegion region = PomHyperlinkDetector.findExpressionRegion(node, textViewer, offset);
        if (region != null) {
          toRet[0] = region;
          return;
        }
        ManagedArtifactRegion manReg = PomHyperlinkDetector.findManagedArtifactRegion(node, textViewer, offset);
        if (manReg != null) {
          toRet[0] = manReg;
          return;
        }
      }
    });
    return toRet[0];
  }
  


}
