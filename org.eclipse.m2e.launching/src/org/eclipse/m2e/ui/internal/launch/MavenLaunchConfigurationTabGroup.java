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

package org.eclipse.m2e.ui.internal.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;

import org.eclipse.m2e.internal.launch.MavenLaunchParticipantInfo;


public class MavenLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

  public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
    List<ILaunchConfigurationTab> tabs = new ArrayList<ILaunchConfigurationTab>();

    tabs.add(new MavenLaunchMainTab(false));
    tabs.add(new MavenJRETab());
    tabs.add(new RefreshTab());
    tabs.add(new SourceLookupTab());

    List<MavenLaunchParticipantInfo> participants = MavenLaunchParticipantInfo.readParticipantsInfo();
    if(!participants.isEmpty()) {
      tabs.add(new MavenLaunchExtensionsTab(participants));
    }

    tabs.add(new EnvironmentTab());
    tabs.add(new CommonTab());

    setTabs(tabs.toArray(new ILaunchConfigurationTab[tabs.size()]));
  }

}
