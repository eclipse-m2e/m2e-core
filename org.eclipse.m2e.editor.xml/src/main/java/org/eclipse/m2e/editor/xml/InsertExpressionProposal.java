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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.sse.ui.internal.contentassist.IRelevanceCompletionProposal;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.editor.xml.internal.Messages;


/**
 * insertion proposal for ${ expressions
 * 
 * @author mkleint
 */
public class InsertExpressionProposal
    implements ICompletionProposal, ICompletionProposalExtension5, IRelevanceCompletionProposal {
  private static final Logger log = LoggerFactory.getLogger(InsertExpressionProposal.class);

  private MavenProject project;

  private String key;

  private Region region;

  private int len = 0;

  public InsertExpressionProposal(Region region, String key, MavenProject mvnproject) {
    assert project != null;
    this.region = region;
    this.key = key;
    this.project = mvnproject;
  }

  public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
    if(project == null) {
      return null;
    }
    String value = PomTemplateContext.simpleInterpolate(project, "${" + key + "}"); //$NON-NLS-1$ //$NON-NLS-2$
    MavenProject mavprj = project;
    String loc = null;
    if(mavprj != null) {
      Model mdl = mavprj.getModel();
      if(mdl.getProperties() != null && mdl.getProperties().containsKey(key)) {
        if(mdl.getLocation("properties") != null) {
          InputLocation location = mdl.getLocation("properties").getLocation(key); //$NON-NLS-1$
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
    }
    StringBuilder buff = new StringBuilder();
    buff.append("<html>"); //$NON-NLS-1$
    if(value != null) {
      buff.append(NLS.bind(Messages.InsertExpressionProposal_hint1, value));
    }
    if(loc != null) {
      buff.append(NLS.bind(Messages.InsertExpressionProposal_hint2, loc));
    }
    buff.append("</html>"); //$NON-NLS-1$
    return buff.toString();
  }

  public void apply(IDocument document) {
    int offset = region.getOffset();
    String replace = "${" + key + "}"; //$NON-NLS-1$ //$NON-NLS-2$
    try {
      document.replace(offset, region.getLength(), replace);
      len = replace.length();
    } catch(BadLocationException e) {
      log.error("Cannot apply proposal", e);
    }
  }

  public Point getSelection(IDocument document) {
    return new Point(region.getOffset() + len, 0);
  }

  public String getAdditionalProposalInfo() {
    //not used anymore
    return null;
  }

  public String getDisplayString() {
    return "${" + key + "}"; //$NON-NLS-1$ //$NON-NLS-2$
  }

  public Image getImage() {
    // TODO  what kind of icon to use?
    return null;
  }

  public IContextInformation getContextInformation() {
    return null;
  }

  public int getRelevance() {
    return 2000;
  }

}
