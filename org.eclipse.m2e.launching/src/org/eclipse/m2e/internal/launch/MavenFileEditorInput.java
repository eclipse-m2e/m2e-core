/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.launch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.editors.text.ILocationProvider;

import org.eclipse.m2e.core.internal.IMavenConstants;


/**
 * Maven file editor input
 * 
 * @author Eugene Kuleshov
 */
public class MavenFileEditorInput implements IStorageEditorInput {
  final String fileName;

  public MavenFileEditorInput(String fileName) {
    this.fileName = fileName;
  }

  public boolean exists() {
    return true;
  }

  public ImageDescriptor getImageDescriptor() {
    return null;
  }

  public String getName() {
    return new File(this.fileName).getName();
  }

  public String getToolTipText() {
    return this.fileName;
  }

  public IStorage getStorage() {
    return new IStorage() {

      public InputStream getContents() throws CoreException {
        try {
          return new FileInputStream(fileName);
        } catch(FileNotFoundException ex) {
          throw new CoreException(new Status(IStatus.ERROR, //
              IMavenConstants.PLUGIN_ID, -1, NLS.bind(Messages.MavenFileEditorInput_0, fileName), ex));
        }
      }

      public IPath getFullPath() {
        return Path.fromOSString(fileName);
      }

      public String getName() {
        return fileName;
      }

      public boolean isReadOnly() {
        return false;
      }

      public <T> T getAdapter(Class<T> adapter) {
        return null;
      }
    };
  }

  public IPersistableElement getPersistable() {
    return null;
//    return new IPersistableElement() {
//
//      public String getFactoryId() {
//        return MavenEditorInputFactory.ID;
//      }
//
//      public void saveState(IMemento memento) {
//        memento.putString("fileName", fileName);
//      }
//    };
  }

  public <T> T getAdapter(Class<T> adapter) {
    if(adapter == ILocationProvider.class) {
      return adapter.cast((ILocationProvider) element -> Path.fromOSString(fileName));
    }
    return null;
  }
}
