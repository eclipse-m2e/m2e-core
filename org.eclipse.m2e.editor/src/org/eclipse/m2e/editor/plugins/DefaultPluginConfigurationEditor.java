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

package org.eclipse.m2e.editor.plugins;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.editor.pom.MavenPomEditorPage;
import org.eclipse.m2e.model.edit.pom.Configuration;
import org.eclipse.m2e.model.edit.pom.Plugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class DefaultPluginConfigurationEditor implements Adapter, IPluginConfigurationExtension {

  protected Plugin plugin = null;
  protected Configuration configuration = null;
  protected MavenPomEditorPage pomEditor = null;
  protected Notifier target;
  
  public Composite createComposite(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(1, true));

    Label label = new Label(composite, SWT.NONE);
    label.setText(Messages.DefaultPluginConfigurationEditor_lblUseXml);
    label.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
    label.setEnabled(false);
    
    return composite;
  }
  
  public void cleanup() {
    if(this.configuration != null) {
      this.configuration.eAdapters().remove(this);
      this.configuration = null;
    }
  }

  public void setPlugin(Plugin plugin) {
    cleanup();
    this.plugin = plugin;
    this.configuration = plugin.getConfiguration();
    if(this.configuration != null) {
      this.configuration.eAdapters().add(this);
    }
  }

  public void setPomEditor(MavenPomEditorPage editor) {
    this.pomEditor = editor;
  }

  public Notifier getTarget() {
    return target;
  }

  public void setTarget(Notifier newTarget) {
    target = newTarget;
  }

  public boolean isAdapterForType(Object type) {
    return DefaultPluginConfigurationEditor.class.equals(type);
  }

  public void notifyChanged(Notification notification) {
  }
}
