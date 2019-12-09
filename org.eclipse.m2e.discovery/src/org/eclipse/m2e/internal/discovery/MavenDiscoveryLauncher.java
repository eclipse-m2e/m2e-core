/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.discovery;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.m2e.core.ui.internal.IMavenDiscovery;


@SuppressWarnings("restriction")
public class MavenDiscoveryLauncher implements IMavenDiscovery {

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.IMavenDiscovery#launch(org.eclipse.swt.widgets.Shell)
   */
  public void launch(Shell shell) {
    MavenDiscovery.launchWizard(shell);
  }

}
