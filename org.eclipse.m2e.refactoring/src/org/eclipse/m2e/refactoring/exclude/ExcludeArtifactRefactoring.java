/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.refactoring.exclude;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.Exclusion;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.impl.PomFactoryImpl;
import org.eclipse.m2e.refactoring.AbstractPomHeirarchyRefactoring;
import org.eclipse.m2e.refactoring.Messages;
import org.eclipse.osgi.util.NLS;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;
import org.sonatype.aether.util.artifact.JavaScopes;


public class ExcludeArtifactRefactoring extends AbstractPomHeirarchyRefactoring {

  private final ArtifactKey[] keys;

  private Map<MavenProject, Change> changeMap;
  
  private Set<ArtifactKey> locatedKeys;

  public ExcludeArtifactRefactoring(IMavenProjectFacade projectFacade, Model model, EditingDomain editingDomain,
      ArtifactKey[] keys, IFile pom) {
    super(projectFacade, model, editingDomain, pom);
    this.keys = keys;
  }

  /* (non-Javadoc)
   * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
   */
  public String getName() {
    StringBuilder builder = new StringBuilder();
    for(ArtifactKey key : keys) {
      builder.append(key.toString()).append(", ");
    }
    builder.delete(builder.length() - 2, builder.length());
    return NLS.bind(Messages.ExcludeArtifactRefactoring_refactoringName, builder.toString());
  }

  protected boolean isChanged(final EditingDomain editingDomain, final MavenProject project,
      final IProgressMonitor progressMonitor) throws CoreException,
      OperationCanceledException, IOException {
    final SubMonitor monitor = SubMonitor.convert(progressMonitor);
    final Model m = getModel(project);
    final List<Dependency> deps = m.getDependencies();
    final IStatus[] status = new IStatus[1];
    final CompoundCommand exclusionCommand = new CompoundCommand();
    final List<Dependency> toRemove = new ArrayList<Dependency>();

    final StringBuilder msg = new StringBuilder();
    
    MavenModelManager modelManager = MavenPlugin.getDefault().getMavenModelManager();
    DependencyNode root = modelManager.readDependencyTree(project, JavaScopes.TEST, monitor.newChild(1));
    root.accept(new DependencyVisitor() {

      private int depth;

      private DependencyNode topLevel;

      public boolean visitLeave(DependencyNode node) {
        depth-- ;
        return status[0] == null;
      }

      public boolean visitEnter(DependencyNode node) {
        if(depth == 1) {
          topLevel = node;
        }
        depth++ ;

        if(node.getDependency() != null) {
          Artifact a = node.getDependency().getArtifact();
          for(ArtifactKey key : keys) {
            if(a.getGroupId().equals(key.getGroupId()) && a.getArtifactId().equals(key.getArtifactId())) {
              if(topLevel == null) {
                // do not touch itself
              } else if(node == topLevel) {
                msg.append(key.toString()).append(',');
                // need to remove top-level dependency
                toRemove.add(findDependency(topLevel));
                locatedKeys.add(key);
              } else {
                // need to add exclusion to top-level dependency
                Dependency dependency = findDependency(topLevel);
                if(dependency == null) {
                  status[0] = new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, NLS.bind(
                      Messages.ExcludeRefactoring_error_parent, topLevel.getDependency().getArtifact().getGroupId(),
                      topLevel.getDependency().getArtifact().getArtifactId()));
                } else {
                  addExclusion(exclusionCommand, dependency, key);
                  locatedKeys.add(key);
                }
              }
              return false;
            }
          }
        }

        return true;
      }

      private void addExclusion(CompoundCommand command, Dependency dep, ArtifactKey key) {
        Exclusion exclusion = PomFactoryImpl.eINSTANCE.createExclusion();
        exclusion.setArtifactId(key.getArtifactId());
        exclusion.setGroupId(key.getGroupId());
        command.append(AddCommand.create(editingDomain, dep,
            PomPackage.eINSTANCE.getDependency_Exclusions(), exclusion));
        msg.append(key.toString()).append(',');
      }

      private Dependency findDependency(String groupId, String artifactId) {
        for(Dependency d : deps) {
          if(d.getGroupId().equals(groupId) && d.getArtifactId().equals(artifactId)) {
            return d;
          }
        }
        return null;
      }

      private Dependency findDependency(DependencyNode node) {
        Artifact artifact;
        if(node.getRelocations().isEmpty()) {
          artifact = node.getDependency().getArtifact();
        } else {
          artifact = node.getRelocations().get(0);
        }
        return findDependency(artifact.getGroupId(), artifact.getArtifactId());
      }
    });

    for(Dependency remove : toRemove) {
      exclusionCommand.append(RemoveCommand.create(editingDomain, remove));
    }
//    for(Iterator<Dependency> rem = toRemove.iterator(); rem.hasNext();) {
//      RemoveCommand.create(editingDomain, model, null, rem.next());
//      exclusionCommand.append(new RemoveCommand(editingDomain, model.getDependencies(), rem.next()));
//    }
    if(!exclusionCommand.isEmpty()) {
      changeMap.put(project, new PomResourceChange(editingDomain, exclusionCommand, getPomFile(project),//
          msg.delete(msg.length() - 1, msg.length()).toString()));
    }
    return !exclusionCommand.isEmpty();
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.refactoring.exclude.AbstractRefactoring#isReady(org.eclipse.core.runtime.IProgressMonitor)
   */
  protected RefactoringStatusEntry[] isReady(IProgressMonitor pm) {
    if(keys == null || keys.length == 0) {
      return new RefactoringStatusEntry[] {new RefactoringStatusEntry(RefactoringStatus.FATAL,
          Messages.ExcludeArtifactRefactoring_noArtifactsSet)};
    } 
    List<RefactoringStatusEntry> entries = new ArrayList<RefactoringStatusEntry>();
    for (ArtifactKey key : keys) {
      if (!locatedKeys.contains(key)) {
        entries.add(new RefactoringStatusEntry(RefactoringStatus.FATAL, NLS.bind(
            Messages.ExcludeArtifactRefactoring_failedToLocateArtifact, key.toString())));
      }
    }
    return entries.toArray(new RefactoringStatusEntry[entries.size()]);
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.refactoring.AbstractRefactoring#getChange(org.apache.maven.project.MavenProject, org.eclipse.core.runtime.IProgressMonitor)
   */
  protected Change getChange(MavenProject project, IProgressMonitor pm) {
    return changeMap.get(project);
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.refactoring.AbstractPomHeirarchyRefactoring#checkInitial(org.eclipse.core.runtime.IProgressMonitor)
   */
  protected void checkInitial(IProgressMonitor pm) {
    locatedKeys = new HashSet<ArtifactKey>(keys.length);
    changeMap = new HashMap<MavenProject, Change>();
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.refactoring.AbstractPomHeirarchyRefactoring#checkFinal(org.eclipse.core.runtime.IProgressMonitor)
   */
  protected void checkFinal(IProgressMonitor pm) {
    // Do nothing
  }
}