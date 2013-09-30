/*******************************************************************************
 * Copyright (c) 2008-2013 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.util.Map.Entry;
import java.util.Set;

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
  private Map<IMavenProjectFacade, List<ILifecycleMappingRequirement>> allProjects = new LinkedHashMap<IMavenProjectFacade, List<ILifecycleMappingRequirement>>();

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
  private final Set<IMavenDiscoveryProposal> selectedProposals = new LinkedHashSet<IMavenDiscoveryProposal>();

  private Map<IMavenProjectFacade, Throwable> errors = new LinkedHashMap<IMavenProjectFacade, Throwable>();

  public LifecycleMappingDiscoveryRequest() {
  }

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
    List<IMavenDiscoveryProposal> result = allproposals.get(requirement);
    if(result == null) {
      return Collections.emptyList();
    }
    return result;
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

    List<IMavenDiscoveryProposal> proposals = allproposals.get(requirement);
    if(proposals != null) {
      for(IMavenDiscoveryProposal proposal : proposals) {
        if(selectedProposals.contains(proposal)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Returns true if mapping configuration is complete after applying selected proposals.
   */
  public boolean isMappingComplete() {
    for(ILifecycleMappingRequirement packagingRequirement : getRequirements()) {
      if(!isRequirementSatisfied(packagingRequirement)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Automatically selects proposals when there is only one possible solution to a problem.
   */

  public void autoCompleteMapping() {

    for(Entry<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> entry : getAllProposals().entrySet()) {

      List<IMavenDiscoveryProposal> proposals = entry.getValue();
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
    return new ArrayList<IMavenDiscoveryProposal>(selectedProposals);
  }

  public void clearSelectedProposals() {
    selectedProposals.clear();
  }

  public void addProject(IMavenProjectFacade facade, ILifecycleMappingRequirement requirement) {
    if(facade != null && requirement != null) {
      List<ILifecycleMappingRequirement> requirements = allProjects.get(facade);
      if(requirements == null) {
        requirements = new ArrayList<ILifecycleMappingRequirement>();
      }
      requirements.add(requirement);
      allProjects.put(facade, requirements);
    }
  }

  public void addError(IMavenProjectFacade facade, Throwable th) {
    errors.put(facade, th);
  }

  public Map<IMavenProjectFacade, Throwable> getErrors() {
    return errors;
  }

  public Collection<ILifecycleMappingRequirement> getRequirements() {
    if(allProjects == null || allProjects.isEmpty()) {
      return Collections.emptyList();
    }
    Set<ILifecycleMappingRequirement> requirements = new LinkedHashSet<ILifecycleMappingRequirement>();
    for(Entry<IMavenProjectFacade, List<ILifecycleMappingRequirement>> entry : allProjects.entrySet()) {
      requirements.addAll(entry.getValue());
    }
    return requirements;
  }

}
