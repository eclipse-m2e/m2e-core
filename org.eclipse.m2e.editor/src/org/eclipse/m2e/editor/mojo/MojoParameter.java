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
 *      Anton Tanasenko. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.mojo;

import java.util.Collections;
import java.util.List;


/**
 * @since 1.6
 */
public class MojoParameter {

  private String name;

  private String type;

  private boolean required;

  private String description;

  private String expression;

  private String defaultValue;

  private List<MojoParameter> nested;

  private boolean multiple;

  private boolean map;

  public MojoParameter(String name, String type, List<MojoParameter> parameters) {
    this.name = name;
    this.type = type;
    nested = parameters;
  }

  public MojoParameter(String name, String type, MojoParameter parameter) {
    this(name, type, Collections.singletonList(parameter));
  }

  public MojoParameter(String name, String type) {
    this(name, type, Collections.<MojoParameter> emptyList());
  }

  public MojoParameter multiple() {
    this.multiple = true;
    return this;
  }

  public MojoParameter map() {
    this.map = true;
    return this;
  }

  public boolean isMultiple() {
    return multiple;
  }

  public boolean isMap() {
    return this.map;
  }

  public List<MojoParameter> getNestedParameters() {
    return nested == null ? Collections.<MojoParameter> emptyList() : Collections.unmodifiableList(nested);
  }

  public String getName() {
    return this.name;
  }

  public String getType() {
    return this.type;
  }

  public boolean isRequired() {
    return this.required;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getExpression() {
    return this.expression;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public String getDefaultValue() {
    return this.defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public String toString() {
    return name + "{" + type + "}"; //$NON-NLS-1$ //$NON-NLS-2$
  }

  public MojoParameter getNestedParameter(String name) {

    List<MojoParameter> params = getNestedParameters();
    if(params.size() == 1) {
      MojoParameter param = params.get(0);
      if(param.isMultiple()) {
        return param;
      }
    }

    for(MojoParameter p : params) {
      if(p.getName().equals(name)) {
        return p;
      }
    }
    return null;
  }

  public MojoParameter getContainer(String[] path) {

    if(path == null || path.length == 0) {
      return this;
    }

    MojoParameter param = this;
    int i = 0;
    while(param != null && i < path.length) {
      param = param.getNestedParameter(path[i]);
      i++ ;
    }

    if(param == null) {
      return null;
    }

    return param;
  }
}
