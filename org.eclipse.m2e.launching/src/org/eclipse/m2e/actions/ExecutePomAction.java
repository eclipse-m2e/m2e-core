/*******************************************************************************
 * Copyright (c) 2008, 2023 Sonatype, Inc. and others.
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

package org.eclipse.m2e.actions;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenExecutableLocation;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.IProjectConfiguration;
import org.eclipse.m2e.internal.launch.LaunchingUtils;
import org.eclipse.m2e.internal.launch.Messages;
import org.eclipse.m2e.ui.internal.launch.MavenLaunchMainTab;


/**
 * Maven launch shortcut
 *
 * @author Dmitri Maximovich
 * @author Eugene Kuleshov
 */
public class ExecutePomAction implements ILaunchShortcut, IExecutableExtension, ILaunchShortcut2 {
  private static final Logger log = LoggerFactory.getLogger(ExecutePomAction.class);

  private boolean showDialog = false;

  private String goalName = null;

  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    if("WITH_DIALOG".equals(data)) { //$NON-NLS-1$
      this.showDialog = true;
    } else {
      this.goalName = (String) data;
    }
  }

  public void launch(IEditorPart editor, String mode) {
    IEditorInput editorInput = editor.getEditorInput();
    if(editorInput instanceof IFileEditorInput fileInput) {
      launch(fileInput.getFile().getParent(), mode);
    }
  }

  public void launch(ISelection selection, String mode) {

    launch(findPomXmlBasedir(selection), mode);
  }

  private void launch(IContainer basecon, String mode) {
    if(basecon == null) {
      return;
    }

    IContainer basedir = findPomXmlBasedir(basecon);

    ILaunchConfiguration launchConfiguration = getLaunchConfiguration(basedir, mode);
    if(launchConfiguration == null) {
      return;
    }

    boolean openDialog = showDialog;
    if(!openDialog) {
      try {
        // if no goals specified
        String goals = launchConfiguration.getAttribute(MavenLaunchConstants.ATTR_GOALS, (String) null);
        openDialog = goals == null || goals.trim().length() == 0;
      } catch(CoreException ex) {
        log.error(ex.getMessage(), ex);
      }
    }

    if(openDialog) {
      ILaunchGroup group = DebugUITools.getLaunchGroup(launchConfiguration, mode);
      String groupId = group != null ? group.getIdentifier() : MavenLaunchMainTab.ID_EXTERNAL_TOOLS_LAUNCH_GROUP;
      DebugUITools.openLaunchConfigurationDialog(getShell(), launchConfiguration, groupId, null);
    } else {
      DebugUITools.launch(launchConfiguration, mode);
    }
  }

  private Shell getShell() {
    return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
  }

  static IContainer findPomXmlBasedir(ISelection selection) {
    IContainer basedir = null;
    if(selection instanceof IStructuredSelection structuredSelection) {
      Object object = structuredSelection.getFirstElement();
      if(object instanceof IContainer container) {
        basedir = container;
      } else if(object instanceof IFile file) {
        basedir = file.getParent();
      } else if(object instanceof IAdaptable adaptable) {
        Object adapter = adaptable.getAdapter(IProject.class);
        if(adapter != null) {
          basedir = (IContainer) adapter;
        } else {
          adapter = adaptable.getAdapter(IFolder.class);
          if(adapter != null) {
            basedir = (IContainer) adapter;
          } else {
            adapter = adaptable.getAdapter(IFile.class);
            if(adapter != null) {
              basedir = ((IFile) object).getParent();
            }
          }
        }
      }
    }
    return findPomXmlBasedir(basedir);
  }

  private static IContainer findPomXmlBasedir(IContainer dir) {
    if(dir == null) {
      return null;
    }
    //first check if this is a pom facade, then we have a base directory!
    IMavenExecutableLocation pomFacade = Adapters.adapt(dir, IMavenExecutableLocation.class);
    if(pomFacade != null) {
      return dir;
    }

    try {
      // loop upwards through the parents as long as we do not cross the project boundary
      while(dir.exists() && dir.getProject() != null && dir.getProject() != dir) {
        // see if pom.xml exists
        if(dir.getType() == IResource.FOLDER) {
          IFolder folder = (IFolder) dir;
          if(folder.findMember(IMavenConstants.POM_FILE_NAME) != null) {
            return folder;
          }
        } else if(dir.getType() == IResource.FILE) {
          if(IMavenConstants.POM_FILE_NAME.equals(((IFile) dir).getName())) {
            return dir.getParent();
          }
        }
        dir = dir.getParent();
      }
    } catch(Exception e) {
      return dir;
    }
    return dir;
  }

  private ILaunchConfiguration createLaunchConfiguration(IContainer basedir, String goal) {
    try {
      ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
      ILaunchConfigurationType launchConfigurationType = launchManager
          .getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);

      String rawConfigName = NLS.bind(Messages.ExecutePomAction_executing, goal, basedir.getLocation().toString());
      String safeConfigName = launchManager.generateLaunchConfigurationName(rawConfigName);

      ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, safeConfigName);
      workingCopy.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, basedir.getLocation().toOSString());
      workingCopy.setAttribute(MavenLaunchConstants.ATTR_GOALS, goal);
      workingCopy.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
      workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_SCOPE, "${project}"); //$NON-NLS-1$
      workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_RECURSIVE, true);

      setProjectConfiguration(workingCopy, basedir);

      // TODO when launching Maven with debugger consider to add the following property
      // -Dmaven.surefire.debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 -Xnoagent -Djava.compiler=NONE"

      return workingCopy;
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    }
    return null;
  }

  private void setProjectConfiguration(ILaunchConfigurationWorkingCopy workingCopy, IContainer basedir) {
    IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
    IFile pomFile = basedir.getFile(IPath.fromOSString(IMavenConstants.POM_FILE_NAME));
    IMavenProjectFacade projectFacade = projectManager.create(pomFile, false, new NullProgressMonitor());
    if(projectFacade != null) {
      IProjectConfiguration configuration = projectFacade.getConfiguration();

      String selectedProfiles = configuration.getSelectedProfiles();
      if(selectedProfiles != null && selectedProfiles.length() > 0) {
        workingCopy.setAttribute(MavenLaunchConstants.ATTR_PROFILES, selectedProfiles);
      }
    }
  }

  private ILaunchConfiguration getLaunchConfiguration(IContainer basedir, String mode) {
    if(goalName != null) {
      return createLaunchConfiguration(basedir, goalName);
    }

    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType launchConfigurationType = launchManager
        .getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);

    // scan existing launch configurations
    IPath basedirLocation = basedir.getLocation();
    if(!showDialog) {
      try {
        List<ILaunchConfiguration> matchingConfigs = getMatchingConfigurations(basedirLocation);

        if(matchingConfigs.size() == 1) {
          log.info("Using existing launch configuration");
          return matchingConfigs.get(0);
        } else if(matchingConfigs.size() > 1) {
          final IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
          ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), //
              new ILabelProvider() {
                public Image getImage(Object element) {
                  return labelProvider.getImage(element);
                }

                public String getText(Object element) {
                  if(element instanceof ILaunchConfiguration configuration) {
                    try {
                      return labelProvider.getText(element) + " : "
                          + configuration.getAttribute(MavenLaunchConstants.ATTR_GOALS, "");
                    } catch(CoreException ex) {
                      // ignore
                    }
                  }
                  return labelProvider.getText(element);
                }

                public boolean isLabelProperty(Object element, String property) {
                  return labelProvider.isLabelProperty(element, property);
                }

                public void addListener(ILabelProviderListener listener) {
                  labelProvider.addListener(listener);
                }

                public void removeListener(ILabelProviderListener listener) {
                  labelProvider.removeListener(listener);
                }

                public void dispose() {
                  labelProvider.dispose();
                }
              });
          dialog.setElements(matchingConfigs.toArray(new ILaunchConfiguration[matchingConfigs.size()]));
          dialog.setTitle(Messages.ExecutePomAction_dialog_title);
          if(ILaunchManager.DEBUG_MODE.equals(mode)) {
            dialog.setMessage(Messages.ExecutePomAction_dialog_debug_message);
          } else {
            dialog.setMessage(Messages.ExecutePomAction_dialog_run_message);
          }
          dialog.setMultipleSelection(false);
          int result = dialog.open();
          labelProvider.dispose();
          return result == Window.OK ? (ILaunchConfiguration) dialog.getFirstResult() : null;
        }

      } catch(CoreException ex) {
        log.error(ex.getMessage(), ex);
      }
    }

    log.info("Creating new launch configuration");

    String newName = launchManager.generateLaunchConfigurationName(basedirLocation.lastSegment());
    try {
      ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, newName);
      workingCopy.setAttribute(MavenLaunchConstants.ATTR_POM_DIR,
          LaunchingUtils.generateProjectLocationVariableExpression(basedir.getProject()));

      setProjectConfiguration(workingCopy, basedir);

      // set other defaults if needed
      // MavenLaunchMainTab maintab = new MavenLaunchMainTab();
      // maintab.setDefaults(workingCopy);
      // maintab.dispose();

      return workingCopy.doSave();
    } catch(Exception ex) {
      log.error("Error creating new launch configuration", ex);
    }
    return null;
  }

  static List<ILaunchConfiguration> getMatchingConfigurations(IPath basedirLocation) throws CoreException {
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType launchConfigurationType = launchManager
        .getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);
    ILaunchConfiguration[] launchConfigurations = launchManager.getLaunchConfigurations(launchConfigurationType);
    ArrayList<ILaunchConfiguration> matchingConfigs = new ArrayList<>();
    for(ILaunchConfiguration configuration : launchConfigurations) {
      try {
        // substitute variables (may throw exceptions)
        String workDir = LaunchingUtils
            .substituteVar(configuration.getAttribute(MavenLaunchConstants.ATTR_POM_DIR, (String) null));
        if(workDir == null) {
          continue;
        }
        IPath workPath = IPath.fromOSString(workDir);
        if(basedirLocation.equals(workPath)) {
          matchingConfigs.add(configuration);
        }
      } catch(CoreException e) {
        log.debug("Skipping launch configuration {}", configuration.getName(), e);
      }

    }
    return matchingConfigs;
  }

  public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
    if(showLaunches()) {
      return findLaunches(ExecutePomAction.findPomXmlBasedir(selection));
    }
    return null;
  }

  public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editorpart) {
    if(showLaunches()) {
      IEditorInput editorInput = editorpart.getEditorInput();
      if(editorInput instanceof IFileEditorInput fileInput) {
        return findLaunches(fileInput.getFile().getParent());
      }
    }
    return null;
  }

  public IResource getLaunchableResource(ISelection selection) {
    if(showLaunches()) {
      return findPomXmlBasedir(selection);
    }
    return null;
  }

  public IResource getLaunchableResource(IEditorPart editorpart) {
    if(showLaunches()) {
      IEditorInput editorInput = editorpart.getEditorInput();
      if(editorInput instanceof IFileEditorInput fileInput) {
        return fileInput.getFile();
      }
    }
    return null;
  }

  private boolean showLaunches() {
    return showDialog;
  }

  private ILaunchConfiguration[] findLaunches(IContainer parent) {
    if(parent == null) {
      return new ILaunchConfiguration[0];
    }
    try {
      List<ILaunchConfiguration> matchingConfigurations = ExecutePomAction
          .getMatchingConfigurations(parent.getLocation());
      return matchingConfigurations.toArray(ILaunchConfiguration[]::new);
    } catch(CoreException ex) {
      ex.printStackTrace();
      return new ILaunchConfiguration[0];
    }
  }

}
