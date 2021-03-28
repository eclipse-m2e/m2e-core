/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.model.edit.pom.util;

import java.io.IOException;
import java.util.Map;

import org.eclipse.emf.common.util.URI;

import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.translators.SSESyncResource;


/**
 * <!-- begin-user-doc --> The <b>Resource </b> associated with the package. <!-- end-user-doc -->
 *
 * @see org.eclipse.m2e.model.edit.pom.util.PomResourceFactoryImpl
 * @generated NOT
 */
public class PomResourceImpl extends SSESyncResource {
  /**
   * Creates an instance of the resource. <!-- begin-user-doc --> <!-- end-user-doc -->
   *
   * @param uri the URI of the new resource.
   * @generated
   */
  public PomResourceImpl(URI uri) {
    super(uri);
  }

  public void load(Map<?, ?> options) throws IOException {
    super.load(options);
  }

  public Model getModel() {
    return (Model) getContents().get(0);
  }

} // PomResourceImpl
