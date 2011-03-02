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

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.editing.AddDependencyOperation;
import org.eclipse.m2e.core.ui.internal.editing.AddExclusionOperation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.CompoundOperation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.core.ui.internal.editing.PomHelper;
import org.eclipse.m2e.core.ui.internal.editing.RemoveDependencyOperation;
import org.eclipse.m2e.refactoring.Messages;
import org.eclipse.osgi.util.NLS;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;
import org.sonatype.aether.util.artifact.JavaScopes;


public class ExcludeArtifactRefactoring extends Refactoring {

  private ArtifactKey[] keys;

  private IFile pomFile;

  private MavenProject exclusionPoint;

  private List<MavenProject> hierarchy;

  private Set<ArtifactKey> locatedKeys;

  public ExcludeArtifactRefactoring(IFile pomFile, ArtifactKey[] keys) {
    this.pomFile = pomFile;
    this.keys = keys;
  }

  public void setExclusionPoint(MavenProject exclusionPoint) {
    this.exclusionPoint = exclusionPoint;
  }

  public void setHierarchy(List<MavenProject> hierarchy) {
    this.hierarchy = hierarchy;
  }

  public IMavenProjectFacade getSource() {
    return getMavenProjectFacade(pomFile);
  }

  protected IMavenProjectFacade getMavenProjectFacade(IFile pom) {
    return MavenPlugin.getDefault().getMavenProjectManager().create(pom, true, new NullProgressMonitor());
  }

  protected IMavenProjectFacade getMavenProjectFacade(MavenProject mavenProject) {
    return MavenPlugin.getDefault().getMavenProjectManager()
        .getMavenProject(mavenProject.getGroupId(), mavenProject.getArtifactId(), mavenProject.getVersion());
  }

  /* (non-Javadoc)
   * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
   */
  public String getName() {
    StringBuilder sb = new StringBuilder();
    sb.append("Exclude: ");
    for(ArtifactKey key : keys) {
      sb.append(key.toString()).append(',');
    }
    sb.deleteCharAt(sb.length() - 1);
    return sb.toString();
  }

