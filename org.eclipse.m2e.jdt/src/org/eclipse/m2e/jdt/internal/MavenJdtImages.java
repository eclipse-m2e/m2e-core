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

package org.eclipse.m2e.jdt.internal;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.jdt.MavenJdtPlugin;


/**
 * @author Eugene Kuleshov
 */
public class MavenJdtImages {

  // object images

  // public static final Image IMG_CLEAR = createImage("clear.gif");
  
  // descriptors
  
  public static final ImageDescriptor JAVA_DOC = create("javadoc.gif"); //$NON-NLS-1$
  
  
  private static ImageDescriptor create(String key) {
    try {
      ImageRegistry imageRegistry = getImageRegistry();
      if(imageRegistry != null) {
        ImageDescriptor imageDescriptor = imageRegistry.getDescriptor(key);
        if(imageDescriptor==null) {
          imageDescriptor = createDescriptor(key);
          imageRegistry.put(key, imageDescriptor);
        }
        return imageDescriptor;
      }
    } catch(Exception ex) {
      MavenLogger.log(key, ex);
    }
    return null;
  }

//  private static Image createImage(String key) {
//    create(key);
//    ImageRegistry imageRegistry = getImageRegistry();
//    return imageRegistry == null ? null : imageRegistry.get(key);
//  }

  private static ImageRegistry getImageRegistry() {
    MavenJdtPlugin plugin = MavenJdtPlugin.getDefault();
    return plugin == null ? null : plugin.getImageRegistry();
  }

  private static ImageDescriptor createDescriptor(String image) {
    return AbstractUIPlugin.imageDescriptorFromPlugin(IMavenConstants.PLUGIN_ID, "icons/" + image); //$NON-NLS-1$
  }

}
