/*******************************************************************************
 * Copyright (c) 2008-2022 Sonatype, Inc.
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

import java.io.Serializable;
import java.util.Objects;


/**
 * VersionlessKey
 *
 * @author igor
 */
record VersionlessKey(String namespace, String id) implements Serializable {
  VersionlessKey {
    Objects.requireNonNull(namespace);
    Objects.requireNonNull(id);
  }

  @Override
  public String toString() {
    return namespace + "/" + id; //$NON-NLS-1$
  }
}
