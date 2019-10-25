/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.refactoring.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.ResourceLocator;


/**
 * @author Eugene Kuleshov
 */
public class RefactoringImages {
  private static final Logger log = LoggerFactory.getLogger(RefactoringImages.class);

  // images

  // public static final Image IMG_CLEAR = createImage("clear.gif");

  // image descriptors

  public static final ImageDescriptor EXCLUDE = create("exclude.gif"); //$NON-NLS-1$

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

//  private static Image createImage(String key) {
//    create(key);
//    ImageRegistry imageRegistry = getImageRegistry();
//    return imageRegistry==null ? null : imageRegistry.get(key);
//  }

  private static ImageRegistry getImageRegistry() {
    Activator plugin = Activator.getDefault();
    return plugin == null ? null : plugin.getImageRegistry();
  }

  private static ImageDescriptor createDescriptor(String image) {
    return ResourceLocator.imageDescriptorFromBundle(Activator.PLUGIN_ID, "icons/" + image).get(); //$NON-NLS-1$
  }

}
