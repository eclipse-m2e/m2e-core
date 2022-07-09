/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

package org.eclipse.m2e.editor.dialogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class MavenModuleSelectionDialogTest extends AbstractMavenProjectTestCase {

  private static final String PROJECT1 = "project1";

  private static final String PROJECT2 = "project2";

  private static final String PROJECT3 = "project3";

  private static final String MODULE1 = "module1";

  @Test
  public void testModuleSelection() throws Exception {
    final IProject[] projects = importProjects("projects/modules", new String[] {//
        PROJECT1 + "/pom.xml", //
        PROJECT2 + "/pom.xml", //
        PROJECT3 + "/pom.xml", //
        PROJECT1 + "/" + MODULE1 + "/pom.xml"}, new ResolverConfiguration());

    waitForJobsToComplete();

    assertEquals("Imported projects", 4, projects.length);
    assertEquals(PROJECT1, projects[0].getName());
    assertEquals(PROJECT2, projects[1].getName());
    assertEquals(PROJECT3, projects[2].getName());
    assertEquals(MODULE1, projects[3].getName());

    final Set<Object> excludedProjects = new HashSet<>();
    excludedProjects.add(projects[0].getLocation());
    excludedProjects.add(projects[3].getLocation());

    Display.getDefault().syncExec(new Runnable() {
      protected TreeViewer viewer;

      protected Color disabledColor;

      @Override
      public void run() {
        disabledColor = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);

        MavenModuleSelectionDialog dialog = new MavenModuleSelectionDialog(Display.getDefault().getActiveShell(),
            excludedProjects) {
          @Override
          protected Control createDialogArea(Composite parent) {
            Control control = super.createDialogArea(parent);
            viewer = getViewer();
            return control;
          }
        };
        dialog.setBlockOnOpen(false);
        dialog.open();
        viewer.expandAll();

        checkEnabledItems(viewer.getTree().getItems());
      }

      private void checkEnabledItems(TreeItem[] items) {
        for(TreeItem item : items) {
          Object data = item.getData();
          assertTrue("Tree element data is IResource", data instanceof IResource);

          assertEquals("Tree element data is disabled if found in the known project list",
              excludedProjects.contains(((IResource) data).getLocation()),
              disabledColor.equals(((IColorProvider) viewer.getLabelProvider()).getForeground(data)));

          checkEnabledItems(item.getItems());
        }
      }
    });
  }
}
