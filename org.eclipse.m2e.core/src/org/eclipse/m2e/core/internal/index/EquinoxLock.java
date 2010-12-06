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

package org.eclipse.m2e.core.internal.index;

import org.eclipse.core.runtime.internal.adaptor.Locker;

import org.apache.maven.index.fs.Lock;

@SuppressWarnings("restriction")
public class EquinoxLock
    implements Lock
{

    private final Locker lock;

    public EquinoxLock( Locker lock )
    {
        this.lock = lock;
    }

    public void release()
    {
        lock.release();
    }

}
