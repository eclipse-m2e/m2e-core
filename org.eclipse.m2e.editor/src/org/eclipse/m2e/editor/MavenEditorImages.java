/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype, Inc.
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

package org.eclipse.m2e.editor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.swt.graphics.Image;


/**
 * @author Eugene Kuleshov
 */
public class MavenEditorImages {
  private static final ILog log = Platform.getLog(MavenEditorImages.class);

  // image descriptors

  public static final ImageDescriptor REFRESH = create("refresh.gif"); //$NON-NLS-1$

  public static final ImageDescriptor COLLAPSE_ALL = create("collapseall.gif"); //$NON-NLS-1$

  public static final ImageDescriptor EXPAND_ALL = create("expandall.gif"); //$NON-NLS-1$

  public static final ImageDescriptor SHOW_GROUP = create("show_group.gif"); //$NON-NLS-1$

  public static final ImageDescriptor SHOW_INHERITED_DEPENDENCIES = create("show_inherited_dependencies.gif"); //$NON-NLS-1$

  public static final ImageDescriptor ADD_MODULE = create("new_project.gif"); //$NON-NLS-1$

  public static final ImageDescriptor ADD_ARTIFACT = create("new_jar.gif"); //$NON-NLS-1$

  public static final ImageDescriptor SELECT_ARTIFACT = create("select_jar.gif"); //$NON-NLS-1$

  public static final ImageDescriptor ADD_PLUGIN = create("new_plugin.gif"); //$NON-NLS-1$

  public static final ImageDescriptor SELECT_PLUGIN = create("select_plugin.gif"); //$NON-NLS-1$

  public static final ImageDescriptor SORT = create("sort.gif"); //$NON-NLS-1$

  public static final ImageDescriptor FILTER = create("filter.gif"); //$NON-NLS-1$

  public static final ImageDescriptor EFFECTIVE_POM = create("effective_pom.gif"); //$NON-NLS-1$

  public static final ImageDescriptor PARENT_POM = create("parent_pom.gif"); //$NON-NLS-1$

  public static final ImageDescriptor WEB_PAGE = create("web.gif"); //$NON-NLS-1$

  public static final ImageDescriptor HIERARCHY = create("hierarchy.gif"); //$NON-NLS-1$

  public static final ImageDescriptor SCOPE = create("scope.gif"); //$NON-NLS-1$

  public static final ImageDescriptor ADVANCED_TABS = create("advanced_tabs.gif"); //$NON-NLS-1$

  public static final ImageDescriptor ELEMENT_OBJECT = create("element_obj.gif"); //$NON-NLS-1$

  public static final ImageDescriptor IMGD_WARNINGS = create("warnings.png"); //$NON-NLS-1$

  public static final ImageDescriptor IMGD_EXECUTION = create("execution_obj.gif"); //$NON-NLS-1$

  public static final ImageDescriptor IMGD_DISCOVERY = create("insp_sbook.gif"); //$NON-NLS-1$

  // images

  public static final Image IMG_CLEAR = createImage("clear.gif"); //$NON-NLS-1$

  public static final Image IMG_CLEAR_DISABLED = createImage("clear_disabled.gif"); //$NON-NLS-1$

  public static final Image IMG_PROJECT = createImage("project_obj.gif"); //$NON-NLS-1$

  // object images

  public static final Image IMG_JAR = createImage("jar_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_INHERITED = createImage("inherited_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_REPOSITORY = createImage("repository_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_PLUGIN = createImage("plugin_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_EXECUTION = getImageRegistry().get("execution_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_GOAL = createImage("goal_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_FILTER = createImage("filter_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_RESOURCE = createImage("resource_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_INCLUDE = createImage("include_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_EXCLUDE = createImage("exclude_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_PERSON = createImage("person_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_ROLE = createImage("role_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_PROPERTY = createImage("property_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_REPORT = createImage("report_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_PROFILE = createImage("profile_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_SCOPE = createImage("scope_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_PARAMETER = createImage("parameter_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_DISCOVERY = getImageRegistry().get("insp_sbook.gif"); //$NON-NLS-1$

  public static final Image IMG_CLOSE = createImage("close.gif"); //$NON-NLS-1$

  private static ImageDescriptor create(String key) {
    try {
      ImageDescriptor imageDescriptor = createDescriptor(key);
      ImageRegistry imageRegistry = getImageRegistry();
      if(imageRegistry != null) {
        imageRegistry.put(key, imageDescriptor);
      }
      return imageDescriptor;
    } catch(Exception ex) {
      log.error(key, ex);
      return null;
    }
  }

  public static Image getImage(ImageDescriptor imageDescriptor) {
    Image image = Custom.images.get(imageDescriptor);
    if(image == null) {
      synchronized(Custom.images) {
        image = Custom.images.get(imageDescriptor);
        if(image == null) {
          image = imageDescriptor.createImage();
          if(image != null) {
            Custom.images.put(imageDescriptor, image);
          }
        }
      }
    }
    return image;
  }

  static class Custom {
    static final Map<ImageDescriptor, Image> images = new ConcurrentHashMap<>();

    static void dispose() {
      for(Image img : images.values()) {
        img.dispose();
      }
      images.clear();
    }

  }

  private static Image createImage(String key) {
    create(key);
    ImageRegistry imageRegistry = getImageRegistry();
    return imageRegistry == null ? null : imageRegistry.get(key);
  }

  private static ImageRegistry getImageRegistry() {
    MavenEditorPlugin plugin = MavenEditorPlugin.getDefault();
    return plugin == null ? null : plugin.getImageRegistry();
  }

  private static ImageDescriptor createDescriptor(String image) {
    return ResourceLocator.imageDescriptorFromBundle(MavenEditorPlugin.PLUGIN_ID, "icons/" + image).get(); //$NON-NLS-1$
  }

}
