/*******************************************************************************
 * Copyright (c) 2020 Metron, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Metron, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.jdt.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.env.AutomaticModuleNaming;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.IModule.IModuleReference;
import org.eclipse.jdt.internal.compiler.env.IModule.IService;


/**
 * Contains data about a classpath entry that can be used to determine whether the entry belongs on the Java 9+
 * modulepath.
 *
 * @author Mike Hogye
 */
@SuppressWarnings("restriction")
class InternalModuleInfo {

  public final String name;

  public final List<String> requiredModuleNames;

  public final List<String> usedServiceNames;

  public final List<String> providedServiceNames;

  public static InternalModuleInfo fromDescription(IModuleDescription description) throws JavaModelException {
    String name = description.getElementName();
    List<String> requiredModules = Arrays.asList(description.getRequiredModuleNames());
    List<String> usedServices = Arrays.asList(description.getUsedServiceNames());
    List<String> providedServices = Arrays.asList(description.getProvidedServiceNames());
    return new InternalModuleInfo(name, requiredModules, usedServices, providedServices);
  }

  public static InternalModuleInfo fromDeclaration(IModule declaration) {
    String name = new String(declaration.name());

    IModuleReference[] requires = declaration.requires();
    List<String> requiredModules = new ArrayList<>(requires.length);
    for(IModuleReference required : requires) {
      requiredModules.add(new String(required.name()));
    }

    char[][] uses = declaration.uses();
    List<String> usedServices = new ArrayList<>(uses.length);
    for(char[] used : uses) {
      usedServices.add(new String(used));
    }

    IService[] provides = declaration.provides();
    List<String> providedServices = new ArrayList<>(provides.length);
    for(IService provided : provides) {
      providedServices.add(new String(provided.name()));
    }

    return new InternalModuleInfo(name, requiredModules, usedServices, providedServices);
  }

  public static InternalModuleInfo withAutomaticName(String name) {
    return new InternalModuleInfo(name, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
  }

  public static InternalModuleInfo withAutomaticNameFromFile(File file) {
    char[] name = AutomaticModuleNaming.determineAutomaticModuleNameFromFileName(file.getAbsolutePath(), true, true);
    return withAutomaticName(new String(name));
  }

  public InternalModuleInfo(String name, List<String> requiredModules, List<String> usedServices,
      List<String> providedServices) {
    this.name = name;
    this.requiredModuleNames = Collections.unmodifiableList(new ArrayList<>(requiredModules));
    this.usedServiceNames = Collections.unmodifiableList(new ArrayList<>(usedServices));
    this.providedServiceNames = Collections.unmodifiableList(new ArrayList<>(providedServices));
  }

  @Override
  public int hashCode() {
    int prime = 73303;
    int result = 1;
    result = prime * result + Objects.hashCode(this.name);
    result = prime * result + Objects.hashCode(this.requiredModuleNames);
    result = prime * result + Objects.hashCode(this.usedServiceNames);
    result = prime * result + Objects.hashCode(this.providedServiceNames);
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if(o == this) {
      return true;
    }
    if(o == null) {
      return false;
    }
    if(o.getClass() != this.getClass()) {
      return false;
    }
    InternalModuleInfo other = (InternalModuleInfo) o;
    return (Objects.equals(other.name, this.name) && Objects.equals(other.requiredModuleNames, this.requiredModuleNames)
        && Objects.equals(other.usedServiceNames, this.usedServiceNames)
        && Objects.equals(other.providedServiceNames, this.providedServiceNames));
  }

}
