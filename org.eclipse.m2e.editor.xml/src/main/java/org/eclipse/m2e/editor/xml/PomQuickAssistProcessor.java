/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Anton Tanasenko - Refactor marker resolutions and quick fixes (Bug #484359)
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.markers.EditorAwareMavenProblemResolution;
import org.eclipse.m2e.core.ui.internal.markers.MavenProblemResolution;
import org.eclipse.m2e.editor.xml.internal.markers.MarkerResolutionWrapper;


@SuppressWarnings("restriction")
public class PomQuickAssistProcessor implements IQuickAssistProcessor {
  static final Logger log = LoggerFactory.getLogger(PomQuickAssistProcessor.class);

  public boolean canAssist(IQuickAssistInvocationContext arg0) {
    return true;
  }

  public boolean canFix(Annotation an) {
    if(an instanceof MarkerAnnotation) {
      MarkerAnnotation mark = (MarkerAnnotation) an;
      String hint = mark.getMarker().getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, null);
      if(hint != null) {
        return true;
      }
    }
    return false;
  }

  public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext context) {
    List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
    Iterator<Annotation> annotationIterator = context.getSourceViewer().getAnnotationModel().getAnnotationIterator();
    while(annotationIterator.hasNext()) {
      Annotation annotation = annotationIterator.next();
      if(annotation instanceof MarkerAnnotation) {
        MarkerAnnotation mark = (MarkerAnnotation) annotation;
        try {
          Position position = context.getSourceViewer().getAnnotationModel().getPosition(annotation);
          int lineNum = context.getSourceViewer().getDocument().getLineOfOffset(position.getOffset()) + 1;
          int currentLineNum = context.getSourceViewer().getDocument().getLineOfOffset(context.getOffset()) + 1;
          if(currentLineNum == lineNum) {
            collectResolutionProposals(proposals, mark, context);
          }
        } catch(Exception e) {
          MvnIndexPlugin.getDefault().getLog()
              .log(new Status(IStatus.ERROR, MvnIndexPlugin.PLUGIN_ID, "Exception in pom quick assist.", e));
        }
      }
    }

    if(proposals.size() > 0) {
      return proposals.toArray(new ICompletionProposal[0]);
    }
    return null;
  }

  private void collectResolutionProposals(List<ICompletionProposal> proposals, MarkerAnnotation mark,
      IQuickAssistInvocationContext context) {
    if(MavenProblemResolution.hasResolutions(mark.getMarker())) {
      List<IMarkerResolution> resolutions = MavenProblemResolution.getResolutions(mark.getMarker());

      for(IMarkerResolution res : resolutions) {
        ICompletionProposal proposal;
        if(res instanceof ICompletionProposal) {
          proposal = (ICompletionProposal) res;
        } else {
          proposal = new MarkerResolutionWrapper(res, mark.getMarker());
        }

        if(proposal instanceof MavenProblemResolution) {
          MavenProblemResolution mres = (MavenProblemResolution) proposal;

          if(mres.includeProposal(proposals)) {
            if(proposal instanceof EditorAwareMavenProblemResolution) {
              EditorAwareMavenProblemResolution eres = (EditorAwareMavenProblemResolution) proposal;
              eres.setQuickAssistContext(context);
            }
          }

        } else {
          proposals.add(proposal);
        }
      }
    }
  }

  public String getErrorMessage() {
    return null;
  }
}
