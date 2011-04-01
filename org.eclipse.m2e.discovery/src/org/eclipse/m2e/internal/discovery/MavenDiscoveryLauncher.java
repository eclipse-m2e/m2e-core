/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.internal.discovery;

import org.eclipse.m2e.core.ui.internal.IMavenDiscovery;
import org.eclipse.swt.widgets.Shell;


@SuppressWarnings("restriction")
public class MavenDiscoveryLauncher implements IMavenDiscovery {

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.IMavenDiscovery#launch(org.eclipse.swt.widgets.Shell)
   */
  public void launch(Shell shell) {
    MavenDiscovery.launchWizard(shell);
  }

}
