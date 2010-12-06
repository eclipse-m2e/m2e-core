/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecycle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.internal.project.IgnoreMojoProjectConfiguration;
import org.eclipse.m2e.core.internal.project.MojoExecutionProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.CustomizableLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.PluginExecutionFilter;


/**
 * LifecycleMappingFactory
 * 
 * @author igor
 */
public class LifecycleMappingFactory {

  public static final String EXTENSION_LIFECYCLE_MAPPINGS = IMavenConstants.PLUGIN_ID + ".lifecycleMappings"; //$NON-NLS-1$

  public static final String EXTENSION_PROJECT_CONFIGURATORS = IMavenConstants.PLUGIN_ID + ".projectConfigurators"; //$NON-NLS-1$

  private static final String ELEMENT_LIFECYCLE_MAPPING = "lifecycleMapping"; //$NON-NLS-1$

  private static final String ATTR_CLASS = "class"; //$NON-NLS-1$

  private static final String ATTR_PACKAGING_TYPE = "packaging-type"; //$NON-NLS-1$

  private static final String ATTR_ID = "id"; //$NON-NLS-1$

  private static final String ELEMENT_CONFIGURATOR = "configurator"; //$NON-NLS-1$

  private static final String ELEMENT_MOJO = "mojo"; //$NON-NLS-1$

  private static final String ELEMENT_IGNORE = "ignore"; //$NON-NLS-1$

  private static final String ELEMENT_EXECUTE = "execute";

  private static final String ATTR_GROUPID = "groupId";

  private static final String ATTR_ARTIFACTID = "artifactId";

  private static final String ATTR_VERSIONRANGE = "versionRange";

  private static final String ATTR_GOALS = "goals";

