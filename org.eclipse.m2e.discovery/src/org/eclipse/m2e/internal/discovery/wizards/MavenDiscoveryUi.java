/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Sonatype, Inc. - Modified to use local installation operation
 *******************************************************************************/
package org.eclipse.m2e.internal.discovery.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.ui.IProvHelpContextIds;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.dialogs.ProvisioningWizardDialog;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.Messages;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.m2e.internal.discovery.DiscoveryActivator;
import org.eclipse.m2e.internal.discovery.operation.MavenDiscoveryInstallOperation;
import org.eclipse.m2e.internal.discovery.operation.RestartInstallOperation;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;


/*
 * This is used to replace the typical discovery install operation with our own which allows us to change 
 * the restart policy and enable early startup for configuration updates.
 * 
 * Copied from org.eclipse.equinox.internal.p2.ui.discovery.DiscoveryUi
 */
public abstract class MavenDiscoveryUi {

  private MavenDiscoveryUi() {
		// don't allow clients to instantiate
	}

	public static boolean install(List<CatalogItem> descriptors, IRunnableContext context) {
		try {
      MavenDiscoveryInstallOperation runner = new MavenDiscoveryInstallOperation(descriptors, true);
			context.run(true, true, runner);
      openInstallWizard(runner.getOperation());
		} catch (InvocationTargetException e) {
      IStatus status = new Status(IStatus.ERROR, DiscoveryActivator.PLUGIN_ID, NLS.bind(
          Messages.ConnectorDiscoveryWizard_installProblems, new Object[] {e.getCause().getMessage()}), e.getCause());
			StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
			return false;
		} catch (InterruptedException e) {
			// canceled
			return false;
		}
		return true;
	}

  public static int openInstallWizard(RestartInstallOperation operation) {
    MavenDiscoveryInstallWizard wizard = new MavenDiscoveryInstallWizard(ProvisioningUI.getDefaultUI(), operation, operation.getIUs(), null);
    WizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
    dialog.create();
    PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IProvHelpContextIds.INSTALL_WIZARD);
    return dialog.open();
  }
}
