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
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.editing.AddExclusionOperation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.CompoundOperation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.core.ui.internal.editing.PomHelper;
import org.eclipse.m2e.core.ui.internal.editing.RemoveDependencyOperation;
import org.eclipse.m2e.refactoring.AbstractPomHeirarchyRefactoring;
import org.eclipse.m2e.refactoring.Messages;
import org.eclipse.osgi.util.NLS;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;
import org.sonatype.aether.util.artifact.JavaScopes;


public class ExcludeArtifactRefactoring extends AbstractPomHeirarchyRefactoring {

  private final ArtifactKey[] keys;

  private Set<ArtifactKey> locatedKeys;

  private Map<IFile, Change> operationMap;

  public ExcludeArtifactRefactoring(ArtifactKey[] keys, IFile pom) {
    super(pom);
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
  protected Change getChange(IFile file, IProgressMonitor pm) {
    return operationMap.get(file);
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.refactoring.AbstractPomHeirarchyRefactoring#checkInitial(org.eclipse.core.runtime.IProgressMonitor)
   */
  protected void checkInitial(IProgressMonitor pm) {
    locatedKeys = new HashSet<ArtifactKey>(keys.length);
    operationMap = new HashMap<IFile, Change>();
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.refactoring.AbstractPomHeirarchyRefactoring#checkFinal(org.eclipse.core.runtime.IProgressMonitor)
   */
  protected void checkFinal(IProgressMonitor pm) {
    // Do nothing
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.refactoring.AbstractPomHeirarchyRefactoring#isAffected(org.eclipse.m2e.core.project.IMavenProjectFacade, org.eclipse.core.runtime.IProgressMonitor)
   */
  protected boolean isAffected(IFile pomFile, IProgressMonitor progressMonitor) throws CoreException {

    final SubMonitor monitor = SubMonitor.convert(progressMonitor);
    final IStatus[] status = new IStatus[1];

    final IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManagerImpl()
        .create(pomFile, true, monitor);
    final MavenProject project = facade.getMavenProject(progressMonitor);
    final org.apache.maven.model.Model m = project.getModel();

    final List<Operation> operations = new ArrayList<Operation>();

    final StringBuilder msg = new StringBuilder();
    final List<org.apache.maven.model.Dependency> dependencies = m.getDependencies();

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
                operations.add(new RemoveDependencyOperation(findDependency(topLevel)));
                locatedKeys.add(key);
              } else {
                // need to add exclusion to top-level dependency
                org.apache.maven.model.Dependency dependency = findDependency(topLevel);
                if(dependency == null) {
                  status[0] = new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, NLS.bind(
                      Messages.ExcludeRefactoring_error_parent, topLevel.getDependency().getArtifact().getGroupId(),
                      topLevel.getDependency().getArtifact().getArtifactId()));
                } else {
                  operations.add(new AddExclusionOperation(dependency, key));
                  locatedKeys.add(key);
                }
              }
              return false;
            }
          }
        }

        return true;
      }

      private org.apache.maven.model.Dependency findDependency(String groupId, String artifactId) {
        for(org.apache.maven.model.Dependency d : dependencies) {
          if(d.getGroupId().equals(groupId) && d.getArtifactId().equals(artifactId)) {
            return d;
          }
        }
        return null;
      }

      private org.apache.maven.model.Dependency findDependency(DependencyNode node) {
        Artifact artifact;
        if(node.getRelocations().isEmpty()) {
          artifact = node.getDependency().getArtifact();
        } else {
          artifact = node.getRelocations().get(0);
        }
        return findDependency(artifact.getGroupId(), artifact.getArtifactId());
      }
    });

    if(operations.size() > 0) {
      operationMap.put(pomFile, PomHelper.createChange(pomFile,
          new CompoundOperation(operations.toArray(new Operation[operations.size()])), msg.toString()));
    }
    return !operations.isEmpty();
  }
}