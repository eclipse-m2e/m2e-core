/*******************************************************************************
 * Copyright (c) 2022-2022 Hannes Wellmann and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Hannes Wellmann - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.embedder;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import org.eclipse.osgi.util.NLS;

import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.converters.ConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

import org.apache.maven.plugin.descriptor.MojoDescriptor;

import org.eclipse.m2e.core.embedder.IMaven.IConfigurationElement;
import org.eclipse.m2e.core.embedder.IMaven.IConfigurationParameter;


class ConfigurationElementImpl implements IConfigurationElement {
  final PlexusConfiguration configuration;

  final String path;

  final ValueFactory valueFactory;

  ConfigurationElementImpl(PlexusConfiguration configuration, String path, ValueFactory valueComputer) {
    this.path = path;
    this.configuration = configuration;
    this.valueFactory = valueComputer;
  }

  @Override
  public IConfigurationParameter get(String name) {
    requireExists();
    PlexusConfiguration child = configuration.getChild(name);
    return new ConfigurationParameterImpl(child, this.path + "/" + name, valueFactory);
  }

  @Override
  public Stream<IConfigurationParameter> children(String name) {
    requireExists();
    return Arrays.stream(configuration.getChildren(name))
        .map(c -> new ConfigurationParameterImpl(c, this.path + "/" + name, valueFactory));
  }

  @Override
  public Stream<IConfigurationParameter> children() throws NoSuchElementException {
    requireExists();
    return Arrays.stream(configuration.getChildren())
        .map(c -> new ConfigurationParameterImpl(c, this.path + "/" + c.getName(), valueFactory));
  }

  void requireExists() {
    if(configuration == null) {
      throw new NoSuchElementException(
          "Plugin execution " + valueFactory.mojo.getId() + "does not have a configuration parameter " + path);
    }
  }

  record ValueFactory(ConverterLookup lookup, MojoDescriptor mojo, ClassLoader pluginRealm,
      ExpressionEvaluator evaluator) {

    private <T> T create(PlexusConfiguration configuration, Class<T> clazz) throws ComponentConfigurationException {
      ConfigurationConverter typeConverter = lookup.lookupConverterForType(clazz);
      Object value = typeConverter.fromConfiguration(lookup, configuration, clazz, mojo.getImplementationClass(),
          pluginRealm, evaluator, null);
      return clazz.cast(value);
    }
  }

  static class ConfigurationParameterImpl extends ConfigurationElementImpl implements IConfigurationParameter {

    private ConfigurationParameterImpl(PlexusConfiguration configuration, String name, ValueFactory valueComputer) {
      super(configuration, name, valueComputer);
    }

    @Override
    public boolean exists() {
      return configuration != null;
    }

    @Override
    public <T> T as(Class<T> clazz) throws NoSuchElementException {
      requireExists();
      try {
        return valueFactory.create(configuration, clazz);
      } catch(ComponentConfigurationException e) {
        //TODO: or throw a IllegalArgument exception
        // Probably the catched exception is thrown on the wrong type. TODO: test that.
        throw new IllegalStateException(
            NLS.bind("Failed to compute configuration for for plugin execution {0}", valueFactory.mojo.getId(), e));
      }
    }

  }

}
