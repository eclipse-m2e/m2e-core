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

package org.eclipse.m2e.actions;

import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;
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
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.internal.launch.Messages;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.ui.internal.launch.MavenLaunchMainTab;
import org.eclipse.m2e.core.util.Util;


/**
 * Maven launch shortcut
 * 
 * @author Dmitri Maximovich
 * @author Eugene Kuleshov
 */
public class ExecutePomAction implements ILaunchShortcut, IExecutableExtension {

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
    if(editorInput instanceof IFileEditorInput) {
      launch(((IFileEditorInput) editorInput).getFile().getParent(), mode);
    }
  }

  public void launch(ISelection selection, String mode) {
    if(selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;
      Object object = structuredSelection.getFirstElement();

      IContainer basedir = null;
      if(object instanceof IProject || object instanceof IFolder) {
        basedir = (IContainer) object;
      } else if(object instanceof IFile) {
        basedir = ((IFile) object).getParent();
      } else if(object instanceof IAdaptable) {
        IAdaptable adaptable = (IAdaptable) object;
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

      launch(basedir, mode);
    }
  }

  @SuppressWarnings("deprecation")
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
        MavenLogger.log(ex);
      }
    }

    if(openDialog) {
      DebugUITools.saveBeforeLaunch();
      // ILaunchGroup group = DebugUITools.getLaunchGroup(launchConfiguration, mode);
      DebugUITools.openLaunchConfigurationDialog(getShell(), launchConfiguration,
          MavenLaunchMainTab.ID_EXTERNAL_TOOLS_LAUNCH_GROUP, null);
    } else {
      DebugUITools.launch(launchConfiguration, mode);
    }
  }

  private Shell getShell() {
    return MavenPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell();
  }

  private IContainer findPomXmlBasedir(IContainer dir) {
    if(dir == null) {
      return null;
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
          if(((IFile) dir).getName().equals(IMavenConstants.POM_FILE_NAME)) {
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
      
      String launchSafeGoalName = goal.replace(':', '-');

      ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, //
          NLS.bind(Messages.ExecutePomAction_executing, launchSafeGoalName, basedir.getLocation().toString().replace('/', '-')));
      workingCopy.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, basedir.getLocation().toOSString());
      workingCopy.setAttribute(MavenLaunchConstants.ATTR_GOALS, goal);
      workingCopy.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
      workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_SCOPE, "${project}"); //$NON-NLS-1$
      workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_RECURSIVE, true);
      
      setProjectConfiguration(workingCopy, basedir);

      IPath path = getJREContainerPath(basedir);
      if(path != null) {
        workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, path.toPortableString());
      }
      
      // TODO when launching Maven with debugger consider to add the following property
      // -Dmaven.surefire.debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 -Xnoagent -Djava.compiler=NONE"

      return workingCopy;
    } catch(CoreException ex) {
      MavenLogger.log(ex);
    }
    return null;
  }

  private void setProjectConfiguration(ILaunchConfigurationWorkingCopy workingCopy, IContainer basedir) {
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    IFile pomFile = basedir.getFile(new Path(IMavenConstants.POM_FILE_NAME));
    IMavenProjectFacade projectFacade = projectManager.create(pomFile, false, new NullProgressMonitor());
    if(projectFacade != null) {
      ResolverConfiguration configuration = projectFacade.getResolverConfiguration();

      String activeProfiles = configuration.getActiveProfiles();
      if(activeProfiles != null && activeProfiles.length() > 0) {
        workingCopy.setAttribute(MavenLaunchConstants.ATTR_PROFILES, activeProfiles);
      }
    }
  }

  // TODO ideally it should use MavenProject, but it is faster to scan IJavaProjects 
  private IPath getJREContainerPath(IContainer basedir) throws CoreException {
    IProject project = basedir.getProject();
    if(project != null && project.hasNature(JavaCore.NATURE_ID)) {
      IJavaProject javaProject = JavaCore.create(project);
      IClasspathEntry[] entries = javaProject.getRawClasspath();
      for(int i = 0; i < entries.length; i++ ) {
        IClasspathEntry entry = entries[i];
        if(JavaRuntime.JRE_CONTAINER.equals(entry.getPath().segment(0))) {
          return entry.getPath();
        }
      }
    }
    return null;
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
//        ILaunch[] launches = launchManager.getLaunches();
//        ILaunchConfiguration[] launchConfigurations = null;
//        if(launches.length > 0) {
//          for(int i = 0; i < launches.length; i++ ) {
//            ILaunchConfiguration config = launches[i].getLaunchConfiguration();
//            if(config != null && launchConfigurationType.equals(config.getType())) {
//              launchConfigurations = new ILaunchConfiguration[] {config};
//            }
//          }
//        }
//        if(launchConfigurations == null) {
//          launchConfigurations = launchManager.getLaunchConfigurations(launchConfigurationType);
//        }

        ILaunchConfiguration[] launchConfigurations = launchManager.getLaunchConfigurations(launchConfigurationType);
        ArrayList<ILaunchConfiguration> matchingConfigs = new ArrayList<ILaunchConfiguration>();
        for(ILaunchConfiguration configuration : launchConfigurations) {
          // substitute variables
          String workDir = Util.substituteVar(configuration.getAttribute(MavenLaunchConstants.ATTR_POM_DIR, (String) null));
          if(workDir == null) {
            continue;
          }
          IPath workPath = new Path(workDir);
          if(basedirLocation.equals(workPath)) {
            matchingConfigs.add(configuration);
          }
        }
        
        if(matchingConfigs.size()==1) {
          MavenPlugin.getDefault().getConsole().logMessage("Using existing launch configuration");
          return matchingConfigs.get(0);
        } else if(matchingConfigs.size()>1) {
          final IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
          ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), // 
              new ILabelProvider() {
                public Image getImage(Object element) {
                  return labelProvider.getImage(element);
                }

                public String getText(Object element) {
                  if(element instanceof ILaunchConfiguration) {
                    ILaunchConfiguration configuration = (ILaunchConfiguration) element;
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
          if (mode.equals(ILaunchManager.DEBUG_MODE)) {
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
        MavenLogger.log(ex);
      }
    }

    MavenPlugin.getDefault().getConsole().logMessage("Creating new launch configuration");

    String newName = launchManager.generateUniqueLaunchConfigurationNameFrom(basedirLocation.lastSegment());
    try {
      ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, newName);
      workingCopy.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, basedirLocation.toString());

      setProjectConfiguration(workingCopy, basedir);
      
      // set other defaults if needed
      // MavenLaunchMainTab maintab = new MavenLaunchMainTab();
      // maintab.setDefaults(workingCopy);
      // maintab.dispose();

      return workingCopy.doSave();
    } catch(Exception ex) {
      MavenLogger.log("Error creating new launch configuration", ex);
    }
    return null;
  }

}
