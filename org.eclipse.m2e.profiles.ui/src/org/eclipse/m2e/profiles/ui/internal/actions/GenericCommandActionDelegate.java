/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Fred Bricon / Red Hat - minor refactoring
 *     
 * Original code taken from <a href="http://wiki.eclipse.org/Platform_Command_Framework#Using_an_IActionDelegate_to_execute_a_command">
 * http://wiki.eclipse.org/Platform_Command_Framework#Using_an_IActionDelegate_to_execute_a_command</a>
 ******************************************************************************/

package org.eclipse.m2e.profiles.ui.internal.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.registry.IWorkbenchRegistryConstants;

import org.eclipse.m2e.profiles.ui.internal.MavenProfilesUIActivator;


/**
 * This action delegate can be used to specify a command with or without parameters be called from an &lt;action/&gt;
 * specified in actionSets, editorActions, viewActions, or popupMenus.
 */
public class GenericCommandActionDelegate implements IWorkbenchWindowActionDelegate, IViewActionDelegate,
    IEditorActionDelegate, IObjectActionDelegate, IExecutableExtension {

  /**
   * The commandId parameter needed when using the &lt;class/&gt; form for this IActionDelegate. Value is "commandId".
   */
  public static final String PARM_COMMAND_ID = "commandId"; //$NON-NLS-1$

  private String commandId = null;

  private Map<?, ?> parameterMap = null;

  private ParameterizedCommand parameterizedCommand = null;

  private IHandlerService handlerService = null;

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
   */
  public void dispose() {
    handlerService = null;
    parameterizedCommand = null;
    parameterMap = null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run(IAction action) {
    if(handlerService == null) {
      // what, no handler service ... no problem
      return;
    }
    try {
      if(commandId != null) {
        handlerService.executeCommand(commandId, null);
      } else if(parameterizedCommand != null) {
        handlerService.executeCommand(parameterizedCommand, null);
      }
      // else there is no command for this delegate
    } catch(Exception e) {
      // exceptions reduced for brevity
      // and we won't just do a print out :-)
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
   *      org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {
    // we don't care, handlers get their selection from the
    // ExecutionEvent application context
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement,
   *      java.lang.String, java.lang.Object)
   */
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
      throws CoreException {
    String id = config.getAttribute(IWorkbenchRegistryConstants.ATT_ID);
    // save the data until our init(*) call, where we can get
    // the services.
    if(data instanceof String) {
      commandId = (String) data;
    } else if(data instanceof Map) {
      parameterMap = (Map<?, ?>) data;
      if(parameterMap.get(PARM_COMMAND_ID) == null) {
        Status status = new Status(IStatus.ERROR, MavenProfilesUIActivator.PLUGIN_ID,
            "The '" + id + "' action won't work without a commandId");
        throw new CoreException(status);
      }
    } else {
      Status status = new Status(IStatus.ERROR, MavenProfilesUIActivator.PLUGIN_ID,
          "The '" + id + "' action won't work without some initialization parameters");
      throw new CoreException(status);
    }
  }

  /**
   * Build a command from the executable extension information.
   * 
   * @param commandService to get the Command object
   */
  private void createCommand(ICommandService commandService) {
    String id = (String) parameterMap.get(PARM_COMMAND_ID);
    if(id == null) {
      return;
    }
    if(parameterMap.size() == 1) {
      commandId = id;
      return;
    }
    try {
      Command cmd = commandService.getCommand(id);
      if(!cmd.isDefined()) {
        // command not defined? no problem ...
        return;
      }
      ArrayList<Parameterization> parameters = new ArrayList<Parameterization>();
      Iterator<?> i = parameterMap.keySet().iterator();
      while(i.hasNext()) {
        String parmName = (String) i.next();
        if(PARM_COMMAND_ID.equals(parmName)) {
          continue;
        }
        IParameter parm = cmd.getParameter(parmName);
        if(parm == null) {
          // asking for a bogus parameter? No problem
          return;
        }
        parameters.add(new Parameterization(parm, (String) parameterMap.get(parmName)));
      }
      parameterizedCommand = new ParameterizedCommand(cmd, parameters.toArray(new Parameterization[parameters.size()]));
    } catch(NotDefinedException e) {
      // command is bogus? No problem, we'll do nothing.
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
   */
  public void init(IWorkbenchWindow window) {
    if(handlerService != null) {
      // already initialized
      return;
    }

    handlerService = window.getService(IHandlerService.class);
    if(parameterMap != null) {
      ICommandService commandService = window.getService(ICommandService.class);
      createCommand(commandService);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IViewActionDelegate#init(org.eclipse.ui.IViewPart)
   */
  public void init(IViewPart view) {
    init(view.getSite().getWorkbenchWindow());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction,
   *      org.eclipse.ui.IEditorPart)
   */
  public void setActiveEditor(IAction action, IEditorPart targetEditor) {
    // we don't actually care about the active editor, since that
    // information is in the ExecutionEvent application context
    // but we need to make sure we're initialized.
    if(targetEditor != null) {
      init(targetEditor.getSite().getWorkbenchWindow());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
   *      org.eclipse.ui.IWorkbenchPart)
   */
  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    // we don't actually care about the active part, since that
    // information is in the ExecutionEvent application context
    // but we need to make sure we're initialized.
    if(targetPart != null) {
      init(targetPart.getSite().getWorkbenchWindow());
    }
  }
}
