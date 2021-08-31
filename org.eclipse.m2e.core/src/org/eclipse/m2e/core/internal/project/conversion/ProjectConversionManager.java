/*******************************************************************************
 * Copyright (c) 2012-2013 Red Hat, Inc.
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

package org.eclipse.m2e.core.internal.project.conversion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.util.dag.CycleDetectedException;

import org.apache.maven.model.Model;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.conversion.AbstractProjectConversionParticipant;
import org.eclipse.m2e.core.project.conversion.IProjectConversionEnabler;
import org.eclipse.m2e.core.project.conversion.IProjectConversionManager;


/**
 * Manages conversion of existing Eclipse projects into Maven ones. <br/>
 * Looks up for {@link AbstractProjectConversionParticipant} contributed by 3rd party eclipse plugins.
 *
 * @author Fred Bricon
 * @since 1.1
 */
public class ProjectConversionManager implements IProjectConversionManager {

  private static final String CONVERSION_PARTICIPANTS_EXTENSION_POINT = "org.eclipse.m2e.core.projectConversionParticipants";

  private static final Logger log = LoggerFactory.getLogger(ProjectConversionManager.class);

  private static final int DEFAULT_WEIGHT = 50;

  private static IProjectConversionEnabler[] enablers;

  private static final String CONVERSION_ENABLER_EXTENSION_POINT = "org.eclipse.m2e.core.conversionEnabler";

  private static List<AbstractProjectConversionParticipant> lookupConversionParticipants(IProject project) {
    List<AbstractProjectConversionParticipant> participants = new ArrayList<>();

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint conversionExtensionPoint = registry.getExtensionPoint(CONVERSION_PARTICIPANTS_EXTENSION_POINT);
    Map<String, Set<String>> restrictedPackagings = new HashMap<>();
    if(conversionExtensionPoint != null) {
      IExtension[] archetypesExtensions = conversionExtensionPoint.getExtensions();
      for(IExtension extension : archetypesExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if("projectConversionParticipant".equals(element.getName())) {
            try {
              if(project.hasNature(element.getAttribute("nature"))) {
                AbstractProjectConversionParticipant projectConversionParticipant = (AbstractProjectConversionParticipant) element
                    .createExecutableExtension("class");
                participants.add(projectConversionParticipant);
              }
            } catch(CoreException ex) {
              log.debug("Can not load IProjectConversionParticipant", ex);
            }
          } else if("conversionParticipantConfiguration".equals(element.getName())) {
            setRestrictedPackagings(restrictedPackagings, element);
          }
        }
      }

      for(AbstractProjectConversionParticipant cp : participants) {
        Set<String> newPackagings = restrictedPackagings.get(cp.getId());
        if(newPackagings != null) {
          for(String p : newPackagings) {
            cp.addRestrictedPackaging(p);
          }
        }
      }
    }
    return participants;
  }

  private static void setRestrictedPackagings(Map<String, Set<String>> restrictedPackagings,
      IConfigurationElement element) {
    String pid = element.getAttribute("conversionParticipantId");
    String packagesAsString = element.getAttribute("compatiblePackagings");
    if(pid != null && packagesAsString != null) {
      try {
        String[] packagingsArray = packagesAsString.split(",");
        Set<String> packagings = new HashSet<>(packagingsArray.length);
        for(String packaging : packagingsArray) {
          String p = packaging.trim();
          if(p.length() > 0) {
            packagings.add(p);
          }
        }

        Set<String> allPackages = restrictedPackagings.get(pid);
        if(allPackages == null) {
          allPackages = new HashSet<>();
          restrictedPackagings.put(pid, allPackages);
        }

        allPackages.addAll(packagings);

      } catch(Exception e) {
        log.debug("Cannot parse restricted packagings ", e);
      }
    }
  }

  @Override
  public void convert(IProject project, Model model, IProgressMonitor monitor) throws CoreException {
    if(model == null) {
      return;
    }
    List<AbstractProjectConversionParticipant> participants = getConversionParticipants(project, model.getPackaging());
    if(participants != null) {
      for(AbstractProjectConversionParticipant participant : participants) {
        participant.convert(project, model, monitor);
      }
    }
  }

  @Override
  @Deprecated
  public List<AbstractProjectConversionParticipant> getConversionParticipants(IProject project) throws CoreException {
    return getConversionParticipants(project, null);
  }

  @Override
  public List<AbstractProjectConversionParticipant> getConversionParticipants(IProject project, String packaging)
      throws CoreException {
    List<AbstractProjectConversionParticipant> allParticipants = lookupConversionParticipants(project);
    List<AbstractProjectConversionParticipant> participants = new ArrayList<>();
    if(allParticipants != null) {
      for(AbstractProjectConversionParticipant participant : allParticipants) {
        if(packaging != null && !participant.isPackagingCompatible(packaging)) {
          continue;
        }
        if(participant.accept(project)) {
          participants.add(participant);
        }
      }
    }

    //Sort the remaining conversion participants
    try {
      ProjectConversionParticipantSorter sorter = new ProjectConversionParticipantSorter(participants);
      return Collections.unmodifiableList(sorter.getSortedConverters());
    } catch(CycleDetectedException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, ex.getMessage()));
    }
  }

  private static IProjectConversionEnabler[] loadProjectConversionEnablers() {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IConfigurationElement[] cf = registry.getConfigurationElementsFor(CONVERSION_ENABLER_EXTENSION_POINT);
    List<IConfigurationElement> list = Arrays.asList(cf);

    Comparator<IConfigurationElement> c = (o1, o2) -> {
      String o1String, o2String;
      int o1int, o2int;
      o1String = o1.getAttribute("weight");
      o2String = o2.getAttribute("weight");
      try {
        o1int = Integer.parseInt(o1String);
      } catch(NumberFormatException nfe1) {
        o1int = DEFAULT_WEIGHT;
      }
      try {
        o2int = Integer.parseInt(o2String);
      } catch(NumberFormatException nfe2) {
        o2int = DEFAULT_WEIGHT;
      }
      return o2int - o1int;
    };
    Collections.sort(list, c);
    ArrayList<IProjectConversionEnabler> retList = new ArrayList<>();
    Iterator<IConfigurationElement> i = list.iterator();
    while(i.hasNext()) {
      try {
        IConfigurationElement element = i.next();
        retList.add((IProjectConversionEnabler) element.createExecutableExtension("class"));
        if(log.isDebugEnabled()) {
          String id = element.getAttribute("id");
          String sWeight = element.getAttribute("weight");
          log.debug("Project conversion enabler found - id: {}, weight: {}", id, sWeight);
        }
      } catch(CoreException ce) {
        log.error(ce.getMessage(), ce);
      }
    }
    return retList.toArray(new IProjectConversionEnabler[retList.size()]);
  }

  @Override
  public IProjectConversionEnabler getConversionEnablerForProject(IProject project) {
    if(enablers == null) {
      enablers = loadProjectConversionEnablers();
    }
    IProjectConversionEnabler result = null;

    for(IProjectConversionEnabler enabler : enablers) {
      if(enabler.accept(project)) {
        result = enabler;
        break;
      }
    }
    if(log.isDebugEnabled()) {
      if(result != null)
        log.debug("Project conversion enabler found for project: {} - Class: {} ", project.getName(), result.getClass()
            .getName());
      else
        log.debug("Project conversion enabler not found for project: {}", project.getName());
    }
    return result;
  }

}
