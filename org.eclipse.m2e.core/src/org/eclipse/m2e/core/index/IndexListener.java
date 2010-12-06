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

package org.eclipse.m2e.core.index;

import org.eclipse.m2e.core.repository.IRepository;



/**
 * IndexListener
 *
 * @author Eugene Kuleshov
 */
public interface IndexListener {
  
  public void indexAdded(IRepository repository);

  public void indexRemoved(IRepository repository);
  
  public void indexChanged(IRepository repository);
  
  public void indexUpdating(IRepository repository);

}
