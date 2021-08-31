/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;

import org.apache.lucene.queryparser.classic.QueryParser;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.search.util.CComboContentAdapter;
import org.eclipse.m2e.core.ui.internal.search.util.ControlDecoration;
import org.eclipse.m2e.core.ui.internal.search.util.Packaging;
import org.eclipse.m2e.core.ui.internal.search.util.SearchEngine;


/**
 * Holds the proposal utility code, previously in the editor.xml plug-in. Provides proposal suggestions for text and
 * combo widgets for various metadata (group, artifact, etc.)
 *
 * @author rgould
 */
public class ProposalUtil {
  private static final Logger log = LoggerFactory.getLogger(ProposalUtil.class);

  public static abstract class Searcher {
    public abstract Collection<String> search() throws CoreException;
  }

  public static final class TextProposal implements IContentProposal {
    private final String text;

    public TextProposal(String text) {
      this.text = text;
    }

    @Override
    public int getCursorPosition() {
      return text.length();
    }

    @Override
    public String getContent() {
      return text;
    }

    @Override
    public String getLabel() {
      return text;
    }

    @Override
    public String getDescription() {
      return null;
    }
  }

  public static void addCompletionProposal(final Control control, final Searcher searcher) {
    FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(
        FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
    ControlDecoration decoration = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
    decoration.setShowOnlyOnFocus(true);
    decoration.setDescriptionText(fieldDecoration.getDescription());
    decoration.setImage(fieldDecoration.getImage());

    IContentProposalProvider proposalProvider = (contents, position) -> {
      final String start = contents.length() > position ? contents.substring(0, position) : contents;
      ArrayList<IContentProposal> proposals = new ArrayList<>();
      try {
        for(final String text : searcher.search()) {
          if(text.startsWith(start)) {
            proposals.add(new TextProposal(text));
          }
        }
      } catch(CoreException e) {
        log.error(e.getMessage(), e);
      }
      return proposals.toArray(new IContentProposal[proposals.size()]);
    };

    IControlContentAdapter contentAdapter;
    if(control instanceof Text) {
      contentAdapter = new TextContentAdapter();
    } else {
      contentAdapter = new CComboContentAdapter();
    }

    ContentAssistCommandAdapter adapter = new ContentAssistCommandAdapter( //
        control, contentAdapter, proposalProvider, //
        IWorkbenchCommandConstants.EDIT_CONTENT_ASSIST, null);
    // ContentProposalAdapter adapter = new ContentProposalAdapter(control, contentAdapter, //
    //     proposalProvider, KeyStroke.getInstance(SWT.MOD1, ' '), null);
    adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
    adapter.setPopupSize(new Point(250, 120));
    adapter.setPopupSize(new Point(250, 120));
  }

  public static void addClassifierProposal(final IProject project, final Text groupIdText, final Text artifactIdText,
      final Text versionText, final Text classifierText, final Packaging packaging) {
    addCompletionProposal(classifierText, new Searcher() {
      @Override
      public Collection<String> search() throws CoreException {
        return getSearchEngine(project).findClassifiers(
            escapeQuerySpecialCharacters(groupIdText.getText()), //
            escapeQuerySpecialCharacters(artifactIdText.getText()),
            escapeQuerySpecialCharacters(versionText.getText()), "", packaging);
      }
    });
  }

  public static void addVersionProposal(final IProject project, final MavenProject mp, final Text groupIdText,
      final Text artifactIdText, final Text versionText, final Packaging packaging) {
    addCompletionProposal(versionText, new Searcher() {
      @Override
      public Collection<String> search() throws CoreException {
        Collection<String> toRet = new ArrayList<>();
        toRet.addAll(getSearchEngine(project).findVersions(escapeQuerySpecialCharacters(groupIdText.getText()), //
            escapeQuerySpecialCharacters(artifactIdText.getText()), "", packaging));
        if(mp != null) {
          //add version props now..
          Properties props = mp.getProperties();
          ArrayList<String> list = new ArrayList<>();
          if(props != null) {
            for(Object prop : props.keySet()) {
              String propString = prop.toString();
              if(propString.endsWith("Version") || propString.endsWith(".version")) { //$NON-NLS-1$//$NON-NLS-2$
                list.add("${" + propString + "}"); //$NON-NLS-1$//$NON-NLS-2$
              }
            }
          }
          Collections.sort(list);
          toRet.addAll(list);
        }
        return toRet;
      }
    });
  }

  public static void addArtifactIdProposal(final IProject project, final Text groupIdText, final Text artifactIdText,
      final Packaging packaging) {
    addCompletionProposal(artifactIdText, new Searcher() {
      @Override
      public Collection<String> search() throws CoreException {
        // TODO handle artifact info
        return getSearchEngine(project).findArtifactIds(escapeQuerySpecialCharacters(groupIdText.getText()), "",
            packaging, null);
      }
    });
  }

  public static void addGroupIdProposal(final IProject project, final Text groupIdText, final Packaging packaging) {
    addCompletionProposal(groupIdText, new Searcher() {
      @Override
      public Collection<String> search() throws CoreException {
        // TODO handle artifact info
        return getSearchEngine(project).findGroupIds(escapeQuerySpecialCharacters(groupIdText.getText()), packaging,
            null);
      }
    });
  }

  //issue 350271
  //http://lucene.apache.org/java/3_2_0/queryparsersyntax.html#Escaping Special Characters
  //for proposal queries, any special chars shall be escaped
  //    + - && || ! ( ) { } [ ] ^ " ~ * ? : \
  private static String escapeQuerySpecialCharacters(String raw) {
    return QueryParser.escape(raw);
  }

  public static SearchEngine getSearchEngine(final IProject project) throws CoreException {
    return M2EUIPluginActivator.getDefault().getSearchEngine(project);
  }

}
