package org.eclipse.m2e.internal.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LaunchingUtils {
  
  private static Logger log = LoggerFactory.getLogger(LaunchingUtils.class);
  
  /**
   * Substitute any variable
   */
  public static String substituteVar(String s) {
    if(s == null) {
      return s;
    }
    try {
      return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(s);
    } catch(CoreException e) {
      log.error("Could not substitute variable {}.", s, e);
      return null;
    }
  }

}
