/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.e44;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.maven.index.fs.Lock;
import org.apache.maven.index.fs.Locker;


/**
 * This class provides reflection-based adaptor for Equinox internal file Locker implementation and is meant to
 * compensate for changes between Equinox 3.9 and 3.10.
 * 
 * @see http://dev.eclipse.org/mhonarc/lists/cross-project-issues-dev/msg09424.html
 * @since 1.5
 */
public class EquinoxLocker implements Locker {

  private static final String E43_LOCATION_HELPER = "org.eclipse.core.runtime.internal.adaptor.BasicLocation";

  private static final String E44_LOCATION_HELPER = "org.eclipse.osgi.internal.location.LocationHelper";

  private static class EquinoxLock implements Lock {

    private Object locker;

    public EquinoxLock(Object locker) {
      this.locker = locker;
    }

    public void release() {
      try {
        Method releaseMethod = locker.getClass().getMethod("release", (Class<?>[]) null);
        releaseMethod.invoke(locker, (Object[]) null);
      } catch(Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public Lock lock(File directory) throws IOException {
    File lockFile = new File(directory, LOCK_FILE);

    ClassLoader cl = EquinoxLocker.class.getClassLoader();

    try {
      Object locker;
      try {
        Class<?> locationHelper = cl.loadClass(E44_LOCATION_HELPER);
        Method createLockerMethod = locationHelper.getMethod("createLocker", File.class, String.class, boolean.class);

        locker = createLockerMethod.invoke(null, lockFile, null /*lockMode*/, false /*debug*/);
      } catch(ClassNotFoundException ex) {
        Class<?> locationHelper = cl.loadClass(E43_LOCATION_HELPER);
        Method createLockerMethod = locationHelper.getMethod("createLocker", File.class, String.class);
        locker = createLockerMethod.invoke(null, lockFile, null /*lockMode*/);
      }

      Method lockMethod = locker.getClass().getMethod("lock", (Class<?>[]) null);
      lockMethod.invoke(locker, (Object[]) null);

      return new EquinoxLock(locker);
    } catch(InvocationTargetException e) {
      if(e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      }
      throw new RuntimeException(e);
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }
}
