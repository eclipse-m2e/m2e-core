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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.editing.AddDependencyOperation;
import org.eclipse.m2e.core.ui.internal.editing.AddExclusionOperation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.CompoundOperation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.core.ui.internal.editing.PomHelper;
import org.eclipse.m2e.core.ui.internal.editing.RemoveDependencyOperation;
import org.eclipse.m2e.core.ui.internal.util.ParentHierarchyEntry;
import org.eclipse.m2e.refactoring.Messages;


@SuppressWarnings("restriction")
public class ExcludeArtifactRefactoring extends Refactoring {
  private static final String PLUGIN_ID = "org.eclipse.m2e.refactoring"; //$NON-NLS-1$

  /**
   * Dependencies to exclude
   */
  final ArtifactKey[] excludes;

//  private IFile pomFile;

  /**
   * Workspace Model to exclude dependencies from
   */
  private ParentHierarchyEntry exclusionPoint;

  private List<ParentHierarchyEntry> hierarchy;

  public ExcludeArtifactRefactoring(ArtifactKey[] keys) {
    this.excludes = keys;
  }

  public void setExclusionPoint(ParentHierarchyEntry exclusionPoint) {
    this.exclusionPoint = exclusionPoint;
  }

  public void setHierarchy(List<ParentHierarchyEntry> hierarchy) {
    this.hierarchy = hierarchy;
    this.exclusionPoint = hierarchy != null ? hierarchy.get(0) : null;
  }