  /**
   * Returns default lifecycle mapping for specified packaging type or null if no such lifecycle mapping
   */
  public static ILifecycleMapping getLifecycleMappingFor(String packagingType) {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_LIFECYCLE_MAPPINGS);
    if(configuratorsExtensionPoint != null) {
      IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
      for(IExtension extension : configuratorExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_LIFECYCLE_MAPPING)) {
            if(packagingType.equals(element.getAttribute(ATTR_PACKAGING_TYPE))) {
              return createLifecycleMapping(element);
            }
          }
        }
      }
    }
    return null;
  }

  protected static ILifecycleMapping createLifecycleMapping(IConfigurationElement element) {
    try {
      ILifecycleMapping mapping = (ILifecycleMapping) element.createExecutableExtension(ATTR_CLASS);
      if(mapping instanceof CustomizableLifecycleMapping) {
        CustomizableLifecycleMapping customizable = (CustomizableLifecycleMapping) mapping;
        for(IConfigurationElement mojo : element.getChildren(ELEMENT_MOJO)) {
          AbstractProjectConfigurator configurator = null;
          if(mojo.getChildren(ELEMENT_IGNORE).length > 0) {
            configurator = new IgnoreMojoProjectConfiguration();
          } else if(mojo.getChildren(ELEMENT_EXECUTE).length > 0) {
            configurator = createMojoExecution(mojo.getChildren(ELEMENT_EXECUTE)[0]);
          } else if(mojo.getChildren(ELEMENT_CONFIGURATOR).length > 0) {
            String configuratorId = mojo.getChildren(ELEMENT_CONFIGURATOR)[0].getAttribute(ATTR_ID);
            configurator = createProjectConfigurator(configuratorId, true/*bare*/);
          } else {
            MavenLogger.log("Invalid lifecycle mapping configuration element: " + mojo.toString());
          }
          if(configurator != null) {
            configurator.addPluginExecutionFilter(createPluginExecutionFilter(mojo));
            customizable.addConfigurator(configurator);
          }
        }
      }
      return mapping;
    } catch(CoreException ex) {
      MavenLogger.log(ex);
    }
    return null;
  }

  private static AbstractProjectConfigurator createMojoExecution(IConfigurationElement configuration) {
    boolean runOnIncremental = true; // TODO
    return new MojoExecutionProjectConfigurator(runOnIncremental);
  }

  private static PluginExecutionFilter createPluginExecutionFilter(IConfigurationElement configuration) {
    String groupId = configuration.getAttribute(ATTR_GROUPID);
    String artifactId = configuration.getAttribute(ATTR_ARTIFACTID);
    String versionRange = configuration.getAttribute(ATTR_VERSIONRANGE);
    String goals = configuration.getAttribute(ATTR_GOALS);
    return new PluginExecutionFilter(groupId, artifactId, versionRange, goals);
  }

  public static ILifecycleMapping getLifecycleMapping(String mappingId) {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_LIFECYCLE_MAPPINGS);
    if(configuratorsExtensionPoint != null) {
      IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
      for(IExtension extension : configuratorExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_LIFECYCLE_MAPPING)) {
            if(mappingId.equals(element.getAttribute(ATTR_ID)))
              return createLifecycleMapping(element);
          }
        }
      }
    }
    return null;
  }

  public static AbstractProjectConfigurator getProjectConfigurator(String configuratorId) {
    return createProjectConfigurator(configuratorId, false/*bare*/);
  }

  protected static AbstractProjectConfigurator createProjectConfigurator(String configuratorId, boolean bare) {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_PROJECT_CONFIGURATORS);
    if(configuratorsExtensionPoint != null) {
      IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
      for(IExtension extension : configuratorExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_CONFIGURATOR)) {
            if(configuratorId.equals(element.getAttribute(AbstractProjectConfigurator.ATTR_ID))) {
              try {
                AbstractProjectConfigurator configurator = (AbstractProjectConfigurator) element
                    .createExecutableExtension(AbstractProjectConfigurator.ATTR_CLASS);

                MavenPlugin plugin = MavenPlugin.getDefault();
                configurator.setProjectManager(plugin.getMavenProjectManager());
                configurator.setMavenConfiguration(plugin.getMavenConfiguration());
                configurator.setMarkerManager(plugin.getMavenMarkerManager());
                configurator.setConsole(plugin.getConsole());

                if(!bare) {
                  for(IConfigurationElement mojo : element.getChildren(ELEMENT_MOJO)) {
                    configurator.addPluginExecutionFilter(createPluginExecutionFilter(mojo));
                  }
                }

                return configurator;
              } catch(CoreException ex) {
                MavenLogger.log(ex);
              }
            }
          }
        }
      }
    }
    return null;
  }

  public static AbstractProjectConfigurator createProjectConfiguratorFor(MojoExecution execution) {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_PROJECT_CONFIGURATORS);
    if(configuratorsExtensionPoint != null) {
      IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
      for(IExtension extension : configuratorExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_CONFIGURATOR)) {
            if(isConfiguratorEnabledFor(element, execution)) {
              try {
                AbstractProjectConfigurator configurator = (AbstractProjectConfigurator) element
                    .createExecutableExtension(AbstractProjectConfigurator.ATTR_CLASS);

                MavenPlugin plugin = MavenPlugin.getDefault();
                configurator.setProjectManager(plugin.getMavenProjectManager());
                configurator.setMavenConfiguration(plugin.getMavenConfiguration());
                configurator.setMarkerManager(plugin.getMavenMarkerManager());
                configurator.setConsole(plugin.getConsole());

                for(IConfigurationElement mojo : element.getChildren(ELEMENT_MOJO)) {
                  configurator.addPluginExecutionFilter(createPluginExecutionFilter(mojo));
                }

                return configurator;
              } catch(CoreException ex) {
                MavenLogger.log(ex);
              }
            }
          }
        }
      }
    }
    return null;
  }

  private static boolean isConfiguratorEnabledFor(IConfigurationElement configuration, MojoExecution execution) {
    for(IConfigurationElement mojo : configuration.getChildren(ELEMENT_MOJO)) {
      if(createPluginExecutionFilter(mojo).match(execution)) {
        return true;
      }
    }
    return false;
  }
}
