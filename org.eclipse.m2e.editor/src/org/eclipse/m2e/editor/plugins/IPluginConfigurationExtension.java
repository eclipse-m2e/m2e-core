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

import org.eclipse.m2e.editor.pom.MavenPomEditorPage;
import org.eclipse.m2e.model.edit.pom.Plugin;
import org.eclipse.swt.widgets.Composite;

public interface IPluginConfigurationExtension {
  public void setPlugin(Plugin plugin);
  public void setPomEditor(MavenPomEditorPage editor);
  public Composite createComposite(Composite parent);
  public void cleanup();
}
