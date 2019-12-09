/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
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

package org.eclipse.m2e.internal.discovery;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;


public class MavenDiscoveryIcons {

  private static final URL baseURL = Platform.getBundle(DiscoveryActivator.PLUGIN_ID).getEntry("/icons/"); //$NON-NLS-1$

  private static ImageRegistry imageRegistry;

  public static final ImageDescriptor WIZARD_BANNER = create("banner.gif"); //$NON-NLS-1$

  public static final ImageDescriptor QUICK_FIX_ICON = create("insp_sbook.gif"); //$NON-NLS-1$

  private static ImageDescriptor create(String string) {
    try {
      return ImageDescriptor.createFromURL(new URL(baseURL, string));
    } catch(MalformedURLException e) {
      return ImageDescriptor.getMissingImageDescriptor();
    }
  }

  public static Image getImage(ImageDescriptor descriptor) {
    ImageRegistry imageRegistry = getImageRegistry();
    Image image = imageRegistry.get(String.valueOf(descriptor.hashCode()));
    if(image == null) {
      image = descriptor.createImage(true);
      imageRegistry.put(String.valueOf(descriptor.hashCode()), image);
    }
    return image;
  }

  private static ImageRegistry getImageRegistry() {
    if(imageRegistry == null) {
      imageRegistry = new ImageRegistry();
    }
    return imageRegistry;
  }
}
