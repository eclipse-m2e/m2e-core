/*******************************************************************************
 * Copyright (c) 2015 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Anton Tanasenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml.internal.mojo;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import org.apache.maven.DefaultMaven;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.UrlModelSource;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.embedder.MavenExecutionContext;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;


/**
 * @author atanasenko
 */
@SuppressWarnings("restriction")
public class MojoParameterMetadataProvider {

  private static final Logger log = LoggerFactory.getLogger(MojoParameterMetadataProvider.class);

  private final MavenProject project;

  private final Plugin plugin;

  protected final MavenImpl maven;

  private final Map<String, List<MojoParameter>> parameters;

  public MojoParameterMetadataProvider(MavenProject project, Plugin plugin) {
    this.project = project;
    this.plugin = plugin;
    maven = (MavenImpl) MavenPlugin.getMaven();
    parameters = new HashMap<>();
  }

  public MojoParameter getParameterRoot(final String className, IProgressMonitor monitor) throws CoreException {

    List<MojoParameter> plist = parameters.get(className);
    if(plist == null) {
      plist = new ArrayList<>();
      this.parameters.put(className, plist);
      final List<MojoParameter> parameters = plist;

      execute(new ICallable<Void>() {
        public Void call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
          PluginDescriptor pd = getPluginDescriptor(context, monitor);

          if(pd != null) {
            Class<?> clazz;
            try {
              clazz = pd.getClassRealm().loadClass(className);
            } catch(ClassNotFoundException ex) {
              return null;
            }

            loadParameters(pd, clazz, parameters, monitor);
          }
          return null;
        }
      }, monitor);
    }

