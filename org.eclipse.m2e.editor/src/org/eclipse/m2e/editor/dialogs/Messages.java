
package org.eclipse.m2e.editor.dialogs;

import org.eclipse.osgi.util.NLS;


public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.m2e.editor.dialogs.messages"; //$NON-NLS-1$

  public static String ManageDependenciesDialog_dependencyExistsWarning;

  public static String ManageDependenciesDialog_dialogInfo;

  public static String ManageDependenciesDialog_dialogTitle;

  public static String ManageDependenciesDialog_pomReadingError;

  public static String ManageDependenciesDialog_projectNotPresentError;

  public static String ManageDependenciesDialog_selectDependenciesLabel;

  public static String ManageDependenciesDialog_selectPOMLabel;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
