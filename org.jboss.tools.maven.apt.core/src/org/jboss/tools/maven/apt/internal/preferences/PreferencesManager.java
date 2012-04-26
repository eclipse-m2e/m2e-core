/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.maven.apt.internal.preferences;

import org.jboss.tools.maven.apt.MavenJdtAptPlugin;
import org.jboss.tools.maven.apt.preferences.AnnotationProcessingMode;
import org.jboss.tools.maven.apt.preferences.IPreferencesManager;
import org.jboss.tools.maven.apt.preferences.PreferencesConstants;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * PreferencesManager
 *
 * @author Fred Bricon
 */
public class PreferencesManager implements IPreferencesManager {

  public PreferencesManager() {
    DefaultScope.INSTANCE.getNode(MavenJdtAptPlugin.PLUGIN_ID);//Initializes AnnotationProcessingPreferenceInitializer
  }
  
  public void setAnnotationProcessorMode(IProject project, AnnotationProcessingMode mode) {
    IEclipsePreferences prefs = getPreferences(project);
    prefs.put(PreferencesConstants.MODE, mode.toString());
    save(prefs);
  }

  private void save(IEclipsePreferences prefs) {
    try {
      // prefs are automatically flushed during a plugin's "super.stop()".
      prefs.flush();
    } catch(BackingStoreException e) {
      //TODO write a real exception handler.
      e.printStackTrace();
    }
  }

  public AnnotationProcessingMode getAnnotationProcessorMode(IProject project) {
    String mode = getString(project, PreferencesConstants.MODE);
    if (mode == null) {
      mode = PreferencesConstants.DEFAULT_OPTIONS.get(PreferencesConstants.MODE);
    }
    return AnnotationProcessingMode.getFromString(mode); 
  }

  private static String getString(IProject project, String optionName) {
    IPreferencesService service = Platform.getPreferencesService();
    IScopeContext[] contexts;
    if (project != null) {
      contexts = new IScopeContext[] { new ProjectScope(project), InstanceScope.INSTANCE, DefaultScope.INSTANCE };
    }
    else {
      contexts = new IScopeContext[] { InstanceScope.INSTANCE, DefaultScope.INSTANCE };
    }
    return service.getString(
        MavenJdtAptPlugin.PLUGIN_ID, 
        optionName, 
        null,  
        contexts);
  }
  
  private IEclipsePreferences getPreferences(IProject project) {
    IScopeContext scopeContext;
    if (project == null) {
      scopeContext = InstanceScope.INSTANCE;
    } else {
      scopeContext = new ProjectScope(project);
    }
    IEclipsePreferences prefs = scopeContext.getNode(MavenJdtAptPlugin.PLUGIN_ID);
    return prefs;
  }

  
  public boolean hasSpecificProjectSettings(IProject project) {
    if (project != null) {
      Preferences[] prefs = new Preferences[]{new ProjectScope(project).getNode(MavenJdtAptPlugin.PLUGIN_ID)};
      IPreferencesService service = Platform.getPreferencesService();
      for (String optionName : PreferencesConstants.DEFAULT_OPTIONS.keySet()) {
        if (service.get(optionName, null, prefs) != null) {
          return true;
        }
      }
    }
    return false;
  }

  public void clearSpecificSettings(IProject project) {
    if (project != null) {
      try {
        IEclipsePreferences prefs = new ProjectScope(project).getNode(MavenJdtAptPlugin.PLUGIN_ID); 
        prefs.clear();
        prefs.flush();
      } catch(BackingStoreException ex) {
        // TODO Auto-generated catch block
        ex.printStackTrace();
      }
    }
  }
  
}
