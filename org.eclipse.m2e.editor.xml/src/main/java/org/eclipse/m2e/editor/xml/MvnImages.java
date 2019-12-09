/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Anton Tanasenko - Refactor marker resolutions and quick fixes (Bug #484359)
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.swt.graphics.Image;


/**
 * @author Eugene Kuleshov
 */
public class MvnImages {
  private static final Logger log = LoggerFactory.getLogger(MvnImages.class);

  // object images

  public static final Image IMG_JAR = createImage("jar_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_JARS = createImage("jars_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_REPOSITORY = createImage("repository_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_PLUGIN = createImage("plugin_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_PLUGINS = createImage("plugins_obj.gif"); //$NON-NLS-1$

  public static final ImageDescriptor IMGD_DISCOVERY = create("insp_sbook.gif"); //$NON-NLS-1$

  public static final ImageDescriptor IMGD_EXECUTION = create("execution_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_DISCOVERY = createImage("insp_sbook.gif"); //$NON-NLS-1$

  public static final Image IMG_EXECUTION = createImage("execution_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_GOAL = createImage("goal_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_FILTER = createImage("filter_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_RESOURCE = createImage("resource_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_RESOURCES = createImage("resources_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_INCLUDE = createImage("include_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_EXCLUDE = createImage("exclude_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_PERSON = createImage("person_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_ROLE = createImage("role_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_PROPERTY = createImage("property_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_PROPERTIES = createImage("properties_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_REPORT = createImage("report_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_PROFILE = createImage("profile_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_PROFILES = createImage("profiles_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_PARAMETER = createImage("parameter_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_LICENSE = createImage("license.png"); //$NON-NLS-1$

  public static final Image IMG_BUILD = createImage("build_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_ELEMENT = createImage("element_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_USER_TEMPLATE = createImage("template_obj.gif"); //$NON-NLS-1$

  public static final Image IMG_OPEN_POM = createImage("open_pom.gif"); //$NON-NLS-1$

  public static final Image IMG_CLOSE = createImage("close.gif"); //$NON-NLS-1$

  public static final ImageDescriptor IMGD_WARNINGS = create("warnings.png"); //$NON-NLS-1$

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

  private static Image createImage(String key) {
    create(key);
    ImageRegistry imageRegistry = getImageRegistry();
    if(imageRegistry == null)
      return null;
    Image img = imageRegistry.get(key);
    if(img == null) {
      create(key);
    }
    return imageRegistry.get(key);
  }

  private static ImageRegistry getImageRegistry() {
    MvnIndexPlugin plugin = MvnIndexPlugin.getDefault();
    return plugin == null ? null : plugin.getImageRegistry();
  }

  private static ImageDescriptor createDescriptor(String image) {
    return ResourceLocator.imageDescriptorFromBundle(MvnIndexPlugin.PLUGIN_ID, "icons/" + image).get(); //$NON-NLS-1$
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
}