    return new MojoParameter("", className, plist); //$NON-NLS-1$
  }

  protected List<MojoParameter> getParameters(PluginDescriptor desc, final Class<?> clazz, IProgressMonitor monitor)
      throws CoreException {

    String key = clazz.getName();
    List<MojoParameter> plist = parameters.get(key);

    if(plist == null) {
      plist = new ArrayList<>();
      this.parameters.put(key, plist);

      loadParameters(desc, clazz, plist, monitor);
    }

    return plist;
  }

  public MojoParameter getMojoParameterRoot(final Collection<String> mojos, IProgressMonitor monitor)
      throws CoreException {
    List<MojoParameter> params = new ArrayList<>();
    Set<String> collected = new HashSet<>();
    for(String mojo : mojos) {
      MojoParameter md = getMojoParameterRoot(mojo, monitor);
      for(MojoParameter p : md.getNestedParameters()) {
        if(!collected.add(p.getName()))
          continue;
        params.add(p);
      }
    }
    return new MojoParameter("", "", params); //$NON-NLS-1$ //$NON-NLS-2$
  }

  public MojoParameter getMojoParameterRoot(IProgressMonitor monitor) throws CoreException {
    return getMojoParameterRoot("*", monitor); //$NON-NLS-1$
  }

  public MojoParameter getMojoParameterRoot(final String mojo, IProgressMonitor monitor) throws CoreException {

    MojoParameter predefParameters = getPredefined();
    if(predefParameters != null) {
      return predefParameters;
    }

    String key = "mojo:" + (mojo == null ? "*" : mojo); //$NON-NLS-1$ //$NON-NLS-2$

    List<MojoParameter> plist = parameters.get(key);

    if(plist == null) {

      final List<MojoParameter> parameters = new ArrayList<>();
      plist = parameters;
      this.parameters.put(key, plist);

      execute(new ICallable<Void>() {
        public Void call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
          PluginDescriptor pd = getPluginDescriptor(context, monitor);
          if(pd != null) {
            loadMojoParameters(pd, mojo, parameters, monitor);
          }
          return null;
        }
      }, monitor);
    }

    return new MojoParameter("", mojo, plist); //$NON-NLS-1$
  }

  private MojoParameter getPredefined() {
    return PREDEF.get(plugin.getGroupId() + ":" + plugin.getArtifactId() + ":" + plugin.getVersion());
  }

  private void execute(final ICallable<Void> callable, IProgressMonitor monitor) throws CoreException {
    MavenExecutionContext context = maven.createExecutionContext();
    context.getExecutionRequest().setCacheTransferError(false);
    context.execute(new ICallable<Void>() {
      public Void call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
        MavenProject mp = getProject(context);
        if(mp != null) {
          return context.execute(mp, callable, monitor);
        }
        return null;
      }
    }, monitor);
  }

  void loadMojoParameters(PluginDescriptor desc, String goal, List<MojoParameter> parameters, IProgressMonitor monitor)
      throws CoreException {

    Set<String> collected = new HashSet<>();

    if(goal.equals("*")) { //$NON-NLS-1$
      for(MojoDescriptor mojo : desc.getMojos()) {
        loadMojoParameters(desc, mojo, parameters, collected, monitor);
      }
      return;
    }

    MojoDescriptor mojo = desc.getMojo(goal);
    loadMojoParameters(desc, mojo, parameters, collected, monitor);
  }

  protected void loadMojoParameters(PluginDescriptor desc, MojoDescriptor mojo, List<MojoParameter> parameters,
      Set<String> collected, IProgressMonitor monitor) throws CoreException {

    if(monitor.isCanceled()) {
      return;
    }
    Class<?> clazz;
    try {
      clazz = mojo.getImplementationClass();
      if(clazz == null) {
        clazz = desc.getClassRealm().loadClass(mojo.getImplementation());
      }
    } catch(ClassNotFoundException | TypeNotPresentException ex) {
      log.warn(ex.getMessage());
      return;
    }

    List<Parameter> ps = mojo.getParameters();
    Map<String, Type> properties = getClassProperties(clazz);

    if(ps != null) {
      for(Parameter p : ps) {
        if(monitor.isCanceled()) {
          return;
        }
        if(!p.isEditable()) {
          continue;
        }

        Type type = properties.get(p.getName());
        if(type == null) {
          continue;
        }

        if(collected.add(p.getName())) {
          addParameter(desc, clazz, type, p.getName(), p.getAlias(), parameters, p.isRequired(), p.getExpression(),
              p.getDescription(), p.getDefaultValue(), monitor);
        }
      }
    }
  }

  protected void loadParameters(PluginDescriptor desc, Class<?> clazz, List<MojoParameter> parameters,
      IProgressMonitor monitor) throws CoreException {
    if(monitor.isCanceled()) {
      return;
    }

    Map<String, Type> properties = getClassProperties(clazz);

    for(Map.Entry<String, Type> e : properties.entrySet()) {
      if(monitor.isCanceled()) {
        return;
      }
      addParameter(desc, clazz, e.getValue(), e.getKey(), null, parameters, false, null, null, null, monitor);
    }
  }

  protected MavenProject getProject(IMavenExecutionContext context) {
    if(project != null) {
      return project;
    }

    ModelSource modelSource = new UrlModelSource(DefaultMaven.class.getResource("project/standalone.xml")); //$NON-NLS-1$
    try {
      return lookup(ProjectBuilder.class).build(modelSource, context.newProjectBuildingRequest()).getProject();
    } catch(ProjectBuildingException | CoreException ex) {
      log.warn(ex.getMessage());
      return null;
    }
  }

  PluginDescriptor getPluginDescriptor(IMavenExecutionContext context, IProgressMonitor monitor) {
    PluginDescriptor desc;

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
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, Messages.MavenImpl_error_lookup,
          ex));
    }
  }

  private void addParameter(PluginDescriptor desc, Class<?> enclosingClass, Type paramType, String name, String alias,
      List<MojoParameter> parameters, boolean required, String expression, String description, String defaultValue,
      IProgressMonitor monitor) throws CoreException {

    Class<?> paramClass = getRawType(paramType);
    if(paramClass == null) {
      return;
    }

    // inline
    if(isInline(paramClass)) {
      parameters.add(configure(new MojoParameter(name, getTypeDisplayName(paramType)), required, expression,
          description, defaultValue));
      if(alias != null) {
        parameters.add(configure(new MojoParameter(alias, getTypeDisplayName(paramType)), required, expression,
            description, defaultValue));
      }
      return;
    }

    // map
    if(Map.class.isAssignableFrom(paramClass)) {
      // we can't do anything with maps, unfortunately
      parameters.add(configure(new MojoParameter(name, getTypeDisplayName(paramType)).map(), required, expression,
          description, defaultValue));
      if(alias != null) {
        parameters.add(configure(new MojoParameter(alias, getTypeDisplayName(paramType)).map(), required, expression,
            description, defaultValue));
      }
      return;
    }

    // properties
    if(Properties.class.isAssignableFrom(paramClass)) {

      MojoParameter inner = new MojoParameter("property", "property", Arrays.asList(
          new MojoParameter("name", "String"), new MojoParameter("value", "String")));

      parameters.add(configure(new MojoParameter(name, getTypeDisplayName(paramType), inner), required, expression,
          description, defaultValue));
      if(alias != null) {
        parameters.add(configure(new MojoParameter(alias, getTypeDisplayName(paramType), inner), required, expression,
            description, defaultValue));
      }
    }

    // collection/array
    Type itemType = getItemType(paramType);

    if(itemType != null) {

      List<MojoParameter> itemParameters = getItemParameters(desc, enclosingClass, name, itemType, monitor);

      parameters.add(configure(new MojoParameter(name, getTypeDisplayName(paramType), itemParameters), required,
          expression, description, defaultValue));

      if(alias != null) {
        itemParameters = getItemParameters(desc, enclosingClass, alias, itemType, monitor);
        parameters.add(configure(new MojoParameter(alias, getTypeDisplayName(paramType), itemParameters), required,
            expression, description, defaultValue));
      }

      return;
    }

    // pojo
    // skip classes without no-arg constructors
    try {
      paramClass.getConstructor(new Class[0]);
    } catch(NoSuchMethodException ex) {
      return;
    }

    List<MojoParameter> params = getParameters(desc, paramClass, monitor);
    parameters.add(configure(new MojoParameter(name, getTypeDisplayName(paramType), params), required, expression,
        description, defaultValue));
    if(alias != null) {
      parameters.add(configure(new MojoParameter(alias, getTypeDisplayName(paramType), params), required, expression,
          description, defaultValue));
    }

  }

  private List<MojoParameter> getItemParameters(PluginDescriptor desc, Class<?> enclosingClass, String name,
      Type paramType, IProgressMonitor monitor) throws CoreException {

    Class<?> paramClass = getRawType(paramType);

    if(paramClass == null || isInline(paramClass)) {
      MojoParameter container = new MojoParameter(toSingular(name), getTypeDisplayName(paramType)).multiple();
      return Collections.singletonList(container);
    }

    if(Map.class.isAssignableFrom(paramClass) || Properties.class.isAssignableFrom(paramClass)) {
      MojoParameter container = new MojoParameter(toSingular(name), getTypeDisplayName(paramType)).multiple().map();
      return Collections.singletonList(container);
    }

    Type itemType = getItemType(paramType);

    if(itemType != null) {
      MojoParameter container = new MojoParameter(toSingular(name), getTypeDisplayName(paramType)).multiple();
      container.setNestedParameters(getItemParameters(desc, enclosingClass, name, itemType, monitor));
      return Collections.singletonList(container);
    }

    @SuppressWarnings("rawtypes")
    List<Class> parameterClasses = getCandidateClasses(desc, enclosingClass, paramClass);

    List<MojoParameter> parameters = new ArrayList<>();
    for(Class<?> clazz : parameterClasses) {

      String paramName;
      if(clazz.equals(paramClass)) {
        paramName = toSingular(name);
      } else {
        paramName = clazz.getSimpleName();
        paramName = Character.toLowerCase(paramName.charAt(0)) + paramName.substring(1);
      }

      MojoParameter container = new MojoParameter(paramName, getTypeDisplayName(clazz)).multiple();
      container.setNestedParameters(getParameters(desc, clazz, monitor));
      parameters.add(container);
    }

    return parameters;
  }

  private static MojoParameter configure(MojoParameter p, boolean required, String expression, String description,
      String defaultValue) {
    p.setRequired(required);
    p.setExpression(expression);
    p.setDescription(description);
    p.setDefaultValue(defaultValue);
    return p;
  }

  private static Class<?> getRawType(Type type) {
    if(type instanceof Class) {
      return (Class<?>) type;
    }
    if(type instanceof ParameterizedType) {
      return (Class<?>) ((ParameterizedType) type).getRawType();
    }
    return null;
  }

  private static Type getItemType(Type paramType) {

    Class<?> paramClass = getRawType(paramType);

    if(paramClass != null && paramClass.isArray()) {
      return paramClass.getComponentType();
    }
    if(!Collection.class.isAssignableFrom(paramClass)) {
      return null;
    }

    if(paramType instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) paramType;

      Type[] args = pt.getActualTypeArguments();
      if(args.length > 0) {
        return args[0];
      }
    }

    return null;
  }

  private static Map<String, Type> getClassProperties(Class<?> clazz) {
    Map<String, Type> props = new HashMap<>();

    for(Method m : clazz.getMethods()) {
      if((m.getModifiers() & Modifier.STATIC) != 0) {
        continue;
      }

      String name = m.getName();

      if((name.startsWith("add") || name.startsWith("set")) && m.getParameterTypes().length == 1) { //$NON-NLS-1$ //$NON-NLS-2$
        String prop = name.substring(3);
        if(!prop.isEmpty()) {
          prop = Character.toLowerCase(prop.charAt(0)) + prop.substring(1);
          if(!props.containsKey(prop)) {
            props.put(prop, m.getGenericParameterTypes()[0]);
          }
        }
      }
    }

    Class<?> pClazz = clazz;
    while(pClazz != null && !pClazz.equals(Object.class)) {

      for(Field f : pClazz.getDeclaredFields()) {
        if((f.getModifiers() & (Modifier.STATIC | Modifier.FINAL)) != 0) {
          continue;
        }

        String prop = f.getName();

        if(!props.containsKey(prop)) {

          props.put(prop, f.getGenericType());

        }
      }
      pClazz = pClazz.getSuperclass();

    }

    return props;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static List<Class> getCandidateClasses(PluginDescriptor desc, Class enclosingClass, Class paramClass) {

    String name = enclosingClass.getName();
    int dot = name.lastIndexOf('.');
    if(dot > 0) {
      String pkg = name.substring(0, dot);

      List<Class> candidateClasses = null;

      ClassPath cp;
      try {
        cp = ClassPath.from(desc.getClassRealm());
      } catch(IOException e) {
        log.error(e.getMessage());
        return Collections.singletonList(enclosingClass);
      }

      for(ClassInfo ci : cp.getTopLevelClasses(pkg)) {
        Class clazz;
        try {
          clazz = desc.getClassRealm().loadClass(ci.getName());
        } catch(ClassNotFoundException e) {
          log.error(e.getMessage(), e);
          continue;
        }

        if((clazz.getModifiers() & (Modifier.ABSTRACT)) != 0) {
          continue;
        }

        if(!paramClass.isAssignableFrom(clazz)) {
          continue;
        }

        // skip classes without no-arg constructors
        try {
          clazz.getConstructor(new Class[0]);
        } catch(NoSuchMethodException ex) {
          continue;
        }

        if(candidateClasses == null) {
          candidateClasses = new ArrayList<Class>();
        }
        candidateClasses.add(clazz);

      }

      if(candidateClasses != null) {
        return candidateClasses;
      }
    }

    return Collections.singletonList(paramClass);
  }

  private static boolean isInline(Class<?> paramClass) {
    return INLINE_TYPES.contains(paramClass.getName()) || paramClass.isEnum();
  }

  private static String getTypeDisplayName(Type type) {
    Class<?> clazz = getRawType(type);

    if(clazz == null) {
      return type.toString();
    }

    if(clazz.isArray()) {
      return getTypeDisplayName(clazz.getComponentType()) + "[]"; //$NON-NLS-1$
    }

    if(type instanceof ParameterizedType) {
      ParameterizedType ptype = (ParameterizedType) type;
      StringBuilder sb = new StringBuilder();
      sb.append(getTypeDisplayName(clazz)).append("&lt;"); //$NON-NLS-1$

      boolean first = true;
      for(Type arg : ptype.getActualTypeArguments()) {
        if(first)
          first = false;
        else
          sb.append(", "); //$NON-NLS-1$
        sb.append(getTypeDisplayName(arg));
      }

      return sb.append("&gt;").toString(); //$NON-NLS-1$
    }

    String name = clazz.getName();
    int idx = name.lastIndexOf('.');
    if(idx == -1) {
      return name;
    }
    // remove common package names
    String pkg = name.substring(0, idx);
    if(pkg.equals("java.lang") || pkg.equals("java.util") || pkg.equals("java.io")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      return clazz.getSimpleName();
    }
    return name;
  }

  private static String toSingular(String name) {
    if(name == null || name.trim().isEmpty()) {
      return name;
    }
    if(name.endsWith("ies")) { //$NON-NLS-1$
      return name.substring(0, name.length() - 3) + "y"; //$NON-NLS-1$
    } else if(name.endsWith("ches")) { //$NON-NLS-1$ $NON-NLS-2$
      return name.substring(0, name.length() - 2);
    } else if(name.endsWith("xes")) { //$NON-NLS-1$
      return name.substring(0, name.length() - 2);
    } else if(name.endsWith("s") && (name.length() != 1)) { //$NON-NLS-1$
      return name.substring(0, name.length() - 1);
    }
    return name;
  }

  private static final Set<String> INLINE_TYPES;

  private static final Map<String, MojoParameter> PREDEF;

  static {
    // @formatter:off
    INLINE_TYPES = ImmutableSet.<String>of(
      byte.class.getName(),
      Byte.class.getName(),
      short.class.getName(),
      Short.class.getName(),
      int.class.getName(),
      Integer.class.getName(),
      long.class.getName(),
      Long.class.getName(),
      float.class.getName(),
      Float.class.getName(),
      double.class.getName(),
      Double.class.getName(),
      boolean.class.getName(),
      Boolean.class.getName(),
      char.class.getName(),
      Character.class.getName(),
  
      String.class.getName(),
      StringBuilder.class.getName(),
      StringBuffer.class.getName(),
  
      File.class.getName(),
      URI.class.getName(),
      URL.class.getName(),
      Date.class.getName(),
  
      "org.codehaus.plexus.configuration.PlexusConfiguration"
    );
    // @formatter:on
  }

  static {
    // @formatter:off
    PREDEF = ImmutableMap.<String, MojoParameter>of(
      "org.eclipse.m2e:lifecycle-mapping:1.0.0",
      new MojoParameter("", "", Collections.singletonList(
        new MojoParameter("lifecycleMappingMetadata", "LifecycleMappingMetadata", Collections.singletonList(
          new MojoParameter("pluginExecutions", "List<PluginExecution>", Collections.singletonList(
            new MojoParameter("pluginExecution", "PluginExecution", Arrays.asList(
              new MojoParameter("pluginExecutionFilter", "PluginExecutionFilter", Arrays.asList(
                new MojoParameter("groupId", "String"),
                new MojoParameter("artifactId", "String"),
                new MojoParameter("versionRange", "String"),
                new MojoParameter("goals", "List<String>", Collections.singletonList(
                  new MojoParameter("goal", "String").multiple()
                ))
              )), 
              new MojoParameter("action", "Action", Arrays.asList(
                new MojoParameter("ignore", "void"),
                new MojoParameter("execute", "Execute", Arrays.asList(
                  new MojoParameter("runOnIncremental", "boolean"),
                  new MojoParameter("runOnConfiguration", "boolean")
                ))
              ))
            )).multiple()
          ))
        ))
      ))
    );
    // @formatter:on
  }

}