  /* (non-Javadoc)
   * @see org.eclipse.ltk.core.refactoring.Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
   */
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    exclusionPoint = getMavenProjectFacade(pomFile).getMavenProject(pm);
    if(exclusionPoint == null) {
      return RefactoringStatus
          .createFatalErrorStatus("MavenProject does not exist, try cleaning workspace & rebuilding");
    }
    return new RefactoringStatus();
  }

  private List<Change> changes;

  /* (non-Javadoc)
   * @see org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)
   */
  public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    changes = new ArrayList<Change>();
    locatedKeys = new HashSet<ArtifactKey>();
    List<IStatus> statuses = new ArrayList<IStatus>();
    SubMonitor monitor = SubMonitor.convert(pm, getHierarchy().size());

    List<Operation> exclusionOp = new ArrayList<Operation>();
    // Exclusion point
    Visitor visitor = locate(exclusionPoint, monitor.newChild(1));
    for(Entry<Dependency, Set<ArtifactKey>> entry : visitor.getSourceMap().entrySet()) {
      exclusionPoint.getOriginalModel().getDependencies();
      locatedKeys.addAll(entry.getValue());
      if(contains(entry.getValue(), entry.getKey())) {
        exclusionOp.add(new RemoveDependencyOperation(entry.getKey()));
      } else {
        for(ArtifactKey key : entry.getValue()) {
          exclusionOp.add(new AddExclusionOperation(entry.getKey(), key));
        }
      }
    }

    // Below exclusion point - pull up dependency to exclusion point
    for(MavenProject project : getDescendants()) {
      visitor = locate(project, monitor.newChild(1));
      for(Entry<Dependency, Set<ArtifactKey>> entry : visitor.getSourceMap().entrySet()) {
        locatedKeys.addAll(entry.getValue());
        if(contains(entry.getValue(), entry.getKey())) {
          changes.add(PomHelper.createChange(getFile(project), new RemoveDependencyOperation(entry.getKey()),
              "Remove dependency {0}"));
        } else {
          CompositeChange change = new CompositeChange("Move dependency {0}");
          change.add(PomHelper.createChange(getFile(project), new RemoveDependencyOperation(entry.getKey()),
              "Remove dependency {0}"));
          exclusionOp.add(new AddDependencyOperation(entry.getKey()));
          for(ArtifactKey key : entry.getValue()) {
            exclusionOp.add(new AddExclusionOperation(entry.getKey(), key));
          }
        }
      }
      if(!visitor.getStatus().isOK()) {
        statuses.add(visitor.getStatus());
      }
    }

    // Above exclusion - Add dep to exclusionPoint
    for(MavenProject project : getAncestors()) {
      visitor = locate(project, monitor.newChild(1));
      for(Entry<Dependency, Set<ArtifactKey>> entry : locate(project, monitor.newChild(1)).getSourceMap().entrySet()) {
        locatedKeys.addAll(entry.getValue());
        if(contains(entry.getValue(), entry.getKey())) {
          if(project.getFile() != null) {
            changes.add(PomHelper.createChange(getFile(project), new RemoveDependencyOperation(entry.getKey()),
                "Remove dependency {0}"));
          }
        } else {
          exclusionOp.add(new AddDependencyOperation(entry.getKey()));
          for(ArtifactKey key : entry.getValue()) {
            exclusionOp.add(new AddExclusionOperation(entry.getKey(), key));
          }
        }
      }
      if(!visitor.getStatus().isOK()) {
        statuses.add(visitor.getStatus());
      }
    }
    changes.add(PomHelper.createChange(getFile(exclusionPoint),
        new CompoundOperation(exclusionOp.toArray(new Operation[exclusionOp.size()])), getName()));

    if(statuses.size() == 1) {
      return RefactoringStatus.create(statuses.get(0));
    } else if(statuses.size() > 1) {
      return RefactoringStatus.create(new MultiStatus("org.eclipse.m2e.refactoring", 0, statuses
          .toArray(new IStatus[statuses.size()]), "Errors occurred creating refactoring", null));
    } else if(locatedKeys.isEmpty()) {
      return RefactoringStatus.createFatalErrorStatus(Messages.AbstractPomHeirarchyRefactoring_noTargets);
    } else if(locatedKeys.size() != keys.length) {
      StringBuilder sb = new StringBuilder();
      for(ArtifactKey key : keys) {
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

  private Visitor locate(MavenProject project, IProgressMonitor monitor) throws CoreException {
    DependencyNode root = MavenPlugin.getDefault().getMavenModelManager()
        .readDependencyTree(project, JavaScopes.TEST, monitor);
    Visitor visitor = new Visitor(project);
    root.accept(visitor);
    return visitor;
  }

  /* (non-Javadoc)
   * @see org.eclipse.ltk.core.refactoring.Refactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
   */
  public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    CompositeChange change = new CompositeChange(getName());
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

  private Collection<MavenProject> getHierarchy() {
    return hierarchy;
  }

  private IFile getFile(MavenProject project) {
    IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(project.getFile().toURI());
    if(files.length == 0) {
      // TODO something
      return null;
    } else {
      return files[0];
    }
  }

  private Collection<MavenProject> getDescendants() {
    List<MavenProject> descendants = new ArrayList<MavenProject>();
    boolean add = true;
    for(MavenProject project : getHierarchy()) {
      if(project == exclusionPoint) {
        add = !add;
      } else if(add) {
        descendants.add(project);
      }
    }
    return descendants;
  }

  private Collection<MavenProject> getAncestors() {
    List<MavenProject> ancestors = new ArrayList<MavenProject>();
    boolean add = false;
    for(MavenProject project : getHierarchy()) {
      if(project == exclusionPoint) {
        add = !add;
      } else if(add) {
        ancestors.add(project);
      }
    }
    return ancestors;
  }

  private class Visitor implements DependencyVisitor {
    private List<IStatus> statuses = new ArrayList<IStatus>();

    private List<Dependency> dependencies;

    private Map<Dependency, Set<ArtifactKey>> sourceMap = new HashMap<Dependency, Set<ArtifactKey>>();

    Visitor(MavenProject project) {
      dependencies = project.getOriginalModel().getDependencies();
    }

    IStatus getStatus() {
      if(statuses.isEmpty()) {
        return Status.OK_STATUS;
      }
      return new MultiStatus("org.eclipse.m2e.refactoring", 0, statuses.toArray(new IStatus[statuses.size()]),
          "Errors occurred", null);
    }

    Map<Dependency, Set<ArtifactKey>> getSourceMap() {
      return sourceMap;
    }

    private int depth;

    private DependencyNode topLevel;

    public boolean visitLeave(DependencyNode node) {
      depth-- ;
      return true;
      // TODO return status == null;
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
            if(topLevel != null) {
              // need to add exclusion to top-level dependency
              Dependency dependency = findDependency(topLevel);
              if(dependency != null) {
                put(dependency, key);
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