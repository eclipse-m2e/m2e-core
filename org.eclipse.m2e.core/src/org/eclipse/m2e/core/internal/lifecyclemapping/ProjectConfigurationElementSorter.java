/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecyclemapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.osgi.util.NLS;

import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;

import org.eclipse.m2e.core.internal.Messages;


/**
 * Sorts a list of Project Configurator Metadata according to their matching {@link IConfigurationElement}s
 */
public class ProjectConfigurationElementSorter {

  private List<String> sortedConfigurators;

  private Map<String, String> incompleteConfigurators;

  private Set<String> missingRequiredConfigurators;

  private final Set<String> allSecondaryConfigurators = new HashSet<>();

  private final Map<String, List<String>> primaryConfigurators = new HashMap<>();

  /**
   * Sorts a list of ids, ordering it by Project Configurator {@link IConfigurationElement}s
   *
   * @param configuratorIds, a collection of configurator ids to sort
   * @param configurators, a map of [id:project configurator's {@link IConfigurationElement}]
   * @throws CycleDetectedException if a cycle is detected between configurators
   */
  public ProjectConfigurationElementSorter(Collection<String> configuratorIds,
      Map<String, IConfigurationElement> configurators) throws CycleDetectedException {

    Assert.isNotNull(configurators, "configuratorConfigElements parameter can not be null");

    //DAG including required and optional configurators
    DAG fullDag = new DAG();

    //DAG including required configurators only
    DAG requirementsDag = new DAG();

    Map<String, String> _incompletes = new HashMap<>();
    Set<String> _missingIds = new HashSet<>();

    //Create a vertex for each configurator.
    for(String key : configuratorIds) {
      requirementsDag.addVertex(key);
      fullDag.addVertex(key);

      IConfigurationElement configurator = configurators.get(key);
      if(configurator == null) {
        _missingIds.add(key);
        continue;
      }
      //Add edge for configurators this configurator should run after
      String[] runsAfter = safeSplit(configurator.getAttribute("runsAfter"));

      //fallback to legacy secondaryTo attribute
      if(runsAfter == null) {
        String secondaryTo = configurator.getAttribute("secondaryTo");
        if(secondaryTo != null) {
          runsAfter = new String[] {secondaryTo};
        }
      }

      if(runsAfter != null && runsAfter.length > 0) {
        allSecondaryConfigurators.add(key);
        for(String id : runsAfter) {
          id = id.trim();
          if(id.isEmpty()) {
            continue;
          }
          boolean isRequired = !id.endsWith("?");
          String predecessorId = sanitize(id);
          if(isRequired) {
            requirementsDag.addEdge(key, predecessorId);
          }

          IConfigurationElement predecessor = configurators.get(predecessorId);
          if(predecessor == null) {
            if(isRequired) {
              _missingIds.add(predecessorId);
              _incompletes.put(key, NLS.bind(Messages.ProjectConfiguratorToRunAfterNotAvailable, key, predecessorId));
            }
          } else {
            fullDag.addEdge(key, predecessorId);
          }
        }
      }

      //Add edges for configurators this configurator should run before

      String[] runsBefore = safeSplit(configurator.getAttribute("runsBefore"));
      if(runsBefore != null) {
        for(String id : runsBefore) {
          id = id.trim();
          if(id.isEmpty()) {
            continue;
          }
          boolean isRequired = id.endsWith("*");
          String successorId = sanitize(id);
          if(isRequired) {
            requirementsDag.addEdge(successorId, key);
          }
          IConfigurationElement successor = configurators.get(successorId);
          if(successor == null) {
            if(isRequired) {
              //missing required matching configElement
              _missingIds.add(successorId);
              _incompletes.put(key, NLS.bind(Messages.ProjectConfiguratorToRunBeforeNotAvailable, key, successorId));
            }
          } else {
            fullDag.addEdge(successorId, key);
          }
        }
      }

    }

    //1st sort, leaving out optional configurators, to detect broken required dependencies
    List<String> sortedExecutions = TopologicalSorter.sort(requirementsDag);

    for(String id : sortedExecutions) {
      boolean isIncompleteOrMissing = _missingIds.contains(id) || _incompletes.containsKey(id);
      if(isIncompleteOrMissing) {
        Set<String> dependents = new HashSet<>();
        getDependents(id, requirementsDag, dependents);
        for(String next : dependents) {
          if(configuratorIds.contains(next) && (_missingIds.contains(next) || !_incompletes.containsKey(next))) {
            _incompletes.put(next, NLS.bind(Messages.ProjectConfiguratorNotAvailable, id, next));
          }
        }
      }
    }

    //2nd sort, including optional dependencies
    sortedExecutions = TopologicalSorter.sort(fullDag);

    List<String> _sortedConfigurators = new ArrayList<>(sortedExecutions.size());

    for(String id : sortedExecutions) {
      //Remove incomplete metadata
      if(!configuratorIds.contains(id) || _incompletes.containsKey(id) || _missingIds.contains(id)) {
        continue;
      }

      List<String> predecessors = fullDag.getChildLabels(id);

      boolean addAsPrimary = true;
      if(predecessors != null && !predecessors.isEmpty()) {
        for(String p : predecessors) {
          if(configuratorIds.contains(p)) {
            addAsPrimary = false;
            break;
          }
        }
      }
      if(addAsPrimary) {
        Set<String> secondaries = new LinkedHashSet<>();
        getDependents(id, fullDag, secondaries);
        primaryConfigurators.put(id, new ArrayList<>(secondaries));
        allSecondaryConfigurators.addAll(secondaries);
      }

      //add to resulting list
      _sortedConfigurators.add(id);
    }

    sortedConfigurators = Collections.unmodifiableList(_sortedConfigurators);
    incompleteConfigurators = Collections.unmodifiableMap(_incompletes);
    missingRequiredConfigurators = Collections.unmodifiableSet(_missingIds);
  }

