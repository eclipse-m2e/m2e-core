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

package org.eclipse.m2e.editor;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * @author Eugene Kuleshov
 */
public class MavenEditorPlugin extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "org.eclipse.m2e.editor"; //$NON-NLS-1$
  
  private static MavenEditorPlugin instance;

  public MavenEditorPlugin() {
  }
  
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    instance = this;
  }
  
  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    instance = null;
  }
  
  public static MavenEditorPlugin getDefault() {
    return instance;
  }
  
}
