/*******************************************************************************
 * Copyright (c) 2016 Anton Tanasenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Anton Tanasenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import org.eclipse.jface.text.templates.Template;
import org.eclipse.swt.graphics.Image;


/**
 * PomTemplate
 *
 * @author atanasenko
 */
public class PomTemplate extends Template {

  private Image image;

  private int relevance;

  private String matchValue;

  private boolean retriggerOnApply;

  public PomTemplate(String name, String description, String contextTypeId, String pattern, boolean isAutoInsertable) {
    super(name, description, contextTypeId, pattern, isAutoInsertable);
  }

  public Image getImage() {
    return this.image;
  }

  public PomTemplate image(Image image) {
    this.image = image;
    return this;
  }

  public int getRelevance() {
    return this.relevance;
  }

  public PomTemplate relevance(int relevance) {
    this.relevance = relevance;
    return this;
  }

  public String getMatchValue() {
    return this.matchValue;
  }

  public PomTemplate matchValue(String matchValue) {
    this.matchValue = matchValue;
    return this;
  }

  public boolean isRetriggerOnApply() {
    return this.retriggerOnApply;
  }

  public PomTemplate retriggerOnApply(boolean retriggerOnApply) {
    this.retriggerOnApply = retriggerOnApply;
    return this;
  }

}
