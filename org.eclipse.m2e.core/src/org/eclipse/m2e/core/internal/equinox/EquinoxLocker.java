/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.equinox;

import java.io.File;

import org.eclipse.osgi.internal.location.LocationHelper;
import org.eclipse.osgi.internal.location.Locker;


/**
 * Adaptor for Equinox internal file Locker implementation
 *
 * @since 1.5
 */
@SuppressWarnings("restriction")
public class EquinoxLocker implements org.apache.maven.index.fs.Locker {

  private static class EquinoxLock implements org.apache.maven.index.fs.Lock {

    private final Locker locker;

    public EquinoxLock(Locker locker) {
      this.locker = locker;
    }

    @Override
    public void release() {
      locker.release();
    }
  }

  @Override
  public org.apache.maven.index.fs.Lock lock(File directory) {
    File lock = new File(directory, LOCK_FILE);
    return new EquinoxLock(LocationHelper.createLocker(lock, null /*lockMode*/, false /*debug*/));
  }
}
