/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc. and others.
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

package org.eclipse.m2e.core.internal.project.registry;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;


/**
 * Workspace state reader
 *
 * @author Eugene Kuleshov
 */
@Component(service = {ProjectRegistryReader.class})
public class ProjectRegistryReader {
  private static final Logger log = LoggerFactory.getLogger(ProjectRegistryReader.class);

  private static final String WORKSPACE_STATE = "workspaceState.ser"; //$NON-NLS-1$

  private File stateFile;

  @Activate
  void init(BundleContext bundleContext) {
    IPath result = Platform.getStateLocation(bundleContext.getBundle());
    File bundleStateLocation = result.toFile();
    setStateLocation(bundleStateLocation);
  }

  public void setStateLocation(File bundleStateLocation) {
    this.stateFile = new File(bundleStateLocation, WORKSPACE_STATE);
  }

  public ProjectRegistry readWorkspaceState(final ProjectRegistryManager managerImpl) {
    if(stateFile.exists()) {
      try (ObjectInputStream is = createObjectInputStream(managerImpl)) {
        return (ProjectRegistry) is.readObject();
      } catch(Exception ex) {
        log.error("Can't read workspace state", ex);
      }
    }
    return null;
  }

  private ObjectInputStream createObjectInputStream(ProjectRegistryManager managerImpl) throws IOException {
    return new ObjectInputStream(new BufferedInputStream(new FileInputStream(stateFile))) {
      {
        enableResolveObject(true);
      }

      @Override
      protected Object resolveObject(Object o) throws IOException {
        if(o instanceof IPathReplace pathReplace) {
          return pathReplace.getPath();
        } else if(o instanceof IFileReplace fileReplace) {
          return fileReplace.getFile();
        } else if(o instanceof MavenProjectManagerImplReplace) {
          return managerImpl;
        }
        return super.resolveObject(o);
      }

      @Override
      protected Class<?> resolveClass(java.io.ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String symbolicName = (String) readObject();
        if(symbolicName == null) {
          return super.resolveClass(desc);
        }
        String versionStr = (String) readObject();
        Version version = Version.parseVersion(versionStr);
        VersionRange range = new VersionRange(VersionRange.LEFT_CLOSED, version, version, VersionRange.RIGHT_CLOSED);
        Bundle[] bundles = Platform.getBundles(symbolicName, range.toString());
        if(bundles == null || bundles.length != 1) {
          throw new ClassNotFoundException("Could not find bundle " + symbolicName + "/" + version //$NON-NLS-1$ //$NON-NLS-2$
              + " required to load class " + desc.getName()); //$NON-NLS-1$
        }
        return bundles[0].loadClass(desc.getName());
      }
    };
  }

  public void writeWorkspaceState(ProjectRegistry state) {
    try (ObjectOutputStream os = createObjectOutputStream()) {
      synchronized(state) { // see MNGECLIPSE-860
        os.writeObject(state);
      }
    } catch(Exception ex) {
      log.error("Can't write workspace state", ex);
    }
  }

  private ObjectOutputStream createObjectOutputStream() throws IOException {
    ClassLoader thisClassloader = getClass().getClassLoader();
    return new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(stateFile))) {
      {
        enableReplaceObject(true);
      }

      @Override
      protected Object replaceObject(Object o) throws IOException {
        if(o instanceof IPath path) {
          return new IPathReplace(path);
        } else if(o instanceof IFile file) {
          return new IFileReplace(file);
        } else if(o instanceof ProjectRegistryManager) {
          return new MavenProjectManagerImplReplace();
        }
        return super.replaceObject(o);
      }

      @Override
      protected void annotateClass(Class<?> cl) throws IOException {
        // if the class is visible through this classloader, assume it will be during reading stream back
        try {
          Class<?> target = cl;
          while(target.isArray()) {
            target = target.getComponentType();
          }

          if(target.isPrimitive() || target.equals(thisClassloader.loadClass(target.getName()))) {
            writeObject(null); // TODO is there a better way?
            return;
          }
        } catch(ClassNotFoundException ex) {
          // fall through
        }

        // foreign class
        Bundle bundle = FrameworkUtil.getBundle(cl);
        if(bundle != null) {
          writeObject(bundle.getSymbolicName());
          writeObject(bundle.getVersion().toString());
        }

        // TODO this will likely fail during desirialization
      }
    };
  }

  /**
   * IPath replacement used for object serialization
   */
  private static final class IPathReplace implements Serializable {
    private static final long serialVersionUID = -2361259525684491181L;

    private final String path;

    public IPathReplace(IPath path) {
      this.path = path.toPortableString();
    }

    public IPath getPath() {
      return IPath.fromPortableString(path);
    }
  }

  /**
   * IFile replacement used for object serialization
   */
  private static final class IFileReplace implements Serializable {
    private static final long serialVersionUID = -7266001068347075329L;

    private final String path;

    public IFileReplace(IFile file) {
      this.path = file.getFullPath().toPortableString();
    }

    public IFile getFile() {
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      return root.getFile(IPath.fromPortableString(path));
    }
  }

  static final class MavenProjectManagerImplReplace implements Serializable {
    private static final long serialVersionUID = 1995671440438776471L;
  }

}
