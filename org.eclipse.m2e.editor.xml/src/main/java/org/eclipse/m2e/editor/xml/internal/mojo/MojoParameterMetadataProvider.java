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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginResolutionException;
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
import org.eclipse.m2e.editor.xml.MvnIndexPlugin;


/**
 * @author atanasenko
 */
@SuppressWarnings("restriction")
public class MojoParameterMetadataProvider {

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

      MavenExecutionContext context = maven.createExecutionContext();
      context.getExecutionRequest().setCacheTransferError(false);
      context.execute(new ICallable<Void>() {
        public Void call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {

          return context.execute(getProject(context), new ICallable<Void>() {
            public Void call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
              PluginDescriptor pd = getPluginDescriptor(context, monitor);

              Class<?> clazz;
              try {
                clazz = pd.getClassRealm().loadClass(className);
              } catch(ClassNotFoundException ex) {
                return null;
              }

              loadParameters(pd, clazz, parameters, monitor);
              return null;
            }
          }, monitor);

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

    String key = "mojo:" + (mojo == null ? "*" : mojo); //$NON-NLS-1$ //$NON-NLS-2$

    List<MojoParameter> plist = parameters.get(key);

    if(plist == null) {

      final List<MojoParameter> parameters = new ArrayList<>();
      plist = parameters;
      this.parameters.put(key, plist);

      MavenExecutionContext context = maven.createExecutionContext();
      context.getExecutionRequest().setCacheTransferError(false);
      context.execute(new ICallable<Void>() {
        public Void call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
          return context.execute(getProject(context), new ICallable<Void>() {
            public Void call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
              loadMojoParameters(getPluginDescriptor(context, monitor), mojo, parameters, monitor);
              return null;
            }
          }, monitor);

        }
      }, monitor);
    }

