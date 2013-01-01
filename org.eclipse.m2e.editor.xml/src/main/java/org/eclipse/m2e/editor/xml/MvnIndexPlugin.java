/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import java.io.IOException;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;


/**
 * @author Lukas Krecan
 */
public class MvnIndexPlugin extends AbstractUIPlugin {
  private static final Logger log = LoggerFactory.getLogger(MvnIndexPlugin.class);

  public static final String PLUGIN_ID = "org.eclipse.m2e.editor.xml"; //$NON-NLS-1$

  private static final String TEMPLATES_KEY = PLUGIN_ID + ".templates"; //$NON-NLS-1$

  private static MvnIndexPlugin defaultInstance;

  private TemplateStore templateStore;

  private ContributionContextTypeRegistry contextTypeRegistry;

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    defaultInstance = this;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    defaultInstance = null;
  }

  public static MvnIndexPlugin getDefault() {
    return defaultInstance;
  }

  /**
   * Returns the template store.
   * 
   * @return the template store.
   */
  public TemplateStore getTemplateStore() {
    if(templateStore == null) {
      templateStore = new ContributionTemplateStore(getTemplateContextRegistry(), getPreferenceStore(), TEMPLATES_KEY);
      try {
        templateStore.load();
      } catch(IOException ex) {
        log.error("Unable to load pom templates", ex); //$NON-NLS-1$
      }
    }
    return templateStore;
  }

  /**
   * Returns the template context type registry.
   * 
   * @return the template context type registry
   */
  public ContextTypeRegistry getTemplateContextRegistry() {
    if(contextTypeRegistry == null) {
      ContributionContextTypeRegistry registry = new ContributionContextTypeRegistry();
      for(PomTemplateContext contextType : PomTemplateContext.values()) {
        registry.addContextType(contextType.getContextTypeId());
      }
      contextTypeRegistry = registry;
    }
    return contextTypeRegistry;
  }

  public ContextTypeRegistry getContextTypeRegistry() {
    if(contextTypeRegistry == null) {
      contextTypeRegistry = new ContributionContextTypeRegistry();
      // TemplateContextType contextType = new TemplateContextType(CONTEXT_TYPE, "POM XML Editor");
      PomTemplateContextType contextType = new PomTemplateContextType();
      contextTypeRegistry.addContextType(contextType);
    }
    return contextTypeRegistry;
  }
}
