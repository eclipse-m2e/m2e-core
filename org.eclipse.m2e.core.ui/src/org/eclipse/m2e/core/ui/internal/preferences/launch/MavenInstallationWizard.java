/*******************************************************************************
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Takari, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.preferences.launch;

import java.util.Set;

import org.eclipse.jface.wizard.Wizard;

import org.eclipse.m2e.core.internal.launch.AbstractMavenRuntime;
import org.eclipse.m2e.core.ui.internal.Messages;


@SuppressWarnings("restriction")
public class MavenInstallationWizard extends Wizard {

  private final MavenInstallationWizardPage runtimePage;

  private AbstractMavenRuntime result;

  public MavenInstallationWizard(Set<String> names) {
    this.runtimePage = new MavenInstallationWizardPage(null, names);
    setWindowTitle(Messages.MavenInstallationWizard_titleNewInstallation);
  }

  public MavenInstallationWizard(AbstractMavenRuntime original, Set<String> names) {
    this.runtimePage = new MavenInstallationWizardPage(original, names);
    setWindowTitle(Messages.MavenInstallationWizard_titleAddInstallation);
  }

  @Override
  public void addPages() {
    addPage(runtimePage);
  }

  @Override
  public boolean performFinish() {
    result = runtimePage.getResult();
    return true;
  }

  public AbstractMavenRuntime getResult() {
    return result;
  }

}