  public ProjectConfigurationElementSorter(Map<String, IConfigurationElement> configurators)
      throws CycleDetectedException {
    this(configurators.keySet(), configurators);
  }

  private static void getDependents(String id, DAG dag, Set<String> dependents) {
    List<String> parents = dag.getParentLabels(id);
    if(parents == null || parents.isEmpty()) {
      return;
    }
    for(String parent : parents) {
      if(dependents.add(parent)) {
        getDependents(parent, dag, dependents);
      }
    }
  }

  private static String sanitize(String id) {
    return (id.endsWith("?") || id.endsWith("*")) ? id.substring(0, id.length() - 1) : id;
  }

  private static String[] safeSplit(String value) {
    return value == null ? null : value.split(",");
  }

  /**
   * @return a sorted, unmodifiable list of configurator ids
   */
  public List<String> getSortedConfigurators() {
    if(sortedConfigurators == null) {
      sortedConfigurators = Collections.emptyList();
    }
    return sortedConfigurators;
  }

  /**
   * @return an unmodifiable Map of ids of incomplete configurators; (each value corresponds to the reason why it was
   *         found to be incomplete)
   */
  public Map<String, String> getIncompleteConfigurators() {
    if(incompleteConfigurators == null) {
      incompleteConfigurators = Collections.emptyMap();
    }
    return incompleteConfigurators;
  }

  /**
   * @return an unmodifiable set of missing configurator ids.
   */
  public Set<String> getMissingConfigurators() {
    if(missingRequiredConfigurators == null) {
      missingRequiredConfigurators = Collections.emptySet();
    }
    return missingRequiredConfigurators;
  }

  /**
   * @return an ordered list of secondary configurator ids to this primaryConfigurator.
   */
  public List<String> getSecondaryConfigurators(String primaryConfigurator) {
    List<String> secondaries = primaryConfigurators.get(primaryConfigurator);
    if(secondaries == null) {
      secondaries = Collections.emptyList();
    }
    return secondaries;
  }

  /**
   * @return true if a configurator id is a root configurator (i.e. has no parent)
   */
  public boolean isRootConfigurator(String configuratorId) {
    if(configuratorId == null || incompleteConfigurators.containsKey(configuratorId)) {
      return false;
    }
    boolean isPrimary = primaryConfigurators.containsKey(configuratorId);
    boolean isSecondary = allSecondaryConfigurators.contains(configuratorId);
    boolean isRoot = (isPrimary && (primaryConfigurators.size() == 1 || !isSecondary)) || (!isPrimary && !isSecondary);
    return isRoot;
  }

  @Override
  public String toString() {
    return "ProjectConfigurationElementSorter [" + getSortedConfigurators() + "]";
  }

}
