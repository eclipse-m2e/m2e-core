/*******************************************************************************
 * Copyright (c) 2008-2013 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation as o.e.m.c.i.l.d.LifecycleMappingMapping
 *      Red Hat, Inc. - refactored as LifecycleMappingDiscoveryRequest
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecyclemapping.discovery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * Holder object, used to discover proposals satisfying lifecycle mapping requirements
 *
 * @since 1.5
 * @author Igor Fedorenko
 * @author Fred Bricon
 */
public class LifecycleMappingDiscoveryRequest {

  /**
   * All proposals to satisfy mapping requirements
   */
  private final Map<IMavenProjectFacade, List<ILifecycleMappingRequirement>> allProjects = new LinkedHashMap<>();

  public Map<IMavenProjectFacade, List<ILifecycleMappingRequirement>> getProjects() {
    return this.allProjects;
  }

  /**
   * All proposals to satisfy mapping requirements
   */
  private Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> allproposals;

  /**
   * Mapping proposals selected for implementation, i.e. bundles to be installed and mojo executions to be ignored.
   */
  private final Set<IMavenDiscoveryProposal> selectedProposals = new LinkedHashSet<>();

  private final Map<IMavenProjectFacade, Throwable> errors = new LinkedHashMap<>();

  public void setProposals(Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposals) {
    this.allproposals = proposals;
  }

  /**
   * Returns all proposals available for provided requirement or empty List.
   */
  public List<IMavenDiscoveryProposal> getProposals(ILifecycleMappingRequirement requirement) {
    if(allproposals == null || requirement == null) {
      return Collections.emptyList();
    }
    return allproposals.getOrDefault(requirement, Collections.emptyList());
  }

  public Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> getAllProposals() {
    if(allproposals == null) {
      return Collections.emptyMap();
    }
    return allproposals;
  }

  public void addSelectedProposal(IMavenDiscoveryProposal proposal) {
    selectedProposals.add(proposal);
  }

  public void removeSelectedProposal(IMavenDiscoveryProposal proposal) {
    selectedProposals.remove(proposal);
  }

  public boolean isRequirementSatisfied(ILifecycleMappingRequirement requirement) {
    if(requirement == null) {
      return true;
    }

    if(allproposals == null || allproposals.isEmpty()) {
      return false;
    }
    List<IMavenDiscoveryProposal> proposals = allproposals.getOrDefault(requirement, Collections.emptyList());
    return proposals.stream().anyMatch(selectedProposals::contains);
  }

  /**
   * Returns true if mapping configuration is complete after applying selected proposals.
   */
  public boolean isMappingComplete() {
    return getRequirements().stream().noneMatch(r -> !isRequirementSatisfied(r));
  }

  /**
   * Automatically selects proposals when there is only one possible solution to a problem.
   */

  public void autoCompleteMapping() {
    for(List<IMavenDiscoveryProposal> proposals : getAllProposals().values()) {
      if(proposals != null && proposals.size() == 1) {
        addSelectedProposal(proposals.get(0));
      }
    }
  }

  public IMavenDiscoveryProposal getSelectedProposal(ILifecycleMappingRequirement requirement) {
    if(allproposals == null) {
      return null;
    }
    List<IMavenDiscoveryProposal> proposals = allproposals.get(requirement);
    if(proposals == null) {
      return null;
    }
    for(IMavenDiscoveryProposal proposal : proposals) {
      if(selectedProposals.contains(proposal)) {
        return proposal;
      }
    }
    return null;
  }

  public List<IMavenDiscoveryProposal> getSelectedProposals() {
    return new ArrayList<>(selectedProposals);
  }

  public void clearSelectedProposals() {
    selectedProposals.clear();
  }

  public void addProject(IMavenProjectFacade facade, ILifecycleMappingRequirement requirement) {
    if(facade != null && requirement != null) {
      List<ILifecycleMappingRequirement> requirements = allProjects.computeIfAbsent(facade, f -> new ArrayList<>());
      requirements.add(requirement);
    }
  }

  public void addError(IMavenProjectFacade facade, Throwable th) {
    errors.put(facade, th);
  }

  public Map<IMavenProjectFacade, Throwable> getErrors() {
    return errors;
  }

  public Collection<ILifecycleMappingRequirement> getRequirements() {
    if(allProjects.isEmpty()) {
      return Collections.emptyList();
    }
    return allProjects.values().stream().flatMap(List::stream).collect(Collectors.toCollection(LinkedHashSet::new));
  }

}
