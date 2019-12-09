/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.internal.markers;

import org.eclipse.core.resources.IMarker;


/**
 * IMarkerLocationService
 * 
 * @author mkleint
 */
public interface IMarkerLocationService {

  /**
   * sets the offset attribute on the marker if the marker is recognized and offset found
   * 
   * @param marker
   */
  void findLocationForMarker(IMarker marker);

}
