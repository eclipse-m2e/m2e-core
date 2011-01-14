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
package org.eclipse.m2e.internal.discovery.markers;

import java.util.Arrays;
import java.util.Collections;

import org.eclipse.core.resources.IMarker;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.internal.discovery.MavenDiscovery;
import org.eclipse.m2e.internal.discovery.Messages;
import org.eclipse.ui.IMarkerResolution;


class DiscoveryWizardProposal implements IMarkerResolution {
  
  static final DiscoveryWizardProposal PROPOSAL = new DiscoveryWizardProposal();

	public String getLabel() {
		return Messages.DiscoveryWizardProposal_Label;
	}

	public void run(IMarker marker) {
    String packaging = marker.getAttribute(IMavenConstants.MARKER_ATTR_PACKAGING, null);
		if (packaging != null)
			MavenDiscovery.launchWizard(Arrays.asList(new String[] {packaging}), Collections.EMPTY_LIST);
	}
}