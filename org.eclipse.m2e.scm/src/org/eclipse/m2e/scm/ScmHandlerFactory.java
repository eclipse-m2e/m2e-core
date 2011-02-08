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

package org.eclipse.m2e.scm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.m2e.core.core.MavenLogger;


/**
 * An SCM handler factory
 * 
 * @author Eugene Kuleshov
 */
public class ScmHandlerFactory {

 
  public static final String EXTENSION_SCM_HANDLERS = "org.eclipse.m2e.scm.scmHandlers"; //$NON-NLS-1$
  
  public static final String EXTENSION_SCM_HANDLERS_UI = "org.eclipse.m2e.scm.scmHandlersUi"; //$NON-NLS-1$
  
  private static final String ELEMENT_SCM_HANDLER = "handler"; //$NON-NLS-1$

  private static final String ELEMENT_SCM_HANDLER_UI = "handlerUi"; //$NON-NLS-1$
  
  private static volatile Map<String, List<ScmHandler>> scms;

  private static volatile Map<String, ScmHandlerUi> scmUis;

  public static synchronized void addScmHandlerUi(ScmHandlerUi handlerUi) {
    getScmUis().put(handlerUi.getType(), handlerUi);
  }

  public static synchronized ScmHandlerUi getHandlerUiByType(String type) {
    return type == null ? null : getScmUis().get(type);
  }

  public static synchronized void addScmHandler(ScmHandler handler) {
    List<ScmHandler> handlers = getScms().get(handler.getType());
    if(handlers == null) {
      handlers = new ArrayList<ScmHandler>();
      getScms().put(handler.getType(), handlers);
    }
    handlers.add(handler);
    Collections.sort(handlers);
  }

  public static synchronized String[] getTypes() {
    Map<String, List<ScmHandler>> scms = getScms();
    return scms.keySet().toArray(new String[scms.size()]);
  }

  public static synchronized ScmHandler getHandler(String url) throws CoreException {
    String type = ScmUrl.getType(url);
    return getHandlerByType(type);
  }

  public static synchronized ScmHandler getHandlerByType(String type) {
    List<ScmHandler> handlers = getScms().get(type);
    if(handlers == null) {
      return null;
    }
    return handlers.get(0);
  }

  private static Map<String, List<ScmHandler>> getScms() {
    if(scms == null) {
      scms = new TreeMap<String, List<ScmHandler>>();
      for(ScmHandler scmHandler : readScmHanderExtensions()) {
        addScmHandler(scmHandler);
      }
    }
    return scms;
  }

  private static Map<String, ScmHandlerUi> getScmUis() {
    if(scmUis == null) {
      scmUis = new TreeMap<String, ScmHandlerUi>();
      List<ScmHandlerUi> scmHandlerUis = readScmHandlerUiExtensions();
      for(ScmHandlerUi scmHandlerUi : scmHandlerUis) {
        addScmHandlerUi(scmHandlerUi);
      }
    }
    return scmUis;
  }

  private static List<ScmHandler> readScmHanderExtensions() {
    List<ScmHandler> scmHandlers = new ArrayList<ScmHandler>();
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint scmHandlersExtensionPoint = registry.getExtensionPoint(EXTENSION_SCM_HANDLERS);
    if(scmHandlersExtensionPoint != null) {
      IExtension[] scmHandlersExtensions = scmHandlersExtensionPoint.getExtensions();
      for(IExtension extension : scmHandlersExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_SCM_HANDLER)) {
            try {
              scmHandlers.add((ScmHandler) element.createExecutableExtension(ScmHandler.ATTR_CLASS));
            } catch(CoreException ex) {
              MavenLogger.log(ex);
            }
          }
        }
      }
    }
    return scmHandlers;
  }
  
  private static List<ScmHandlerUi> readScmHandlerUiExtensions() {
    ArrayList<ScmHandlerUi> scmHandlerUis = new ArrayList<ScmHandlerUi>();
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint scmHandlersUiExtensionPoint = registry.getExtensionPoint(EXTENSION_SCM_HANDLERS_UI);
    if(scmHandlersUiExtensionPoint != null) {
      IExtension[] scmHandlersUiExtensions = scmHandlersUiExtensionPoint.getExtensions();
      for(IExtension extension : scmHandlersUiExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_SCM_HANDLER_UI)) {
            try {
              scmHandlerUis.add((ScmHandlerUi) element.createExecutableExtension(ScmHandlerUi.ATTR_CLASS));
            } catch(CoreException ex) {
              MavenLogger.log(ex);
            }
          }
        }
      }
    }
    return scmHandlerUis;
  }

}