    return new MojoParameter("", mojo, plist); //$NON-NLS-1$
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
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, ex.getMessage(), ex));
    }
    List<Parameter> ps = mojo.getParameters();
    if(ps != null) {
      for(Parameter p : ps) {
        if(monitor.isCanceled()) {
          return;
        }
        if(!p.isEditable()) {
          continue;
        }
        if(collected.add(p.getName())) {
          addParameter(desc, getType(clazz, p.getName()), p.getName(), p.getAlias(), parameters, p.isRequired(),
              p.getExpression(), p.getDescription(), p.getDefaultValue(), monitor);
        }
      }
    }
  }

  protected void loadParameters(PluginDescriptor desc, Class<?> clazz, List<MojoParameter> parameters,
      IProgressMonitor monitor) throws CoreException {
    if(monitor.isCanceled()) {
      return;
    }
    PropertyDescriptor[] propertyDescriptors;
    try {
      propertyDescriptors = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
    } catch(IntrospectionException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, ex.getMessage(), ex));
    }

    for(PropertyDescriptor pd : propertyDescriptors) {
      if(monitor.isCanceled()) {
        return;
      }
      if(pd.getWriteMethod() == null) {
        continue;
      }
      String name = pd.getName();
      addParameter(desc, getType(clazz, name), name, null, parameters, false, null, null, null, monitor);
    }
  }

  protected MavenProject getProject(IMavenExecutionContext context) throws CoreException {
    if(project != null) {
      return project;
    }

    ModelSource modelSource = new UrlModelSource(DefaultMaven.class.getResource("project/standalone.xml")); //$NON-NLS-1$
    try {
      return lookup(ProjectBuilder.class).build(modelSource, context.newProjectBuildingRequest()).getProject();
    } catch(ProjectBuildingException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, ex.getMessage(), ex));
    }
  }

  PluginDescriptor getPluginDescriptor(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
    PluginDescriptor desc;

    List<RemoteRepository> remoteRepos = context.getSession().getCurrentProject().getRemotePluginRepositories();
    try {
      desc = lookup(MavenPluginManager.class).getPluginDescriptor(plugin, remoteRepos, context.getRepositorySession());
    } catch(PluginResolutionException | PluginDescriptorParsingException | InvalidPluginDescriptorException ex) {
      throw new CoreException(new Status(IStatus.ERROR, MvnIndexPlugin.PLUGIN_ID, ex.getMessage(), ex));
    }

    try {
      lookup(BuildPluginManager.class).getPluginRealm(context.getSession(), desc);
      return desc;
    } catch(PluginResolutionException | PluginManagerException ex) {
      throw new CoreException(new Status(IStatus.ERROR, MvnIndexPlugin.PLUGIN_ID, ex.getMessage(), ex));
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

  private void addParameter(PluginDescriptor desc, Type paramType, String name, String alias,
      List<MojoParameter> parameters, boolean required, String expression, String description, String defaultValue,
      IProgressMonitor monitor) throws CoreException {

    Class<?> paramClass = getRawType(paramType);

    // inline
    if(INLINE_TYPES.contains(paramClass)) {
      parameters.add(configure(new MojoParameter(name, getTypeDisplayName(paramType)), required, expression,
          description, defaultValue));
      if(alias != null) {
        parameters.add(configure(new MojoParameter(alias, getTypeDisplayName(paramType)), required, expression,
            description, defaultValue));
      }
      return;
    }

    // map
    if(Map.class.isAssignableFrom(paramClass) || Properties.class.isAssignableFrom(paramClass)) {
      // we can't do anything with maps, unfortunately
      parameters.add(configure(new MojoParameter(name, getTypeDisplayName(paramType)).map(), required, expression,
          description, defaultValue));
      if(alias != null) {
        parameters.add(configure(new MojoParameter(alias, getTypeDisplayName(paramType)).map(), required, expression,
            description, defaultValue));
      }
      return;
    }

    // collection/array
    Type itemType = getItemType(paramType);

    if(itemType != null) {
      MojoParameter inner = new MojoParameter(toSingular(name), getTypeDisplayName(itemType)).multiple();
      getItemParameters(desc, name, itemType, inner, monitor);

      parameters.add(configure(new MojoParameter(name, getTypeDisplayName(paramType), inner), required, expression,
          description, defaultValue));

      if(alias != null) {
        inner = new MojoParameter(toSingular(alias), getTypeDisplayName(itemType)).multiple();
        getItemParameters(desc, alias, itemType, inner, monitor);
        parameters.add(configure(new MojoParameter(alias, getTypeDisplayName(paramType), inner), required, expression,
            description, defaultValue));
      }

      return;
    }

    // pojo
    List<MojoParameter> params = getParameters(desc, paramClass, monitor);
    parameters.add(configure(new MojoParameter(name, getTypeDisplayName(paramType), params), required, expression,
        description, defaultValue));
    if(alias != null) {
      parameters.add(configure(new MojoParameter(alias, getTypeDisplayName(paramType), params), required, expression,
          description, defaultValue));
    }

  }

  private void getItemParameters(PluginDescriptor desc, String name, Type paramType, MojoParameter container,
      IProgressMonitor monitor) throws CoreException {

    Class<?> paramClass = getRawType(paramType);

    if(INLINE_TYPES.contains(paramClass)) {
      return;
    }

    if(Map.class.isAssignableFrom(paramClass) || Properties.class.isAssignableFrom(paramClass)) {
      container.map();
      return;
    }

    Type itemType = getItemType(paramType);

    if(itemType != null) {
      MojoParameter inner = new MojoParameter(toSingular(name), getTypeDisplayName(paramType)).multiple();
      getItemParameters(desc, name, itemType, inner, monitor);
      container.setNestedParameters(Collections.singletonList(inner));
      return;
    }

    container.setNestedParameters(getParameters(desc, paramClass, monitor));
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

    if(paramClass.isArray()) {
      return paramClass.getComponentType();
    }
    if(!Collection.class.isAssignableFrom(paramClass)) {
      return null;
    }

    if(paramType instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) paramType;
      paramClass = (Class<?>) pt.getRawType();

      Type[] args = pt.getActualTypeArguments();
      if(args.length > 0) {
        return args[0];
      }
    }

    return null;
  }

  private static Type getType(Class<?> clazz, String property) {

    String title = Character.toTitleCase(property.charAt(0)) + property.substring(1);

    Method setter = findMethod(clazz, "set" + title); //$NON-NLS-1$
    if(setter == null) {
      setter = findMethod(clazz, "add" + title); //$NON-NLS-1$
    }

    if(setter != null) {
      Type[] paramTypes = setter.getGenericParameterTypes();
      if(paramTypes.length > 0) {
        return paramTypes[0];
      }
    }

    Field field = findField(clazz, property);
    if(field != null) {
      return field.getGenericType();
    }

    return null;
  }

  private static Method findMethod(Class<?> clazz, String name) {
    while(clazz != null) {
      for(Method m : clazz.getDeclaredMethods()) {
        if(Modifier.isPublic(m.getModifiers()) && m.getName().equals(name)) {
          return m;
        }
      }
      clazz = clazz.getSuperclass();
    }
    return null;
  }

  private static Field findField(Class<?> clazz, String name) {
    while(clazz != null) {
      for(Field f : clazz.getDeclaredFields()) {
        if(f.getName().equals(name)) {
          return f;
        }
      }
      clazz = clazz.getSuperclass();
    }
    return null;
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

  private static final Set<Class<?>> INLINE_TYPES = new HashSet<>();
  static {
    INLINE_TYPES.add(byte.class);
    INLINE_TYPES.add(Byte.class);
    INLINE_TYPES.add(short.class);
    INLINE_TYPES.add(Short.class);
    INLINE_TYPES.add(int.class);
    INLINE_TYPES.add(Integer.class);
    INLINE_TYPES.add(long.class);
    INLINE_TYPES.add(Long.class);
    INLINE_TYPES.add(float.class);
    INLINE_TYPES.add(Float.class);
    INLINE_TYPES.add(double.class);
    INLINE_TYPES.add(Double.class);
    INLINE_TYPES.add(boolean.class);
    INLINE_TYPES.add(Boolean.class);
    INLINE_TYPES.add(char.class);
    INLINE_TYPES.add(Character.class);

    INLINE_TYPES.add(String.class);
    INLINE_TYPES.add(StringBuilder.class);
    INLINE_TYPES.add(StringBuffer.class);

    INLINE_TYPES.add(File.class);
    INLINE_TYPES.add(URI.class);
    INLINE_TYPES.add(URL.class);
    INLINE_TYPES.add(Date.class);
  }
}
