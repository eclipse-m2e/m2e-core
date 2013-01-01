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

package org.eclipse.m2e.core.internal.index.nexus;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.internal.adaptor.BasicLocation;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.index.fs.Lock;
import org.apache.maven.index.fs.Locker;


@SuppressWarnings("restriction")
public class EquinoxLocker implements Locker {

  public Lock lock(File directory) throws IOException {
    org.eclipse.core.runtime.internal.adaptor.Locker lock = BasicLocation.createLocker(new File(directory, LOCK_FILE),
        null);

    if(lock.lock()) {
      return new EquinoxLock(lock);
    }

    throw new IOException(NLS.bind("Could not acquire lock on directory {0}", directory));
  }

}
