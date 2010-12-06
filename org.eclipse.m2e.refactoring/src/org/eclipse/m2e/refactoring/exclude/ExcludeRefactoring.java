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

package org.eclipse.m2e.refactoring.exclude;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.Exclusion;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.impl.PomFactoryImpl;
import org.eclipse.m2e.refactoring.AbstractPomRefactoring;
import org.eclipse.m2e.refactoring.Messages;
import org.eclipse.m2e.refactoring.PomRefactoringException;
import org.eclipse.m2e.refactoring.PomVisitor;
import org.eclipse.m2e.refactoring.RefactoringModelResources;
import org.eclipse.osgi.util.NLS;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;
import org.sonatype.aether.util.artifact.JavaScopes;


/**
 * Exclude artifact refactoring implementation
 * 
 * @author Anton Kraev
 */
public class ExcludeRefactoring extends AbstractPomRefactoring {

  private String excludedArtifactId;

  private String excludedGroupId;

  /**
   * @param file
   */
  public ExcludeRefactoring(IFile file, String excludedGroupId, String excludedArtifactId) {
    super(file);
    this.excludedGroupId = excludedGroupId;
    this.excludedArtifactId = excludedArtifactId;
  }

  public PomVisitor getVisitor() {
    return new PomVisitor() {

      public CompoundCommand applyChanges(RefactoringModelResources resources, IProgressMonitor pm)
          throws CoreException, IOException {
        final CompoundCommand command = new CompoundCommand();

        final List<Dependency> toRemove = new ArrayList<Dependency>();

        Model model = resources.getTmpModel();

        final List<Dependency> deps = model.getDependencies();

        final IStatus[] status = new IStatus[] {null};

        pm.beginTask(Messages.ExcludeRefactoring_task_loading, 1);
        MavenModelManager modelManager = MavenPlugin.getDefault().getMavenModelManager();
        DependencyNode root = modelManager.readDependencyTree(resources.getPomFile(), JavaScopes.TEST, pm);
        pm.worked(1);
        root.accept(new DependencyVisitor() {

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

          private int depth;

          private DependencyNode topLevel;

          private Set<Dependency> excluded = new HashSet<Dependency>();

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
              if(a.getGroupId().equals(excludedGroupId) && a.getArtifactId().equals(excludedArtifactId)) {
                if(topLevel == null) {
                  // do not touch itself
                } else if(node == topLevel) {
                  // need to remove top-level dependency
                  toRemove.add(findDependency(topLevel));
                } else {
                  // need to add exclusion to top-level dependency
                  Dependency dependency = findDependency(topLevel);
                  if(dependency == null) {
                    status[0] = new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, NLS.bind(Messages.ExcludeRefactoring_error_parent,
                        topLevel.getDependency().getArtifact().getGroupId(),
                        topLevel.getDependency().getArtifact().getArtifactId()));
                  }
                  if(excluded.add(dependency)) {
                    addExclusion(command, dependency);
                  }
                }
                return false;
              }
            }

            return true;
          }

        });

        if(status[0] != null) {
          throw new PomRefactoringException(status[0]);
        }

        for(Iterator<Dependency> rem = toRemove.iterator(); rem.hasNext();) {
          command.append(new RemoveCommand(editingDomain, model.getDependencies(), rem.next()));
        }

        // XXX scan management as well

        return command;
      }

      private void addExclusion(CompoundCommand command, Dependency dep) {
        Exclusion exclusion = PomFactoryImpl.eINSTANCE.createExclusion();
        exclusion.setArtifactId(excludedArtifactId);
        exclusion.setGroupId(excludedGroupId);
        command.append(new AddCommand(editingDomain, dep.getExclusions(), exclusion));
      }
    };
  }

  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    return new RefactoringStatus();
  }

  public String getName() {
    return Messages.ExcludeRefactoring_name;
  }

  public String getTitle() {
    return NLS.bind(Messages.ExcludeRefactoring_title, new Object[] {excludedGroupId, excludedArtifactId, file.getParent().getName()});
  }

  public boolean scanAllArtifacts() {
    //do not scan other artifacts
    return false;
  }

}
