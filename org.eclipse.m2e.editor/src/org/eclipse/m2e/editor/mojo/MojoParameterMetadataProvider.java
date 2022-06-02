/*******************************************************************************
 * Copyright (c) 2015 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Anton Tanasenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.mojo;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.PlatformUI;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import org.apache.maven.DefaultMaven;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.UrlModelSource;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.embedder.MavenExecutionContext;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.eclipse.m2e.editor.MavenEditorPlugin;


/**
 * @author atanasenko
 */
@SuppressWarnings("restriction")
public class MojoParameterMetadataProvider implements IMojoParameterMetadataProvider {

  private static final Logger log = LoggerFactory.getLogger(MojoParameterMetadataProvider.class);

  private static final Cache<String, MojoParameter> cache = CacheBuilder.newBuilder().maximumSize(100).softValues()
      .build();

  protected final MavenImpl maven;

  public MojoParameterMetadataProvider() {
    maven = (MavenImpl) MavenPlugin.getMaven();
  }

  @Override
  public MojoParameter getClassConfiguration(final ArtifactKey pluginKey, final String className) throws CoreException {

    try {
      String key = pluginKey.toPortableString() + "/" + className;

      return cache.get(key, () -> execute(pluginKey, (context, monitor) -> {
        PluginDescriptor pd = getPluginDescriptor(pluginKey, context, monitor);

        List<MojoParameter> parameters = null;
        if(pd != null) {
          Class<?> clazz;
          try {
            clazz = pd.getClassRealm().loadClass(className);
          } catch(ClassNotFoundException ex) {
            return null;
          }
          parameters = new PlexusConfigHelper().loadParameters(pd.getClassRealm(), clazz, monitor);
        }
        return new MojoParameter("", className, parameters); //$NON-NLS-1$
      }));

    } catch(ExecutionException e) {
      Throwable t = e.getCause();
      if(t instanceof CoreException coreEx) {
        throw coreEx;
      }
      if(t instanceof RuntimeException runtimeEx) {
        throw runtimeEx;
      }
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, e.getMessage(), e));
    }
  }

  @Override
  public MojoParameter getMojoConfiguration(ArtifactKey pluginKey, final Collection<String> mojos)
      throws CoreException {
    List<MojoParameter> params = new ArrayList<>();
    Set<String> collected = new HashSet<>();
    for(String mojo : mojos) {
      MojoParameter md = getMojoConfiguration(pluginKey, mojo);
      for(MojoParameter p : md.getNestedParameters()) {
        if(!collected.add(p.getName()))
          continue;
        params.add(p);
      }
    }
    return new MojoParameter("", "", params); //$NON-NLS-1$ //$NON-NLS-2$
  }

  @Override
  public MojoParameter getMojoConfiguration(ArtifactKey pluginKey) throws CoreException {
    return getMojoConfiguration(pluginKey, "*"); //$NON-NLS-1$
  }

  @Override
  public MojoParameter getMojoConfiguration(final ArtifactKey pluginKey, final String mojo) throws CoreException {

    MojoParameter predefParameters = getPredefined(pluginKey);
    if(predefParameters != null) {
      return predefParameters;
    }

    String key = pluginKey.toPortableString() + "/mojo/" + (mojo == null ? "*" : mojo); //$NON-NLS-1$ //$NON-NLS-2$

    try {
      return cache.get(key, () -> execute(pluginKey, (context, monitor) -> {
        PluginDescriptor pd = getPluginDescriptor(pluginKey, context, monitor);

        List<MojoParameter> parameters = null;
        if(pd != null) {
          parameters = loadMojoParameters(pd, mojo, monitor);
        }
        return new MojoParameter("", mojo, parameters); //$NON-NLS-1$
      }));

    } catch(ExecutionException e) {
      Throwable t = e.getCause();
      if(t instanceof CoreException coreEx) {
        throw coreEx;
      }
      if(t instanceof RuntimeException runtimeEx) {
        throw runtimeEx;
      }
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, e.getMessage(), e));
    }
  }

  List<MojoParameter> loadMojoParameters(PluginDescriptor desc, String goal, IProgressMonitor monitor)
      throws CoreException {

    PlexusConfigHelper helper = new PlexusConfigHelper();

    if("*".equals(goal)) { //$NON-NLS-1$
      List<MojoParameter> parameters = new ArrayList<>();
      Set<String> collected = new HashSet<>();
      for(MojoDescriptor mojo : desc.getMojos()) {
        for(MojoParameter p : loadMojoParameters(desc, mojo, helper, monitor)) {
          if(collected.add(p.getName())) {
            parameters.add(p);
          }
        }
      }
      return parameters;
    }
    return loadMojoParameters(desc, desc.getMojo(goal), helper, monitor);
  }

  private MojoParameter getPredefined(ArtifactKey pluginKey) {
    return PREDEF.get(pluginKey.getGroupId() + ":" + pluginKey.getArtifactId() + ":" + pluginKey.getVersion());
  }

  <T> T execute(final ArtifactKey pluginKey, final ICallable<T> callable) throws CoreException {

    final Object[] result = new Object[1];
    final CoreException[] innerException = new CoreException[1];

    try {
      PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(true, true, monitor -> {
        monitor.beginTask(org.eclipse.m2e.editor.internal.Messages.PomTemplateContext_resolvingPlugin, 100);
        try {

          MavenExecutionContext context = maven.createExecutionContext();
          context.getExecutionRequest().setCacheTransferError(false);
          T res = context.execute((context1, monitor1) -> {
            MavenProject mp = getProject(context1);
            if(mp != null) {
              return context1.execute(mp, callable, monitor1);
            }
            return null;
          }, monitor);

          if(monitor.isCanceled())
            return;
          result[0] = res;

        } catch(CoreException ex) {
          if(monitor.isCanceled())
            return;
          innerException[0] = ex;
        }

      });
    } catch(InvocationTargetException | InterruptedException ex) {
      throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, ex.getMessage(), ex));
    }
    if(innerException[0] != null) {
      throw innerException[0];
    }

    @SuppressWarnings("unchecked")
    T res = (T) result[0];
    return res;
  }

  MavenProject getProject(IMavenExecutionContext context) {

    ModelSource modelSource = new UrlModelSource(DefaultMaven.class.getResource("project/standalone.xml")); //$NON-NLS-1$
    try {
      return lookup(ProjectBuilder.class).build(modelSource, context.newProjectBuildingRequest()).getProject();
    } catch(ProjectBuildingException | CoreException ex) {
      log.warn(ex.getMessage());
      return null;
    }
  }

  PluginDescriptor getPluginDescriptor(ArtifactKey pluginKey, IMavenExecutionContext context,
      IProgressMonitor monitor) {
    PluginDescriptor desc;

    Plugin plugin = new Plugin();
    plugin.setGroupId(pluginKey.getGroupId());
    plugin.setArtifactId(pluginKey.getArtifactId());
    plugin.setVersion(pluginKey.getVersion());

    List<RemoteRepository> remoteRepos = context.getSession().getCurrentProject().getRemotePluginRepositories();
    try {
      desc = lookup(MavenPluginManager.class).getPluginDescriptor(plugin, remoteRepos, context.getRepositorySession());
      lookup(BuildPluginManager.class).getPluginRealm(context.getSession(), desc);
      return desc;
    } catch(Exception ex) {
      log.warn(ex.getMessage());
      return null;
    }

  }

  <T> T lookup(Class<T> clazz) throws CoreException {
    try {
      return maven.getPlexusContainer().lookup(clazz);
    } catch(ComponentLookupException ex) {
      throw new CoreException(
          new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, Messages.MavenImpl_error_lookup, ex));
    }
  }

  List<MojoParameter> loadMojoParameters(PluginDescriptor desc, MojoDescriptor mojo, PlexusConfigHelper helper,
      IProgressMonitor monitor) throws CoreException {

    IMojoParameterMetadata metadata = null;
    String mojoConfigurator = mojo.getComponentConfigurator();
    if(mojoConfigurator != null) {
      metadata = readMojoParameterMetadata(mojoConfigurator);
    }
    if(metadata == null) {
      metadata = new DefaultMojoParameterMetadata();
    }

    return metadata.loadMojoParameters(desc, mojo, helper, monitor);
  }

  public static final String EXTENSION_MOJO_PARAMETER_METADATA = MavenEditorPlugin.MvnIndex_PLUGIN_ID
      + ".mojoParameterMetadata"; //$NON-NLS-1$

  private static IMojoParameterMetadata readMojoParameterMetadata(String mojoConfigurator) {

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint mappingsExtensionPoint = registry.getExtensionPoint(EXTENSION_MOJO_PARAMETER_METADATA);
    if(mappingsExtensionPoint != null) {
      IExtension[] mappingsExtensions = mappingsExtensionPoint.getExtensions();
      for(IExtension extension : mappingsExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if("mojoParameterMetadata".equals(element.getName()) //$NON-NLS-1$
              && mojoConfigurator.equals(element.getAttribute("configurator"))) { //$NON-NLS-1$
            try {
              return (IMojoParameterMetadata) element.createExecutableExtension("class"); //$NON-NLS-1$
            } catch(CoreException ex) {
              log.error(ex.getMessage(), ex);
            }
          }
        }
      }
    }
    return null;
  }

  private static final Map<String, MojoParameter> PREDEF;

  static {
    // @formatter:off
    PREDEF = Map
        .of("org.eclipse.m2e:lifecycle-mapping:1.0.0",
            new MojoParameter("", "",
                Collections
                    .singletonList(
                        new MojoParameter("lifecycleMappingMetadata", "LifecycleMappingMetadata",
                            Collections.singletonList(new MojoParameter("pluginExecutions", "List<PluginExecution>",
                                Collections.singletonList(new MojoParameter("pluginExecution", "PluginExecution",
                                    Arrays.asList(
                                        new MojoParameter("pluginExecutionFilter", "PluginExecutionFilter",
                                            Arrays.asList(new MojoParameter("groupId", "String"),
                                                new MojoParameter("artifactId", "String"),
                                                new MojoParameter("versionRange", "String"),
                                                new MojoParameter("goals", "List<String>",
                                                    Collections.singletonList(
                                                        new MojoParameter("goal", "String").multiple())))),
                                        new MojoParameter("action", "Action",
                                            Arrays.asList(new MojoParameter("ignore", "void"),
                                                new MojoParameter("execute", "Execute",
                                                    Arrays.asList(new MojoParameter("runOnIncremental", "boolean"),
                                                        new MojoParameter("runOnConfiguration", "boolean")))))))
                                                            .multiple())))))));
    // @formatter:on
  }

}
