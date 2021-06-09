/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc.
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

package org.eclipse.m2e.editor.pom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.core.ui.internal.util.XmlUtils;
import org.eclipse.m2e.editor.internal.MarkerHoverControl;
import org.eclipse.m2e.editor.pom.PomHyperlinkDetector.ExpressionRegion;
import org.eclipse.m2e.editor.pom.PomHyperlinkDetector.ManagedArtifactRegion;


public class PomTextHover implements ITextHover, ITextHoverExtension, ITextHoverExtension2 {

  public PomTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
  }

  public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {

    if(hoverRegion instanceof ExpressionRegion) {
      return getLabelForRegion((ExpressionRegion) hoverRegion).toString();
    } else if(hoverRegion instanceof ManagedArtifactRegion) {
      ManagedArtifactRegion region = (ManagedArtifactRegion) hoverRegion;
      return getLabelForRegion(region).toString();
    }

    return null;
  }

  /**
   * @param region
   */
  public static StyledString getLabelForRegion(ManagedArtifactRegion region) {
    MavenProject mavprj = region.project;
    if(mavprj != null) {
      String version = null;
      if(region.isDependency) {
        version = PomTemplateContext.searchDM(mavprj, region.groupId, region.artifactId);
      }
      if(region.isPlugin) {
        version = PomTemplateContext.searchPM(mavprj, region.groupId, region.artifactId);
      }
      StyledString ret = new StyledString();
      if(version != null) {
        ret.append(org.eclipse.m2e.editor.internal.Messages.PomTextHover_managed_version);
        ret.append(version, StyledString.DECORATIONS_STYLER);//not happy with decorations but how to just do bold text
      } else {
        ret.append(org.eclipse.m2e.editor.internal.Messages.PomTextHover_managed_version_missing);
      }
      InputLocation openLocation = PomHyperlinkDetector.findLocationForManagedArtifact(region, mavprj);
      if(openLocation != null) {
        //MNGECLIPSE-2539 apparently you can have an InputLocation with null input source.
        // check!
        InputSource source = openLocation.getSource();
        if(source != null) {
          ret.append(" "); // a space after the version value
          ret.append(
              NLS.bind(org.eclipse.m2e.editor.internal.Messages.PomTextHover_managed_location, source.getModelId()));
        }
      } else {
        ret.append(" "); // a space after the version value
        ret.append(org.eclipse.m2e.editor.internal.Messages.PomTextHover_managed_location_missing);
      }
      return ret;
    }
    return new StyledString(""); //$NON-NLS-1$
  }

  /**
   * @param hoverRegion
   */
  public static StyledString getLabelForRegion(ExpressionRegion region) {
    MavenProject mavprj = region.project;
    if(mavprj != null) {
      String value = PomTemplateContext.simpleInterpolate(region.project, "${" + region.property + "}"); //$NON-NLS-1$ //$NON-NLS-2$
      String loc = null;
      Model mdl = mavprj.getModel();
      if(mdl.getProperties() != null && mdl.getProperties().containsKey(region.property)) {
        if(mdl.getLocation(PomEdits.PROPERTIES) != null) {
          InputLocation location = mdl.getLocation(PomEdits.PROPERTIES).getLocation(region.property);
          if(location != null) {
            //MNGECLIPSE-2539 apparently you can have an InputLocation with null input source.
            // check!
            InputSource source = location.getSource();
            if(source != null) {
              loc = source.getModelId();
            }
          }
        }
      }
      StyledString ret = new StyledString();
      ret.append(org.eclipse.m2e.editor.internal.Messages.PomTextHover_eval1);
      ret.append(value, StyledString.DECORATIONS_STYLER); //not happy with decorations but how to just do bold text
      if(loc != null) {
        ret.append(" "); //$NON-NLS-1$
        ret.append(NLS.bind(org.eclipse.m2e.editor.internal.Messages.PomTextHover_eval2, loc));
      }
      return ret;
    }
    return new StyledString(""); //$NON-NLS-1$
  }

  public IRegion getHoverRegion(final ITextViewer textViewer, final int offset) {
    IDocument document = textViewer.getDocument();
    if(document == null) {
      return null;
    }
    final IRegion[] regs = new IRegion[2];
    XmlUtils.performOnCurrentElement(document, offset, (node, structured) -> {
      ExpressionRegion region = PomHyperlinkDetector.findExpressionRegion(node, textViewer, offset);
      if(region != null) {
        regs[0] = region;
        return;
      }
      ManagedArtifactRegion manReg = PomHyperlinkDetector.findManagedArtifactRegion(node, textViewer, offset);
      if(manReg != null) {
        regs[1] = manReg;
        return;
      }
    });
    CompoundRegion toRet = new CompoundRegion(textViewer, offset);
    if(regs[0] != null) {
      toRet.addRegion(regs[0]);
    }
    if(regs[1] != null) {
      toRet.addRegion(regs[1]);
    }
    if(textViewer instanceof ISourceViewer) {
      ISourceViewer sourceViewer = (ISourceViewer) textViewer;
      IAnnotationModel model = sourceViewer.getAnnotationModel();
      if(model != null) { //eg. in tests
        Iterator<Annotation> it = model.getAnnotationIterator();
        while(it.hasNext()) {
          Annotation ann = it.next();
          if(ann instanceof MarkerAnnotation) {
            Position pos = sourceViewer.getAnnotationModel().getPosition(ann);
            if(pos.includes(offset)) {
              toRet.addRegion(
                  new PomHyperlinkDetector.MarkerRegion(pos.getOffset(), pos.getLength(), (MarkerAnnotation) ann));
            }
          }
        }
      }
    }

    return toRet.getRegions().size() > 0 ? toRet : null;
  }

  public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
    return hoverRegion;
  }

  public IInformationControlCreator getHoverControlCreator() {
    return parent -> new MarkerHoverControl(parent);
  }

  public static class CompoundRegion implements IRegion {

    private int length = Integer.MIN_VALUE;

    private int offset = Integer.MAX_VALUE;

    private final List<IRegion> regions = new ArrayList<>();

    public final ITextViewer textViewer;

    public final int textOffset;

    public CompoundRegion(ITextViewer textViewer, int textOffset) {
      this.textViewer = textViewer;
      this.textOffset = textOffset;
    }

    public int getLength() {
      return length;
    }

    public int getOffset() {
      return offset;
    }

    public void addRegion(IRegion region) {
      regions.add(region);
      int start = Math.min(region.getOffset(), offset);
      int end = Math.max(region.getOffset() + region.getLength(), offset + length);
      offset = start;
      length = end - start;
    }

    public List<IRegion> getRegions() {
      return regions;
    }

  }

}
