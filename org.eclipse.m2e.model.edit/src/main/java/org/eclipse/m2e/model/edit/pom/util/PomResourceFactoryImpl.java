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

/**
 * <copyright>
 * </copyright>
 *
 * $Id: PomResourceFactoryImpl.java 20719 2009-02-02 05:52:56Z mpoindexter $
 */
package org.eclipse.m2e.model.edit.pom.util;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryImpl;

/**
 * <!-- begin-user-doc --> The <b>Resource Factory</b> associated with the
 * package. <!-- end-user-doc -->
 * 
 * @see org.eclipse.m2e.model.edit.pom.util.PomResourceImpl
 */
public class PomResourceFactoryImpl extends ResourceFactoryImpl {

	/**
	 * Creates an instance of the resource factory. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 */
	public PomResourceFactoryImpl() {

	}

	@Override
	public Resource createResource(URI uri) {
		return new PomResourceImpl(uri);
	}

} // PomResourceFactoryImpl
