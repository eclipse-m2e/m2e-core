/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.jdt.ui.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


public class AddDependencyQuickFixProcessor implements IQuickFixProcessor {

  public boolean hasCorrections(ICompilationUnit unit, int problemId) {
    switch(problemId) {
      case IProblem.UndefinedName:
      case IProblem.ImportNotFound:
      case IProblem.UndefinedType:
      case IProblem.UnresolvedVariable:
      case IProblem.MissingTypeInMethod:
      case IProblem.MissingTypeInConstructor:
        IJavaElement parent = unit.getParent();
        if(parent != null) {
          IJavaProject project = parent.getJavaProject();
          if(project != null) {
            return MavenPlugin.isMavenProject(project.getProject());
          }
        }
    }
    return false;
  }

  public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations)
      throws CoreException {
    Map<IMavenProjectFacade, Set<ArtifactKey>> possibleKeys = new HashMap<>();
    for(IProblemLocation location : locations) {
      switch(location.getProblemId()) {
        case IProblem.ImportNotFound:
        case IProblem.UndefinedName:
        case IProblem.UndefinedType:
        case IProblem.UnresolvedVariable:
        case IProblem.MissingTypeInMethod:
        case IProblem.MissingTypeInConstructor:
          handleImportNotFound(context, location, possibleKeys);
      }
    }
    int relevance = getRelevance();
    return possibleKeys.entrySet().stream()
        .flatMap(entry -> entry.getValue().stream()
            .map(key -> new AddDependencyJavaCompletionProposal(key, entry.getKey().getPom(), relevance)))
        .toArray(IJavaCompletionProposal[]::new);
  }

  private void handleImportNotFound(IInvocationContext context, IProblemLocation problemLocation,
      Map<IMavenProjectFacade, Set<ArtifactKey>> possibleKeys) throws CoreException {
    CompilationUnit cu = context.getASTRoot();
    ASTNode selectedNode = problemLocation.getCoveringNode(cu);
    if(selectedNode != null) {
      String className = getClassName(selectedNode);
      if(className != null) {
        IMavenProjectFacade currentFacade = Adapters.adapt(cu.getJavaElement().getJavaProject(),
            IMavenProjectFacade.class);
        if(currentFacade != null) {
          Set<ArtifactKey> artifacts = findMatchingArtifacts(className, currentFacade);
          if(!artifacts.isEmpty()) {
            possibleKeys.computeIfAbsent(currentFacade, x -> new HashSet<>()).addAll(artifacts);
          }
        }
      }
    }
  }

  static Set<ArtifactKey> findMatchingArtifacts(String className, IMavenProjectFacade currentFacade)
      throws CoreException {
    Set<ArtifactKey> possibleKey = new HashSet<ArtifactKey>();
    if(isResolveMissingWorkspaceProject()) {
      SearchPattern typePattern = SearchPattern.createPattern(className, IJavaSearchConstants.TYPE,
          IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
      IJavaSearchScope workspaceScope = SearchEngine.createWorkspaceScope();
      SearchEngine searchEngine = new SearchEngine();
      SearchRequestor requestor = new SearchRequestor() {

        @Override
        public void acceptSearchMatch(SearchMatch aMatch) {
          Object element = aMatch.getElement();
          if(element instanceof IType) {
            IType type = (IType) element;
            IMavenProjectFacade facade = Adapters.adapt(type.getJavaProject(), IMavenProjectFacade.class);
            if(facade != null) {
              ArtifactKey artifactKey = facade.getArtifactKey();
              if(!artifactKey.equals(currentFacade.getArtifactKey())) {
                possibleKey.add(artifactKey);
              }
            }
          }
        }
      };
      searchEngine.search(typePattern, new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
          workspaceScope, requestor, null);
    }
    return possibleKey;
  }

  private String getClassName(ASTNode selectedNode) {
    String className = null;
    if(selectedNode instanceof Name) {
      ITypeBinding typeBinding = ((Name) selectedNode).resolveTypeBinding();
      if(typeBinding != null) {
        className = typeBinding.getBinaryName();
      }
      if(className == null && selectedNode instanceof SimpleName) { // fallback if the type cannot be resolved
        className = ((SimpleName) selectedNode).getIdentifier();
      }
    }
    return className;
  }

  private static boolean isResolveMissingWorkspaceProject() {
    return InstanceScope.INSTANCE.getNode(IMavenConstants.PLUGIN_ID)
        .getBoolean(MavenPreferenceConstants.P_RESOLVE_MISSING_PROJECTS, true);
  }

  private static int getRelevance() {
    return InstanceScope.INSTANCE.getNode(IMavenConstants.PLUGIN_ID)
        .getInt(MavenPreferenceConstants.P_DEFAULT_COMPLETION_PROPOSAL_RELEVANCE, 100);
  }

}
