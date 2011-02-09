
package org.eclipse.m2e.core.ui.internal.console;

import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;


public class MavenConsoleService implements MavenConsole {

  public void logMessage(String msg) {
    M2EUIPluginActivator.getDefault().getMavenConsoleImpl().logMessage(msg);
  }

  public void logError(String msg) {
    M2EUIPluginActivator.getDefault().getMavenConsoleImpl().logError(msg);
  }

}
