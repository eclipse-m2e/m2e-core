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

package org.eclipse.m2e.refactoring;

import org.eclipse.osgi.util.NLS;


/**
 * @author mkleint
 *
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.m2e.refactoring.messages"; //$NON-NLS-1$

  public static String AbstractPomRefactoring_error;

  public static String AbstractPomRefactoring_loading;

  public static String AbstractPomRefactoring_task;
  public static String ExcludeRefactoring_error_parent;

  public static String ExcludeRefactoring_name;

  public static String ExcludeRefactoring_task_loading;

  public static String ExcludeRefactoring_title;

  public static String MavenRenameWizardPage_cbRenameWorkspace;

  public static String MavenRenameWizardPage_desc;

  public static String MavenRenameWizardPage_lblArtifactId;

  public static String MavenRenameWizardPage_lblGroupId;

  public static String MavenRenameWizardPage_lblVersion;

  public static String MavenRenameWizardPage_title;

  public static String RefactoringMavenMenuCreator_action_exclude;

  public static String RenameRefactoring_1;

  public static String RenameRefactoring_name;

  public static String RenameRefactoring_title;

  public static String SaveDirtyFilesDialog_message_not_saved;
  public static String SaveDirtyFilesDialog_title;

  public static String SaveDirtyFilesDialog_title_error;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