  @Override
  public String getName() {
    StringBuilder sb = new StringBuilder();
    for(ArtifactKey key : excludes) {
      sb.append(key.toString()).append(',');
    }
    sb.deleteCharAt(sb.length() - 1);
    return NLS.bind(Messages.MavenExcludeWizard_title, sb.toString());
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws OperationCanceledException {
    return new RefactoringStatus();
  }

  private List<Change> changes;

  @Override
  public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    return MavenPlugin.getMaven().execute(new ICallable<RefactoringStatus>() {
      public RefactoringStatus call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
        return checkFinalConditions0(monitor);
      }
    }, pm);
  }

  RefactoringStatus checkFinalConditions0(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    if(hierarchy == null || exclusionPoint == null) {
      return RefactoringStatus.createFatalErrorStatus(Messages.ExcludeArtifactRefactoring_unableToLocateProject);
    }

    changes = new ArrayList<Change>();
    Set<ArtifactKey> locatedKeys = new HashSet<ArtifactKey>();
    List<IStatus> statuses = new ArrayList<IStatus>();
    SubMonitor monitor = SubMonitor.convert(pm, 3);

    List<Operation> exclusionOp = new ArrayList<Operation>();
    // Exclusion point
    for(Entry<Dependency, Set<ArtifactKey>> entry : getDependencyExcludes(exclusionPoint, monitor.newChild(1))
        .entrySet()) {
      locatedKeys.addAll(entry.getValue());
      Dependency dependency = entry.getKey();
      if(contains(entry.getValue(), dependency)) {
        exclusionOp.add(new RemoveDependencyOperation(dependency));
      } else {
        for(ArtifactKey key : entry.getValue()) {
          if(!hasExclusion(exclusionPoint, dependency, key)) {
            exclusionOp.add(new AddExclusionOperation(dependency, key));
          }
        }
      }
    }

    // Below exclusion point - pull up dependency to exclusion point
    for(ParentHierarchyEntry project : getWorkspaceDescendants()) {
      List<Operation> operations = new ArrayList<Operation>();
      for(Entry<Dependency, Set<ArtifactKey>> entry : getDependencyExcludes(project, monitor.newChild(1)).entrySet()) {
        locatedKeys.addAll(entry.getValue());
        Dependency dependency = entry.getKey();
        operations.add(new RemoveDependencyOperation(dependency));
        if(!contains(entry.getValue(), dependency)) {
          if(!hasDependency(exclusionPoint, dependency)) {
            exclusionOp.add(new AddDependencyOperation(dependency));
          }
          for(ArtifactKey key : entry.getValue()) {
            if(!hasExclusion(exclusionPoint, dependency, key)) {
              exclusionOp.add(new AddExclusionOperation(dependency, key));
            }
          }
        }
      }
      if(operations.size() > 0) {
        IFile pom = project.getResource();
        changes.add(PomHelper.createChange(pom,
            new CompoundOperation(operations.toArray(new Operation[operations.size()])), getName(pom)));
      }
    }

    // Above exclusion - Add dep to exclusionPoint
    for(ParentHierarchyEntry project : getWorkspaceAncestors()) {
      for(Entry<Dependency, Set<ArtifactKey>> entry : getDependencyExcludes(project, monitor.newChild(1)).entrySet()) {
        locatedKeys.addAll(entry.getValue());
        Dependency dependency = entry.getKey();
        if(contains(entry.getValue(), dependency)) {
          IFile pom = project.getResource();
          if(pom != null) {
            statuses.add(new Status(IStatus.INFO, PLUGIN_ID, NLS.bind(
                Messages.ExcludeArtifactRefactoring_removeDependencyFrom, toString(dependency), pom.getFullPath())));
            changes.add(PomHelper.createChange(pom, new RemoveDependencyOperation(dependency), getName(pom)));
          }
        } else {
          exclusionOp.add(new AddDependencyOperation(dependency));
          for(ArtifactKey key : entry.getValue()) {
            if(!hasExclusion(exclusionPoint, dependency, key)) {
              exclusionOp.add(new AddExclusionOperation(dependency, key));
            }
          }
        }
      }
    }
    if(!exclusionOp.isEmpty()) {
      IFile pom = exclusionPoint.getResource();
      changes.add(PomHelper.createChange(pom,
          new CompoundOperation(exclusionOp.toArray(new Operation[exclusionOp.size()])), getName(pom)));
    }

    if(statuses.size() == 1) {
      return RefactoringStatus.create(statuses.get(0));
    } else if(statuses.size() > 1) {
      return RefactoringStatus.create(new MultiStatus(PLUGIN_ID, 0, statuses.toArray(new IStatus[statuses.size()]),
          Messages.ExcludeArtifactRefactoring_errorCreatingRefactoring, null));
    } else if(locatedKeys.isEmpty()) {
      return RefactoringStatus.createFatalErrorStatus(Messages.ExcludeArtifactRefactoring_noTargets);
    } else if(locatedKeys.size() != excludes.length) {
      StringBuilder sb = new StringBuilder();
      for(ArtifactKey key : excludes) {
        if(!locatedKeys.contains(key)) {
          sb.append(key.toString()).append(',');
        }
      }
      sb.deleteCharAt(sb.length() - 1);
      return RefactoringStatus.createErrorStatus(NLS.bind(Messages.ExcludeArtifactRefactoring_failedToLocateArtifact,
          sb.toString()));
    }
    return new RefactoringStatus();
  }

  private String getName(IFile file) {
    return new StringBuilder().append(file.getName()).append(" - ").append(file.getProject().getName()).toString(); //$NON-NLS-1$
  }

  /**
   * Map key is one of <dependency> element of specified (workspace) model. Map value is set of <excludes> element keys
   * to be added to the <dependency>.
   */
  private Map<Dependency, Set<ArtifactKey>> getDependencyExcludes(ParentHierarchyEntry model, IProgressMonitor monitor)
      throws CoreException {
    IMavenProjectFacade facade = model.getFacade();
    MavenProject project = model.getProject();
    DependencyNode root = MavenPlugin.getMavenModelManager().readDependencyTree(facade, project, JavaScopes.TEST,
        monitor);
    Visitor visitor = new Visitor(model);
    root.accept(visitor);
    return visitor.getSourceMap();
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws OperationCanceledException {
    CompositeChange change = new CompositeChange(Messages.ExcludeArtifactRefactoring_changeTitle);
    change.addAll(changes.toArray(new Change[changes.size()]));
    return change;
  }

  private static boolean matches(Dependency d, ArtifactKey a) {
    return d.getArtifactId().equals(a.getArtifactId()) && d.getGroupId().equals(a.getGroupId());
  }

  private static boolean contains(Set<ArtifactKey> keys, Dependency d) {
    for(ArtifactKey key : keys) {
      if(matches(d, key)) {
        return true;
      }
    }
    return false;
  }

  private Collection<ParentHierarchyEntry> getHierarchy() {
    return hierarchy;
  }

  private Collection<ParentHierarchyEntry> getWorkspaceDescendants() {
    List<ParentHierarchyEntry> descendants = new ArrayList<ParentHierarchyEntry>();
    for(ParentHierarchyEntry project : getHierarchy()) {
      if(project == exclusionPoint) {
        break;
      }
      if(project.getFacade() != null) {
        descendants.add(project);
      }
    }
    return descendants;
  }

  private Collection<ParentHierarchyEntry> getWorkspaceAncestors() {
    List<ParentHierarchyEntry> ancestors = new ArrayList<ParentHierarchyEntry>();
    boolean add = false;
    for(ParentHierarchyEntry project : getHierarchy()) {
      if(project == exclusionPoint) {
        add = !add;
      } else if(add) {
        if(project.getFacade() != null) {
          ancestors.add(project);
        }
      }
    }
    return ancestors;
  }

  private static String toString(Dependency dependency) {
    return NLS.bind(
        "{0}:{1}:{2}", new String[] {dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()}); //$NON-NLS-1$
  }

  private boolean hasDependency(ParentHierarchyEntry project, Dependency dependency) {
    List<Dependency> dependencies = project.getProject().getOriginalModel().getDependencies();
    if(dependencies == null) {
      return false;
    }
    for(Dependency dep : dependencies) {
      if(dep.getArtifactId().equals(dependency.getArtifactId()) && dep.getGroupId().equals(dependency.getGroupId())
          && (dep.getVersion() == null || dep.getVersion().equals(dependency.getVersion()))) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasExclusion(ParentHierarchyEntry project, Dependency d, ArtifactKey exclusion) {
    List<Dependency> dependencies = project.getProject().getOriginalModel().getDependencies();
    if(dependencies == null) {
      return false;
    }
    Dependency dependency = null;
    for(Dependency dep : dependencies) {
      if(dep.getArtifactId().equals(d.getArtifactId()) && dep.getGroupId().equals(d.getGroupId())
          && (dep.getVersion() == null || dep.getVersion().equals(d.getVersion()))) {
        dependency = dep;
        break;
      }
    }
    if(dependency == null || dependency.getExclusions() == null) {
      return false;
    }
    for(Exclusion ex : dependency.getExclusions()) {
      if(ex.getArtifactId().equals(exclusion.getArtifactId()) && ex.getGroupId().equals(exclusion.getGroupId())) {
        return true;
      }
    }
    return false;
  }

  private class Visitor implements DependencyVisitor {
    private List<Dependency> dependencies;

    private Map<Dependency, Set<ArtifactKey>> sourceMap = new HashMap<Dependency, Set<ArtifactKey>>();

    Visitor(ParentHierarchyEntry project) {
      dependencies = new ArrayList<Dependency>();
      dependencies.addAll(project.getProject().getOriginalModel().getDependencies());
//      for(Profile profile : project.getActiveProfiles()) {
//        dependencies.addAll(profile.getDependencies());
//      }
    }

    Map<Dependency, Set<ArtifactKey>> getSourceMap() {
      return sourceMap;
    }

    private int depth;

    private DependencyNode topLevel;

    public boolean visitLeave(DependencyNode node) {
      depth-- ;
      return true;
    }

    public boolean visitEnter(DependencyNode node) {
      if(depth == 1) {
        topLevel = node;
      }
      depth++ ;

      if(node.getDependency() != null) {
        Artifact a = node.getDependency().getArtifact();
        for(ArtifactKey exclude : excludes) {
          if(a.getGroupId().equals(exclude.getGroupId()) && a.getArtifactId().equals(exclude.getArtifactId())) {
            if(topLevel != null) {
              // need to add exclusion to top-level dependency
              Dependency dependency = findDependency(topLevel);
              if(dependency != null) {
                put(dependency, exclude);
              }
            }
            return true;
          }
        }
      }
      return true;
    }

    private void put(Dependency dep, ArtifactKey key) {
      Set<ArtifactKey> keys = sourceMap.get(dep);
      if(keys == null) {
        keys = new HashSet<ArtifactKey>();
        sourceMap.put(dep, keys);
      }
      keys.add(key);
    }

    private Dependency findDependency(String groupId, String artifactId) {
      for(Dependency d : dependencies) {
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
  }
}
