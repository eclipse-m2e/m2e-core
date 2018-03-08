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
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.service.resolver.VersionRange;

import org.codehaus.plexus.util.IOUtil;

import org.eclipse.m2e.core.internal.MavenPluginActivator;


/**
 * Workspace state reader
 * 
 * @author Eugene Kuleshov
 */
public class ProjectRegistryReader {
  private static final Logger log = LoggerFactory.getLogger(ProjectRegistryReader.class);

  private static final String WORKSPACE_STATE = "workspaceState.ser"; //$NON-NLS-1$

  private final File stateFile;

  private static PackageAdmin packageAdmin;

  public ProjectRegistryReader(File stateLocationDir) {
    this.stateFile = new File(stateLocationDir, WORKSPACE_STATE);
  }

  public ProjectRegistry readWorkspaceState(final ProjectRegistryManager managerImpl) {
    if(stateFile.exists()) {
      final PackageAdmin packageAdmin = getPackageAdmin();
      ObjectInputStream is = null;
      try {
        is = new ObjectInputStream(new BufferedInputStream(new FileInputStream(stateFile))) {
          {
            enableResolveObject(true);
          }

          protected Object resolveObject(Object o) throws IOException {
            if(o instanceof IPathReplace) {
              return ((IPathReplace) o).getPath();
            } else if(o instanceof IFileReplace) {
              return ((IFileReplace) o).getFile();
            } else if(o instanceof MavenProjectManagerImplReplace) {
              return managerImpl;
            }
            return super.resolveObject(o);
          }

          protected java.lang.Class<?> resolveClass(java.io.ObjectStreamClass desc) throws IOException,
              ClassNotFoundException {
            String symbolicName = (String) readObject();
            if(symbolicName == null) {
              return super.resolveClass(desc);
            }
            String versionStr = (String) readObject();
            Version version = Version.parseVersion(versionStr);
            VersionRange versionRange = new VersionRange(version, true, version, true);
            Bundle[] bundles = packageAdmin.getBundles(symbolicName, versionRange.toString());
            if(bundles == null || bundles.length != 1) {
              throw new ClassNotFoundException("Could not find bundle " + symbolicName + "/" + version //$NON-NLS-1$ //$NON-NLS-2$
                  + " required to load class " + desc.getName()); //$NON-NLS-1$
            }
            return bundles[0].loadClass(desc.getName());
          };
        };
        return (ProjectRegistry) is.readObject();
      } catch(Exception ex) {
        log.error("Can't read workspace state", ex);
      } finally {
        IOUtil.close(is);
      }
    }
    return null;
  }

  private static synchronized PackageAdmin getPackageAdmin() {
    // TODO inject dependencies already!
    if(packageAdmin == null) {
      BundleContext context = MavenPluginActivator.getDefault().getBundleContext();
      ServiceReference<PackageAdmin> serviceReference = context.getServiceReference(PackageAdmin.class);
      packageAdmin = context.getService(serviceReference);
    }
    return packageAdmin;
  }

  public void writeWorkspaceState(ProjectRegistry state) {
    final ClassLoader thisClassloader = getClass().getClassLoader();

    final PackageAdmin packageAdmin = getPackageAdmin();

    ObjectOutputStream os = null;
    try {
      os = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(stateFile))) {
        {
          enableReplaceObject(true);
        }

        protected Object replaceObject(Object o) throws IOException {
          if(o instanceof IPath) {
            return new IPathReplace((IPath) o);
          } else if(o instanceof IFile) {
            return new IFileReplace((IFile) o);
          } else if(o instanceof ProjectRegistryManager) {
            return new MavenProjectManagerImplReplace();
          }
          return super.replaceObject(o);
        }

        protected void annotateClass(java.lang.Class<?> cl) throws IOException {
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
          Bundle bundle = packageAdmin.getBundle(cl);
          if(bundle != null) {
            writeObject(bundle.getSymbolicName());
            writeObject(bundle.getVersion().toString());
          }

          // TODO this will likely fail during desirialization
        };
      };
      synchronized(state) { // see MNGECLIPSE-860
        os.writeObject(state);
      }
    } catch(Exception ex) {
      log.error("Can't write workspace state", ex);
    } finally {
      IOUtil.close(os);
    }
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
      return Path.fromPortableString(path);
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
      return root.getFile(Path.fromPortableString(path));
    }
  }

  static final class MavenProjectManagerImplReplace implements Serializable {
    private static final long serialVersionUID = 1995671440438776471L;
  }

}
