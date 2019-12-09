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

package org.eclipse.m2e.editor.xml.internal.mojo;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;

import org.eclipse.m2e.editor.xml.mojo.IMojoParameterMetadata;
import org.eclipse.m2e.editor.xml.mojo.MojoParameter;
import org.eclipse.m2e.editor.xml.mojo.PlexusConfigHelper;


/**
 * DefaultMojoParameterMetadataSource
 *
 * @author sleepless
 */
public class DefaultMojoParameterMetadata implements IMojoParameterMetadata {
  private final Logger log = LoggerFactory.getLogger(getClass());

  public List<MojoParameter> loadMojoParameters(PluginDescriptor desc, MojoDescriptor mojo, PlexusConfigHelper helper,
      IProgressMonitor monitor) throws CoreException {

    if(monitor.isCanceled()) {
      return Collections.emptyList();
    }
    Class<?> clazz;
    try {
      clazz = mojo.getImplementationClass();
      if(clazz == null) {
        clazz = desc.getClassRealm().loadClass(mojo.getImplementation());
      }
    } catch(ClassNotFoundException | TypeNotPresentException ex) {
      log.warn(ex.getMessage());
      return Collections.emptyList();
    }

    List<Parameter> ps = mojo.getParameters();
    Map<String, Type> properties = helper.getClassProperties(clazz);

    List<MojoParameter> parameters = new ArrayList<>();

    if(ps != null) {
      for(Parameter p : ps) {
        if(monitor.isCanceled()) {
          return Collections.emptyList();
        }
        if(!p.isEditable()) {
          continue;
        }

        Type type = properties.get(p.getName());
        if(type == null) {
          continue;
        }

        helper.addParameter(desc.getClassRealm(), clazz, type, p.getName(), p.getAlias(), parameters, p.isRequired(),
            p.getExpression(), p.getDescription(), p.getDefaultValue(), monitor);
      }
    }

    return parameters;
  }

}
