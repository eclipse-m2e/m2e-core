/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Konrad Windszus
 *******************************************************************************/

package org.eclipse.m2e.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.util.tracker.ServiceTracker;


/**
 * Provides meaningful exception in case the service cannot be retrieved to better trace issues with services provided
 * by DS components.
 *
 * @param <T> the tracked service class
 */
public class ExtendedServiceTracker<T> implements AutoCloseable {

  private final Class<T> trackedClass;

  private final ServiceTracker<T, T> serviceTracker;

  private final ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime> scrServiceTracker;

  public ExtendedServiceTracker(BundleContext bundleContext, Class<T> trackedClass) {
    this.trackedClass = trackedClass;
    serviceTracker = new ServiceTracker<T, T>(bundleContext, trackedClass, null);
    serviceTracker.open();
    scrServiceTracker = new ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime>(bundleContext,
        ServiceComponentRuntime.class, null);
    scrServiceTracker.open();
  }

  public Optional<T> getOptional() {
    return Optional.ofNullable(serviceTracker.getService());
  }

  public T getNotNull() {
    T service = serviceTracker.getService();
    if(service == null) {
      ServiceComponentRuntime scr = scrServiceTracker.getService();
      if(scr == null) {
        throw new IllegalStateException("Could not get service of type " + trackedClass
            + " and 'ServiceComponentRuntime' is not available either for further debugging hints");
      }
      List<ComponentDescriptionDTO> components = getComponentDescriptionDTOs(scr, trackedClass);
      if(components.isEmpty()) {
        throw new IllegalStateException("Could not get service of type " + trackedClass
            + ". The providing bundle is probably not properly installed or was not started as no DS component was found providing that service interface");
      } else {
        // only dump the first found component
        throw new IllegalStateException("Could not get service of type " + trackedClass + ": Providing "
            + getRelevantComponentInfo(scr, components.get(0)));
      }
    }
    return service;
  }

  static List<ComponentDescriptionDTO> getComponentDescriptionDTOs(ServiceComponentRuntime scr,
      Class<?> serviceInterface) {
    return scr.getComponentDescriptionDTOs().stream()
        .filter(c -> Arrays.asList(c.serviceInterfaces).contains(serviceInterface.getName()))
        .collect(Collectors.toList());
  }

  public String getRelevantComponentInfo(ServiceComponentRuntime scr, ComponentDescriptionDTO component) {
    StringBuilder info = new StringBuilder("DS Component ").append(component.name).append(" from bundle ")
        .append(component.bundle.symbolicName);
    Collection<ComponentConfigurationDTO> componentConfigurations = scr.getComponentConfigurationDTOs(component);
    if(componentConfigurations.isEmpty()) {
      info.append(" is not available"); // why?
    } else {
      for(ComponentConfigurationDTO componentConfiguration : componentConfigurations) {
        info.append(" has state \"").append(getState(componentConfiguration)).append("\"");
      }
    }
    return info.toString();
  }

  static String getState(ComponentConfigurationDTO componentConfiguration) {
    String state;
    switch(componentConfiguration.state) {
      case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION:
        state = "Unsatisfied configuration";
        break;
      case ComponentConfigurationDTO.UNSATISFIED_REFERENCE:
        state = "Unsatisfied reference(s): "
            + String.join(", ", Arrays.asList(componentConfiguration.unsatisfiedReferences.toString()));
        break;
      case ComponentConfigurationDTO.SATISFIED:
        state = "Satisfied";
        break;
      case ComponentConfigurationDTO.ACTIVE:
        state = "Active";
        break;
      case ComponentConfigurationDTO.FAILED_ACTIVATION:
        state = "Activate method failed: " + componentConfiguration.failure;
        break;
      default:
        state = "Unknown state: " + componentConfiguration.state;
    }
    return state;
  }

  @Override
  public void close() throws Exception {
    serviceTracker.close();
    scrServiceTracker.close();
  }

}
