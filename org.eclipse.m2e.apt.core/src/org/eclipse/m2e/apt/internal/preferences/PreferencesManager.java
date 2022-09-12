/*******************************************************************************
 * Copyright (c) 2012-2019 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.apt.internal.preferences;

import java.util.Properties;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.apt.MavenJdtAptPlugin;
import org.eclipse.m2e.apt.preferences.AnnotationProcessingMode;
import org.eclipse.m2e.apt.preferences.IPreferencesManager;
import org.eclipse.m2e.apt.preferences.PreferencesConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * PreferencesManager
 *
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
public class PreferencesManager implements IPreferencesManager {

  private static final Logger log = LoggerFactory.getLogger(PreferencesManager.class);

  public PreferencesManager() {
    DefaultScope.INSTANCE.getNode(MavenJdtAptPlugin.PLUGIN_ID);//Initializes AnnotationProcessingPreferenceInitializer
  }

  @Override
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
      log.error("Error saving preferences", e);
    }
  }

  @Override
  public AnnotationProcessingMode getAnnotationProcessorMode(IProject project) {
    String mode = getString(project, PreferencesConstants.MODE);
    if(mode == null) {
      mode = PreferencesConstants.DEFAULT_OPTIONS.get(PreferencesConstants.MODE);
    }
    return AnnotationProcessingMode.getFromString(mode);
  }

  private String getString(IProject project, String optionName) {
    if(project == null) {
      return getString(getWorkspaceContexts(), optionName);
    }

    //Read Eclipse project pref
    String value = new ProjectScope(project).getNode(MavenJdtAptPlugin.PLUGIN_ID).get(optionName, null);
    if(value == null) {
      //Read Maven property
      value = getStringFromMavenProps(project, optionName);
    }
    if(value == null) {
      //Read Eclipse Workspace pref
      value = getString(getWorkspaceContexts(), optionName);
    }
    return value;
  }

  private String getStringFromMavenProps(IProject project, String optionName) {
    if(PreferencesConstants.MODE.equals(optionName)) {
      AnnotationProcessingMode mode = getPomAnnotationProcessorMode(project);
      return mode == null ? null : mode.name();
    }

    if(PreferencesConstants.ANNOTATION_PROCESS_DURING_RECONCILE.equals(optionName)) {
      return getPomAnnotationProcessDuringReconcile(project);
    }

    return null;
  }

  private static IScopeContext[] getWorkspaceContexts() {
    return new IScopeContext[] {InstanceScope.INSTANCE, DefaultScope.INSTANCE};
  }

  private String getString(IScopeContext[] contexts, String optionName) {
    if(contexts == null) {
      return null;
    }
    IPreferencesService service = Platform.getPreferencesService();
    return service.getString(MavenJdtAptPlugin.PLUGIN_ID, optionName, null, contexts);
  }

  private IEclipsePreferences getPreferences(IProject project) {
    IScopeContext scopeContext;
    if(project == null) {
      scopeContext = InstanceScope.INSTANCE;
    } else {
      scopeContext = new ProjectScope(project);
    }
    return scopeContext.getNode(MavenJdtAptPlugin.PLUGIN_ID);
  }

  @Override
  public boolean hasSpecificProjectSettings(IProject project) {
    if(project != null) {
      Preferences[] prefs = new Preferences[] {new ProjectScope(project).getNode(MavenJdtAptPlugin.PLUGIN_ID)};
      IPreferencesService service = Platform.getPreferencesService();
      for(String optionName : PreferencesConstants.DEFAULT_OPTIONS.keySet()) {
        if(service.get(optionName, null, prefs) != null) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void clearSpecificSettings(IProject project) {
    if(project != null) {
      try {
        IEclipsePreferences prefs = new ProjectScope(project).getNode(MavenJdtAptPlugin.PLUGIN_ID);
        prefs.clear();
        prefs.flush();
      } catch(BackingStoreException ex) {
        log.error("Error saving " + project.getName() + "preferences", ex);
      }
    }
  }

  private static final Properties EMPTY = new Properties();

  private static Properties getMavenProperties(IProject project) {
    try {
      if(!project.isAccessible() || !project.hasNature(IMavenConstants.NATURE_ID)) {
        return EMPTY;
      }
      IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);
      if(facade == null) {
        IFile pom = project.getFile(IMavenConstants.POM_FILE_NAME);
        facade = MavenPlugin.getMavenProjectRegistry().create(pom, true, new NullProgressMonitor());
      }
      if(facade != null) {
        MavenProject mavenProject = facade.getMavenProject(new NullProgressMonitor());
        return mavenProject.getProperties();
      }
    } catch(CoreException ex) {
      log.error("Error loading maven project for " + project.getName(), ex);
    }
    return EMPTY;
  }

  @Override
  public AnnotationProcessingMode getPomAnnotationProcessorMode(IProject project) {
    if(project != null) {
      Properties properties = getMavenProperties(project);
      return AnnotationProcessingMode.getFromStringOrNull(properties.getProperty(M2E_APT_ACTIVATION_PROPERTY));
    }
    return null;
  }

  @Override
  public String getPomAnnotationProcessDuringReconcile(IProject project) {
    if(project != null) {
      Properties properties = getMavenProperties(project);
      return properties.getProperty(M2E_APT_PROCESS_DURING_RECONCILE_PROPERTY);
    }
    return null;
  }

  @Override
  public void setAnnotationProcessDuringReconcile(IProject project, boolean enable) {
    IEclipsePreferences prefs = getPreferences(project);
    prefs.put(PreferencesConstants.ANNOTATION_PROCESS_DURING_RECONCILE, String.valueOf(enable));
    save(prefs);
  }

  @Override
  public boolean shouldEnableAnnotationProcessDuringReconcile(IProject project) {
    String option = getString(project, PreferencesConstants.ANNOTATION_PROCESS_DURING_RECONCILE);
    if(option == null) {
      option = PreferencesConstants.DEFAULT_OPTIONS.get(PreferencesConstants.ANNOTATION_PROCESS_DURING_RECONCILE);
    }
    return option.equalsIgnoreCase("true");
  }

}
