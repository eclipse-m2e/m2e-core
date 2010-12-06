/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.util;

import java.util.ArrayList;
import java.util.Collection;

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
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.util.search.CComboContentAdapter;
import org.eclipse.m2e.core.util.search.ControlDecoration;
import org.eclipse.m2e.core.util.search.Packaging;
import org.eclipse.m2e.core.util.search.SearchEngine;


/**
 * Holds the proposal utility code, previously in the editor.xml plug-in. Provides proposal suggestions for text and
 * combo widgets for various metadata (group, artifact, etc.)
 * 
 * @author rgould
 */
public class ProposalUtil {

  public static abstract class Searcher {
    public abstract Collection<String> search() throws CoreException;
  }

  public static final class TextProposal implements IContentProposal {
    private final String text;

    public TextProposal(String text) {
      this.text = text;
    }

    public int getCursorPosition() {
      return text.length();
    }

    public String getContent() {
      return text;
    }

    public String getLabel() {
      return text;
    }

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

    IContentProposalProvider proposalProvider = new IContentProposalProvider() {
      public IContentProposal[] getProposals(String contents, int position) {
        ArrayList<IContentProposal> proposals = new ArrayList<IContentProposal>();
        try {
          for(final String text : searcher.search()) {
            proposals.add(new TextProposal(text));
          }
        } catch(CoreException e) {
          MavenLogger.log(e);
        }
        return proposals.toArray(new IContentProposal[proposals.size()]);
      }
    };

    IControlContentAdapter contentAdapter;
    if(control instanceof Text) {
      contentAdapter = new TextContentAdapter();
    } else {
      contentAdapter = new CComboContentAdapter();
    }

    ContentAssistCommandAdapter adapter = new ContentAssistCommandAdapter( //
        control, contentAdapter, proposalProvider, //
        ContentAssistCommandAdapter.CONTENT_PROPOSAL_COMMAND, null);
    // ContentProposalAdapter adapter = new ContentProposalAdapter(control, contentAdapter, //
    //     proposalProvider, KeyStroke.getInstance(SWT.MOD1, ' '), null);
    adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
    adapter.setPopupSize(new Point(250, 120));
    adapter.setPopupSize(new Point(250, 120));
  }

  public static void addClassifierProposal(final IProject project, final Text groupIdText, final Text artifactIdText,
      final Text versionText, final Text classifierText, final Packaging packaging) {
    addCompletionProposal(classifierText, new Searcher() {
      public Collection<String> search() throws CoreException {
        return getSearchEngine(project).findClassifiers(groupIdText.getText(), //
            artifactIdText.getText(), versionText.getText(), classifierText.getText(), packaging);
      }
    });
  }

  public static void addVersionProposal(final IProject project, final Text groupIdText, final Text artifactIdText,
      final Text versionText, final Packaging packaging) {
    addCompletionProposal(versionText, new Searcher() {
      public Collection<String> search() throws CoreException {
        return getSearchEngine(project).findVersions(groupIdText.getText(), //
            artifactIdText.getText(), versionText.getText(), packaging);
      }
    });
  }

  public static void addArtifactIdProposal(final IProject project, final Text groupIdText, final Text artifactIdText,
      final Packaging packaging) {
    addCompletionProposal(artifactIdText, new Searcher() {
      public Collection<String> search() throws CoreException {
        // TODO handle artifact info
        return getSearchEngine(project).findArtifactIds(groupIdText.getText(), artifactIdText.getText(), packaging,
            null);
      }
    });
  }

  public static void addGroupIdProposal(final IProject project, final Text groupIdText, final Packaging packaging) {
    addCompletionProposal(groupIdText, new Searcher() {
      public Collection<String> search() throws CoreException {
        // TODO handle artifact info
        return getSearchEngine(project).findGroupIds(groupIdText.getText(), packaging, null);
      }
    });
  }

  public static SearchEngine getSearchEngine(final IProject project) throws CoreException {
    return MavenPlugin.getDefault().getSearchEngine(project);
  }

}
