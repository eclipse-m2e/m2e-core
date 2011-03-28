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

package org.eclipse.m2e.core.embedder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.collection.DependencyGraphTransformationContext;
import org.sonatype.aether.collection.DependencyGraphTransformer;
import org.sonatype.aether.collection.UnsolvableVersionConflictException;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.util.graph.DefaultDependencyNode;
import org.sonatype.aether.util.graph.transformer.ConflictIdSorter;
import org.sonatype.aether.util.graph.transformer.TransformationContextKeys;
import org.sonatype.aether.version.Version;
import org.sonatype.aether.version.VersionConstraint;


class NearestVersionConflictResolver implements DependencyGraphTransformer {

  public DependencyNode transformGraph(DependencyNode node, DependencyGraphTransformationContext context)
      throws RepositoryException {
    List<?> sortedConflictIds = (List<?>) context.get(TransformationContextKeys.SORTED_CONFLICT_IDS);
    if(sortedConflictIds == null) {
      ConflictIdSorter sorter = new ConflictIdSorter();
      sorter.transformGraph(node, context);

      sortedConflictIds = (List<?>) context.get(TransformationContextKeys.SORTED_CONFLICT_IDS);
    }

    Map<?, ?> conflictIds = (Map<?, ?>) context.get(TransformationContextKeys.CONFLICT_IDS);
    if(conflictIds == null) {
      throw new RepositoryException("conflict groups have not been identified");
    }

    Map<DependencyNode, Integer> depths = new IdentityHashMap<DependencyNode, Integer>(conflictIds.size());
    for(Object key : sortedConflictIds) {
      ConflictGroup group = new ConflictGroup(key);
      depths.clear();
      selectVersion(node, null, 0, depths, group, conflictIds);
      pruneNonSelectedVersions(group, conflictIds);
    }

    return node;
  }

  private void selectVersion(DependencyNode node, DependencyNode parent, int depth,
      Map<DependencyNode, Integer> depths, ConflictGroup group, Map<?, ?> conflictIds) throws RepositoryException {
    Integer smallestDepth = depths.get(node);
    if(smallestDepth == null || smallestDepth.intValue() > depth) {
      depths.put(node, Integer.valueOf(depth));
    } else {
      return;
    }

    Object key = conflictIds.get(node);
    if(group.key.equals(key)) {
      Position pos = new Position(parent, depth);

      if(parent != null) {
        group.positions.add(pos);
      }

      VersionConstraint constraint = node.getVersionConstraint();

      boolean backtrack = false;
      boolean hardConstraint = !constraint.getRanges().isEmpty();

      if(hardConstraint) {
        if(group.constraints.add(constraint)) {
          if(group.version != null && !constraint.containsVersion(group.version)) {
            backtrack = true;
          }
        }
      }

      if(isAcceptable(group, node.getVersion())) {
        group.candidates.put(node, pos);

        if(backtrack) {
          backtrack(group);
        } else if(group.version == null || isNearer(pos, node.getVersion(), group.position, group.version)) {
          group.winner = node;
          group.version = node.getVersion();
          group.position = pos;
        }
      } else {
        if(backtrack) {
          backtrack(group);
        }
        return;
      }
    }

    depth++ ;

    for(DependencyNode child : node.getChildren()) {
      selectVersion(child, node, depth, depths, group, conflictIds);
    }
  }

  private boolean isAcceptable( ConflictGroup group, Version version )
  {
      for ( VersionConstraint constraint : group.constraints )
      {
          if ( !constraint.containsVersion( version ) )
          {
              return false;
          }
      }
      return true;
  }

  private void backtrack(ConflictGroup group) throws UnsolvableVersionConflictException {
    group.winner = null;
    group.version = null;

    for(Iterator<Map.Entry<DependencyNode, Position>> it = group.candidates.entrySet().iterator(); it.hasNext();) {
      Map.Entry<DependencyNode, Position> entry = it.next();

      Version version = entry.getKey().getVersion();
      Position pos = entry.getValue();

      if(!isAcceptable(group, version)) {
        it.remove();
      } else if(group.version == null || isNearer(pos, version, group.position, group.version)) {
        group.winner = entry.getKey();
        group.version = version;
        group.position = pos;
      }
    }

    if(group.version == null) {
      throw newFailure(group);
    }
  }

  private UnsolvableVersionConflictException newFailure(ConflictGroup group) {
    Collection<String> versions = new LinkedHashSet<String>();
    for(VersionConstraint constraint : group.constraints) {
      versions.add(constraint.toString());
    }
    return new UnsolvableVersionConflictException(group.key, versions);
  }

  private boolean isNearer(Position pos1, Version ver1, Position pos2, Version ver2) {
    if(pos1.depth < pos2.depth) {
      return true;
    } else if(pos1.depth == pos2.depth && pos1.parent == pos2.parent && ver1.compareTo(ver2) > 0) {
      return true;
    }
    return false;
  }

  private void pruneNonSelectedVersions(ConflictGroup group, Map<?, ?> conflictIds) {
    for(Position pos : group.positions) {
      ConflictNode conflictNode = null;

      for(ListIterator<DependencyNode> it = pos.parent.getChildren().listIterator(); it.hasNext();) {
        DependencyNode child = it.next();

        Object key = conflictIds.get(child);

        if(group.key.equals(key)) {
          if(!group.pruned && group.position.depth == pos.depth && group.version.equals(child.getVersion())) {
            group.pruned = true;
          } else if(pos.equals(group.position)) {
            it.remove();
          } else if(conflictNode == null) {
            conflictNode = new ConflictNode(child, group.winner);
            it.set(conflictNode);
          } else {
            it.remove();

            if(conflictNode.getVersion().compareTo(child.getVersion()) < 0) {
              conflictNode.setDependency(child.getDependency());
              conflictNode.setVersion(child.getVersion());
              conflictNode.setVersionConstraint(child.getVersionConstraint());
              conflictNode.setPremanagedVersion(child.getPremanagedVersion());
              conflictNode.setRelocations(child.getRelocations());
            }
          }
        }
      }
    }
  }

  static final class ConflictGroup {

    final Object key;

    final Collection<VersionConstraint> constraints = new HashSet<VersionConstraint>();

    final Map<DependencyNode, Position> candidates = new IdentityHashMap<DependencyNode, Position>(32);

    DependencyNode winner;

    Version version;

    Position position;

    final Collection<Position> positions = new LinkedHashSet<Position>();

    boolean pruned;

    public ConflictGroup(Object key) {
      this.key = key;
      this.position = new Position(null, Integer.MAX_VALUE);
    }

    @Override
    public String toString() {
      return key + " > " + version; //$NON-NLS-1$
    }

  }

  static final class Position {

    final DependencyNode parent;

    final int depth;

    final int hash;

    public Position(DependencyNode parent, int depth) {
      this.parent = parent;
      this.depth = depth;
      hash = 31 * System.identityHashCode(parent) + depth;
    }

    @Override
    public boolean equals(Object obj) {
      if(this == obj) {
        return true;
      } else if(!(obj instanceof Position)) {
        return false;
      }
      Position that = (Position) obj;
      return this.parent == that.parent && this.depth == that.depth;
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public String toString() {
      return depth + " > " + parent; //$NON-NLS-1$
    }

  }

  static final class ConflictNode extends DefaultDependencyNode {

    public ConflictNode(DependencyNode node, DependencyNode related) {
      super(node);
      setAliases(Collections.singletonList(related.getDependency().getArtifact()));
    }
  }

}
